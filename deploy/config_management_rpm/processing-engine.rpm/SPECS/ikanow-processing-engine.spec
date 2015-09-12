%define __os_install_post %{nil}
###########################################################################
# Spec file for Infinit.e system configuration.
Summary: IKANOW processing (harvesting, enrichment, generic and custom processing)
Name: ikanow-processing-engine
Version: INFINITE_VERSION
Release: INFINITE_RELEASE
Requires: ikanow-config >= v0.5, ikanow-db-instance, ikanow-index-engine >= v0.5
License: None
Group: ikanow
BuildArch: noarch
Prefix: /mnt/opt

%description
IKANOW  harvesting and cleansing services

#
# SCRIPTLETS, IN ORDER OF EXECUTION
#

%prep
	# (create build files)
	zcat $RPM_SOURCE_DIR/ikanow-processing-engine.tgz | tar -xvf -

%pre
	if [ $1 -eq 2 ]; then
#
# THIS IS AN UPGRADE
#
		service infinite-px-engine stop || :
	fi

%install
#
# INSTALL *AND* UPGRADE
#	
	# Set up symbolic links to JARs
	cd $RPM_BUILD_DIR/mnt/opt/infinite-home/lib
	ln -s -f infinit.e.data_model-INFINITE_VERSION-INFINITE_RELEASE.jar infinit.e.data_model.jar
	ln -s -f infinit.e.harvest.library-INFINITE_VERSION-INFINITE_RELEASE.jar infinit.e.harvest.library.jar
	ln -s -f infinit.e.query.library-INFINITE_VERSION-INFINITE_RELEASE.jar infinit.e.query.library.jar
	ln -s -f infinit.e.processing.custom.library-INFINITE_VERSION-INFINITE_RELEASE.jar infinit.e.processing.custom.library.jar
	ln -s -f infinit.e.processing.generic.library-INFINITE_VERSION-INFINITE_RELEASE.jar infinit.e.processing.generic.library.jar
	ln -s -f infinit.e.core.server-INFINITE_VERSION-INFINITE_RELEASE.jar infinit.e.core.server.jar
	ln -s -f infinit.e.mongo-indexer-INFINITE_VERSION-INFINITE_RELEASE.jar infinit.e.mongo-indexer.jar
	cd $RPM_BUILD_DIR/mnt/opt/infinite-home/lib/plugins
	ln -s -f infinit.e.hadoop.prototyping_engine-INFINITE_VERSION-INFINITE_RELEASE.jar infinit.e.hadoop.prototyping_engine.jar
	
%post
#
# INSTALL *ONLY*
#	
	if [ $1 -eq 1 ]; then
		ln -s /usr/share/java/elasticsearch/lib/ /mnt/opt/infinite-home/lib/es-libs 
	fi
#
# INSTALL *AND* UPGRADE
#	
	# Create symlink to hadoop-infinite if it doesn't already exist
	if [ ! -f /opt/hadoop-infinite ]; then
		ln -s /mnt/opt/hadoop-infinite /opt/hadoop-infinite
	fi
	
	# Increase tomcat limits
	if [ -d /etc/security ]; then
		if [ -f /etc/security/limits.conf ]; then
			sed -i /"^tomcat.*"/d /etc/security/limits.conf
		fi
		
		echo "tomcat    soft    nofile          65536" >> /etc/security/limits.conf
		echo "tomcat    hard    nofile          65536" >> /etc/security/limits.conf
		echo "tomcat    soft    nproc          4096" >> /etc/security/limits.conf
		echo "tomcat    hard    nproc          4096" >> /etc/security/limits.conf
	fi

	# Install Hadoop prototyping engine
	mongo custommr /mnt/opt/infinite-home/db-scripts/hadoop_prototype_engine.js || echo "Mongo not configured, couldn't load Hadoop Prototyping Engine"

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
	fi
	# (Add infinite instance to start-up-on-boot list)
	chkconfig --add infinite-px-engine
	chkconfig infinite-px-engine on
	# (service started in posttrans)

	# Ensure we have the most up-to-date version of es compat layer (since index-engine is not normally upgraded unless es version changes)
	if [ -d /usr/share/elasticsearch/ ]; then
		# Check between 1.0 and 1.3
		if ls /usr/share/elasticsearch/lib/ | grep -q -F "elasticsearch-1.0"; then
			yes | cp /mnt/opt/infinite-home/es-compat/1.0/elasticsearch_compatibility.jar /usr/share/elasticsearch/lib	
		elif ls /usr/share/elasticsearch/lib/ | grep -q -F "elasticsearch-1.3"; then
			yes | cp /mnt/opt/infinite-home/es-compat/1.3/elasticsearch_compatibility.jar /usr/share/elasticsearch/lib	
		elif ls /usr/share/elasticsearch/lib/ | grep -q -F "elasticsearch-1.4"; then
			yes | cp /mnt/opt/infinite-home/es-compat/1.4/elasticsearch_compatibility.jar /usr/share/elasticsearch/lib	
		fi 		
	else
		yes | cp /mnt/opt/infinite-home/es-compat/0.19/elasticsearch_compatibility.jar /usr/share/java/elasticsearch/lib	
	fi		

