#!/bin/sh
###########################################################################
# restore_script.sh PORT PATH_OF_BACKUP_TGZ
###########################################################################
# Get MongoDB port from $1
###########################################################################
MONGO_PORT="$1"

if [ `pwd` != "/opt/db-home" ]; then
	echo "Run this from /opt/db-home"
	exit -1
fi
echo "Restore Infinit.e MongoDB Data"
echo "Let's restore from the last backup"
tar xzvf $2
mongorestore --host localhost:$MONGO_PORT opt/db-home/db/
rm -rf  opt/
echo "Finished restoring from the backup"
