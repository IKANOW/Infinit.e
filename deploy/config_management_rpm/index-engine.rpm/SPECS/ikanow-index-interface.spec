# Ignore unpackaged files - only package INTERFACE_ONLY
%define _unpackaged_files_terminate_build 0
###########################################################################
# Spec file for Infinit.e system configuration.
Summary: IKANOW index interface SOLR API
Name: ikanow-index-interface
Version: INFINITE_VERSION
Release: INFINITE_RELEASE
Requires: ikanow-index-engine
License: None
Group: ikanow
BuildArch: noarch
Prefix: /mnt/opt

%description
IKANOW index engine interface only

###########################################################################
# SCRIPTLETS, IN ORDER OF EXECUTION
%prep
	# (create build files)
	zcat $RPM_SOURCE_DIR/ikanow-index-engine.tgz | tar -xvf -

%pre
%post
%preun
%posttrans
%files
/mnt/opt/elasticsearch-infinite/config/INTERFACE_ONLY