%preun
	if [ $1 -eq 0 ]; then
#
# THIS IS AN *UN"INSTALL NOT AN UPGRADE
#
		# (Stop instance)
		service infinite-px-engine stop
		# (Remove instance from the start-up-on-boot list)
		chkconfig infinite-px-engine off
		chkconfig --del infinite-px-engine
		# Remove symbolic link to ES library
		rm -f /mnt/opt/infinite-home/lib/es-libs
	fi

%postun
	# (Nothing to do)

%posttrans
#
# FINAL STEP FOR INSTALLS AND UPGRADES
#
	# Ensure latest mapping installed
	sh /opt/infinite-home/scripts/load_custom_template_into_es.sh

	# Start service
	service infinite-px-engine start

	#Fix for old mongo jar left in Hadoop folder:
	rm -f /mnt/opt/hadoop-infinite/jars/mongo-2.7.2.jar
	rm -f /usr/lib/hadoop/lib/mongo-2.7.2.jar

###########################################################################
%files
%defattr(-,tomcat,tomcat)
%attr(755,root,root) /etc/init.d/infinite-px-engine
%attr(-,root,root) /etc/cron.d/infinite-px-engine
%dir /mnt/opt/infinite-home
%dir /mnt/opt/infinite-home/bin
/mnt/opt/infinite-home/bin/STOPFILE
%attr(755,tomcat,tomcat) /mnt/opt/infinite-home/bin/custommr.sh
%attr(755,tomcat,tomcat) /mnt/opt/infinite-home/scripts/setup_hadoop.sh
%attr(755,tomcat,tomcat) /mnt/opt/infinite-home/bin/reindex_from_db.sh
%attr(755,tomcat,tomcat) /mnt/opt/infinite-home/bin/infinite_indexer.sh
%config %attr(755,tomcat,tomcat) /mnt/opt/infinite-home/bin/do_harvest_cycle.sh
%config %attr(755,tomcat,tomcat) /mnt/opt/infinite-home/bin/infinite-px-engine.sh
%config %attr(755,tomcat,tomcat) /mnt/opt/infinite-home/bin/sync_features.sh
%config %attr(755,tomcat,tomcat) /mnt/opt/infinite-home/bin/generate_temporal_aggregations.sh
%config %attr(755,tomcat,tomcat) /mnt/opt/infinite-home/bin/weekly_sources_report.sh
%config %attr(755,tomcat,tomcat) /mnt/opt/infinite-home/bin/reset_bad_harvest.sh
%config %attr(755,tomcat,tomcat) /mnt/opt/infinite-home/bin/harvest_redistribute.sh
%dir /mnt/opt/infinite-home/db-scripts
/mnt/opt/infinite-home/db-scripts/calc_freq_counts.js
/mnt/opt/infinite-home/db-scripts/reset_bad_harvest.js
/mnt/opt/infinite-home/db-scripts/update_doc_counts.js
/mnt/opt/infinite-home/db-scripts/rebuild_entity_feature.js
/mnt/opt/infinite-home/db-scripts/rebuild_assoc_feature.js
/mnt/opt/infinite-home/db-scripts/hadoop_prototype_engine.js
/mnt/opt/infinite-home/db-scripts/temporal_entity_aggregation.js
/mnt/opt/infinite-home/db-scripts/harvest_redistribution_script.js
%attr(755,tomcat,tomcat) /mnt/opt/infinite-home/scripts/load_custom_template_into_es.sh
%attr(755,tomcat,tomcat) /mnt/opt/infinite-home/scripts/partially_update_doc_mapping.sh
%attr(755,tomcat,tomcat) /mnt/opt/infinite-home/scripts/check_for_stuck_jobs.sh
/mnt/opt/infinite-home/templates/elasticsearch-inf-template.json
/mnt/opt/infinite-home/templates/doc_mapping_changes.json

