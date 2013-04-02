#!/bin/bash
#
# infinite-get-latest-rpms.sh <user> <pass>
#
# COMMENT THE RPMS YOU WANT FOR THE SPECIFIC MACHINE
#CONFIG_URL='https://ikanow.jira.com/builds//artifact/INF-OSS/JOB1/build-latestSuccessful/Configuration-RPM'
#INDEX_URL='https://ikanow.jira.com/builds//artifact/INF-OSS/JOB1/build-latestSuccessful/Index-Engine-RPM'
#INTERFACE_URL='https://ikanow.jira.com/builds//artifact/INF-OSS/JOB1/build-latestSuccessful/Interface-Engine-RPM'
#PROCESSING_URL='https://ikanow.jira.com/builds//artifact/INF-OSS/JOB1/build-latestSuccessful/Processing-Engine-RPM'
#DB_URL='https://ikanow.jira.com/builds//artifact/INF-OSS/JOB1/build-latestSuccessful/Database-RPM'

GREP=$1

for rpm in $CONFIG_URL $INTERFACE_URL $INDEX_URL $PROCESSING_URL $DB_URL $RAID_URL; do
        if [ -z "$rpm" ]; then continue; fi
        if [ ! -z "$GREP" ]; then
                if echo $rpm | grep -v -q -i -P "$GREP"; then continue; fi
        fi
        RPM_NAME=`curl -s $rpm?os_authType=basic | grep -o ">[^<]*.rpm" | sed s/'>'//`
        echo $RPM_NAME
        if [ ! -f "./$RPM_NAME" ]; then
                curl -o $RPM_NAME -s $rpm/$RPM_NAME?os_authType=basic
        fi
done
