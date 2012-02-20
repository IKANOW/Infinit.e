//
// Full recalc of all statistics
//

////////////////////////////////////////////////////////////

// PHASE 1: UPDATE THE GAZ

// 1. Map reduce job to calc doc counts and frequencies

//(consistent with the rest of the system, treats all groups as the same for freq/doc count purposes)

var my_query = {};
	// ^^^(change for debug)
var my_limit = 0;

m = function() {
        if ((this.entities != null) && (this.communityId != null)) {
    		var commid = this.communityId;
            for (var i = 0; i < this.entities.length; ++i) {
                var entity = this.entities[i];
        		
        		if ((entity.index != null) && (entity.frequency != null)) {
                        emit({ index: entity.index, comm: commid } , { dc: 1, tf: parseInt(entity.frequency) } );
                }
            }
        }
}
r = function(k, vals) {
        var sum = { dc:0, tf:0 };
        vals.forEach(function(value) {
                sum.tf += value.tf;
                sum.dc += value.dc;
        });
        return sum;
}

print("Entity feature update, pre-initialization ... " + Date());

db.getMongo().getDB( "feature" ).entity.update({}, { $unset: { batch_resync: 1 } }, false, true);

print("Entity feature update, starting... " + Date());
res = db.metadata.mapReduce( m, r, { out: { replace: "tmpCalcFreqCounts" }, query: my_query, limit: my_limit } );
//Tidy up/finalize performed below

//////////////////////////////////////////////////////////////
//
// PHASE 2: UPDATE THE FEEDS
//

m2 = function() {
    if ((this.entities != null) && (this.communityId != null)) {
		var commid = this.communityId;
        for (var i = 0; i < this.entities.length; ++i) {
            var entity = this.entities[i];
        		
            db.tmpCalcFreqCounts.find({ "_id": { "index": entity.index, "comm": commid  } }).limit(1).forEach(function(gaz) {
                    entity.doccount = gaz.value.dc;
                    entity.totalfrequency = gaz.value.tf;
            })
        }
        emit(this._id, this);
        emit(this._id, { _id: null }); // (have to emit twice to get into the reduce code which lets me manipulate the DB...)
   }
}

r2 = function(k, vals) {
		var val = null;
		if (vals.length > 1) {
				val = (vals[0]._id == null ? vals[1] : vals[0]);
		}
		else val = val[0];
		if (null == val._id) {
		    return {_id:null};		
		}
		if (null == val._id) {
		    return {_id:null};		
		}
		
		db.metadata.save(val);
		return { _id: null };
}

print("Document update, starting... " + Date());
res = db.metadata.mapReduce( m2, r2, { out: "tmpCalcFreqCounts2", query: my_query, limit: my_limit } );

//Tidy up:
db.tmpCalcFreqCounts2.drop();

//////////////////////////////////////////////////////////////
//
// PHASE 1.POSTFIX: MODIFY EXISTING ENTITY FEATURES
//

db = db.getMongo().getDB( "admin" );
db.getMongo().getDB( "admin" ).runCommand({renameCollection:"doc_metadata.tmpCalcFreqCounts",to:"feature.tmpCalcFreqCounts"});

m3 = function() {
        emit(this._id, this.value);
        emit(this._id, { dc: null }); // (have 2 emit twice to get into the reduce code which lets me manipulate the DB...)
}

r3 = function(k, vals) {
	var val = null;
	if (vals.length > 1) {
			val = (vals[0].dc == null ? vals[1] : vals[0]);
	}
	else val = val[0];
	if (null == val.dc) {
	    return {dc:null};		
	}
	
	db.entity.update({ index: k.index, communityId: k.comm }, { $set : { batch_resync: true, doccount: val.dc, totalfreq: val.tf } }, false, false);
	return { dc: null };
}

print("Entity feature completion, starting... " + Date());
db = db.getMongo().getDB( "feature" );
res = db.tmpCalcFreqCounts.mapReduce( m3, r3, { out: "tmpCalcFreqCounts3", query: {} } );

//Tidy up:
db.tmpCalcFreqCounts.drop();
db.tmpCalcFreqCounts3.drop();

print("Batch update completed... " + Date());
