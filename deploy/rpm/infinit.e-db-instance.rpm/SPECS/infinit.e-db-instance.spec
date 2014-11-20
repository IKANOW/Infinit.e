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
Requires: mongodb-org-server, mongodb-org-tools, mongodb-org-shell, mongodb-org-mongos, vim-common

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
# NO INSTALL *OR* UPGRADE

%post

###########################################################################
# INSTALL *AND* UPGRADE

	if [ -d /etc/security ]; then
		if [ -f /etc/security/limits.conf ]; then
			sed -i -r /"^(soft|hard) (nofile|nproc).*"/d /etc/security/limits.conf
			sed -i -r /"^mongod.*"/d /etc/security/limits.conf
		fi		
		echo 'mongod soft nofile 64000' >> /etc/security/limits.conf
		echo 'mongod hard nofile 64000' >> /etc/security/limits.conf
		echo 'mongod soft nproc 32000' >> /etc/security/limits.conf
		echo 'mongod hard nproc 32000' >> /etc/security/limits.conf
	fi
	if [ -d /etc/security/limits.d/ ]; then
		echo 'mongod soft nproc 32000' > /etc/security/limits.d/99-mongod.conf
		echo 'mongod hard nproc 32000' >> /etc/security/limits.d/99-mongod.conf
		echo 'mongod soft nofile 64000' >> /etc/security/limits.d/99-mongod.conf
		echo 'mongod hard nofile 64000' >> /etc/security/limits.d/99-mongod.conf
	fi

	###########################################################################
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
	#(do data and backups separately to handle the upgrade case)
	if [ ! -d /opt/db-home/data ]; then
		if [ -d /dbarray ]; then
			mkdir -p /dbarray/mongo-data
			chown -R mongod:mongod /dbarray/mongo-data
			ln -sf /dbarray/mongo-data /opt/db-home/data
			chown -h mongod.mongod /opt/db-home/data
		elif [ -d /raidarray ]; then
			mkdir -p /raidarray/mongo-data
			chown -R mongod:mongod /raidarray/mongo-data
			ln -sf /raidarray/mongo-data /opt/db-home/data
			chown -h mongod.mongod /opt/db-home/data
		else
			mkdir /opt/db-home/data
			chown -R mongod:mongod /opt/db-home/data
		fi
	fi
	if [ ! -d /opt/db-home/backups ]; then
		if [ -d /dbarray ]; then
			mkdir -p /dbarray/mongo-backups
			chown -R mongod:mongod /dbarray/mongo-backups
			ln -sf /dbarray/mongo-backups /opt/db-home/backups
			chown -h mongod.mongod /opt/db-home/backups
		elif [ -d /raidarray ]; then
			mkdir -p /raidarray/mongo-backups
			chown -R mongod:mongod /raidarray/mongo-backups
			ln -sf /raidarray/mongo-backups /opt/db-home/backups
			chown -h mongod.mongod /opt/db-home/backups
		else
			mkdir /opt/db-home/backups
			chown -R mongod:mongod /opt/db-home/backups
		fi
	fi
	#2.6 onwards has changed log dir - ensure old one exists:
	mkdir -p /var/log/mongo
	chown mongod.mongod /var/log/mongo
	
###########################################################################
# INSTALL ONLY

	if [ $1 -eq 1 ]; then
		chkconfig --add mongo_infinite
		chkconfig mongo_infinite on

		# (Stop mongodb if it was started by the above install)
		service mongod stop
		chkconfig --del mongod
		
		rm -rf /data
		ln -sf /opt/db-home/data/ /data
		
		service mongo_infinite start
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
# NO UPGRADE ONLY

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
		echo "(upgrade)"
	fi

%postun
	# (Nothing to do)

%posttrans

###########################################################################
# FINAL STEP FOR INSTALLS AND UPGRADES

	# (Start mongo if it isn't already running (else does nothing)	
	service mongo_infinite start

###########################################################################
# FILE LISTS

%files
%defattr(-,mongod,mongod)
# (DB config)
%dir /mnt/opt/db-home/
%attr(755,mongod,mongod) /mnt/opt/db-home/arbiter-manager.sh
%attr(755,mongod,mongod) /mnt/opt/db-home/master_backup-script.sh
%attr(755,mongod,mongod) /mnt/opt/db-home/backup-script.sh
%attr(755,mongod,mongod) /mnt/opt/db-home/master_compact-script.sh
%attr(644,mongod,mongod) /mnt/opt/db-home/compact-script.js
%attr(755,mongod,mongod) /mnt/opt/db-home/infdb_aws
%attr(755,mongod,mongod) /mnt/opt/db-home/infdb_standard
%attr(755,mongod,mongod) /mnt/opt/db-home/restore-script.sh
%attr(755,mongod,mongod) /mnt/opt/db-home/start_balancer.sh
%attr(755,mongod,mongod) /mnt/opt/db-home/rotate-logs-script.sh
%attr(755,mongod,mongod) /mnt/opt/db-home/setupAdminShards.sh
%attr(755,mongod,mongod) /mnt/opt/db-home/
%attr(755,mongod,mongod) /usr/bin/infdb
%attr(755,mongod,mongod) /etc/rc.d/init.d/mongo_infinite
%attr(644,root,root) /etc/cron.d/infinite-db
