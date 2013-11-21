#!/bin/bash
################################################################################
# s3.url = 
SERVICE_PROPERTY_FILE='/opt/infinite-home/config/infinite.service.properties'
S3_BASE_URL=`grep "^s3.url=" $SERVICE_PROPERTY_FILE | sed s/'s3.url='// | sed s/' '//g`
CLUSTER_NAME=`grep "^elastic.cluster=" $SERVICE_PROPERTY_FILE | sed s/'elastic.cluster='// | sed s/"^\s*"// | sed s/"\s*$"//`

ES_HOME=/opt/elasticsearch-infinite/
BACKUP_DIR=$ES_HOME/data

if [ "$S3_BASE_URL" != "" ]; then
	S3_URL="elasticsearch.$S3_BASE_URL"
fi

cd $ES_HOME/backups

DAY_OF_WEEK=$(date +%w)
WEEK_OF_YEAR=$(date +%W)
FILENAME=index_backup_${CLUSTER_NAME}_`hostname`_${DAY_OF_WEEK}.tgz
END_FILENAME=index_backup_${CLUSTER_NAME}_`hostname`_latest.tgz
	 
function do_backup() {
	# Disable flushing
	curl -XPUT 'localhost:9200/_settings' -d '{ "index":{ "translog.disable.flush": true } }'

	# Do backup (1/day-of-week, so 7 daily backups max)
	tar czf $FILENAME $BACKUP_DIR

	# Re-enable flushing
	curl -XPUT 'localhost:9200/_settings' -d '{ "index":{ "translog.disable.flush": false } }'

	#Transfer:
	if [ "$S3_URL" != "" ]; then
		split -b1000m $FILENAME $FILENAME-
    	s3cmd -f put $FILENAME-* s3://$S3_URL
    fi

	################################################################################
	# Weekly, do a transfer to a remote backup location S3 vs non
	################################################################################
    if [ `date +%w` -eq 0 ] && [ "$S3_URL" != "" ]; then 
   		for i in $(ls $FILENAME-*); do
   			NEW_FILENAME=$(echo $i | sed "s/_${DAY_OF_WEEK}.tgz/_${WEEK_OF_YEAR}.tgz/")
            s3cmd -f put $i s3://backup.$S3_URL/$NEW_FILENAME
   		done
    fi
                                                                
	################################################################################
	# Tidy up:
	rm -f $FILENAME-*
	mv $FILENAME $END_FILENAME
}
do_backup
