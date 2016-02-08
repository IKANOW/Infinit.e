#!/bin/sh

# Requires fuse and fuse-libs rpms
DB=db001

export JAVA_HOME=/usr/java/latest
export LD_LIBRARY_PATH=/usr/java/jdk1.8.0_31/jre/lib/amd64/server/:/usr/hdp/2.2.4.2-2/usr/lib/
export HADOOP_HOME=${HADOOP_HOME:-/usr/hdp/2.2.4.2-2/hadoop}
CLASSPATH=/usr/hdp/current/hadoop-hdfs-client/:/etc/hadoop/conf
for jar in `find $HADOOP_HOME -name "*.jar"`; do
CLASSPATH+="$jar:"
done
#echo $CLASSPATH
export CLASSPATH
#/usr/hdp/2.2.4.2-2/hadoop/bin/fuse_dfs
/usr/hdp/2.2.4.2-2/hadoop/bin/fuse_dfs rw -ononempty -oserver=$DB -oport=8020 /opt/hadoop-fileshare
