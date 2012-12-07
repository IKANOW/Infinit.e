###########################################################################
# Spec file for Infinit.e system configuration.
Summary: Infinit.e index engine SOLR API
Name: infinit.e-index-engine
Version: INFINITE_VERSION
Release: INFINITE_RELEASE
Requires: elasticsearch >= 0.18.7, infinit.e-config
License: None
Group: Infinit.e
BuildArch: noarch
Prefix: /mnt/opt

%description
Infinit.e index engine using ElasticSearch

###########################################################################
# SCRIPTLETS, IN ORDER OF EXECUTION
%prep
	# (create build files)
	zcat $RPM_SOURCE_DIR/infinit.e-index-engine.tgz | tar -xvf -

%pre
	if [ $1 -eq 2 ]; then

###########################################################################
# THIS IS AN UPGRADE
		service infinite-index-engine stop || :
	fi
	
%post

###########################################################################
# INSTALL *AND* UPGRADE

	if [ -d /etc/security ]; then
		if [ -f /etc/security/limits.conf ]; then
			sed -i /"^elasticsearch.*"/d /etc/security/limits.conf
		fi
		
		echo "elasticsearch    soft    nofile          262144" >> /etc/security/limits.conf
		echo "elasticsearch    hard    nofile          262144" >> /etc/security/limits.conf
		echo "elasticsearch    -       memlock         unlimited" >> /etc/security/limits.conf
	fi

###########################################################################
# INSTALL *AND* UPGRADE
# Handle relocation:

	if [ "$RPM_INSTALL_PREFIX" != "/opt" ]; then
		echo "(Creating links from /opt to $RPM_INSTALL_PREFIX)"
	 	if [ -d /opt/elasticsearch-infinite ] && [ ! -h /opt/elasticsearch-infinite ]; then
			echo "Error: /opt/elasticsearch-infinite exists"
			exit 1
		else
			ln -sf $RPM_INSTALL_PREFIX/elasticsearch-infinite /opt
			chown -h elasticsearch.elasticsearch /opt/elasticsearch-infinite 
		fi 
	fi
	
	# (Add ES instance to start-up-on-boot list)
	chkconfig --add infinite-index-engine
	chkconfig infinite-index-engine on
	
	# Load AWS plugin:
	USE_AWS=`grep "^use.aws=" /mnt/opt/infinite-home/config/infinite.service.properties | sed s/'use.aws='// | sed s/' '//g`
	if [ "$USE_AWS" = "1" ]; then
		/usr/share/java/elasticsearch/bin/plugin -install cloud-aws
	fi	
	
	# (service started in posttrans)

%preun
	if [ $1 -eq 0 ]; then

###########################################################################
# THIS IS AN *UN"INSTALL NOT AN UPGRADE

		# (Stop ES instance)
		service infinite-index-engine stop
		
		# (Remove ES instance from the start-up-on-boot list)
		chkconfig infinite-index-engine off
		chkconfig --del infinite-index-engine
		
		# Handle relocation:
		if [ "$RPM_INSTALL_PREFIX" != "/opt" ]; then
			if [ -h /opt/elasticsearch-infinite ]; then
				rm /opt/elasticsearch-infinite
			fi
		fi
		
		# Reset limits
		if [ -f /etc/security/limits.conf ]; then
			sed -i /"^elasticsearch.*"/d /etc/security/limits.conf
		fi
		
		# Let the world know I no longer exist
		USE_AWS=`grep "^use.aws=" /mnt/opt/infinite-home/config/infinite.service.properties | sed s/'use.aws='// | sed s/' '//g`
		if [ "$USE_AWS" = "1" ]; then
			this_instance=$(curl -s http://169.254.169.254/1.0/meta-data/instance-id)
			if [ "$this_instance" != "" ]; then
				aws deltags $this_instance --tag index-node >/dev/null
			fi
		fi
	fi

###########################################################################
# UNINSTALL *AND* UPGRADE


%posttrans

###########################################################################
# FINAL STEP FOR INSTALLS AND UPGRADES
	# Start service
	service infinite-index-engine start
	
	# Let the world know I exist
	USE_AWS=`grep "^use.aws=" /mnt/opt/infinite-home/config/infinite.service.properties | sed s/'use.aws='// | sed s/' '//g`
	if [ "$USE_AWS" = "1" ]; then
		this_instance=$(curl -s http://169.254.169.254/1.0/meta-data/instance-id)
		if [ "$this_instance" != "" ]; then
			aws ctags $this_instance --tag index-node=1 >/dev/null
		fi
	fi

###########################################################################
# FILE LISTS

%files
%defattr(-,elasticsearch,elasticsearch)
%dir /mnt/opt/elasticsearch-infinite/
%dir /mnt/opt/elasticsearch-infinite/config
%dir /mnt/opt/elasticsearch-infinite/data
%dir /mnt/opt/elasticsearch-infinite/scripts

%config %attr(755,root,root) /etc/init.d/infinite-index-engine
%config /etc/sysconfig/infinite-index-engine
%attr(-,root,root) /etc/cron.d/infinite-index-engine

%attr(755,elasticsearch,elasticsearch) /mnt/opt/elasticsearch-infinite/master_backup_index.sh
%attr(755,elasticsearch,elasticsearch) /mnt/opt/elasticsearch-infinite/scripts/write_es_yml_files.sh
%config /mnt/opt/elasticsearch-infinite/config/elasticsearch.yml.TEMPLATE
%config /mnt/opt/elasticsearch-infinite/config/logging.yml

%dir /usr/share/java/elasticsearch/plugins/scoringscripts/
/usr/share/java/elasticsearch/plugins/scoringscripts/querydecayscript.jar

