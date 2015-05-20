#!/bin/sh
################################################################################
# backup-script.sh
################################################################################
DB_HOME=/opt/db-home/
BACKUP_DIR=$DB_HOME/data

cd $DB_HOME/backups

################################################################################
# s3.url = 
SERVICE_PROPERTY_FILE='/opt/infinite-home/config/infinite.service.properties'
S3_URL=`grep "^s3.url=" $SERVICE_PROPERTY_FILE | sed s/'s3.url='// | sed s/' '//g`
S3_URL="mongo.$S3_URL"

CLUSTER_NAME=$(infdb cluster-name)

################################################################################
# $1 arg = port to access MongoDB on
################################################################################
if [ "$1" = "" ]; then
	MONGO_PORT="27017"
else
	MONGO_PORT="$1"
fi

# Use different name convention for config servers since there are 1/3/5 with identical names (use hostname instead)
DAY_OF_MONTH=$(date +%d)
WEEK_OF_YEAR=$(date +%W)
if [ "$MONGO_PORT" = "27016" ]; then
	FILENAME=db_backup_`hostname`_${DAY_OF_MONTH}_${MONGO_PORT}.tgz
	if [ `date +%w` -eq 0 ]; then
		END_FILENAME=db_backup_`hostname`_latestweekly_${MONGO_PORT}.tgz
	else
		END_FILENAME=db_backup_`hostname`_latest_${MONGO_PORT}.tgz
	fi	
else 
	FILENAME=db_backup_${CLUSTER_NAME}_${DAY_OF_MONTH}_${MONGO_PORT}.tgz
	if [ `date +%w` -eq 0 ]; then
		END_FILENAME=db_backup_${CLUSTER_NAME}_latestweekly_${MONGO_PORT}.tgz
	else
		END_FILENAME=db_backup_${CLUSTER_NAME}_latest_${MONGO_PORT}.tgz
	fi	
fi

################################################################################
# ?
################################################################################
if [[ $(infdb is_mongos) == "true" || $(infdb is_dbinstance) == "true" ]]; then 
	mongos_ip=""
else
	mongos_ip=$(infdb mongos_ip)
fi

################################################################################
# Only run this script if I'm the master (since there can be 0+ slaves)
################################################################################

if mongo localhost:$MONGO_PORT --eval "a = db.isMaster(); print(tojson(a)); " | grep -q '"ismaster" : true'; then

	################################################################################
	# Stop balancer
	################################################################################
	mongo $mongos_ip <<EOF
use config
db.settings.update( { "_id": "balancer" }, { "\$set" : { "stopped": true } } , true );
while( db.locks.findOne({ "_id": "balancer" }).state ) { print("waiting..."); sleep(1000); }
exit
EOF

	echo "Backup Infinit.e DB Started `date`" >> $DB_HOME/bak_${MONGO_PORT}.log
	BACKUP_TMPDB=$DB_HOME/db_${MONGO_PORT}

	echo "Remove the existing backup directory $BACKUP_TMPDB to make room for todays if it exists"
	rm -rf $BACKUP_TMPDB

	echo "Make a new backup directory in $BACKUP_TMPDB"
	mkdir $BACKUP_TMPDB

	if [ `date +%w` -eq 0 ]; then
		# Weekly - Full backup: 
		mongodump --host localhost --port $MONGO_PORT --out $BACKUP_TMPDB
		echo "Dump the entire existing database to file `date`" >> $DB_HOME/bak_${MONGO_PORT}.log
	else
		# Daily - Incremental backup (except for admin): 
		mongodump --host localhost --port $MONGO_PORT --out $BACKUP_TMPDB --db config 
		mongodump --host localhost --port $MONGO_PORT --out $BACKUP_TMPDB --db social 
		mongodump --host localhost --port $MONGO_PORT --out $BACKUP_TMPDB --db ingest
		mongodump --host localhost --port $MONGO_PORT --out $BACKUP_TMPDB --db gui
		mongodump --host localhost --port $MONGO_PORT --out $BACKUP_TMPDB --db security
		mongodump --host localhost --port $MONGO_PORT --out $BACKUP_TMPDB --db custommr --collection customlookup
		echo "Dump the entire admin database to file `date`" >> $DB_HOME/bak_${MONGO_PORT}.log
		mongodump --host localhost --port $MONGO_PORT --out $BACKUP_TMPDB --db local --collection oplog.rs
	fi	

	# Restart balancer
	################################################################################
	mongo $mongos_ip <<EOF
use config
db.settings.update( { "_id": "balancer" }, { "\$set" : { "stopped": false } } , true );
exit
EOF
	
	# tar up the backup
	echo "Create a compressed version of the backup `date`" >> $DB_HOME/bak_${MONGO_PORT}.log
	tar -cvzf $FILENAME $BACKUP_TMPDB 
	
	################################################################################
	# Transfer: S3 vs non
	################################################################################
	if [ "$S3_URL" != "mongo." ]; then
		split -b1000m $FILENAME $FILENAME-
    	s3cmd -f put $FILENAME-* s3://$S3_URL
	fi
	
	################################################################################
	# Weekly, do a transfer to a remote backup location S3 vs non
	################################################################################
    if [ `date +%w` -eq 0 ] && [ "$S3_URL" != "" ]; then 
   		for i in $(ls $FILENAME-*); do
   			NEW_FILENAME=$(echo $i | sed "s/_${DAY_OF_MONTH}_/_${WEEK_OF_YEAR}_/")
            s3cmd -f put $i s3://backup.$S3_URL/$NEW_FILENAME
   		done
    fi
	
	################################################################################
	# Tidy up:
	rm -rf $BACKUP_TMPDB
	rm -f $FILENAME-*
	mv $FILENAME $END_FILENAME
	
	echo "Finished making the backup for `date`"  >> $DB_HOME/bak_${MONGO_PORT}.log
fi