#(es compatibility)
%attr(-,elasticsearch,elasticsearch) /mnt/opt/infinite-home/es-compat/1.4/elasticsearch_compatibility.jar
%attr(-,elasticsearch,elasticsearch) /mnt/opt/infinite-home/es-compat/1.3/elasticsearch_compatibility.jar
%attr(-,elasticsearch,elasticsearch) /mnt/opt/infinite-home/es-compat/1.0/elasticsearch_compatibility.jar
%attr(-,elasticsearch,elasticsearch) /mnt/opt/infinite-home/es-compat/0.19/elasticsearch_compatibility.jar

%dir /mnt/opt/infinite-home/lib
%dir /mnt/opt/infinite-home/lib/extractors
%dir /mnt/opt/infinite-home/lib/unbundled
%dir /mnt/opt/infinite-home/lib/plugins
/mnt/opt/infinite-home/lib/infinit.e.data_model.jar
/mnt/opt/infinite-home/lib/infinit.e.harvest.library.jar
/mnt/opt/infinite-home/lib/infinit.e.query.library.jar
/mnt/opt/infinite-home/lib/infinit.e.processing.custom.library.jar
/mnt/opt/infinite-home/lib/infinit.e.processing.generic.library.jar
/mnt/opt/infinite-home/lib/infinit.e.core.server.jar
/mnt/opt/infinite-home/lib/infinit.e.mongo-indexer.jar
/mnt/opt/infinite-home/lib/infinit.e.data_model-INFINITE_VERSION-INFINITE_RELEASE.jar
/mnt/opt/infinite-home/lib/infinit.e.harvest.library-INFINITE_VERSION-INFINITE_RELEASE.jar
/mnt/opt/infinite-home/lib/infinit.e.query.library-INFINITE_VERSION-INFINITE_RELEASE.jar
/mnt/opt/infinite-home/lib/infinit.e.processing.custom.library-INFINITE_VERSION-INFINITE_RELEASE.jar
/mnt/opt/infinite-home/lib/infinit.e.processing.generic.library-INFINITE_VERSION-INFINITE_RELEASE.jar
/mnt/opt/infinite-home/lib/infinit.e.core.server-INFINITE_VERSION-INFINITE_RELEASE.jar
/mnt/opt/infinite-home/lib/infinit.e.mongo-indexer-INFINITE_VERSION-INFINITE_RELEASE.jar
/mnt/opt/infinite-home/lib/unbundled/jcifs-1.3.17.jar
/mnt/opt/infinite-home/lib/unbundled/tika-app-1.0_gson-2.2.2_sl4f-1.7.7.jar
/mnt/opt/infinite-home/lib/unbundled/GridFSZipFile.jar
/mnt/opt/infinite-home/lib/unbundled/j-calais-0.2.1-jar-with-dependencies.jar
/mnt/opt/infinite-home/lib/plugins/infinit.e.hadoop.prototyping_engine.jar
/mnt/opt/infinite-home/lib/plugins/infinit.e.hadoop.prototyping_engine-INFINITE_VERSION-INFINITE_RELEASE.jar

# Merge of hadoop rpm

%dir /mnt/opt/hadoop-infinite/
%dir /mnt/opt/hadoop-infinite/mapreduce/
%dir /mnt/opt/hadoop-infinite/mapreduce/hadoop/
%dir /mnt/opt/hadoop-infinite/jars/
%dir /mnt/opt/hadoop-infinite/scripts/

###########################################################################
# jars
###########################################################################

