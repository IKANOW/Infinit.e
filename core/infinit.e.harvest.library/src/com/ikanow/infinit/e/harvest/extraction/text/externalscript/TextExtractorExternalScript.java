/*******************************************************************************
 * Copyright 2012, The Infinit.e Open Source Project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.ikanow.infinit.e.harvest.extraction.text.externalscript;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bson.types.ObjectId;

import com.ikanow.infinit.e.data_model.InfiniteEnums.ExtractorDocumentLevelException;
import com.ikanow.infinit.e.data_model.interfaces.harvest.EntityExtractorEnum;
import com.ikanow.infinit.e.data_model.interfaces.harvest.ITextExtractor;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.data_model.utils.IkanowSecurityManager;
import com.ikanow.infinit.e.harvest.utils.AuthUtils;
import com.ikanow.infinit.e.harvest.utils.PropertiesManager;

public class TextExtractorExternalScript implements ITextExtractor
{
	//user customizable options
	public static class Options {
		public static final String DEBUG = "debug"; //turns on prints to stdout
		public static final String STDERR = "stderr"; //turns on prints to stderr
		public static final String ERRTOFULLTEXT = "errtofulltext";
		public static final String SCRIPT = "script"; //path of the script to be run 
		public static final String ARG = "arg"; //arguments for scripts (arg1,arg2)
		public static final String TIMEOUT = "timeout"; //int ms "0" means wait forever
	}

	protected PropertiesManager _props = null;
	protected String _defaultUserAgent = null;

	protected IkanowSecurityManager _secManager = null;

	private String pre_path = "/opt/infinite-home/extractor-scripts/";
	private boolean _debug = false;
	private boolean _firstTime = true;
	private boolean _error_to_fulltext = false;
	private PrintStream _out = System.out;
	private static long MAX_RUNTIME = 300000L; //5 ms default, 0 means run forever
	private static boolean _admin_override = false;
	PropertiesManager props = new PropertiesManager();

	@Override
	public String getName() { return "externalscript"; }

	@Override
	public void extractText(DocumentPojo partialDoc) throws ExtractorDocumentLevelException 
	{
		if (null != partialDoc)
		{
			//Looking for triggers to know what script to run and with what parameters.
			//These are specified in the featureEngine.engineConfig of the source
			Map<String, String> options = partialDoc.getTempSource().getExtractorOptions();
			if (null != options)
			{
				if (_firstTime)
				{
					_firstTime = false;
					//Begin User Overrides
					
					
					//print to stderr
					String user_stderr = options.get(Options.STDERR);
					if (null != user_stderr && user_stderr.equalsIgnoreCase("true"))
					{
						_debug = true;
						_out = System.err;
						_out.println("Output Redirected to STDERR by Admin User via Extractor Option.");
					}
					
					// Turn on printing script errors to full text of the doc
					String user_scriptErr = options.get(Options.ERRTOFULLTEXT);
					if (null != user_scriptErr && user_scriptErr.equalsIgnoreCase("true")){
						_error_to_fulltext = true;
						if (_debug)
							_out.println("Script Errors will be Printed to FullText (enabled by Extractor Option.)");
					}
					
					//Turn on Prints
					String toDebug = options.get(Options.DEBUG);
					//admin only
					if (null != toDebug && toDebug.equalsIgnoreCase("true") 
							&& (_admin_override || (null != partialDoc.getSource() && null != partialDoc.getTempSource().getOwnerId() &&
							AuthUtils.isAdmin(partialDoc.getTempSource().getOwnerId())))){
						_debug = true;
						_out.println("Debugging Enabled by Extractor Options");
					}
					
					//Runtime override
					try{
						String user_timeout = options.get(Options.TIMEOUT);
						if (null != user_timeout)
						{
							long proposed_timeout = Long.parseLong(user_timeout);
							long max_time = props.getMaxTimePerSource();
							
							if (max_time > proposed_timeout || _admin_override || 
									(null != partialDoc.getSource() && null != partialDoc.getTempSource().getOwnerId() &&
									AuthUtils.isAdmin(partialDoc.getTempSource().getOwnerId())))
							{
								MAX_RUNTIME = proposed_timeout;
								if (_debug)
									_out.println("Timeout Overridden to: " + MAX_RUNTIME + "ms");
							}
							else
							{
								MAX_RUNTIME = max_time;
								if (_debug)
									_out.println("Timeout Overridden but was too large. Set to max time of: " + MAX_RUNTIME + "ms");
							}
						}
					}
					catch (NumberFormatException nfe)
					{
						if (_debug)
							_out.println("Timeout value provided was not a number. Timeout will remain " + MAX_RUNTIME + "ms. Max time allowed is" +  props.getMaxTimePerSource());
					}
					
					//End User/Admin overrides
				}

				String script = options.get(Options.SCRIPT);
				if (null != script )
				{
					if (script.startsWith(pre_path))
						script = script.replace(pre_path, "");

					//pull out the directory and the file separately
					Pattern filePattern = Pattern.compile("^(.+)/([^/]+)$");
					Matcher m = filePattern.matcher(script);

					String dir = null;
					String file = null;
					try{
						if (m.matches())
						{
							dir = m.group(1);
							file = m.group(2);
						}
					}
					catch (IllegalStateException e)
					{ 
						throw new ExtractorDocumentLevelException("The Script was not in a proper format.");
					}

					if (dir != null && file != null)
					{
						if (_debug)
						{
							_out.println("**Directory of Script: " + dir);
							_out.println("**Filename: " + file);
						}
						boolean allowed = false;
						if (dir.contains("_all"))
						{
							allowed = true;
						}
						else if ( null != partialDoc && null != partialDoc.getTempSource() && null != partialDoc.getTempSource().getCommunityIds())
						{
							for (ObjectId objId : partialDoc.getTempSource().getCommunityIds() )
							{
								String tempCommId = objId.toString();
								if (dir.contains(tempCommId))
								{
									allowed=true;
									break;
								}
							}
						}
						else
						{
							throw new ExtractorDocumentLevelException("The Source does is not a member of any communities.");
						}


						if (allowed==true)
						{
							if (_debug)
								_out.println("Script passed permission verification");

							ArrayList<String> arg_list = new ArrayList<String>();
							//search for args. Stop searching when null value is found
							int arg_count = 0;
							while(true)
							{
								String arg = options.get(Options.ARG+(arg_count+1));
								if (null != arg)
								{
									arg_list.add(arg);
									arg_count++;
								}
								else
									break;
							}
							//args must be in sequential order. Args after gaps will be skipped.

							ArrayList<String> args = new ArrayList<String>(arg_list.size() + 1);
							args.add(script);

							for (int j = 0; j<arg_list.size(); j++)
							{
								args.add(arg_list.get(j));
							}

							ProcessBuilder pb = new ProcessBuilder(args);


							pb.environment().put("Path","/usr/local/sbin:/usr/local/bin:/sbin:/bin:/usr/sbin:/usr/bin:/root/bin");
							//set pwd
							pb.directory(new File(pre_path));
							
							if (_error_to_fulltext)
								pb.redirectErrorStream(true);
							Process p = null;

							try{
								p = pb.start();
								StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream(), p);
								outputGobbler.start();
								outputGobbler.join(MAX_RUNTIME);

								if (_debug && outputGobbler.isAlive()) {
									_out.println("The script `" + script + "` has exceeded the maximum runtime (" + MAX_RUNTIME + "ms) and has been stopped.");
									outputGobbler.interrupt();
									p.destroy();
									
									partialDoc.setFullText(outputGobbler.getOutput());
									
								}
								else
								{
									if (_debug)
									{
										_out.println("The provided script has successfully run to completion");
									}
									partialDoc.setFullText(outputGobbler.getOutput());
								}
								
								
								
							}catch (IOException e)
							{
								e.printStackTrace();
								throw new ExtractorDocumentLevelException("An IO exception occurred running the external file");
							} catch (InterruptedException e) {
								e.printStackTrace();
								throw new ExtractorDocumentLevelException("A thread interrupted exception occurred.");
								
							}
						}
						else //script directory did not match community id
						{
							throw new ExtractorDocumentLevelException("The Source does not have proper permission to access the script file.");
						}

					}
					else //dir or file was null
					{
						if (dir == null)
							throw new ExtractorDocumentLevelException("Could not parse directory from script");
						if (file == null)
							throw new ExtractorDocumentLevelException("Could not parse filename from script");
					}	
				}
				else //no script provided
				{
					throw new ExtractorDocumentLevelException("Could not find required information [" + Options.SCRIPT + "]");
				}
			}
		}
	}
	
	public void setAdminOverrdide(boolean setAdmin)
	{
		_admin_override = setAdmin;
	}

	@Override
	public String getCapability(EntityExtractorEnum capability) {
		return null;
	}


	class StreamGobbler extends Thread
	{
		private InputStream is;
		private Process _process;
		private boolean _interrupt = false;
		private ByteArrayOutputStream _output_stream;
		private final byte[] buffer = new byte[16384];
		private long _MAX_FILE_SIZE = 25000000L; //25MB


		StreamGobbler(InputStream is, Process process)
		{
			this.is = is;
			this._process = process;
			_output_stream = new ByteArrayOutputStream();
		}

		public void run()
		{
			try
			{
				long stream_size = 0L;
				int sleepLen[] = { 1, 10, 50 }; 
				int sleepPos = 0;
				for (;;) {
					int available = is.available();
					
					if( available == 0) {
						
						try{
							_process.exitValue();
							break;
						}
						catch (IllegalThreadStateException e)
						{
							try {
		                        Thread.sleep( sleepLen[sleepPos] );
		                        sleepPos++;
		                        if (sleepPos > 2)
		                        	sleepPos = 2;
		                    }
		                    catch( InterruptedException e2 ) {
		                        break;
		                    }
		                    continue;
						} 
	                }		
					sleepPos = 0;
					if (available > buffer.length)
						available = buffer.length;
					
					int rsz = is.read(buffer, 0, available);
					stream_size += buffer.length;
					if (rsz < 0 || _interrupt)
						break;
					_output_stream.write(buffer, 0, rsz);

					if (stream_size > _MAX_FILE_SIZE)
					{
						throw new ExtractorDocumentLevelException("Script Output Larger than Max Size Allowed.");
					}

				}

			} catch (IOException ioe)
			{
				ioe.printStackTrace();  
			} catch (ExtractorDocumentLevelException e) {
				e.printStackTrace();
			}
			finally 
			{
				try {
					if (null != is)
						is.close();
				} catch (IOException e) {}
			}
		}

		public void interrupt() {
			super.interrupt();

			try {
				if (null != is)
					is.close();
			} catch (InterruptedIOException e) {
				Thread.currentThread().interrupt();
				if (_debug)
					_out.println("Interrupted via InterruptedIOException");
			}
			catch (IOException e) {
				if (!isInterrupted()) {
					e.printStackTrace();
				} else {
					if (_debug)
						_out.println("Interrupted");
				}
			}
			if (_debug)
				_out.println("Shutting down thread");
		}

		public String getOutput()
		{
			try {
				return _output_stream.toString("UTF-8");
			} catch (UnsupportedEncodingException e) {
				return null;
			}
		}
	}
}