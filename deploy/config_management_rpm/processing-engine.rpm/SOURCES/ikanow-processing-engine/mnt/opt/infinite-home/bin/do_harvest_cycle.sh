#!/bin/sh
if [ $# -lt 2 ]; then
        echo "--source|--community key|id [delay]"
        exit 0
fi
runuser - tomcat -c "/opt/infinite-home/bin/infinite-px-engine.sh $1 $2"
if [ $# -eq 3 ]; then
	echo "Sleeping for $3 seconds..."
	sleep $3
fi

