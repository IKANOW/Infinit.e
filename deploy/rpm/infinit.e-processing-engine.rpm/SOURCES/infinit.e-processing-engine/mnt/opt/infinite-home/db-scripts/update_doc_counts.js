//
// Calculate per source document counts
//

////////////////////////////////////////////////////////////

// PHASE 1: GET A LIST OF SOURCES IN DOC_METADATA

var my_query = {};
	// ^^^(change for debug)
var my_limit = 0;

m = function() {
	if ((null != this.key) && (null != this.communityIds)) {
		emit( this.key, { communityIds: this.communityIds } )
	}
}
r = function(k, vals) {
	// (Should never get called since this.key in map is unique)
	return vals[0];
}

// 1.1: CREATE THE LIST IN THE INGEST COLLECTION

res = db.source.mapReduce(m, r, { out: { replace: "tmpDocCounts" }, query: my_query, limit: my_limit } );

// 1.2 MOVE TO THE DOC_METADATA COLLECTION

db.getMongo().getDB( "admin" ).runCommand({renameCollection:"ingest.tmpDocCounts",to:"doc_metadata.tmpDocCounts"});

////////////////////////////////////////////////////////////

// PHASE 2: COUNT THE DOCUMENTS PER SOURCE AND PER COMMUNITY

m2 = function() {
	var doc_count = db.metadata.count({sourceKey:this._id});
	emit ( this._id, { doccount: doc_count, communityIds: this.value.communityIds } );
	for (i = 0; i < this.value.communityIds.length; ++i) {
		var communityId = this.value.communityIds[i];
		var source_key = (this._id + '#' + communityId);
		var doc_count2 = db.metadata.count({ sourceKey: source_key });
			//^^ handles multi-community sources (also ensures r2 will be called for both communities and sources)
		emit ( this._id, { doccount: doc_count2, communityIds: this.value.communityIds } );		
		emit ( communityId, { doccount: doc_count, communityIds: null } );	
		emit ( communityId, { doccount: 0, communityIds: null } ); // (ensure gets to reduce)
	}
}
r2 = function(k, vals) {
	// Will only be called for communities?
	var doc_count = 0;
        vals.forEach(function(value) {
		doc_count += value.doccount;
        });
	if (null == vals[0].communityIds) { // ie is a community
		db.doc_counts.save( { _id: k, doccount: doc_count, batch_resync:true } );
	}
        return { doccount: doc_count, communityIds: vals[0].communityIds };
}

//(pre tidy up)
db = db.getMongo().getDB( "doc_metadata" );

db.doc_counts.update({}, { $unset: { batch_resync: 1 } }, false, true);
db.doc_counts.ensureIndex({ _id:1, doccount:1 }); // (ensures the whole object is in memory for performance)

res = db.tmpDocCounts.mapReduce( m2, r2, { out: { replace: "tmpDocCounts2" }, query: {} } );
//(remove old communities)
db.doc_counts.remove({ "batch_resync": { "$exists": false } })
db.doc_counts.update({}, { $unset: { batch_resync: 1 } }, false, true);
//(other tidy up)
db.tmpDocCounts.drop()

////////////////////////////////////////////////////////////

// PHASE 3: UPDATE THE SOURCES TABLE

// 3.1 Move back to ingest so we can update the sources table

db.getMongo().getDB( "admin" ).runCommand({renameCollection:"doc_metadata.tmpDocCounts2",to:"ingest.tmpDocCounts2"});
db = db.getMongo().getDB( "ingest" );

m3 = function() {
	if (null != this.value.communityIds) {
	        emit(this._id, this.value);
	        emit(this._id, { doccount: null }); // (have 2 emit twice to get into the reduce code which lets me manipulate the DB...)
	}
}

r3 = function(k, vals) {
		var val = null;
		if (vals.length > 1) {
				val = (vals[0].doccount == null ? vals[1] : vals[0]);
		}
		else val = val[0];
		if (null == val.doccount) {
		    return {doccount:null};		
		}

        db.source.update({ key: k }, { "$set" : { "harvest.doccount": val.doccount } }, false, false);
        return { doccount: null };
}

res = db.tmpDocCounts2.mapReduce( m3, r3, { out: { replace: "tmpDocCounts3" }, query: {} } );

//(tidy up)
db.tmpDocCounts2.drop()
db.tmpDocCounts3.drop()


