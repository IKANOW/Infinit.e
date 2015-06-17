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
        if ((this.associations != null) && (this.communityId != null)) {
    		var commid = this.communityId;
            for (var i = 0; i < this.associations.length; ++i) {
                var assoc = this.associations[i];
        		
        		var assoc_index = '#' + assoc.entity1_index + assoc.entity2_index + assoc.verb_category + assoc.geo_index;
        			// (the # will tell the mongo reindexer that the index has not been calculated)
        		
        		var toEmit = {
        				doccount: 1, assoc_type: assoc.assoc_type
        		}
    			if (null != assoc.entity1) {
    				if (assoc.entity1.length > 256) {
	    				var tmp = assoc.entity1.substring(0, 256);
	    				var index = tmp.lastIndexOf(' ');
	    				var index2 = tmp.lastIndexOf('.');
	    				if (index < index2) index = index2;
	    				assoc.entity1 = tmp.substring(0, index);
    				}    				
    				toEmit.entity1 = [ assoc.entity1 ];
        			if (null != assoc.entity1_index) {
        				toEmit.entity1_index = assoc.entity1_index;
        				toEmit.entity1.push(assoc.entity1_index);
        			}
    			}
    			if (null != assoc.entity2) {
    				if (assoc.entity2.length > 256) {
	    				var tmp = assoc.entity2.substring(0, 256);
	    				var index = tmp.lastIndexOf(' ');
	    				var index2 = tmp.lastIndexOf('.');
	    				if (index < index2) index = index2;
	    				assoc.entity2 = tmp.substring(0, index);
    				}    				
    				toEmit.entity2 = [ assoc.entity2 ];
        			if (null != assoc.entity2_index) {
        				toEmit.entity2_index = assoc.entity2_index;
        				toEmit.entity2.push(assoc.entity2_index);
        			}
    			}
    			if (null != assoc.verb) {
    				toEmit.verb = [ assoc.verb ];
    			}
    			if (null != assoc.verb_category) {
    				toEmit.verb_category = assoc.verb_category;
    				// And add to verb (for easy searching)
    				if (null == assoc.verb) {
    					toEmit.verb = [ assoc.verb_category ];
    				}
    				else if (assoc.verb != assoc.verb_category) {
    					toEmit.verb.push(assoc.verb_category)
    				}
    			}
    			if (null != assoc.geo_index) {
    				toEmit.geo_index = assoc.geo_index;
    			}            		
                emit({ index: assoc_index, communityId: commid } , toEmit ); 
            }
        }
}

r = function(k, vals) {
        var sum = { doccount: 0, assoc_type:null,
        		//These are all optional:
        			//entity1_index: null, entity2_index: null, verb_category:null, geo_index:null, 
        			//entity1: [], entity2: [], verb: []
        		};
        var setentity1 = {};
        var setentity2 = {};
        var setverb = {};
        vals.forEach(function(value) 
        {
        		if (0 == sum.doccount) { // first time
        			if (null != value.entity1_index) {
        				sum.entity1_index = value.entity1_index;
        			}
        			if (null != value.entity2_index) {
        				sum.entity2_index = value.entity2_index;
        			}
        			if (null != value.verb_category) {
        				sum.verb_category = value.verb_category;
        			}
        			if (null != value.geo_index) {
        				sum.geo_index = value.geo_index;
        			}
        			sum.assoc_type = value.assoc_type;
        		}
                sum.doccount += value.doccount;
                // Array values:
                if (null != value.entity1) {
	                for (var i = 0; i < value.entity1.length; ++i) {
	                	var element = value.entity1[i]
	                	if (null != element) {
			                if (!setentity1.hasOwnProperty(element)) {
			                	setentity1[element] = 1;
			                	if (null == sum.entity1) {
			                		sum.entity1 = [];
			                	}
			                	if (sum.entity1.length < 512) {
			                		sum.entity1.push(element);
			                	}
			                }
	                	}
	                }
                }
                if (null != value.entity2) {
	                for (var i = 0; i < value.entity2.length; ++i) {
	                	var element = value.entity2[i]
	                	if (null != element) {
			                if (!setentity2.hasOwnProperty(element)) {
			                	setentity2[element] = 1;
			                	if (null == sum.entity2) {
			                		sum.entity2 = [];
			                	}
			                	if (sum.entity2.length < 512) {
				                	sum.entity2.push(element);
			                	}
			                }
	                	}
	                }
                }
                if (null != value.verb) {
	                for (var i = 0; i < value.verb.length; ++i) {
	                	var element = value.verb[i]
	                	if (null != element) {
			                if (!setverb.hasOwnProperty(element)) {
			                	setverb[element] = 1;
			                	if (null == sum.verb) {
			                		sum.verb = [];
			                	}
			                	sum.verb.push(element);
			                }
	                	}
	                }
                }
        });
        return sum;
}

print("Entity feature rebuild, starting... (note: if you want to delete the index first, you need to do that manually)" + Date());
res = db.metadata.mapReduce( m, r, { out: { replace: "tmpCalcEventFeatures" }, query: my_query, limit: my_limit } );
//Tidy up/finalize performed below

//////////////////////////////////////////////////////////////
//
// PHASE 1.POSTFIX: MODIFY EXISTING ENTITY FEATURES
//

db = db.getMongo().getDB( "feature" );
db.tmpCalcEventFeatures.drop();
// (another painful lesson - if this collection exists, it's *really* *really* bad!!)

db = db.getMongo().getDB( "admin" );
db.getMongo().getDB( "admin" ).runCommand({renameCollection:"doc_metadata.tmpCalcEventFeatures",to:"feature.tmpCalcEventFeatures"});

m3 = function() {
        emit(this._id, this.value);
        emit(this._id, { doccount: null }); // (have 2 emit twice to get into the reduce code which lets me manipulate the DB...)
}

r3 = function(k, vals) 
{
		var val = null;
		if (vals.length > 1) {
				val = (vals[0].doccount == null ? vals[1] : vals[0]);
		}
		else val = vals[0];
		if (null == val.doccount) {
		    return {doccount:null};		
		}		
		val.db_sync_doccount = val.doccount;
		val.db_sync_time = new Date().getTime().toString();
        db.association.update({ index: k.index, communityId: k.communityId }, { $set: val }, true, false);
        return { doccount: null };
}

print("Entity feature completion, starting... " + Date());
db = db.getMongo().getDB( "feature" );
db.association.ensureIndex({"index":1}); // (in case it's been dropped rather than just emptied...)
res = db.tmpCalcEventFeatures.mapReduce( m3, r3, { out: "tmpCalcEventFeatures3" } );

//Tidy up:
db.tmpCalcEventFeatures.drop();
db.tmpCalcEventFeatures3.drop();

print("Batch update completed... " + Date());
