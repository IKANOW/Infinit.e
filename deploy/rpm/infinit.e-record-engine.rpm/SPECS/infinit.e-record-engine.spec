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
	#Insert or update record-oriented widgets
	sh /mnt/opt/logstash-infinite/scripts/insert_or_update_widgets.sh > /dev/null
	
	# Kill existing folder if it exists
	rm -rf /mnt/opt/tomcat-infinite/interface-engine/webapps/infinit.e.records
	# Restart hosting service (tomcat)
	service tomcat6-interface-engine restart
	
###########################################################################
# FILE LISTS

%files
%defattr(-,tomcat,tomcat)

%attr(-,root,root) /etc/cron.d/infinite-logstash
%attr(-,root,root) /etc/yum.repos.d/logstash.repo

%dir /mnt/opt/logstash-infinite
%dir /mnt/opt/logstash-infinite/scripts
%dir /mnt/opt/logstash-infinite/templates

/mnt/opt/logstash-infinite/scripts/logstash_online_install.sh
/mnt/opt/logstash-infinite/scripts/insert_or_update_widgets.sh
%config /mnt/opt/logstash-infinite/templates/elasticsearch-inf-template.json
%config /mnt/opt/logstash-infinite/templates/test-output-template.conf
%config /mnt/opt/logstash-infinite/templates/transient-record-output-template.conf
%config /mnt/opt/logstash-infinite/templates/stashed-record-output-template.conf

/mnt/opt/tomcat-infinite/interface-engine/webapps/infinit.e.records.war
