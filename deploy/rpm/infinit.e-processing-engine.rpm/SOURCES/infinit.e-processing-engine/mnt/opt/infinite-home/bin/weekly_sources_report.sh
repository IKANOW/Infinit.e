#!/bin/bash
CONFDIR=/opt/infinite-home/config/
SCRIPTDIR=/opt/infinite-home/db-scripts/
MONGODB=
SERVERADDR=`/sbin/ifconfig | grep -o "addr:[0-9.]*" | grep -v "127.0.0.1"`

SERVICE_PROPERTY_FILE='/opt/infinite-home/config/infinite.service.properties'
FROMUSER=`grep "^log.files.mail.from=" $SERVICE_PROPERTY_FILE | sed s/'log.files.mail.from='// | sed s/' '//g`
TOUSER=`grep "^log.files.mail.to=" $SERVICE_PROPERTY_FILE | sed s/'log.files.mail.to='// | sed s/' '//g`

MASTER=`curl -s http://localhost:9200/_cluster/state | grep  -o "master_node.:.[^\"]*"| grep -o "[^\"]*$" | grep -o "[^-].*"`
IS_MASTER=$(curl -s http://localhost:9200/_nodes/_local | grep -q "$MASTER" && echo "true")

if [ -d $CONFDIR ]; then
	if [ -f $CONFDIR/infinite.service.properties ]; then
		MONGODB=`grep '^db.server=' $CONFDIR/infinite.service.properties | sed s/'db.server='// | sed s/' '//g`
		MONGODP=`grep '^db.port=' $CONFDIR/infinite.service.properties | sed s/'db.port='// | sed s/' '//g`
	fi		
fi

if echo $IS_MASTER  | grep -qi "true"; then
	if [ ! -z "$MONGODB" ]; then
		if [ -x /usr/bin/mongo ]; then
	
			echo "From: $FROMUSER" > /tmp/email.txt
			echo "Subject: weekly 'problem sources' report [$SERVERADDR]" >> /tmp/email.txt
		
			#First off, ensure everyone has an "error_reported" field:
	                /usr/bin/mongo --quiet $MONGODB:$MONGODP/ingest --eval \
	                'db.source.update({error_reported:{$exists:false}},{$set:{error_reported:false}}, false, true)' 
	
			#Then, first show all new problem sources
	
			echo "" >> /tmp/email.txt
			echo "*************************** HARVEST: NEW" >> /tmp/email.txt
			echo "" >> /tmp/email.txt
		
			/usr/bin/mongo --quiet $MONGODB:$MONGODP/ingest --eval \
			'db.source.find({error_reported:false, $or:[{"harvest.harvest_status":"error"},{isApproved:false}]},{_id:0,url:1,"harvest.harvest_message":1}).forEach(printjson);' \
				>> /tmp/email.txt
	
			#(Update these)
			/usr/bin/mongo --quiet $MONGODB:$MONGODP/ingest --eval \
			'db.source.update({error_reported:false, $or:[{"harvest.harvest_status":"error"},{isApproved:false}]},{$set:{error_reported:true}}, false, true);' 
	
			#Then, fixed sources:
	
			echo "" >> /tmp/email.txt
			echo "*************************** HARVEST: FIXED" >> /tmp/email.txt
			echo "" >> /tmp/email.txt
		
			/usr/bin/mongo --quiet $MONGODB:$MONGODP/ingest --eval \
			'db.source.find({error_reported:true,"harvest.harvest_status":"success",isApproved:true},{_id:0,url:1,"harvest.harvest_message":1}).forEach(printjson);' \
				>> /tmp/email.txt
	
			#(Update these)
	                /usr/bin/mongo --quiet $MONGODB:$MONGODP/ingest --eval \
	                'db.source.update({error_reported:true,"harvest.harvest_status":"success",isApproved:true},{$set:{error_reported:false}}, false, true)' 
	
			#Then, all the other sources
		
			echo "" >> /tmp/email.txt
			echo "*************************** HARVEST: OLD" >> /tmp/email.txt
			echo "" >> /tmp/email.txt
		
			echo "" >> /tmp/email.txt
			echo "-----HARVEST: ERROR; APPROVED: TRUE" >> /tmp/email.txt
			echo "" >> /tmp/email.txt
		
			/usr/bin/mongo --quiet $MONGODB:$MONGODP/ingest --eval \
			'db.source.find({"harvest.harvest_status":"error",isApproved:true},{_id:0,url:1,"harvest.harvest_message":1}).forEach(printjson);' \
				>> /tmp/email.txt
	
			echo "" >> /tmp/email.txt
			echo "-----HARVEST: ERROR; APPROVED: FALSE" >> /tmp/email.txt
			echo "" >> /tmp/email.txt
			
			/usr/bin/mongo --quiet $MONGODB:$MONGODP/ingest --eval \
			'db.source.find({"harvest.harvest_status":"error",isApproved:false},{_id:0,url:1,"harvest.harvest_message":1}).forEach(printjson);' \
				>> /tmp/email.txt
	
			echo "" >> /tmp/email.txt
			echo "-----HARVEST: SUCCESS; APPROVED: FALSE" >> /tmp/email.txt
			echo "" >> /tmp/email.txt
	
			/usr/bin/mongo --quiet $MONGODB:$MONGODP/ingest --eval \
			'db.source.find({"harvest.harvest_status":"success",isApproved:false},{_id:0,url:1,"harvest.harvest_message":1}).forEach(printjson);' \
				>> /tmp/email.txt
				
			sendmail $TOUSER < /tmp/email.txt
		fi
	fi
fi

