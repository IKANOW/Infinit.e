#!/bin/bash
mkdir /opt/hadoop-fileshare

NAMENODE=$(cat /opt/hadoop-infinite/mapreduce/hadoop/core-site.xml | grep -A 1 'fs.defaultFS' | grep -o -P 'hdfs://[^<]+' | sed s/hdfs/dfs/)

hadoop-fuse-dfs $NAMENODE /opt/hadoop-fileshare

#delete existing from rc.local
sed -i '/hadoop-fuse-dfs/d' /etc/rc.local
# add again:
echo "hadoop-fuse-dfs $NAMENODE /opt/hadoop-fileshare" >> /etc/rc.local