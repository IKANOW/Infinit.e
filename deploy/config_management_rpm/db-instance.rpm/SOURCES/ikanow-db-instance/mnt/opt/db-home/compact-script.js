
if (rs.isMaster().ismaster) {
	try {
		rs.stepDown();
	}
	catch (e) {}
	// after stepdown connections are dropped. do an operation to cause reconnect:
	rs.isMaster();
	sleep(1000); // give someone else a chance to step up as master 

//	now ready to go.

//	wait for another node to become primary -- it may need data from us for the last
//	small sliver of time, and if we are already compacting it cannot get it while the
//	compaction is running.

	for (i = 0; i < 60; ++i) { // Wait up to 10mins
		var m = rs.isMaster();
		if( m.ismaster ) {
			print("ERROR: no one took over during our stepDown duration. we are primary again!");
		}
		else if( m.primary ) {
			break; // someone else is, great
		}
		print("waiting...");
		sleep(10000);
	}
	if (i >= 60) {
		print("Aborting ")
		throw ("Didn't become master after 10 minutes");
	}
}

//someone else is primary, so we are ready to proceed with a compaction
print("Compacting doc_metadata: " + new Date().toString());
db = db.getMongo().getDB("doc_metadata");
printjson( db.runCommand({compact:"metadata"}) );
print("Compacting doc_content: " + new Date().toString());
db = db.getMongo().getDB("doc_content");
printjson( db.runCommand({compact:"gzip_content"}) );
db = db.getMongo().getDB("feature");
print("Compacting feature.entity: " + new Date().toString());
printjson( db.runCommand({compact:"entity"}) );
print("Compacting feature.association: " + new Date().toString());
printjson( db.runCommand({compact:"association"}) );
print("Compacting ingest.source: " + new Date().toString());
db = db.getMongo().getDB("ingest");
printjson( db.runCommand({compact:"source"}) );
print("Compacting custommr.customlookup: " + new Date().toString());
db = db.getMongo().getDB("custommr");
printjson( db.runCommand({compact:"customlookup"}) );
print("Completed compaction: " + new Date().toString());
