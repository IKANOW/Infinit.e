#!/bin/bash
# Install via YUM
yes | yum install logstash logstash-contrib
chkconfig logstash off 
chown -R tomcat.tomcat /opt/logstash
# (installs to /opt)
#OR DO THIS VIA RPM?

#(just for posterity, this is the command needed for the tar.gz downlaoded from elasticsearch not via rpm)
#tar xzvf /opt/logstash-1.4.0/logstash-contrib-1.4.0.tar.gz --strip-components 1

#Link the logstash to infinit.e
cd /opt/logstash-infinite
ln -s /opt/logstash
chown -h tomcat.tomcat logstash

#TO INSTALL THE LOGSTASH HARVESTER, SIMPLY:
if [ "$1" == "full" ]; then
	cd /opt/logstash-infinite
	ln -s /etc/logstash/conf.d/ logstash.conf.d
	chmod a+w /etc/logstash/conf.d/
	chmod a+w /opt/logstash
	
	#(needed for geoip support)
	cd /var/lib/logstash
	ln -s /opt/logstash/vendor
	
	#Configure logstash to start on boot (and then start it up)
	chkconfig logstash on
	/etc/init.d/logstash start	
fi