#check if custom is already running
runningCustom=`pgrep -f 'com.ikanow.infinit.e.core.CoreMain --custom'`
#if no process ids returned for custom, run job (need JAVA_OPTS to log to correct syslog when configured)

LIB=/opt/infinite-home/lib
EXTRA_JAVA_ARGS="$JAVA_OPTS -Dfile.encoding=UTF-8 -classpath $LIB/infinit.e.data_model.jar:$LIB/infinit.e.harvest.library.jar:$LIB/infinit.e.query.library.jar:$LIB/infinit.e.processing.custom.library.jar:$LIB/infinit.e.processing.generic.library.jar:$LIB/infinit.e.core.server.jar:/opt/hadoop-infinite/lib/*:$LIB/unbundled/*:$LIB/es-libs/*"

if [ -z "$runningCustom" ]; then
		java ${EXTRA_JAVA_ARGS} com.ikanow.infinit.e.core.CoreMain --custom
fi
