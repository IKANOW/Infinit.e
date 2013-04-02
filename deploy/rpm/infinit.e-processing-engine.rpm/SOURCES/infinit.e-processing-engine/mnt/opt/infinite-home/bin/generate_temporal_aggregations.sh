#!/bin/bash

# Get relevant environment vars

CONFDIR=/opt/infinite-home/config/
BINDIR=/opt/infinite-home/bin/
LIBDIR=/opt/infinite-home/lib/
SCRIPTDIR=/opt/infinite-home/db-scripts/
LOGDIR=/opt/infinite-home/logs
MONGODB=
if [ -d $CONFDIR ]; then
        if [ -f $CONFDIR/infinite.service.properties ]; then
                MONGODB=`grep "^db.server=" $CONFDIR/infinite.service.properties | sed s/'db.server='// | sed s/' '//g`
                MONGODP=`grep "^db.port=" $CONFDIR/infinite.service.properties | sed s/'db.port='// | sed s/' '//g`
        fi
fi

# Administratively off:
if [ -f $BINDIR/STOP_TEMPORAL_AGGREGATIONS ] && [ -z "$1" ]; then
	exit 0
fi

IS_MASTER=$(curl -s http://localhost:9200/_cluster/nodes/_local |\
 				grep -q `curl -s http://localhost:9200/_cluster/state | grep  -o "master_node.:.[^\"]*"| grep -o "[^\"]*$"` \
				&& echo "true")

if echo $IS_MASTER  | grep -qi "true"; then
	if [ ! -z "$MONGODB" ]; then
		if [ -x /usr/bin/mongo ]; then
		
			echo "Starting temporal aggregation" > $LOGDIR/temporal_time.txt 
            date >> $LOGDIR/temporal_time.txt
            
            # (Update feature/document entity counts)
			/usr/bin/mongo --quiet $SCRIPTDIR/temporal_entity_aggregation.js >> $LOGDIR/temporal_time.txt
            date >> $LOGDIR/temporal_time.txt
			echo "Completed temporal aggregation" >> $LOGDIR/temporal_time.txt
		fi
	fi
fi