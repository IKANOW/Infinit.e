###########################################################################
# Spec file for Infinit.e system configuration.
Summary: Infinit.e db instance
Name: infinit.e-db-instance
Version: INFINITE_VERSION
Release: INFINITE_RELEASE
License: None
Group: Infinit.e
BuildArch: noarch
Prefix: /mnt/opt
Requires: mongo-10gen, mongo-10gen-server

%description
Infinit.e Mongo DB installation and update

###########################################################################
# SCRIPTLETS, IN ORDER OF EXECUTION

%prep
	# (create build files)
	zcat $RPM_SOURCE_DIR/infinit.e-db-instance.tgz | tar -xvf -

%pre
	if [ $1 -eq 2 ]; then
	###########################################################################
	# THIS IS AN UPGRADE
		# (Stop mongodb)
		service mongo_infinite soft_stop
	fi
	
%install

###########################################################################
# INSTALL *AND* UPGRADE

%post

###########################################################################
# INSTALL *AND* UPGRADE
	# Handle relocation:
	if [ "$RPM_INSTALL_PREFIX" != "/opt" ]; then
		echo "(Creating links from /opt to $RPM_INSTALL_PREFIX)"
	 	if [ -d /opt/db-home ] && [ ! -h /opt/db-home ]; then
			echo "Error: /opt/db-home exists"
			exit 1
		else
			ln -sf $RPM_INSTALL_PREFIX/db-home /opt
			chown -h mongod.mongod /opt/db-home 
		fi 
	fi

	###########################################################################
	# Copy the infdb file to usr/bin based on install type (EC2 vs non)
	SERVICE_PROPERTY_FILE='/opt/infinite-home/config/infinite.service.properties' 
	USE_AWS=`grep "^use.aws=" $SERVICE_PROPERTY_FILE | sed s/'use.aws='// | sed s/' '//g`
	if [ "$USE_AWS" = "0" ]; then
		cp /opt/db-home/infdb_standard /usr/bin/infdb
	else
		cp /opt/db-home/infdb_aws /usr/bin/infdb
	fi

	if [ ! -d /opt/db-home/data ]; then
		mkdir /opt/db-home/data
		chown -R mongod:mongod /opt/db-home/data
	fi
	chkconfig --add mongo_infinite

###########################################################################
# INSTALL ONLY
	if [ $1 -eq 1 ]; then
		# (Stop mongodb if it was started by the above install)
		service mongo_infinite soft_stop
		
		rm -rf /data
		ln -sf /opt/db-home/data/ /data
		
		service mongo_infinite start
		sh /opt/db-home/write_cron.sh
		sh /opt/db-home/setupAdminShards.sh
			
		################################################################################
		# echo "untar geo collection and add it to the MongoDB server via mongorestore"
		################################################################################
		GEO=`mongo localhost/feature --quiet --eval '{ db.geo.count() }'`
		if [ "$GEO" = "0" ]; then
			CONFIG_PROPS=/opt/infinite-install/config/infinite.configuration.properties
			GEO_LOC=`grep "^db.geo_archive=" $CONFIG_PROPS | sed s/'db.geo_archive='// | sed s/' '//g`
			GEO_FILE_LOC=/opt/infinite-install/data/feature/geo.bson.tar.gz
				#(also hardwired below)
			mkdir -p /opt/infinite-install/data/feature
			
			echo "$GEO_LOC" | grep -q -P "^https?://" && curl -L "$GEO_LOC" -o $GEO_FILE_LOC 
			echo "$GEO_LOC" | grep -q -P "^s3://" && s3cmd get $GEO_LOC $GEO_FILE_LOC
			echo "$GEO_LOC" | grep -q -P "^/" && if [ "$GEO_LOC" != "$GEO_FILE_LOC" ]; then cp $GEO_LOC $GEO_FILE_LOC; fi
			 
			if [ -f $GEO_FILE_LOC ]; then
				cd /opt/infinite-install/data/feature/
				tar -zxvf geo.bson.tar.gz
				mongorestore /opt/infinite-install/data/feature/geo.bson
				rm -f /opt/infinite-install/data/feature/geo.bson
			fi
		fi
	fi
	
###########################################################################
# UPGRADE ONLY
#
	if [ $1 -eq 2 ]; then
		service mongo_infinite start
	fi
	
%preun

###########################################################################
# UNINSTALL *AND* UPGRADE
		
	if [ $1 -eq 0 ]; then

		###########################################################################
		# THIS IS AN *UN*INSTALL NOT AN UPGRADE
		###########################################################################
		
		# (Hard Stop mongo processes)	
		service mongo_infinite stop
	
		# Handle relocation:
		if [ "$RPM_INSTALL_PREFIX" != "/opt" ]; then
			if [ -h /opt/db-home ]; then
				rm /opt/db-home
			fi
		fi
		
		###########################################################################
		# Remove symbolic link, but leave /mnt/mongo-db
		rm -f /data
		
		###########################################################################
		# Remove configuration
		rm -f /etc/mongod.conf
	else
		###########################################################################
		# THIS IS AN UPGRADE NOT AN *UN*INSTALL
		###########################################################################
		 
		# (Soft Stop mongo processes)	
		service mongo_infinite soft_stop
	fi

%postun
	# (Nothing to do)

%posttrans

###########################################################################
# FINAL STEP FOR INSTALLS AND UPGRADES



###########################################################################
# FILE LISTS

%files
%defattr(-,mongod,mongod)
# (DB config)
%dir /mnt/opt/db-home/
%attr(755,mongod,mongod) /mnt/opt/db-home/backup-script.sh
%attr(755,mongod,mongod) /mnt/opt/db-home/infdb_aws
%attr(755,mongod,mongod) /mnt/opt/db-home/infdb_standard
%attr(755,mongod,mongod) /mnt/opt/db-home/sync_from_master.sh
%attr(755,mongod,mongod) /mnt/opt/db-home/restore-script.sh
%attr(755,mongod,mongod) /mnt/opt/db-home/start_balancer.sh
%attr(755,mongod,mongod) /mnt/opt/db-home/rotate-logs-script.sh
%attr(755,mongod,mongod) /mnt/opt/db-home/setupAdminShards.sh
%attr(755,mongod,mongod) /mnt/opt/db-home/migrate-beta-v0s_1.js
%attr(755,mongod,mongod) /mnt/opt/db-home/
%attr(755,mongod,mongod) /usr/bin/infdb
%config %attr(755,mongod,mongod) /etc/rc.d/init.d/mongo_infinite
