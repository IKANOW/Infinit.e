#check if custom is already running
runningCustom=`pgrep -f 'infinit.e.core.server.*.jar --custom'`
#if no process ids returned for custom, run job
if [ -z "$runningCustom" ]; then
        java -jar /mnt/opt/infinite-home/lib/infinit.e.core.server.jar --custom
fi
