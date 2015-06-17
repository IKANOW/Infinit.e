#!/bin/bash

# Get relevant environment vars

CONFDIR=/opt/infinite-home/config/
BINDIR=/opt/infinite-home/bin/
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
if [ -f $BINDIR/STOP_BATCH_SYNC_FILE ] && [ -z "$1" ]; then
	exit 0
fi

MASTER=`curl -s http://localhost:9200/_cluster/state | grep  -o "master_node.:.[^\"]*"| grep -o "[^\"]*$" | grep -o "[^-].*"`
IS_MASTER=$(curl -s http://localhost:9200/_nodes/_local | grep -q "$MASTER" && echo "true")

if echo $IS_MASTER  | grep -qi "true"; then
	if [ ! -z "$MONGODB" ]; then
		if [ -x /usr/bin/mongo ]; then
		
			# HANDLE LOCK:
			#(first just handle the case where there is no lock set yet - set to a dummy date so will succeed)
			mongo $MONGODB/feature --eval 'db.recalc_sync_lock.insert({_id:ObjectId("4f985f98d4eefff2ed6963dc"), "last_sync":new Date(new Date().getTime()-2*3600*1000*24)})'
			if mongo $MONGODB/feature --eval 'db.recalc_sync_lock.findAndModify({query: {last_sync:{$lt:new Date(new Date().getTime()-3600*1000*24)}}, update: {$set:{last_sync:new Date()}}})' | grep "null"; then
				echo "Database is locked, skipping"
				exit
			fi
		
			echo "Starting sync_features.sh" > $LOGDIR/sync_time.txt 
            date >> $LOGDIR/sync_time.txt
            
            # (Remove any lingering soft deleted items)
			/usr/bin/mongo --quiet $MONGODB/doc_metadata --eval 'db.metadata.remove({"url": /^\?DEL\?/});'
            date >> $LOGDIR/sync_time.txt
			echo "Completed soft deletion mop up" >> $LOGDIR/sync_time.txt
            
            # (Update feature/document entity counts)
			/usr/bin/mongo --quiet $MONGODB/doc_metadata $SCRIPTDIR/calc_freq_counts.js >> $LOGDIR/sync_time.txt
            date >> $LOGDIR/sync_time.txt
			echo "Completed frequency recalculation phase 1" >> $LOGDIR/sync_time.txt

			# (Phase 2 of frequency update)
			$BINDIR/infinite_indexer.sh --entity --verify --query feature.tmpCalcFreqCounts >> $LOGDIR/sync_time.txt
			/usr/bin/mongo --quiet $MONGODB/feature --eval '{db.tmpCalcFreqCounts.drop()}'
            date >> $LOGDIR/sync_time.txt
			echo "Completed frequency recalculation phase 2" >> $LOGDIR/sync_time.txt

			# (Update per-source document counts)			 
			/usr/bin/mongo --quiet $MONGODB/ingest $SCRIPTDIR/update_doc_counts.js
            date >> $LOGDIR/sync_time.txt
			echo "Completed doc counts recalculation" >> $LOGDIR/sync_time.txt

            # REMOVE LOCK
			mongo $MONGODB/feature --eval 'db.recalc_sync_lock.drop()'            
		fi
	fi
fi