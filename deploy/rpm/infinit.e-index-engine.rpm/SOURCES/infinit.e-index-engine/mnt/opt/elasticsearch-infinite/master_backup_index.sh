#!/bin/bash
################################################################################
# s3.url = 
SERVICE_PROPERTY_FILE='/opt/infinite-home/config/infinite.service.properties'
S3_BASE_URL=`grep "^s3.url=" $SERVICE_PROPERTY_FILE | sed s/'s3.url='// | sed s/' '//g`

ES_HOME=/opt/elasticsearch-infinite/
BACKUP_DIR=$ES_HOME/data

if [ "$S3_BASE_URL" != "" ]; then
	S3_URL="elasticsearch.$S3_BASE_URL"
fi

function do_backup() {
	# Disable flushing
	curl -XPUT 'localhost:9200/_settings' -d '{ "index":{ "translog.disable.flush": true } }'

	# Do backup (1/day-of-month, so 31 daily backups max)
	tar czf $ES_HOME/index_backup_`hostname`_`date +%d`.tgz $BACKUP_DIR

	# Re-enable flushing
	curl -XPUT 'localhost:9200/_settings' -d '{ "index":{ "translog.disable.flush": false } }'

	# Tidy up
	rm -f $ES_HOME/index_backup_`hostname`_most_recent.tgz

	#Transfer:
	if [ "$S3_URL" != "" ]; then
    	s3cmd -f put $ES_HOME/index_backup_`hostname`_`date +%d`.tgz s3://$S3_URL
        mv $ES_HOME/index_backup_`hostname`_`date +%d`.tgz $ES_HOME/index_backup_`hostname`_most_recent.tgz
        s3cmd -f put $ES_HOME/index_backup_`hostname`_most_recent.tgz s3://$S3_URL
    fi
                
	# Weekly, do a transfer to a remote backup location
	if [ `date +%w` -eq 0 ] && [ "$S3_URL" != "" ]; then 
		s3cmd -f put $ES_HOME/index_backup_`hostname`_most_recent.tgz s3://backup.$S3_URL/index_backup_`hostname`_`date +%Y%m%d`.tgz
	fi
}
do_backup
