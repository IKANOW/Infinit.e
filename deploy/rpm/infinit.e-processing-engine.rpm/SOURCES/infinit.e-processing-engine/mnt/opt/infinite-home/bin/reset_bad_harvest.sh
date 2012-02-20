#!/bin/bash
CONFDIR=/opt/infinite-home/config/
SCRIPTDIR=/opt/infinite-home/db-scripts/
MONGODB=
if [ -d $CONFDIR ]; then
        if [ -f $CONFDIR/infinite.service.properties ]; then
                MONGODB=`grep "^db.server=" $CONFDIR/infinite.service.properties | sed s/'db.server='// | sed s/' '//g`
                MONGODP=`grep "^db.port=" $CONFDIR/infinite.service.properties | sed s/'db.port='// | sed s/' '//g`
        fi
fi

if [ ! -z "$MONGODB" ]; then
        if [ -x /usr/bin/mongo ]; then
            /usr/bin/mongo --quiet $MONGODB:$MONGODP/config $SCRIPTDIR/reset_bad_harvest.js
        fi
fi
