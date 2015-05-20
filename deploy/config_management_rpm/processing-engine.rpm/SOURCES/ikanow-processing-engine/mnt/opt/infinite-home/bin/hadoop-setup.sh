#!/bin/bash

#Script to set up custom map reduce scheduling environment
#Step 1: Create necessary dirs at infinite.service.properties.hadoop.configpath
#Step 2: Create necessary hadoop map reduce config files at configpath/hadoop
#Step 3: Change owner all files in config path to user hdfs group hadoop

# Get relevant environment vars

CONFDIR=/opt/infinite-home/config/
HOSTNAME=`hostname`

if [ -d $CONFDIR ]; then
	if [ -f $CONFDIR/infinite.service.properties ]; then
		HADOOPCONFIGPATH=`grep "^hadoop.configpath=" $CONFDIR/infinite.service.properties | sed s/'hadoop.configpath='// | sed s/' '//g`		
	fi		
fi

#STEP 1
mkdir -p $HADOOPCONFIGPATH/config/xmlFiles
mkdir -p $HADOOPCONFIGPATH/hadoop
mkdir -p $HADOOPCONFIGPATH/jars

#STEP 2
echo "<configuration><property><name>fs.default.name</name><value>hdfs://$HOSTNAME:8020/</value></property></configuration>" > $HADOOPCONFIGPATH/hadoop/core-site.xml
echo "<configuration><property><name>mapred.job.tracker</name><value>$HOSTNAME:8021</value></property><property><name>mapred.child.java.opts</name><value>-Xmx325692290</value></property></configuration>" > $HADOOPCONFIGPATH/hadoop/mapred-site.xml

#STEP 3
chown -R hdfs:hadoop $HADOOPCONFIGPATH