//
// Full recalc of all statistics
//

////////////////////////////////////////////////////////////

// PHASE 1: UPDATE THE GAZ

// 1. Map reduce job to calc doc counts and frequencies

//(consistent with the rest of the system, treats all groups as the same for freq/doc count purposes)

var my_query = {};
	// ^^^(change for debug)

m = function() {
        if ((this.entities != null) && (this.communityId != null)) {
    		var commid = this.communityId;
            for (var i = 0; i < this.entities.length; ++i) {
                var entity = this.entities[i];
        		
        		if ((entity.index != null) && (entity.frequency != null)) {
                        emit({ index: entity.index, comm: commid } , { dc: 1, tf: parseInt(entity.frequency)} );
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

print("Entity feature update, starting... " + Date());

res = db.metadata.mapReduce( m, r, { out: { replace: "tmpCalcFreqCounts", db: "feature" }, query: my_query } );

print("Batch update phase 1 completed... " + Date());
