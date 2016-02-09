#!/bin/bash
# Install via YUM
if [ "$1" != "--norpm" ]; then
	yes | yum install logstash-2.1.2
else
    shift
fi

chkconfig logstash off 
chown -R tomcat.tomcat /opt/logstash
# (installs to /opt)

#Link the logstash to infinit.e
rm -f /opt/logstash-infinite/logstash > /dev/null
cd /opt/logstash-infinite
ln -s /opt/logstash
chown -h tomcat.tomcat logstash
/bin/cp /opt/logstash-infinite/templates/etc_sysconfig_logstash /etc/sysconfig/logstash

#Install/update the plugins: (note this is duplicated in the rpm)
export JAVA_HOME=/usr/java/default
/opt/logstash/bin/plugin unpack /mnt/opt/logstash-infinite/plugins/plugins_package.tar.gz
/opt/logstash/bin/plugin install --local --no-verify /mnt/opt/logstash-infinite/plugins/logstash-output-mongodb-2.0.3.gem
#(don't try to install webhdfs for now)
#/opt/logstash/bin/plugin install --local --no-verify /mnt/opt/logstash-infinite/plugins/logstash-output-webhdfs-2.0.2.gem
/opt/logstash/bin/plugin install --local --no-verify /mnt/opt/logstash-infinite/plugins/logstash-output-s3-2.0.4.gem
#(See above)
#/bin/find /opt/logstash/vendor/local_gems -name "logstash-output-webhdfs.gemspec"|xargs sed -i 's/ö/o/g'

#TO INSTALL THE LOGSTASH HARVESTER, SIMPLY:
if [[ "$1" == "full" || "$1" == "slave" ]]; then
	
	if [ "$1" == "full" ]; then
		rm -f /opt/logstash-infinite/logstash.conf.d > /dev/null
		cd /opt/logstash-infinite
		ln -s /etc/logstash/conf.d/ logstash.conf.d
	else
		rm -f /opt/logstash-infinite/dist.logstash.conf.d > /dev/null
		cd /opt/logstash-infinite
		ln -s /etc/logstash/conf.d/ dist.logstash.conf.d
	fi
	chmod a+w /etc/logstash/conf.d/
	chmod a+w /opt/logstash
	
	#(needed for geoip support)
	rm -f /var/lib/logstash/vendor > /dev/null
	cd /var/lib/logstash
	ln -s /opt/logstash/vendor
	
	#Configure logstash to start on boot (and then start it up)
	chkconfig logstash on
	/etc/init.d/logstash start	
fi
