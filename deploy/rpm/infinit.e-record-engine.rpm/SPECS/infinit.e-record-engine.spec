###########################################################################
# Spec file for Infinit.e record engine install
Summary: Infinit.e record engine install
Name: infinit.e-record-engine
Version: INFINITE_VERSION
Release: INFINITE_RELEASE
Requires: infinit.e-interface-engine >= v0.3, elasticsearch >= 1.0
License: None
Group: Infinit.e
BuildArch: noarch
Prefix: /mnt/opt

%description
Infinit.e base enterprise install

###########################################################################
# SCRIPTLETS, IN ORDER OF EXECUTION
%prep
	# (create build files)
	zcat $RPM_SOURCE_DIR/infinit.e-record-engine.tgz | tar -xvf -
	
%pre

%post
	if [ "$RPM_INSTALL_PREFIX" != "/opt" ]; then
		echo "(Creating links from /opt to $RPM_INSTALL_PREFIX)"
	 	if [ -d /opt/logstash-infinite ] && [ ! -h /opt/logstash-infinite ]; then
			echo "Error: /opt/logstash-infinite exists"
			exit 1
		else
			ln -sf $RPM_INSTALL_PREFIX/logstash-infinite /opt
			chown -h tomcat.tomcat /opt/logstash-infinite 
		fi 
	fi
	# Set up new tmp directory for logstash
	if [ -d /raidarray ]; then
		mkdir -p /raidarray/tmp
		chmod 1777 /raidarray/tmp
	else
		mkdir -p /mnt/tmp
		chmod 1777 /mnt/tmp
	fi

%preun
	if [ $1 -eq 0 ]; then
		# Handle relocation:
		if [ "$RPM_INSTALL_PREFIX" != "/opt" ]; then
			if [ -h /opt/elasticsearch-infinite ]; then
				rm /opt/elasticsearch-infinite
			fi
		fi
	fi
	
%posttrans
#
# FINAL STEP FOR INSTALLS AND UPGRADES
#
	if rpm -q logstash > /dev/null; then
		#Logstash RPM already installed:
		sh /mnt/opt/logstash-infinite/scripts/logstash_install.sh --norpm
	fi	

	#Centos5 doesn't support the logstash repo so remove
	 if cat /etc/redhat-release | grep -iq 'release 5'; then
	 	rm -f /etc/yum.repos.d/logstash.repo
	 fi	

	#Update template and ensure recs_dummy exists and has a non-trivial mapping:
	sh /opt/logstash-infinite/scripts/load_custom_template_into_es.sh > /dev/null

	#Insert or update record-oriented widgets
	sh /mnt/opt/logstash-infinite/scripts/insert_or_update_widgets.sh > /dev/null
	
	#Ensure if binary mode is turned off then http versions of templates are used
	USE_BINARY=`grep "^elastic.logstash=" /opt/infinite-install/config/infinite.configuration.properties | sed s/'elastic.logstash='// | sed s/' '//g`
	if [ "$USE_BINARY" != "binary" ]; then
		if [ -d /etc/logstash/conf.d/ ]; then
			for i in `ls /etc/logstash/conf.d/*.auto.conf`; do 
				if ! grep -q 'elasticsearch_http' $i; then 
					echo "Deleting incompatible stream $i (will be regenerated in a few minutes)"
					rm -f $i; 
				fi; 
			done
		fi
	elif [ -d /etc/logstash/conf.d/ ]; then
		#Kill any templates with old-style protocols
		for i in `ls /etc/logstash/conf.d/*.auto.conf`; do 
			if ! grep -q 'protocol => "transport"' $i; then 
				echo "Deleting old stream $i (will be regenerated in a few minutes)"
				rm -f $i; 
			fi; 
		done
	fi
	#(will be regenerated on restart)	
	
	# Kill existing folder if it exists
	rm -rf /mnt/opt/tomcat-infinite/interface-engine/webapps/infinit.e.records
	# Restart hosting service (tomcat), unless just restarted
	find /var/run/ -name "tomcat6-interface-engine.pid" -mmin +10 | grep -q pid && service tomcat6-interface-engine restart
	true
	
###########################################################################
# FILE LISTS

%files
%defattr(-,tomcat,tomcat)

%attr(-,root,root) /etc/cron.d/infinite-logstash
%attr(-,root,root) /etc/yum.repos.d/logstash.repo
/etc/profile.d/infinite-logstash.sh

%dir /mnt/opt/logstash-infinite
%dir /mnt/opt/logstash-infinite/scripts
%dir /mnt/opt/logstash-infinite/templates

%attr(755,tomcat,tomcat) /mnt/opt/logstash-infinite/scripts/logstash_install.sh
%attr(755,tomcat,tomcat) /mnt/opt/logstash-infinite/scripts/insert_or_update_widgets.sh
%attr(755,tomcat,tomcat) /mnt/opt/logstash-infinite/scripts/remove_logstash_tmpfiles.sh
%attr(755,tomcat,tomcat) /mnt/opt/logstash-infinite/scripts/load_custom_template_into_es.sh
%config /mnt/opt/logstash-infinite/templates/elasticsearch-inf-template.json
%config /mnt/opt/logstash-infinite/templates/test-output-template.conf
%config /mnt/opt/logstash-infinite/templates/transient-record-output-template.conf
%config /mnt/opt/logstash-infinite/templates/stashed-record-output-template.conf
%config /mnt/opt/logstash-infinite/templates/transient-record-output-template_http.conf
%config /mnt/opt/logstash-infinite/templates/stashed-record-output-template_http.conf
%config /mnt/opt/logstash-infinite/templates/etc_sysconfig_logstash

/mnt/opt/tomcat-infinite/interface-engine/webapps/infinit.e.records.war
