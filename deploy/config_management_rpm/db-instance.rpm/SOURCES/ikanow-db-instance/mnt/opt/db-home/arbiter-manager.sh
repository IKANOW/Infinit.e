#!/bin/bash
if [ "$(infdb is_config)" == "true" ]; then
	MONGO_IP=$(infdb my_ip)
	echo "I am $MONGO_IP"
	
	mongo config --quiet --port 27016 --eval {} &>/dev/null || exit 0
			
	#Get all active replica sets
	REPLICA_SETS=$(mongo config --quiet --port 27016 --eval 'db.shards.find().forEach(function(x){ printjson(x._id) })' | grep -P -o "[0-9]+")
	for x in $REPLICA_SETS; do
	
		#Check if arbiters are actually needed:
		members=$(infdb all_members_of_set $x)
		
		if [ -z "$members" ]; then
			continue
		fi
		
		num_members=$( echo $members | wc -w )
		rs_master=$(infdb master_of_set $x)
		rsport=$((27017+$x))
		arbport=$((26217+$x))
		echo "Replica set: $x, master: $rs_master"
		
		#If even number of members, add an arbiter
		#(NOTE THIS IS A TEMP SITUATION - ONCE THE SHARD IS UP THEN THE CONFIG DB SERVERS WILL
		# SPOT THE NEW SHARD AND CREATE AN ODD SET TO REPLACE THIS TEMP ONE)
		if [ $(($num_members % 2)) -eq 0 ]; then		
			echo "Even number of members, ensure arbiter status is correct"
			
			#(config DB arbiters live on 26217+, instance arbiters live on 27217+)
			JUSTADDED="false";
			if [ "$(infdb mongo_running $arbport 2>&1)" != "true" ]; then
				echo "New arbiter on $arbport"
				mkdir -p /data/arbiter$x
				chown mongod.mongod /data/arbiter$x
				JUSTADDED="true"
				runuser -s /bin/bash - mongod -c "ulimit -S -c 0 >/dev/null 2>&1 ; mongod --replSet replica_set$x --logpath /var/log/mongo/mongod.arbiter.rs$x.log --port $arbport --logappend --nojournal --dbpath /data/arbiter$x --fork --oplogSize 1" >/dev/null
				sleep 5				
			fi
			
			if [ "$(infdb mongo_running $arbport 2>&1)" == "true" ]; then
				echo "Registering new arbiter"
				#OK this has 3 bits...
				
				#1 .. check I'm not in some null state
				if [ "$JUSTADDED" != "true" ]; then
					mongo localhost:$arbport << EOF
use admin
var m = rs.status().members
var found = false;
for (x in m) { 
	if (m[x].name == "$MONGO_IP:$arbport") {
		found = true; 
		if (m[x].stateStr == "REMOVED") {
			print("Closing degenerate arbiter $MONGO_IP:$arbport: " + m[x].stateStr);
			db.shutdownServer();
			sleep(5000);
			break;
		}
	}
}
if (!found) {
	print("Closing missing arbiter $MONGO_IP:$arbport");
	db.shutdownServer();
	sleep(5000);
}
EOF
					if [ "$(infdb mongo_running $arbport 2>&1)" != "true" ]; then
						echo "New arbiter on $arbport"
						mkdir -p /data/arbiter$x
						chown mongod.mongod /data/arbiter$x
						runuser -s /bin/bash - mongod -c "ulimit -S -c 0 >/dev/null 2>&1 ; mongod --replSet replica_set$x --logpath /var/log/mongo/mongod.arbiter.rs$x.log --port $arbport --logappend --nojournal --dbpath /data/arbiter$x --fork --oplogSize 1" >/dev/null
						sleep 5				
					fi
				fi
				#(end if didn't just add a new arbiter, in which case can skip this)
									
				#2 .. add myself to the RS (will fail out if already exists)
				echo "*** Add $MONGO_IP:$arbport to $rs_master"
				mongo $rs_master << EOF
use admin
rs.addArb("$MONGO_IP:$arbport")
exit
EOF
				#3 .. remove any non config arbiters if they exist
				mongo $rs_master << EOF
use admin
var m = rs.config().members
for (x in m) { 
	if (m[x].arbiterOnly) { 
		if (/:27/.test(m[x].host)) {
			print("*** Removing arbiter " +  m[x].host)
			rs.remove(m[x].host)
		} 
	}
}
EOF
			fi
			#(end if arbiter is running)
		else
			# Odd # members, remove my arbiter
			echo "Odd number of members, no arbiter required"
			if [ "$(infdb mongo_running $arbport 2>&1)" == "true" ]; then
				echo "Removing arbiter and shutting it down"
				mongo $rs_master << EOF
use admin
rs.remove("$MONGO_IP:$arbport")
exit
EOF
				mongo localhost:$arbport << EOF
use admin
db.shutdownServer()
exit
EOF
			fi				
		fi	
		#(end if even vs odd number of members in rs)		
	done
	#(end loop over replica sets)
fi
