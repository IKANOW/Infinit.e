#!/bin/sh
################################################################################
CRONPATH="/etc/cron.d"
TEMPLATE="# /etc/cron.d/infinite: infinite crontab\n\

# Unlike any other crontab you don't have to run the \`crontab'\n\
# command to install the new version when you edit this file\n\
# since it is in /etc/cron.d. These files also have username fields,\n\
# that none of the other crontabs do.\n\
\
# use full cron commands after a cron modification\n\
# stop the cron service\n\
# sudo stop cron\n\
# start the cron service\n\
# sudo start cron\n\
# verify the cron log\n\
# tail -f /var/log/syslog\n\
# verify the infinite log \n\
# tail -f /opt/infinite-home/logs/logfile.log\n\
\
SHELL=/bin/sh\n\
PATH=/usr/local/sbin:/usr/local/bin:/sbin:/bin:/usr/sbin:/usr/bin\n\
\n\
# m h dom mon dow user  command\n\
# For the (MT) midnight one, backup the slave database\n\
# This assumes that a ebs volume or nfs volume has been mounted\n";

file="$CRONPATH/infinite-db"
balancer_stopper="false"

echo -e $TEMPLATE > $file

if [ $(infdb is_config) == "true" ]; then 
	balancer_stopper="true"
	echo "01 01    * * *   root    /opt/db-home/backup-script.sh 27016" >> $file
fi

if [ $(infdb is_dbinstance) == "true" ]; then
	balancer_stopper="true"
	repl_sets=$(infdb repl_sets)
	for x in $repl_sets
	do
		port=$((27017+$x))
		echo "01 01    * * *   root    /opt/db-home/backup-script.sh $port" >> $file
	done
fi

if [ $(echo $balancer_stopper) == "true" ]; then
	# Start balancer back up in an hour
	echo "00 03    * * *   root    /opt/db-home/start_balancer.sh" >> $file
fi


echo "# For the (MT) midnight one, do more - used to rotate the mongodb logs" >> $file
echo "00 01    * * *   root    /opt/db-home/rotate-logs-script.sh" >> $file
