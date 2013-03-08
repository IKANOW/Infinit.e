#!/bin/sh

# First time: only do secondaries (iteration = 1)
# Second time: only do masters (iteration = 2)
iteration=$1

do_compact()
{
	shardno=$1
	port=$2
	mongo --port $port /opt/db-home/compact-script.js
}


if [ $(infdb is_dbinstance) == "true" ]; then
	repl_sets=$(infdb repl_sets)
	for x in $repl_sets
	do
		port=$((27017+$x))
		
		is_master=$(mongo --quiet --port $port --eval 'print(rs.isMaster().ismaster)')
		
		# First time: only do secondaries	
		if [ $iteration -eq 1 ]; then
			if [ "$is_master" != "false" ]; then
				continue
			fi
		fi	
		# Second time: only do masters
		if [ $iteration -eq 2 ]; then
			if [ "$is_master" != "true" ]; then
				continue
			fi
		fi	
	
		# Compact DB
		do_compact $x $port
	
		# Then back it up
		/opt/db-home/backup-script.sh $port
	done
fi
