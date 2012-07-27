#!/bin/sh

if [[ $(infdb is_mongos) == "true" || $(infdb is_dbinstance) == "true" ]]; then 
	mongos_ip=""
else
	mongos_ip=$(infdb mongos_ip)
fi
mongo $mongos_ip <<EOF
use config
db.settings.update( { "_id": "balancer" }, { "$set" : { "stopped": false } } , true );
exit
EOF