#!/bin/sh
EXPIRY_LIMIT=7
LOG_LOCATION=/var/log/mongo
LOG_REPOS=/data/log

echo "Rotate the mongodb logs"

# get rid of backups older than 7 days
find $LOG_REPOS -mtime +$EXPIRY_LIMIT -and -name "*.log.*" | xargs rm -f

#rotate the log file
killall -SIGUSR1 mongod
killall -SIGUSR1 mongos
mkdir -p $LOG_REPOS
mv $LOG_LOCATION/*.log.* $LOG_REPOS
