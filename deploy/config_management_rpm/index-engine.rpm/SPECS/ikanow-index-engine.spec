###########################################################################
# Spec file for IKANOW index 
Summary: IKANOW index engine SOLR API
Name: ikanow-index-engine
Version: INFINITE_VERSION
Release: INFINITE_RELEASE
Requires: elasticsearch >= 1.4, ikanow-config, python-simplejson
License: None
Group: ikanow
BuildArch: noarch
Prefix: /mnt/opt

%description
IKANOW index engine using ElasticSearch

###########################################################################
# SCRIPTLETS, IN ORDER OF EXECUTION
%prep
	# (create build files)
	zcat $RPM_SOURCE_DIR/ikanow-index-engine.tgz | tar -xvf -

%pre
	if [ $1 -eq 2 ]; then

###########################################################################
# PRE: THIS IS AN UPGRADE
		service infinite-index-engine stop || :
	fi
	
###########################################################################
# PRE: INSTALL *AND* UPGRADE

	# VERSION 1.x additional steps
	if [ -d /usr/share/elasticsearch/ ]; then
		# link old link for bw compatibility:
		cd /usr/share/java
		rm -rf /usr/share/java/elasticsearch
		ln -sf /usr/share/elasticsearch/
	fi	
	
%post

###########################################################################
# POST: INSTALL *AND* UPGRADE

	if [ -d /etc/security ]; then
		if [ -f /etc/security/limits.conf ]; then
			sed -i /"^elasticsearch.*"/d /etc/security/limits.conf
		fi
		
		echo "elasticsearch    soft    nofile          262144" >> /etc/security/limits.conf
		echo "elasticsearch    hard    nofile          262144" >> /etc/security/limits.conf
		echo "elasticsearch    -       memlock         unlimited" >> /etc/security/limits.conf
	fi

	# VERSION 1.x additional steps
	if [ -d /usr/share/elasticsearch/ ]; then
		mkdir -p /usr/share/elasticsearch/plugins
	
		# install plugins:
		# (temporary, will be done in preinstall code once 1.0 becomes the default)
		cd /usr/share/elasticsearch/plugins

		# Check between 1.0 and 1.3
		if ls /usr/share/elasticsearch/lib/ | grep -q -F "elasticsearch-1.0"; then
		
			rm -rf analysis-icu
			unzip /mnt/opt/elasticsearch-infinite/plugins/1.0/analysis-icu.zip
			rm -rf bigdesk
			unzip /mnt/opt/elasticsearch-infinite/plugins/1.0/bigdesk.zip
			rm -rf head
			unzip /mnt/opt/elasticsearch-infinite/plugins/1.0/head.zip
			
			USE_AWS=`grep "^use.aws=" /mnt/opt/infinite-home/config/infinite.service.properties | sed s/'use.aws='// | sed s/' '//g`
			rm -rf cloud-aws
			if [ "$USE_AWS" = "1" ]; then
				unzip /mnt/opt/elasticsearch-infinite/plugins/1.0/cloud-aws.zip
			fi
			
			#(re-)install compatibility layer
			yes | cp /mnt/opt/elasticsearch-infinite/plugins/1.0/elasticsearch_compatibility.jar /usr/share/elasticsearch/lib
			
		elif ls /usr/share/elasticsearch/lib/ | grep -q -F "elasticsearch-1.3"; then
			
			rm -rf analysis-icu
			unzip /mnt/opt/elasticsearch-infinite/plugins/1.3/analysis-icu.zip
			rm -rf bigdesk
			unzip /mnt/opt/elasticsearch-infinite/plugins/1.3/bigdesk.zip
			rm -rf head
			unzip /mnt/opt/elasticsearch-infinite/plugins/1.3/head.zip
							
			USE_AWS=`grep "^use.aws=" /mnt/opt/infinite-home/config/infinite.service.properties | sed s/'use.aws='// | sed s/' '//g`
			rm -rf cloud-aws
			if [ "$USE_AWS" = "1" ]; then
				unzip /mnt/opt/elasticsearch-infinite/plugins/1.3/cloud-aws.zip
			fi
			
			#(re-)install compatibility layer
			yes | cp /mnt/opt/elasticsearch-infinite/plugins/1.3/elasticsearch_compatibility.jar /usr/share/elasticsearch/lib
			
		elif ls /usr/share/elasticsearch/lib/ | grep -q -F "elasticsearch-1.4"; then
			
			rm -rf analysis-icu
			unzip /mnt/opt/elasticsearch-infinite/plugins/1.4/analysis-icu.zip
			rm -rf bigdesk
			unzip /mnt/opt/elasticsearch-infinite/plugins/1.4/bigdesk.zip
			rm -rf head
			unzip /mnt/opt/elasticsearch-infinite/plugins/1.4/head.zip
			rm -rf elasticsearch-hdfs
			unzip /mnt/opt/elasticsearch-infinite/plugins/1.4/elasticsearch-hdfs.zip
							
			USE_AWS=`grep "^use.aws=" /mnt/opt/infinite-home/config/infinite.service.properties | sed s/'use.aws='// | sed s/' '//g`
			rm -rf cloud-aws
			if [ "$USE_AWS" = "1" ]; then
				unzip /mnt/opt/elasticsearch-infinite/plugins/1.4/cloud-aws.zip
			fi
			
			#(re-)install compatibility layer
			yes | cp /mnt/opt/elasticsearch-infinite/plugins/1.4/elasticsearch_compatibility.jar /usr/share/elasticsearch/lib
		fi 		
		
	else
		#(re-)install compatibility layer
		yes | cp /mnt/opt/elasticsearch-infinite/plugins/0.19/elasticsearch_compatibility.jar /usr/share/java/elasticsearch/lib		
	fi

