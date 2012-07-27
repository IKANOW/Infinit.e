###########################################################################
# Spec file for Infinit.e system configuration.
Summary: Infinit.e system configuration
Name: infinit.e-config
Version: INFINITE_VERSION
Release: INFINITE_RELEASE
Requires: curl iptables
License: None
Group: Infinit.e
BuildArch: noarch
Prefix: /mnt/opt

%description
Infinit.e system configuration

###########################################################################
# SCRIPTLETS, IN ORDER OF EXECUTION
%prep
	# (create build files)
	zcat $RPM_SOURCE_DIR/infinit.e-config.tgz | tar -xvf -

%pre

###########################################################################
# Check to make sure that 
# /opt/infinite-install/config/infinite.configuration.properties exists 
	if [[ ! -f "/opt/infinite-install/config/infinite.configuration.properties" ]]; then
		echo "ERROR: /opt/infinite-install/config/infinite.configuration.properties is missing."
		echo "Create a copy of infinite.configuration.properties.TEMPLATE and update the properties"
		echo "before attempting to install infinit.e-config*.rpm."
		exit -1
	fi

	if [ $1 -eq 2 ]; then
		if [ -f /opt/splunk/bin/splunk ]; then
			# THIS IS AN UPGRADE - (Stop splunk)
			service splunk stop || :
		fi
	fi

	
%install
###########################################################################
# INSTALL *AND* UPGRADE
	# (All files created from the tarball)

%post
###########################################################################
# INSTALL ONLY
	if [ $1 -eq 1 ]; then
		# Create and Save 80 -> 8080 redirect
		iptables -A PREROUTING -t nat -i eth0 -p tcp --dport 80 -j REDIRECT --to-port 8080
		iptables -A OUTPUT -t nat -p tcp -d localhost --dport 80 -j REDIRECT --to-port 8080
		iptables -A OUTPUT -t nat -p tcp -d `hostname` --dport 80 -j REDIRECT --to-port 8080
		iptables -A PREROUTING -t nat -i eth0 -p tcp --dport 443 -j REDIRECT --to-port 8443
		iptables -A OUTPUT -t nat -p tcp -d localhost --dport 443 -j REDIRECT --to-port 8443
		iptables -A OUTPUT -t nat -p tcp -d `hostname` --dport 443 -j REDIRECT --to-port 8443
		service iptables save
	fi
	
###########################################################################
# INSTALL *AND* UPGRADE
	# Handle relocation:
	if [ "$RPM_INSTALL_PREFIX" != "/opt" ]; then
		echo "(Creating links from /opt to $RPM_INSTALL_PREFIX)"
	 	if [ -d /opt/infinite-home ] && [ ! -h /opt/infinite-home ]; then
			echo "Error: /opt/infinite-home exists"
			exit 1
		else
			ln -sf $RPM_INSTALL_PREFIX/infinite-home /opt
			chown -h tomcat.tomcat /opt/infinite-home 
		fi 
		if [ -f /opt/splunk/bin/splunk ]; then
		 	if [ -d /opt/splunk-infinite ] && [ ! -h /opt/splunk-infinite ]; then
				echo "Error: /opt/splunk-infinite exists"
				exit 1
			else
				ln -sf $RPM_INSTALL_PREFIX/splunk-infinite /opt
				chown -h splunk.splunk /opt/splunk-infinite 
			fi
		fi 
	fi
	
	###########################################################################
	# Relocate Splunk data storage
	# This is necessary because Splunk uses the partition of SPLUNK_HOME not
	# SPLUNK_DB in order to decide if disk space is low (in some circumstances)
	if [ -f /opt/splunk/bin/splunk ]; then
		if ! grep -q "^minFreeSpace = 1" /opt/splunk/etc/system/local/server.conf; then
			echo "[diskUsage]" >> /opt/splunk/etc/system/local/server.conf
			echo "minFreeSpace = 1" >> /opt/splunk/etc/system/local/server.conf
		fi
		# Move the DB location, if it hasn't already been moved
		if ! grep -q "^SPLUNK_DB" /opt/splunk/etc/splunk-launch.conf; then
			echo "SPLUNK_DB=/opt/splunk-infinite" >> /opt/splunk/etc/splunk-launch.conf
		fi
	fi
	
	###########################################################################
	# (Add splunk to start-up-on-boot list)
	if [ -f /opt/splunk/bin/splunk ]; then
		chkconfig --add splunk
		chkconfig splunk on
	fi
	
	###########################################################################
	# Rewrite the property files (infinite.api.properties & infinite.service.properties)
	# with values in infinite.configuration.properties
	sh /opt/infinite-home/scripts/rewrite_property_files.sh
	
	###########################################################################
	# (service started in posttrans)

