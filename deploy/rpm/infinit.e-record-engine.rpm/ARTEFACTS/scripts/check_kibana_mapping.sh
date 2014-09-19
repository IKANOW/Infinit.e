#!/bin/bash
#
# This will check for old versions of the kibana integration with the incorrect source key mapping
# 
for i in $(curl -s -XGET 'localhost:9200/_cat/indices?v' | awk  '{ print $2 }' | grep recs_); do
	curl -s -XGET "http://localhost:9200/$i/_mapping/_default_" | grep -o -P "sourceKey.*?\"index\":\"[^\"]*\"" | grep -q "not_analyzed" || echo "PROBLEM INDEX: $i"
done