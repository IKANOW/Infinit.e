#!/bin/bash
SCRIPTDIR=/opt/infinite-home/db-scripts

MASTER=`curl -s http://localhost:9200/_cluster/state | grep -o "master_node.:.[^\"]*"| grep -o "[^\"]*$" | grep -o "[^-].*"`
IS_MASTER=$(curl -s http://localhost:9200/_cluster/nodes/_local | grep -q "$MASTER" && echo "true")

if echo $IS_MASTER  | grep -qi "true"; then
	/usr/bin/mongo --quiet ingest --eval "var isMidNoon = $1" $SCRIPTDIR/harvest_redistribution_script.js
fi