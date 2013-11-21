#!/bin/sh
#(add 2nd arg to delete specified hostname, eg sh test.sh <hostname> del)
NEW_HOSTNAME=$1
INTERNAL_IP=$(curl curl -s http://169.254.169.254/latest/meta-data/local-ipv4)
if [ ! -z $NEW_HOSTNAME ]; then
	sed -i -r "/\s*$NEW_HOSTNAME(\s|$)/d" /etc/hosts
	if [ ! -z $INTERNAL_IP ] && [ -z $2 ]; then
		echo "$INTERNAL_IP    $NEW_HOSTNAME" >> /etc/hosts
		hostname $NEW_HOSTNAME
	fi	
fi
