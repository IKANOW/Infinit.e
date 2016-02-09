# encoding: utf-8
require "logstash/namespace"
require "logstash/outputs/base"
require "zlib"
require "stud/buffer"


# This output will write events to files on disk. You can use fields
# from the event as parts of the filename and/or path.
class LogStash::Outputs::File < LogStash::Outputs::Base
  include Stud::Buffer

  config_name "rotating_file"
  milestone 2

  # The path to the file to write. Event fields can be used here,
  # like "/var/log/logstash/%{host}/%{application}"
  # One may also utilize the path option for date-based log
  # rotation via the joda time format. This will use the event
  # timestamp.
  # E.g.: path => "./test-%{+YYYY-MM-dd}.txt" to create
  # ./test-2013-05-29.txt
  config :path, :validate => :string, :required => true

  # Specify a directory path where files are moved when they segment
  config :final_path, :validate => :string

  # The maximum size of file to write. When the file exceeds this
  # threshold, it will be moved to the final_path. If the filename exists here
  # a random string will be appended
  #
  config :max_size, :validate => :number, :default => 67108864

  # The format to use when writing events to the file. This value
  # supports any string and can include %{name} and other dynamic
  # strings.
  #
  # If this setting is omitted, the full json representation of the
  # event will be written as a single line.
  config :message_format, :validate => :string

  # Flush interval (in seconds) for flushing writes to log files.
  # 0 will flush on every message.
  config :flush_interval, :validate => :number, :default => 2

  # Interval after which stale files are closed and rotated (default 10 minutes)
  config :segment_period, :validate => :number, :default => 600

  # Gzip the output stream before writing to disk.
  config :gzip, :validate => :boolean, :default => false

  @ls_mutex = Mutex.new

  public
  def register
    require "fileutils" # For mkdir_p
    require "thread"

    @ls_mutex = Mutex.new

    workers_not_supported

    @files = {}
    now = Time.now
    @last_flush_cycle = now
    @last_stale_cleanup_cycle = now
    flush_interval = @flush_interval.to_i
    max_size = @max_size.to_i
    @stale_cleanup_interval = 10
    segment_period = @segment_period.to_i
    @last_write_log = now - 1000
    
    # Create a buffer that will generate "ping"s for us
    buffer_initialize(
      :max_interval => @stale_cleanup_interval
    )
    # Ensure that always have an object
    buffer_receive([])
    
  end # def register

  public
  def receive(event)
  begin
  @ls_mutex.synchronize {
    if not output?(event)
       # (Don't need this any more since using buffer)
       #close_stale_files
       return
    end
    
    #(Always update stale cycle to minimize buffer vs event collision - if this vaguely works probably need to put a mutex in...)
    now = Time.now
    @last_stale_cleanup_cycle = now

    path = event.sprintf(@path)
	if @final_path
	   final_path = event.sprintf(@final_path)
	end
    
    fd = open(path, final_path)

    if @message_format
      output = event.sprintf(@message_format)
    else
      output = event.to_json
    end

    fd.write(output)
    fd.write("\n")

	fd.segment_size += output.size + 1;
	if fd.segment_size > max_size	
	  #Flush and close file then move
	  fd.close
	  if fd.final_path
	     move(path, fd.final_path, 0)
	  end
      @files.delete(path)
	else
       internal_flush(fd)
    end
    close_stale_files
  }
  rescue Exception => e
    return unless Time.now - @last_write_log >= 600
    @last_write_log = Time.now  
    @logger.error("Exception while writing: %s" % e.backtrace.join(":"), :exception => e)
  end
  end # def receive

  def teardown
    @logger.debug("Teardown: closing files")
    buffer_clear_pending
    @files.each do |path, fd|
      begin
        fd.close
        if fd.final_path
           move(path, fd.final_path, 0)
        end
        @logger.debug("Closed file #{path}", :fd => fd)
      rescue Exception => e
        @logger.error("Exception while flushing and closing files.", :exception => e)
      end
    end
    finished
  end

  # (called from stud/buffer)
  def flush(events, teardown=false)
    begin
    @ls_mutex.synchronize {
       close_stale_files
  
       # Ensure that always have an object
       buffer_receive([])
    }
    rescue Exception => e
      @logger.error("Exception while calling buffer flush: %s" % e.backtrace.join(":"), :exception => e)
    end     
  end  

  private
  def internal_flush(fd)
    if flush_interval > 0
      flush_pending_files
    else
      fd.flush
    end
  end

  # every flush_interval seconds or so (triggered by events, but if there are no events there's no point flushing files anyway)
  def flush_pending_files
    return unless Time.now - @last_flush_cycle >= flush_interval
    @logger.debug("Starting flush cycle")
    @files.each do |path, fd|
      @logger.debug("Flushing file", :path => path, :fd => fd)
      fd.flush
    end
    @last_flush_cycle = Time.now
  end

  # every 10 seconds or so (triggered by events, but if there are no events there's no point closing files anyway)
  def close_stale_files
    now = Time.now
    return unless now - @last_stale_cleanup_cycle >= @stale_cleanup_interval
    @logger.info("Starting stale files cleanup cycle", :files => @files)
    
    if @segment_period <= 0    
       inactive_files = @files.select { |path, fd| not fd.active }
       @logger.debug("%d stale files found" % inactive_files.count, :inactive_files => inactive_files)
       inactive_files.each do |path, fd|
         @logger.info("Closing file %s" % path)
         fd.close
         @files.delete(path)
       end
       
       # mark all files as inactive, a call to write will mark them as active again
       @files.each { |path, fd| fd.active = false }
       @last_stale_cleanup_cycle = now
       
    else
       # Handle file segmentation
       segmented_files = @files.select { |path, fd| now.to_i - fd.segment_time > @segment_period }
       @logger.debug("%d segmenting files found" % segmented_files.count, :segmented_files => segmented_files)
       segmented_files.each do |path, fd|
         @logger.info("Segmenting file %s" % path)
         fd.close
         if fd.final_path
            move(path, fd.final_path, 0)
         end
         @files.delete(path)
       end
    end     
  end

  def move(from_path, to_path, count)
    stat = File.stat(to_path) rescue nil
    if stat
       move(from_path, to_path + "." + Time.now.to_i.to_s + "." + (count+1).to_s, count + 1)
    else
       dir = File.dirname(to_path)
       if !Dir.exists?(dir)
         @logger.info("Creating directory", :directory => dir)
         FileUtils.mkdir_p(dir)
       end
       File.rename(from_path, to_path)
     end
  end

  def open(path, final_path)
    return @files[path] if @files.include?(path) and not @files[path].nil?

    @logger.info("Opening file", :path => path)

    dir = File.dirname(path)
    if !Dir.exists?(dir)
      @logger.info("Creating directory", :directory => dir)
      FileUtils.mkdir_p(dir)
    end

    # work around a bug opening fifos (bug JRUBY-6280)
    stat = File.stat(path) rescue nil
    if stat and stat.ftype == "fifo" and RUBY_PLATFORM == "java"
      fd = java.io.FileWriter.new(java.io.File.new(path))
    else
      fd = File.new(path, "a")
    end
    if gzip
      fd = Zlib::GzipWriter.new(fd)
    end
    io = IOWriter.new(fd)
    io.final_path = final_path
    io.segment_size = 0;
    if stat
       io.segment_size = stat.size
    end
    io.segment_time = Time.now.to_i
    @files[path] = io
  end
end # class LogStash::Outputs::File

# wrapper class
class IOWriter
  def initialize(io)
    @io = io
  end
  def write(*args)
    @io.write(*args)
    @active = true
  end
  def flush
    @io.flush
    if @io.class == Zlib::GzipWriter
      @io.to_io.flush
    end
  end
  def method_missing(method_name, *args, &block)
    if @io.respond_to?(method_name)
      @io.send(method_name, *args, &block)
    else
      super
    end
  end
  attr_accessor :active
  attr_accessor :final_path
  attr_accessor :segment_time
  attr_accessor :segment_size
  
end
