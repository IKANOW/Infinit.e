###########################################################################
#
# Spec file for Infinit.e system configuration.
#
Summary: Infinit.e search engine REST API
Name: infinit.e-interface-engine
Version: INFINITE_VERSION
Release: INFINITE_RELEASE
Requires: tomcat6, infinit.e-config, infinit.e-index-engine, elasticsearch >= 0.18.7-9
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
		service tomcat6-interface-engine stop || :
		
		#Legacy code: these no longer live in the webapps dir, need to delete any old generated directories...
		rm -rf /mnt/opt/tomcat-infinite/interface-engine/webapps/infinit.e.api.server*
		rm -rf /mnt/opt/tomcat-infinite/interface-engine/webapps/infinit.e.web*
		rm -rf /mnt/opt/tomcat-infinite/interface-engine/webapps/infinit.e.manager*
	fi
	
%install
#
# INSTALL *AND* UPGRADE
#	
	# (tomcat6 instance initialization)
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
	# Add tomcat6 instance to start-up-on-boot list
	chkconfig --add tomcat6-interface-engine
	chkconfig tomcat6-interface-engine on
	
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
		# (Stop tomcat6 instance)
		service tomcat6-interface-engine stop
		# (Remove tomcat6 instance from the start-up-on-boot list)
		chkconfig tomcat6-interface-engine off
		chkconfig --del tomcat6-interface-engine
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
	# Create AppConstants.js file
	sh /mnt/opt/tomcat-infinite/interface-engine/scripts/create_appconstants.sh
	#(App constants file is copied to relevant locations by start code below)

	# Start service
	service tomcat6-interface-engine start
	
###########################################################################
#
# FILE LISTS
#

%files
%defattr(-,tomcat,tomcat)
%config %attr(755,root,root) /etc/init.d/tomcat6-interface-engine
%dir /mnt/opt/tomcat-infinite/
%config %attr(755,root,root) /mnt/opt/tomcat-infinite/tomcat6
%dir /mnt/opt/tomcat-infinite/interface-engine
%dir /mnt/opt/tomcat-infinite/interface-engine/conf
%dir /mnt/opt/tomcat-infinite/interface-engine/lib
%dir /mnt/opt/tomcat-infinite/interface-engine/logs
%dir /mnt/opt/tomcat-infinite/interface-engine/webapps
%dir /mnt/opt/tomcat-infinite/interface-engine/scripts

%config /etc/sysconfig/tomcat6-interface-engine
%config /mnt/opt/tomcat-infinite/interface-engine/conf/catalina.policy
%config /mnt/opt/tomcat-infinite/interface-engine/conf/catalina.properties
%config /mnt/opt/tomcat-infinite/interface-engine/conf/context.xml
%config /mnt/opt/tomcat-infinite/interface-engine/conf/logging.properties
%config /mnt/opt/tomcat-infinite/interface-engine/templates/server.xml.TEMPLATE
%config /mnt/opt/tomcat-infinite/interface-engine/templates/AppConstants.js.INTERNAL
%config /mnt/opt/tomcat-infinite/interface-engine/conf/tomcat-users.xml
%config /mnt/opt/tomcat-infinite/interface-engine/conf/web.xml
/mnt/opt/tomcat-infinite/interface-engine/lib/ExtendedAccessLogValve.jar
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
/mnt/opt/tomcat-infinite/interface-engine/templates/AppConstants.js.TEMPLATE