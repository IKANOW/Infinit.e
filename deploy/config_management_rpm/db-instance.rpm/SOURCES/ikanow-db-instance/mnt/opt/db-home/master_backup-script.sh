#!/bin/sh

# If I'm a config server then back that up:

if [ $(infdb is_config) == "true" ]; then 
	/opt/db-home/backup-script.sh 27016
fi

# If I host a DB instance then I might also need to backup (if I'm also a master)  

if [ $(infdb is_dbinstance) == "true" ]; then
	repl_sets=$(infdb repl_sets)
	for x in $repl_sets
	do
		port=$((27017+$x))
		/opt/db-home/backup-script.sh $port
	done
fi

