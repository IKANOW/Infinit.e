#!/bin/bash

CDH=0
HDP=0
REQUIRED=0

while getopts "ae:" opt; do
    case $opt in
    e)
        REQUIRED=1
        echo "Setting environment to $OPTARG"
        if [ "$OPTARG" = "CDH" ]; then
            CDH=1
        elif [ "$OPTARG" = "HDP" ]; then
            HDP=1
        else
            echo "Error, $OPTARG is not a valid option"
            exit 1
        fi
        ;;
    a)
        echo "Automatic config based on rpms installed"
        CDH=`rpm -qa|grep cdh|wc -l`
        HDP=`rpm -qa|grep hdp|wc -l`
        REQUIRED=1
        ;;
    \?)
        echo "Invalid option: -$OPTARG" >&2
        ;;
    *)
        if [ "$CDH" -eq 0 ] || [ "$HDP" -eq 0 ]; then
            echo "Please specify an environment"
            exit 1
        fi
    esac
done

if [ "$REQUIRED" -eq 0 ]; then
    echo "Missing required fields"
    exit 1
fi

if [ "$CDH" -gt 0 ] && [ "$HDP" -gt 0 ]; then
    echo "Both HDP and CDH appear to be installed"
    exit 255
elif [ "$CDH" -gt 0 ]; then
    rm /mnt/opt/hadoop-infinite/mapreduce/hadoop/core-site.xml
    rm /mnt/opt/hadoop-infinite/mapreduce/hadoop/hdfs-site.xml
    rm /mnt/opt/hadoop-infinite/mapreduce/hadoop/mapred-site.xml
    ln -s /etc/hadoop/conf/core-site.xml /mnt/opt/hadoop-infinite/mapreduce/hadoop/core-site.xml
    ln -s /etc/hadoop/conf/hdfs-site.xml /mnt/opt/hadoop-infinite/mapreduce/hadoop/hdfs-site.xml
    ln -s /etc/hadoop/conf/mapred-site.xml /mnt/opt/hadoop-infinite/mapreduce/hadoop/mapred-site.xml
    if [ $? -ne 0 ]; then
        echo "Failed to create symlink"
    fi
elif [ "$HDP" -gt 0 ]; then
    rm /mnt/opt/hadoop-infinite/mapreduce/hadoop/core-site.xml
    rm /mnt/opt/hadoop-infinite/mapreduce/hadoop/hdfs-site.xml
    rm /mnt/opt/hadoop-infinite/mapreduce/hadoop/mapred-site.xml
    ln -s /usr/hdp/current/hadoop-client/conf/core-site.xml /mnt/opt/hadoop-infinite/mapreduce/hadoop/core-site.xml
    ln -s /usr/hdp/current/hadoop-client/conf/hdfs-site.xml /mnt/opt/hadoop-infinite/mapreduce/hadoop/hdfs-site.xml
    ln -s /usr/hdp/current/hadoop-client/conf/mapred-site.xml /mnt/opt/hadoop-infinite/mapreduce/hadoop/mapred-site.xml
    if [ $? -ne 0 ]; then
        echo "Failed to create symlink"
    fi
else
    echo "Failed to find CDH or HDP"
    exit 1
fi

