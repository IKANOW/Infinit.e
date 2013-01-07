//
// Calculate per source document counte
//

db = db.getMongo().getDB( "ingest" );

////////////////////////////////////////////////////////////

// PHASE 1: GET A LIST OF SOURCES IN DOC_METADATA

print("Phase 1: " + new Date().toString());

var my_query = {};
	// ^^^(change for debug)
var my_limit = 0;

m = function() {
	var initial_doccount = 0;
	if ((null != this.harvest) && (null != this.harvest.doccount)) {
		initial_doccount = this.harvest.doccount;
	}
	if ((null != this.key) && (null != this.communityIds)) {
		emit( this.key, { communityIds: this.communityIds, initial_doccount: initial_doccount, _id: this._id } )
	}
}
r = function(k, vals) {
	// (Should never get called since this.key in map is unique)
	return vals[0];
}

// 1.1: CREATE THE LIST IN THE INGEST COLLECTION

res = db.source.mapReduce(m, r, { out: { replace: "tmpDocCounts" }, query: my_query, limit: my_limit } );

// 1.2 MOVE TO THE DOC_METADATA COLLECTION

db = db.getMongo().getDB( "doc_metadata" );
db.tmpDocCounts.drop();
	// (otherwise the rename will fail if "doc_metadata.tmpDocCounts" happens to exist)

db.getMongo().getDB( "admin" ).runCommand({renameCollection:"ingest.tmpDocCounts",to:"doc_metadata.tmpDocCounts"});

////////////////////////////////////////////////////////////

// PHASE 2: COUNT THE DOCUMENTS PER SOURCE AND PER COMMUNITY

print("Phase 2: " + new Date().toString());

///////////////////////////////
//2a: enrich tmpDocCounts with document counts
f1 = function(x) {

	var doc_count = db.metadata.count({sourceKey:x._id});
	for (i = 0; i < x.value.communityIds.length; ++i) {
		doc_count += db.metadata.count({sourceKey: (x._id + '#' + x.value.communityIds[i])});
	}
	x.value.doccount = doc_count;
	db.tmpDocCounts.save(x);
}
db.tmpDocCounts.find().batchSize(1000).forEach(f1);

///////////////////////////////
//2b: sum the document counts across communities
m2 = function() {
	for (i = 0; i < this.value.communityIds.length; ++i) {
		emit ( this.value.communityIds[i], { doccount: this.value.doccount } );		
	}
}
r2 = function(k, vals) {
	var doc_count = 0;
    vals.forEach(function(value) {
		doc_count += value.doccount;
    });
    return { doccount: doc_count };
}

//(pre tidy up)
db.doc_counts.update({}, { $unset: { batch_resync: 1 } }, false, true);

db.doc_counts.ensureIndex({ _id:1, doccount:1 });
	// (ensures the whole object is in memory for performance - for queries later)

res = db.tmpDocCounts.mapReduce( m2, r2, { out: { replace: "tmpDocCounts2" }, query: {} } );

///////////////////////////////
//2c: Write the results to the table
f3 = function(x) {
	
	db.doc_counts.update({_id: x._id}, {"$set": { doccount: x.value.doccount, batch_resync: true }  }, false, false);
}
db.tmpDocCounts2.find().batchSize(1000).forEach(f3);

//(remove old communities)
db.doc_counts.remove({ "batch_resync": { "$exists": false } })
db.doc_counts.update({}, { $unset: { batch_resync: 1 } }, false, true);
//(more tidy up)
db.tmpDocCounts2.drop()

//(^^^TESTED)

////////////////////////////////////////////////////////////

// PHASE 3: UPDATE THE SOURCES TABLE

print("Phase 3: " + new Date().toString());

db = db.getMongo().getDB( "ingest" );
db.tmpDocCounts.drop();
	// (otherwise the rename will fail if "ingest.tmpDocCounts" happens to exist)

// 3.1 Move back to ingest so we can update the sources table

db.getMongo().getDB( "admin" ).runCommand({renameCollection:"doc_metadata.tmpDocCounts",to:"ingest.tmpDocCounts"});


// 3.2 Going to merge the current sources with the correct count: 

var tmpDocCounts2 = {};
var tmpDocCount2_array = [];
f4 = function(x) {
	if (x.value.doccount != x.value.initial_doccount) {
		tmpDocCounts2[x.value._id] = x.value.doccount - x.value.initial_doccount;
		tmpDocCount2_array.push(x.value._id);
	}
}
db.tmpDocCounts.find().batchSize(1000).forEach(f4);

query = {};
if (tmpDocCount2_array.length < 1000) {
	query = {_id: { "$in": tmpDocCount2_array } };
}

// Some handy but un-used utility code:
//var localDB = db;
//var configDB = db.getMongo().getDB("config");
//if (configDB.shards.count() > 0) {
//	print("Handling sharded DB");
//	var ingestInfo = configDB.databases.findOne({_id:"ingest"});
//	var rsInfo = configDB.shards.findOne({_id:ingestInfo.primary});
//	var hostToTry = rsInfo.host.split("/")[1].split(",")[0];
//	print("Trying: " + hostToTry);
//	var conn = new Mongo(hostToTry);
//	var adminDB = conn.getDB("admin");
//	rsInfo = adminDB.isMaster();
//	hostToTry = rsInfo.primary;
//	print("Confirmed master for ingest: " + hostToTry);
//	conn = new Mongo(hostToTry);
//	localDB = conn.getDB("ingest");
//}

f5 = function(x) {

	var doccount_inc = tmpDocCounts2[x._id.toString()];
	if (null != doccount_inc) {
		db.source.update({ _id: x._id }, { "$inc" : { "harvest.doccount": doccount_inc } }, false, false);
	}
}
db.source.find(query).batchSize(1000).forEach(f5);

//(tidy up)
db.tmpDocCounts.drop()

//(^^^TESTED)

print("Complete: " + new Date().toString() + " , Updated: " + tmpDocCount2_array.length);
