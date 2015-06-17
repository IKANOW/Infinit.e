#!/bin/bash

#TODO: not working .. plus can't use this because _source.enabled is left alone, need to see if there's an includes....
curl -s -XGET 'localhost:9200/doc_4c927585d591d31d7b37097a/document_index/_mapping' | grep -o '_source.........................' | grep '"enabled":false' || exit 0

echo "UPDATING ALL DOCUMENT INDEXES, MIGHT TAKE A WHILE"

curl -XPUT 'http://localhost:9200/doc_4c927585d591d31d7b37097a/document_index/_mapping?ignore_conflicts=false' -d @/opt/infinite-home/templates/doc_mapping_changes.json

for i in $(mongo --quiet social --eval 'db.community.find({},{_id:1}).forEach(function(x){print(x._id.str);})'); do
	curl -XPUT "http://localhost:9200/doc_${i}/document_index/_mapping?ignore_conflicts=false" -d @/opt/infinite-home/templates/doc_mapping_changes.json || echo "fail: $i"		 
done