%dir /mnt/opt/hadoop-infinite/lib_hadoop/
/mnt/opt/hadoop-infinite/lib_hadoop/avro.jar
/mnt/opt/hadoop-infinite/lib_hadoop/commons-cli-1.2.jar
/mnt/opt/hadoop-infinite/lib_hadoop/commons-configuration-1.6.jar
/mnt/opt/hadoop-infinite/lib_hadoop/hadoop-annotations-2.5.0-cdh5.3.1.jar
/mnt/opt/hadoop-infinite/lib_hadoop/hadoop-auth-2.5.0-cdh5.3.1.jar
/mnt/opt/hadoop-infinite/lib_hadoop/hadoop-common-2.5.0-cdh5.3.1.jar
/mnt/opt/hadoop-infinite/lib_hadoop/hadoop-core-2.5.0-mr1-cdh5.3.1.jar
/mnt/opt/hadoop-infinite/lib_hadoop/hadoop-hdfs-2.5.0-cdh5.3.1.jar
/mnt/opt/hadoop-infinite/lib_hadoop/jackson-all-1.8.10.jar
/mnt/opt/hadoop-infinite/lib_hadoop/protobuf-java-2.5.0.jar
/mnt/opt/hadoop-infinite/lib_hadoop/slf4j-api-1.7.12.jar

%dir /mnt/opt/hadoop-infinite/lib_yarn/
/mnt/opt/hadoop-infinite/lib_yarn/commons-configuration-1.6.jar
/mnt/opt/hadoop-infinite/lib_yarn/protobuf-java-2.5.0.jar
/mnt/opt/hadoop-infinite/lib_yarn/hadoop-mapreduce-client-shuffle-2.6.0.2.2.4.2-2.jar
/mnt/opt/hadoop-infinite/lib_yarn/avro.jar
/mnt/opt/hadoop-infinite/lib_yarn/jackson-core-asl-1.9.13.jar
/mnt/opt/hadoop-infinite/lib_yarn/jaxb-api-2.2.2.jar
/mnt/opt/hadoop-infinite/lib_yarn/hadoop-mapreduce-client-common-2.6.0.2.2.4.2-2.jar
/mnt/opt/hadoop-infinite/lib_yarn/jackson-xc-1.9.13.jar
/mnt/opt/hadoop-infinite/lib_yarn/hadoop-yarn-api-2.6.0.2.2.4.2-2.jar
/mnt/opt/hadoop-infinite/lib_yarn/jsr305-1.3.9.jar
/mnt/opt/hadoop-infinite/lib_yarn/commons-cli-1.2.jar
/mnt/opt/hadoop-infinite/lib_yarn/jackson-mapper-asl-1.9.13.jar
/mnt/opt/hadoop-infinite/lib_yarn/jackson-jaxrs-1.9.13.jar
/mnt/opt/hadoop-infinite/lib_yarn/hadoop-mapreduce-client-core-2.6.0.2.2.4.2-2.jar
/mnt/opt/hadoop-infinite/lib_yarn/htrace-core-3.0.4.jar
/mnt/opt/hadoop-infinite/lib_yarn/hadoop-yarn-common-2.6.0.2.2.4.2-2.jar
/mnt/opt/hadoop-infinite/lib_yarn/jersey-client-1.9.jar
/mnt/opt/hadoop-infinite/lib_yarn/hadoop-mapreduce-client-jobclient-2.6.0.2.2.4.2-2.jar
/mnt/opt/hadoop-infinite/lib_yarn/jersey-core-1.9.jar
/mnt/opt/hadoop-infinite/lib_yarn/hadoop-hdfs-2.6.0.2.2.4.2-2.jar
/mnt/opt/hadoop-infinite/lib_yarn/jetty-util-6.1.26.hwx.jar
/mnt/opt/hadoop-infinite/lib_yarn/hadoop-common-2.6.0.2.2.4.2-2.jar
/mnt/opt/hadoop-infinite/lib_yarn/hadoop-yarn-client-2.6.0.2.2.4.2-2.jar
/mnt/opt/hadoop-infinite/lib_yarn/hadoop-auth-2.6.0.2.2.4.2-2.jar
/mnt/opt/hadoop-infinite/lib_yarn/hadoop-annotations-2.6.0.2.2.4.2-2.jar
/mnt/opt/hadoop-infinite/lib_yarn/jackson-core-2.2.3.jar
/mnt/opt/hadoop-infinite/lib_yarn/slf4j-api-1.7.12.jar

###########################################################################
# Install scripts
###########################################################################
%attr(755,tomcat,tomcat) /mnt/opt/hadoop-infinite/scripts/install_fuse.sh

