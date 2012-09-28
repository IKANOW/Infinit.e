LIB=/opt/infinite-home/lib
CONFIG_LOCATION="/opt/infinite-home/config"
LAST_CLEANSE="0"
RESET_FILE="/opt/infinite-home/bin/RESET_FILE"
SYNC_FILE="/opt/infinite-home/bin/SYNC_FILE"
STOP_FILE="/opt/infinite-home/bin/STOPFILE"
ALLSTOP_FILE="/opt/infinite-home/bin/ALLSTOPFILE"
SECURITY_POLICY="/opt/infinite-home/bin/security.policy"
EXTRA_JAVA_ARGS=" -Xms2048m -Xmx2048m -Xmn512m -Djava.security.policy=$SECURITY_POLICY -Dcom.sun.management.jmxremote -Dsun.net.client.defaultConnectTimeout=30000 -Dsun.net.client.defaultReadTimeout=30000 -classpath infinit.e.harvest.library.jar:*:infinit.e.core.server.jar"

if [ -f $ALLSTOP_FILE ] && [ -z "$1" ]; then
	exit 0
fi

harvest()
{
	echo "Starting harvest $1 $2"
	#do harvest cycle
	if [ -f "$RESET_FILE" ]; then
		echo "Reseting Harvester"
		java ${EXTRA_JAVA_ARGS} com.ikanow.infinit.e.core.CoreMain --harvest --config $CONFIG_LOCATION --reset $1 $2
		rm -f $RESET_FILE
	else
		echo "Doing normal harvest"
		java ${EXTRA_JAVA_ARGS} com.ikanow.infinit.e.core.CoreMain --harvest --config $CONFIG_LOCATION $1 $2
	fi
}

optimize()
{
	#currently nothing to do
	echo "Starting optimize"
}

sync()
{
	echo "Syncing"
	java ${EXTRA_JAVA_ARGS} com.ikanow.infinit.e.core.CoreMain --sync --from $LAST_CLEANSE --config $CONFIG_LOCATION 

	LAST_CLEANSE="$(date +%s)"
	rm -f "$SYNC_FILE"
}

# Go to a dir where the classpath should just work!
cd $LIB
# Run the harvester
LAST_CLEANSE="$(date --date '7 days ago' +%s)"
while true; do
	harvest $1 $2
	if [ -f "$STOP_FILE" ] || [ -f "$ALLSTOP_FILE" ]; then
		exit 0
	fi
	if [ -f "$SYNC_FILE" ]; then
		optimize
		sync
		if [ -f "$STOP_FILE" ] || [ -f "$ALLSTOP_FILE" ]; then
			exit 0
		fi
	fi
done
