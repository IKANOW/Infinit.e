#!/bin/bash

curl -XPUT localhost:9200/_template/custom -d @/opt/infinite-home/templates/elasticsearch-inf-template.json