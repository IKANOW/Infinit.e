#!/bin/sh
###########################################################################
# restore_script.sh
###########################################################################
# Get MongoDB port from $1
###########################################################################
if [ "$1" = "" ]; then
	MONGO_PORT="27017"
else
	MONGO_PORT="$1"
fi

echo "Restore Infinit.e MongoDB Data"
echo "Let's restore from the last backup"
mongorestore --host localhost:$MONGO_PORT /mnt-backup/db
echo "Finished restoring from the backup"
