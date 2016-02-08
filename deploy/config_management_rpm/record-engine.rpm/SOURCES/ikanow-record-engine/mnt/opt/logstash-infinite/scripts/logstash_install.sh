#!/bin/bash
# Install via YUM
if [ "$1" != "--norpm" ]; then
	#(so can be called from RPM)
	if test -e /opt/infinite-install/rpms/logstash-1.4.0*; then
		rpm -U /opt/infinite-install/rpms/logstash-1.4.0* /opt/infinite-install/rpms/logstash-contrib-1.4.0*
	else
		yes | yum install logstash-1.4.0 logstash-contrib-1.4.0
	fi
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
