#!/bin/bash

# Get relevant environment vars

CONFDIR=/opt/infinite-home/config/
BINDIR=/opt/infinite-home/bin/
SCRIPTDIR=/opt/infinite-home/db-scripts/
LOGDIR=/opt/infinite-home/logs
MONGODB=

IS_MASTER=$(curl -s http://localhost:9200/_cluster/nodes/_local |\
 				grep -q `curl -s http://localhost:9200/_cluster/state | grep  -o "master_node.:.[^\"]*"| grep -o "[^\"]*$"` \
				&& echo "true")

if [ -d $CONFDIR ]; then
	if [ -f $CONFDIR/infinite.service.properties ]; then
		MONGODB=`grep "^db.server=" $CONFDIR/infinite.service.properties | sed s/'db.server='// | sed s/' '//g`
		MONGODP=`grep "^db.port=" $CONFDIR/infinite.service.properties | sed s/'db.port='// | sed s/' '//g`
	fi		
fi

if echo $IS_MASTER  | grep -qi "true"; then
	if [ ! -z "$MONGODB" ]; then
		if [ -x /usr/bin/mongo ]; then
			# (Harvester must be stopped)
			# (Note all the other harvesters need to be stopped also, accomplished via cron jobs)
			service infinite-px-engine status && exit
			echo "Starting sync_features.sh" > $LOGDIR/sync_time.txt 
            date >> $LOGDIR/sync_time.txt
            
			/usr/bin/mongo --quiet $MONGODB:$MONGODP/doc_metadata $SCRIPTDIR/calc_freq_counts.js
            date >> $LOGDIR/sync_time.txt
			echo "Completed frequency recalculation" >> $LOGDIR/sync_time.txt
			 
			/usr/bin/mongo --quiet $MONGODB:$MONGODP/config $SCRIPTDIR/update_doc_counts.js
            date >> $LOGDIR/sync_time.txt
			echo "Completed doc counts recalculation" >> $LOGDIR/sync_time.txt

			#(since this will delete the entire entity DB if called after the calc_freq_counts script fails for any reason
			#  - believe me, I *know* - will check that it worked first...)
			if mongo --quiet $MONGODB:$MONGODP/feature --eval '{ db.entity.find({batch_resync:true}).limit(100).length() }' | grep -q 100; then
				/usr/bin/mongo --quiet $MONGODB:$MONGODP/feature --eval 'db.entity.remove({ "batch_resync": { "$exists": false } })'
				rm -rf $LOGDIR/feature_sync_log.txt
				$BINDIR/reindex_from_db.sh --entity --rebuild 100000 $LOGDIR/feature_sync_log.txt
				echo "Completed DB pruning and index resync" >> $LOGDIR/sync_time.txt
			else
				echo "Skipped DB pruning and index resync" >> $LOGDIR/sync_time.txt
			fi			
            date >> $LOGDIR/sync_time.txt
		fi
	fi
fi