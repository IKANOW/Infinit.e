%define _unpackaged_files_terminate_build 0
###########################################################################
#
# Spec file for Infinit.e system configuration.
#
Summary: Infinit.e search engine REST API
Name: infinit.e-interface-engine
Version: INFINITE_VERSION
Release: INFINITE_RELEASE
Requires: tomcat7, tomcat7-webapps, tomcat7-admin-webapps, infinit.e-config >= v1.0, infinit.e-index-engine >= v0.5, infinit.e-processing-engine >= v1.0
License: None
Group: Infinit.e
BuildArch: noarch
Prefix: /mnt/opt

%description
Infinit.e search engine REST API

###########################################################################
#
# SCRIPTLETS, IN ORDER OF EXECUTION
#

%prep
	# (create build files)
	zcat $RPM_SOURCE_DIR/infinit.e-interface-engine.tgz | tar -xvf -

%pre
	if [ $1 -eq 2 ]; then
#
# THIS IS AN UPGRADE
#
		if [ -f /etc/init.d/tomcat7-interface-engine ]; then
			service tomcat7-interface-engine stop || :		
		else
			service tomcat6-interface-engine stop || :
			chkconfig tomcat6-interface-engine off
			chkconfig --del tomcat6-interface-engine			
		fi		
		
		#Remove some work directory artfacts that have occasionally not been updated on restart
		rm -rf /mnt/opt/tomcat-infinite/interface-engine/work/Catalina/		 
		
		#Legacy code: these no longer live in the webapps dir, need to delete any old generated directories...
		rm -rf /mnt/opt/tomcat-infinite/interface-engine/webapps/infinit.e.api.server*
		rm -rf /mnt/opt/tomcat-infinite/interface-engine/webapps/infinit.e.web*
		rm -rf /mnt/opt/tomcat-infinite/interface-engine/webapps/infinit.e.manager*
	fi
	
%install
#
# INSTALL *AND* UPGRADE
#	
	# Check some required tomcat/j8 dirs:
	for i in /usr/share/java-1.8.0 /usr/lib/java-1.8.0 /usr/lib/jvm-exports/jre; do
		if [ ! -d $i ]; then
			mkdir -p $i 
		fi
	done	

	# (tomcat instance initialization)
	cd $RPM_BUILD_DIR/mnt/opt/tomcat-infinite/interface-engine/
	ln -s -f infinit.e.api.server-INFINITE_VERSION-INFINITE_RELEASE.war infinit.e.api.server.war
	ln -s -f infinit.e.web-INFINITE_VERSION-INFINITE_RELEASE.war infinit.e.web.war
	ln -s -f infinit.e.manager-INFINITE_VERSION-INFINITE_RELEASE.war infinit.e.manager.war

%post

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

#
# INSTALL *AND* UPGRADE
#	
	# Add tomcat7 instance to start-up-on-boot list
	chkconfig --add tomcat7-interface-engine
	chkconfig tomcat7-interface-engine on
	
	# Handle relocation:
	if [ "$RPM_INSTALL_PREFIX" != "/opt" ]; then
		echo "(Creating links from /opt to $RPM_INSTALL_PREFIX)"
	 	if [ -d /opt/tomcat-infinite ] && [ ! -h /opt/tomcat-infinite ]; then
			echo "Error: /opt/tomcat-infinite exists"
			exit 1
		else
			ln -sf $RPM_INSTALL_PREFIX/tomcat-infinite /opt
			chown -h tomcat.tomcat /opt/tomcat-infinite
		fi 
	fi
	# (service started in posttrans)

%preun
	if [ $1 -eq 0 ]; then
#
# THIS IS AN *UN"INSTALL NOT AN UPGRADE
#
		# (Stop tomcat6/7 instance)
		# (Remove tomcat6/7 instance from the start-up-on-boot list)
		if [ -f /etc/init.d/tomcat6-interface-engine ]; then 
			service tomcat6-interface-engine stop
			chkconfig tomcat6-interface-engine off
			chkconfig --del tomcat6-interface-engine
		fi
		#(tomcat 7)
		if [ -f /etc/init.d/tomcat7-interface-engine ]; then 
			service tomcat7-interface-engine stop
			chkconfig tomcat7-interface-engine off
			chkconfig --del tomcat7-interface-engine
		fi
		# Handle relocation:
		if [ "$RPM_INSTALL_PREFIX" != "/opt" ]; then
			if [ -h /opt/tomcat-infinite ]; then
				rm /opt/tomcat-infinite
			fi
		fi
	fi
#
# UNINSTALL *AND* UPGRADE
#	
	# (Tidy up expanded WAR files)
	rm -rf /mnt/opt/tomcat-infinite/interface-engine/webapps/ROOT
	rm -rf /mnt/opt/tomcat-infinite/interface-engine/webapps/api
	rm -rf /mnt/opt/tomcat-infinite/interface-engine/webapps/manager
	rm -rf /mnt/opt/tomcat-infinite/interface-engine/webapps/internal

