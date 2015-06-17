#!/bin/bash

# Get relevant environment vars

MASTER=`curl -s http://localhost:9200/_cluster/state | grep  -o "master_node.:.[^\"]*"| grep -o "[^\"]*$" | grep -o "[^-].*"`
IS_MASTER=$(curl -s http://localhost:9200/_nodes/_local | grep -q "$MASTER" && echo "true")

#Remove any jobs that are more than 10 minutes old:

if echo $IS_MASTER  | grep -qi "true"; then
	if [ -x /usr/bin/mongo ]; then
		mongo custommr --eval 'db.customlookup.remove({jobidS:"CHECKING_COMPLETION", lastChecked: {$lt: new Date(new Date().getTime() - 600000)}})'		
	fi
fi