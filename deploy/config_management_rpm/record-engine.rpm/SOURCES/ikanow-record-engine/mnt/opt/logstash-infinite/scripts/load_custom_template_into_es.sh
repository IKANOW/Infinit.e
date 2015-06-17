#!/bin/bash

curl -s -XPUT localhost:9200/_template/logstash -d@/opt/logstash-infinite/templates/elasticsearch-inf-template.json
curl -s -XPUT 'http://localhost:9200/recs_dummy/'	
curl -s -XPUT 'http://localhost:9200/recs_dummy/dummy/1' -d '{}'
curl -s -XDELETE 'http://localhost:9200/recs_dummy/dummy/1'