###########################################################################
# POST: INSTALL *AND* UPGRADE
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
	#(do data and backups separately to handle the upgrade case)
	if [ ! -d /opt/elasticsearch-infinite/data ]; then
		if [ -d /raidarray ]; then
			mkdir -p /raidarray/elasticsearch-data
			chown -R elasticsearch.elasticsearch /raidarray/elasticsearch-data
			ln -sf /raidarray/elasticsearch-data /opt/elasticsearch-infinite/data
			chown -h elasticsearch.elasticsearch /opt/elasticsearch-infinite/data
		else
			mkdir /opt/elasticsearch-infinite/data
			chown -R elasticsearch.elasticsearch /opt/elasticsearch-infinite/data
		fi
	fi
	if [ ! -d /opt/elasticsearch-infinite/backups ]; then
		if [ -d /raidarray ]; then
			mkdir -p /raidarray/elasticsearch-backups
			chown -R elasticsearch.elasticsearch /raidarray/elasticsearch-backups
			ln -sf /raidarray/elasticsearch-backups /opt/elasticsearch-infinite/backups
			chown -h elasticsearch.elasticsearch /opt/elasticsearch-infinite/backups
		else
			mkdir /opt/elasticsearch-infinite/backups
			chown -R elasticsearch.elasticsearch /opt/elasticsearch-infinite/backups
		fi
	fi
	
	# (Add ES instance to start-up-on-boot list)
	chkconfig --add infinite-index-engine
	chkconfig infinite-index-engine on
	
	# (service started in posttrans)

%preun
	if [ $1 -eq 0 ]; then

###########################################################################
# PREUN: THIS IS AN *UN"INSTALL NOT AN UPGRADE

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
# PRE-UN: UNINSTALL *AND* UPGRADE


%posttrans

###########################################################################
# POST-TRANS: FINAL STEP FOR INSTALLS AND UPGRADES
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
%dir /mnt/opt/elasticsearch-infinite/scripts
%dir /mnt/opt/elasticsearch-infinite/plugins
#(mostly temporary):
/mnt/opt/elasticsearch-infinite/plugins/1.4/analysis-icu.zip
/mnt/opt/elasticsearch-infinite/plugins/1.4/bigdesk.zip
/mnt/opt/elasticsearch-infinite/plugins/1.4/cloud-aws.zip
/mnt/opt/elasticsearch-infinite/plugins/1.4/head.zip
/mnt/opt/elasticsearch-infinite/plugins/1.4/elasticsearch-hdfs.zip
/mnt/opt/elasticsearch-infinite/plugins/1.3/analysis-icu.zip
/mnt/opt/elasticsearch-infinite/plugins/1.3/bigdesk.zip
/mnt/opt/elasticsearch-infinite/plugins/1.3/head.zip
/mnt/opt/elasticsearch-infinite/plugins/1.3/cloud-aws.zip
/mnt/opt/elasticsearch-infinite/plugins/1.0/analysis-icu.zip
/mnt/opt/elasticsearch-infinite/plugins/1.0/bigdesk.zip
/mnt/opt/elasticsearch-infinite/plugins/1.0/head.zip
/mnt/opt/elasticsearch-infinite/plugins/1.0/cloud-aws.zip
/mnt/opt/elasticsearch-infinite/plugins/1.4/elasticsearch_compatibility.jar
/mnt/opt/elasticsearch-infinite/plugins/1.3/elasticsearch_compatibility.jar
/mnt/opt/elasticsearch-infinite/plugins/1.0/elasticsearch_compatibility.jar
/mnt/opt/elasticsearch-infinite/plugins/0.19/elasticsearch_compatibility.jar

%config %attr(755,root,root) /etc/init.d/infinite-index-engine
%config /etc/sysconfig/infinite-index-engine
%attr(-,root,root) /etc/cron.d/infinite-index-engine

%attr(755,elasticsearch,elasticsearch) /mnt/opt/elasticsearch-infinite/master_backup_index.sh
%attr(755,elasticsearch,elasticsearch) /mnt/opt/elasticsearch-infinite/scripts/write_es_yml_files.sh
%attr(755,elasticsearch,elasticsearch) /mnt/opt/elasticsearch-infinite/scripts/check_es_indices.sh
%attr(755,elasticsearch,elasticsearch) /mnt/opt/elasticsearch-infinite/scripts/esindexcheck.py
%config /mnt/opt/elasticsearch-infinite/config/elasticsearch.yml.TEMPLATE
%config /mnt/opt/elasticsearch-infinite/config/logging.yml

%dir /usr/share/java/elasticsearch/plugins/scoringscripts/
/usr/share/java/elasticsearch/plugins/scoringscripts/querydecayscript.jar
