#!/bin/sh
################################################################################
# backup-script.sh
################################################################################
DB_HOME=/opt/db-home/
BACKUP_DIR=$DB_HOME/data

################################################################################
# s3.url = 
SERVICE_PROPERTY_FILE='/opt/infinite-home/config/infinite.service.properties'
S3_URL=`grep "^s3.url=" $SERVICE_PROPERTY_FILE | sed s/'s3.url='// | sed s/' '//g`
S3_URL="mongodb.$S3_URL"

################################################################################
# $1 arg = port to access MongoDB on
################################################################################
if [ "$1" = "" ]; then
	MONGO_PORT="27017"
else
	MONGO_PORT="$1"
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
#
################################################################################
mongo $mongos_ip <<EOF
use config
db.settings.update( { _id: "balancer" }, { $set : { stopped: true } } , true );
while( db.locks.findOne({_id: "balancer"}).state ) { print("waiting..."); sleep(1000); }
exit
EOF

################################################################################
# Only run this script if I'm the master (since there can be 0+ slaves)
################################################################################

if mongo localhost:$MONGO_PORT --eval "a = db.isMaster(); print(tojson(a)); " | grep -q '"ismaster" : true'; then
	echo "Backup Infinit.e DB Started `date`" >> $DB_HOME/bak.log

	echo "Remove the existing backup directory $DB_HOME/db to make room for todays if it exists"
	rm -rf $DB_HOME/db

	echo "Make a new backup directory in $DB_HOME/db"
	mkdir $DB_HOME/db

	mongodump --host localhost --out $DB_HOME/db
	echo "Dump the existing database to file" >> $DB_HOME/bak.log

	# tar up the backup
	echo "Create a compressed version of the backup" >> $DB_HOME/bak.log
	tar -cvzf $DB_HOME/db_backup_`hostname`_`date +%d`.tgz $DB_HOME/db 
	
	################################################################################
	# Transfer: S3 vs non
	################################################################################
	if [ "$S3_URL" != "" ]; then
    	s3cmd -f put $DB_HOME/db_backup_`hostname`_`date +%d`.tgz s3://$S3_URL
    	mv $DB_HOME/db_backup_`hostname`_`date +%d`.tgz $DB_HOME/db_backup_`hostname`_most_recent.tgz
    	s3cmd -f put $DB_HOME/db_backup_`hostname`_most_recent.tgz s3://$S3_URL
	if
	
	# Tidy up:
	rm -rf $DB_HOME/db
	
	################################################################################
	# Weekly, do a transfer to a remote backup location S3 vs non
	################################################################################
    if [ `date +%w` -eq 0 && "$S3_URL" != "" ]; then 
            s3cmd -f put $DB_HOME/db_backup_`hostname`_most_recent.tgz s3://backup.$S3_URL/db_backup_`hostname`_`date +%Y%m%d`.tgz
    fi
		
	echo "Finished making the backup for `date`"  >> $DB_HOME/bak.log
fi
