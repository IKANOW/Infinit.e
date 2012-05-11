#check if this is master (running NameNode)
runningNameNode=`pgrep -f namenode.NameNode`
#check if custom is already running
runningCustom=`pgrep -f 'infinit.e.core.server.*.jar --custom'`
#if no process id's returned for namenode and custom, run job
if [ -z "$runningNameNode" ] && [-z "$runningCustom" ]; then
        java -jar /mnt/opt/infinite-home/lib/infinit.e.core.server.jar --custom
fi
