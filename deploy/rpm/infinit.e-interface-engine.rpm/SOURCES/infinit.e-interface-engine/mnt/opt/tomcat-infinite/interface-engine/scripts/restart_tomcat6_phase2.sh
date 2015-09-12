#!/bin/bash
#(keep a rolling log of last 100 restarts)
echo "Restarted tomcat at $(date)" >> /tmp/tomcat_restarts
if [ -f /tmp/tomcat_restarts ]; then
	NUM_LINES=$(wc -l /tmp/tomcat_restarts|awk '{print $1}')
	[ $NUM_LINES -gt 100 ] && sed -i "1,$(($NUM_LINES - 100)) d" /tmp/tomcat_restarts
fi

#restart tomcat
/sbin/service tomcat6-interface-engine restart
