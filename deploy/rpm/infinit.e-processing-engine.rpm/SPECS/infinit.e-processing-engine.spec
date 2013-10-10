%define __os_install_post %{nil}
###########################################################################
# Spec file for Infinit.e system configuration.
Summary: Infinit.e processing (harvesting, enrichment, generic and custom processing)
Name: infinit.e-processing-engine
Version: INFINITE_VERSION
Release: INFINITE_RELEASE
Requires: tomcat6, infinit.e-config, infinit.e-db-instance, elasticsearch >= 0.18.7-8
License: None
Group: Infinit.e
BuildArch: noarch
Prefix: /mnt/opt

%description
Infinit.e harvesting and cleansing services

###########################################################################
#
# SCRIPTLETS, IN ORDER OF EXECUTION
#

%prep
	# (create build files)
	zcat $RPM_SOURCE_DIR/infinit.e-processing-engine.tgz | tar -xvf -

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
	# Increase tomcat limits
	if [ -d /etc/security ]; then
		if [ -f /etc/security/limits.conf ]; then
			sed -i /"^tomcat.*"/d /etc/security/limits.conf
		fi
		
		echo "tomcat    soft    nofile          65536" >> /etc/security/limits.conf
		echo "tomcat    hard    nofile          65536" >> /etc/security/limits.conf
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
	# Start service
	service infinite-px-engine start

###########################################################################
%files
%defattr(-,tomcat,tomcat)
%attr(755,root,root) /etc/init.d/infinite-px-engine
%attr(-,root,root) /etc/cron.d/infinite-px-engine
%dir /mnt/opt/infinite-home
%dir /mnt/opt/infinite-home/bin
%dir /mnt/opt/infinite-home/lib/plugins
/mnt/opt/infinite-home/bin/STOPFILE
/mnt/opt/infinite-home/bin/STOP_SYNC_FILE
%attr(755,tomcat,tomcat) /mnt/opt/infinite-home/bin/custommr.sh
%attr(755,tomcat,tomcat) /mnt/opt/infinite-home/bin/hadoop-setup.sh
%attr(755,tomcat,tomcat) /mnt/opt/infinite-home/bin/reindex_from_db.sh
%config %attr(755,tomcat,tomcat) /mnt/opt/infinite-home/bin/do_harvest_cycle.sh
%config %attr(755,tomcat,tomcat) /mnt/opt/infinite-home/bin/infinite-px-engine.sh
%config %attr(755,tomcat,tomcat) /mnt/opt/infinite-home/bin/sync_features.sh
%config %attr(755,tomcat,tomcat) /mnt/opt/infinite-home/bin/generate_temporal_aggregations.sh
%config %attr(755,tomcat,tomcat) /mnt/opt/infinite-home/bin/weekly_sources_report.sh
%config %attr(755,tomcat,tomcat) /mnt/opt/infinite-home/bin/reset_bad_harvest.sh
%config /mnt/opt/infinite-home/bin/security.policy
%dir /mnt/opt/infinite-home/db-scripts
/mnt/opt/infinite-home/db-scripts/calc_freq_counts.js
/mnt/opt/infinite-home/db-scripts/reset_bad_harvest.js
/mnt/opt/infinite-home/db-scripts/update_doc_counts.js
/mnt/opt/infinite-home/db-scripts/rebuild_entity_feature.js
/mnt/opt/infinite-home/db-scripts/rebuild_assoc_feature.js
/mnt/opt/infinite-home/db-scripts/hadoop_prototype_engine.js
/mnt/opt/infinite-home/db-scripts/temporal_entity_aggregation.js

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
/mnt/opt/infinite-home/lib/unbundled/tika-app-1.0_with_gson_2.2.2.jar
/mnt/opt/infinite-home/lib/unbundled/GridFSZipFile.jar
/mnt/opt/infinite-home/lib/unbundled/j-calais-0.2.1-jar-with-dependencies.jar
/mnt/opt/infinite-home/lib/plugins/infinit.e.hadoop.prototyping_engine.jar
/mnt/opt/infinite-home/lib/plugins/infinit.e.hadoop.prototyping_engine-INFINITE_VERSION-INFINITE_RELEASE.jar