%postun
	# (Nothing to do)

%posttrans
#
# FINAL STEP FOR INSTALLS AND UPGRADES
#
	#Insert or update base widgets
	sh /mnt/opt/infinite-home/db-scripts/insert_or_update_widgets.sh > /dev/null

	# Create AppConstants.js file
	sh /mnt/opt/tomcat-infinite/interface-engine/scripts/create_appconstants.sh
	#(App constants file is copied to relevant locations by start code below)

	# Start service
	
	service tomcat7-interface-engine start
	
###########################################################################
#
# FILE LISTS
#

%files
%defattr(-,tomcat,tomcat)
/mnt/opt/infinite-home/db-scripts/insert_or_update_widgets.sh

#(moved this to prevent accidental tomcat6 use moving forwards)
%config %attr(755,root,root) /mnt/opt/tomcat-infinite/interface-engine/scripts/old_tomcat6/init.d/tomcat6-interface-engine
%config %attr(-,root,root) /mnt/opt/tomcat-infinite/interface-engine/scripts/old_tomcat6/cron.d/tomcat6-interface-engine
%config %attr(755,root,root) /etc/init.d/tomcat7-interface-engine
%config %attr(-,root,root) /etc/cron.d/tomcat7-interface-engine
%dir /mnt/opt/tomcat-infinite/
%config %attr(755,root,root) /mnt/opt/tomcat-infinite/tomcat6
%config %attr(755,root,root) /mnt/opt/tomcat-infinite/tomcat7
%dir /mnt/opt/tomcat-infinite/interface-engine
%dir /mnt/opt/tomcat-infinite/interface-engine/conf
%dir /mnt/opt/tomcat-infinite/interface-engine/lib
%dir /mnt/opt/tomcat-infinite/interface-engine/logs
%dir /mnt/opt/tomcat-infinite/interface-engine/webapps
%dir /mnt/opt/tomcat-infinite/interface-engine/scripts

%config /etc/sysconfig/tomcat6-interface-engine
%config /etc/sysconfig/tomcat7-interface-engine
%config /mnt/opt/tomcat-infinite/interface-engine/conf/catalina.policy
%config /mnt/opt/tomcat-infinite/interface-engine/conf/catalina.properties
%config /mnt/opt/tomcat-infinite/interface-engine/conf/context.xml
%config /mnt/opt/tomcat-infinite/interface-engine/conf/web.xml
%config /mnt/opt/tomcat-infinite/interface-engine/conf/logging.properties
%config /mnt/opt/tomcat-infinite/interface-engine/templates/server.xml.TEMPLATE
%config /mnt/opt/tomcat-infinite/interface-engine/templates/AppConstants.js.INTERNAL
%config /mnt/opt/tomcat-infinite/interface-engine/conf/tomcat-users.xml
/mnt/opt/tomcat-infinite/interface-engine/lib/ExtendedAccessLogValve.jar
/mnt/opt/tomcat-infinite/interface-engine/lib/ecj-4.3.1.jar
/mnt/opt/tomcat-infinite/interface-engine/infinit.e.api.server-INFINITE_VERSION-INFINITE_RELEASE.war
/mnt/opt/tomcat-infinite/interface-engine/infinit.e.api.server.war
/mnt/opt/tomcat-infinite/interface-engine/infinit.e.web-INFINITE_VERSION-INFINITE_RELEASE.war
/mnt/opt/tomcat-infinite/interface-engine/infinit.e.web.war
/mnt/opt/tomcat-infinite/interface-engine/infinit.e.manager-INFINITE_VERSION-INFINITE_RELEASE.war
/mnt/opt/tomcat-infinite/interface-engine/infinit.e.manager.war
/mnt/opt/tomcat-infinite/interface-engine/scripts/create_event_list.js
/mnt/opt/tomcat-infinite/interface-engine/scripts/create_entity_list.js
/mnt/opt/tomcat-infinite/interface-engine/scripts/random_query_generator.sh
/mnt/opt/tomcat-infinite/interface-engine/scripts/create_appconstants.sh
%attr(755,tomcat,tomcat) /mnt/opt/tomcat-infinite/interface-engine/scripts/restart_tomcat6_phase1.sh
%attr(755,tomcat,tomcat) /mnt/opt/tomcat-infinite/interface-engine/scripts/restart_tomcat6_phase2.sh
%attr(755,tomcat,tomcat) /mnt/opt/tomcat-infinite/interface-engine/scripts/restart_tomcat7_phase1.sh
%attr(755,tomcat,tomcat) /mnt/opt/tomcat-infinite/interface-engine/scripts/restart_tomcat7_phase2.sh
/mnt/opt/tomcat-infinite/interface-engine/templates/AppConstants.js.TEMPLATE
