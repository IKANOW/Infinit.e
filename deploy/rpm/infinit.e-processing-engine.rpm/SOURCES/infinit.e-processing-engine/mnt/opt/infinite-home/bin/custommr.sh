#check if custom is already running
runningCustom=`pgrep -f 'infinit.e.core.server.*.jar --custom'`
#if no process ids returned for custom, run job (need JAVA_OPTS to log to correct syslog when configured)
if [ -z "$runningCustom" ]; then
        java $JAVA_OPTS -jar /mnt/opt/infinite-home/lib/infinit.e.core.server.jar --custom
fi
