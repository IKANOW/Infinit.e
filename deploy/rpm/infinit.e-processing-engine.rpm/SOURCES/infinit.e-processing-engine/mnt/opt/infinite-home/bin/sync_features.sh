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
		
			# HANDLE LOCK:
			#(first just handle the case where there is no lock set yet - set to a dummy date so will succeed)
			mongo $MONGODB:$MONGODP/feature --eval 'db.sync_lock.insert({_id:ObjectId("4f985f98d4eefff2ed6963dc"), "last_sync":new Date(new Date().getTime()-2*3600*1000*24)})'
			if mongo $MONGODB:$MONGODP/feature --eval 'db.sync_lock.findAndModify({query: {last_sync:{$lt:new Date(new Date().getTime()-3600*1000*24)}}, update: {$set:{last_sync:new Date()}}})' | grep "null"; then
				echo "Database is locked, skipping"
				exit
			fi
		
			# (Harvester must be stopped)
			# (Note all the other harvesters need to be stopped also, accomplished via cron jobs)
			service infinite-px-engine status && exit
			echo "Starting sync_features.sh" > $LOGDIR/sync_time.txt 
            date >> $LOGDIR/sync_time.txt
            
            # (Remove any lingering soft deleted items)
			/usr/bin/mongo --quiet $MONGODB:$MONGODP/doc_metadata --eval 'db.metadata.remove({"sourceKey": /^\?DEL\?/});'
            date >> $LOGDIR/sync_time.txt
			echo "Completed soft deletion mop up" >> $LOGDIR/sync_time.txt
            
            # (Update feature/document entity counts)
			/usr/bin/mongo --quiet $MONGODB:$MONGODP/doc_metadata $SCRIPTDIR/calc_freq_counts.js
            date >> $LOGDIR/sync_time.txt
			echo "Completed frequency recalculation" >> $LOGDIR/sync_time.txt

			# (Update per-source document counts)			 
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
            
            # REMOVE LOCK
			mongo $MONGODB:$MONGODP/feature --eval 'db.sync_lock.drop()'            
		fi
	fi
fi