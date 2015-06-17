#!/bin/sh
#
# check_es_indices.sh [path root] [-fix]
#
ES_HOME=/usr/share/elasticsearch
ES_CLASSPATH=$ES_CLASSPATH:$ES_HOME/lib/elasticsearch-*.jar:$ES_HOME/lib/*:$ES_HOME/lib/sigar/*
INDEXPATH=/opt/elasticsearch-infinite/data/
if [ ! -z $1 ]; then
        INDEXPATH=$1
fi
for i in $(find $INDEXPATH -type d | grep ".*/index$"); do
        sudo -u elasticsearch java -cp $ES_CLASSPATH -ea:org.apache.lucene... org.apache.lucene.index.CheckIndex $2 $i
done