%preun
	if [ $1 -eq 0 ]; then

###########################################################################
# THIS IS AN *UN"INSTALL NOT AN UPGRADE
		# Handle relocation:
		if [ "$RPM_INSTALL_PREFIX" != "/opt" ]; then
			if [ -h /opt/infinite-home ]; then
				rm /opt/infinite-home
			fi
		fi
		if [ -f /opt/splunk/bin/splunk ]; then
			###########################################################################
			# (Stop splunk)
			service splunk stop
			###########################################################################
			# (Remove splunk from the start-up-on-boot list)
			chkconfig splunk off
			chkconfig --del splunk
		fi
	fi

%postun
###########################################################################
# (Nothing to do)

%posttrans

###########################################################################
# FINAL STEP FOR INSTALLS AND UPGRADES
	if [ -f /opt/splunk/bin/splunk ]; then
		# (Start splunk up)
		service splunk start
	fi

###########################################################################
# Get node discovery mode from infinite.service.properties-elastic.node.discovery
###########################################################################
	PROPERTY_FILE='/opt/infinite-install/config/infinite.configuration.properties'
	USE_AWS=`grep "^use.aws=" $PROPERTY_FILE | sed s/'use.aws='// | sed s/' '//g`
	echo "ES USE_AWS = $USE_AWS"
	# Only run if this is an EC2 backed install
	if [ "$USE_AWS" = "1" ]; then	
		echo "Set EC2 Cluster Name and Cluster Name in properties file"
		sh /opt/infinite-home/scripts/set_cluster.sh
	fi

###########################################################################
# FILE LISTS

%files
%defattr(-,tomcat,tomcat)

# (Infinite config)
%dir /mnt/opt/infinite-home/
%dir /mnt/opt/infinite-home/logs
%dir /mnt/opt/infinite-home/config
%dir /mnt/opt/infinite-home/licenses
%dir /mnt/opt/infinite-home/scripts
%config /mnt/opt/infinite-home/config/infinite.service.properties.TEMPLATE
%config /mnt/opt/infinite-home/config/infinite.api.properties.TEMPLATE
%config /mnt/opt/infinite-home/config/python.cfg.TEMPLATE
%config /mnt/opt/infinite-home/config/log4j.api.properties
%config /mnt/opt/infinite-home/config/log4j.service.properties
%config /mnt/opt/infinite-home/config/event_schema.xml
%config /mnt/opt/infinite-home/licenses/ThirdPartyNotices_Appliance.pdf

/mnt/opt/infinite-home/scripts/AlchemyLimitExceededAlert.python
/mnt/opt/infinite-home/scripts/APITimeAlert.python
/mnt/opt/infinite-home/scripts/WeeklyExtractorStatus.python
/mnt/opt/infinite-home/scripts/WeeklyAPITimeStatus.python
/mnt/opt/infinite-home/scripts/APINumResultsCheck.sh
/mnt/opt/infinite-home/scripts/rewrite_property_files.sh
/mnt/opt/infinite-home/scripts/set_cluster.sh
%attr(-,root,root) /etc/cron.d/infinite-logging

# (Splunk config)
%dir %attr(-,splunk,splunk) /mnt/opt/splunk-infinite
%config %attr(-,splunk,splunk) /opt/splunk/etc/apps/search/local/inputs.conf
%config %attr(755,root,root) /etc/init.d/splunk

# S3 config
%config %attr(600,root,root) /root/.s3cfg
%config %attr(600,root,root) /root/.awssecret
%config %attr(755,root,root) /usr/bin/aws

# system config
%attr(-,root,root) /etc/logrotate.d/mail
