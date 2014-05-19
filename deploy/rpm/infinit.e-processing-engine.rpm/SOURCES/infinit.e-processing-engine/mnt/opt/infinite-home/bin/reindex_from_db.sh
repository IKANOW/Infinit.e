#!/bin/bash
#
# "Usage reindex_from_db.sh --doc|--events|--entity [--rebuild] <stepsize> <saved_state>"
# reindexes the DB in chunks

if [ $# -lt 3 ]
then
  echo "Usage reindex_from_db.sh --doc|--event|--entity [--rebuild] <stepsize> <saved_state>"
  exit -1
fi

TYPE=$1
shift

REBUILD=""
if [ "$1" = "--rebuild" ]; then
	REBUILD="--rebuild"
	shift
fi

LIMIT=$1
STATE=$2
DONE="$STATE.log"

#
# INDEX DOCS
#

# Get start position
START=0
if [ -f $STATE ]; then
	START=`tail -n 1 $STATE`
	if [ "$STATE" = "Done" ]; then
		exit 0;
	fi
	START=$(($START+$LIMIT))
fi

echo "Starting at $START"

while [ 1 ]; do
	if [ "$START" != "0" ]; then
		REBUILD=""
	else
		rm -f $DONE
	fi 
	echo "Chunk starting at $START"
	/opt/infinite-home/bin/infinite_indexer.sh $TYPE $REBUILD --skip $START --limit $LIMIT | tee -a $DONE || exit -1
	echo $START >> $STATE
	if grep "^Found 0 records to sync" $DONE; then
		echo "Done" >> $STATE
		exit 0
	fi 
	START=$(($START+$LIMIT))
done
