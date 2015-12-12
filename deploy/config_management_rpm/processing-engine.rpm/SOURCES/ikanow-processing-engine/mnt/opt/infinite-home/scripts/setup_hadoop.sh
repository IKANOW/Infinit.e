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
    rm /opt/hadoop-infinite/lib
    ln -s /opt/hadoop-infinite/lib_hadoop /opt/hadoop-infinite/lib
    if [ $? -ne 0 ]; then
        echo "Failed to create symlink"
    fi
    runuser hdfs -c "hadoop fs -mkdir /user/"
    runuser hdfs -c "hadoop fs -mkdir /user/tomcat"
    runuser hdfs -c "hadoop fs -chmod a+w /user/tomcat"
elif [ "$HDP" -gt 0 ]; then
    rm /opt/hadoop-infinite/lib
    ln -s /opt/hadoop-infinite/lib_yarn /opt/hadoop-infinite/lib
    if [ $? -ne 0 ]; then
        echo "Failed to create symlink"
    fi
    runuser hdfs -c "hadoop fs -mkdir /user/"
    runuser hdfs -c "hadoop fs -mkdir /user/tomcat"
    runuser hdfs -c "hadoop fs -chmod a+w /user/tomcat"
else
    echo "Failed to find CDH or HDP"
    exit 1
fi

