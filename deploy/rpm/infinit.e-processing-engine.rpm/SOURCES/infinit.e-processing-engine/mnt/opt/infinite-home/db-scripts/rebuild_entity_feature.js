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
        		
        		if ((entity.index != null) && (entity.frequency != null)) 
        		{ 
                        emit({ index: entity.index, comm: commid } , 
                        		{ dc: 1, tf: parseInt(entity.frequency), linkdata: entity.linkdata,
                        			alias: [ entity.actual_name ], type: entity.type, dim: entity.dimension, disname:entity.disambiguated_name, geotag:entity.geotag, ontology_type:entity.ontology_type } );
                }
            }
        }
}
r = function(k, vals) {
        var sum = { dc:0, tf:0, alias:[], linkdata: [], disname:"", type:"", dim:"",geotag:null, ontology_type:null };
        var set = {};
        var setLinkData = {};
        vals.forEach(function(value) 
        {
        		if (0 == sum.dc) { // first time
        			sum.disname = value.disname;
        			sum.dim = value.dim;
        			sum.type = value.type;
        		}
                sum.tf += value.tf;
                sum.dc += value.dc;
                // Alias:
                if (null != value.alias[0]) { // (apparently null is allowed)
	                if (!set.hasOwnProperty(value.alias[0])) {
	                	set[value.alias[0]] = 1;
	                	sum.alias.push(value.alias[0]);
	                }
                }
                if (null != value.linkdata) {
	                for (var i = 0; i < value.linkdata.length; ++i) {
	                	var link = value.linkdata[i]
		                if (!setLinkData.hasOwnProperty(link)) {
		                	setLinkData[link] = 1;
		                	sum.linkdata.push(link);
		                }                	
	                }
                }
                if ( value.geotag != null )
               	{
                	sum.geotag = value.geotag;
               	}
                if ( value.ontology_type != null )
                {
                	sum.ontology_type = value.ontology_type;
                }
        });
        return sum;
}

print("Entity feature rebuild, starting... (note: if you want to delete the index first, you need to do that manually)" + Date());
res = db.metadata.mapReduce( m, r, { out: { replace: "tmpCalcFreqCounts" }, query: my_query, limit: my_limit } );
//Tidy up/finalize performed below

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

r3 = function(k, vals) 
{
		var val = null;
		if (vals.length > 1) {
				val = (vals[0].dc == null ? vals[1] : vals[0]);
		}
		else val = vals[0];
		if (null == val.dc) {
		    return {dc:null};		
		}
	
        var setjson = { doccount: val.dc, totalfreq: val.tf,
        				db_sync_time: new Date().getTime().toString(), db_sync_doccount: val.dc,
        		type: val.type, dimension: val.dim, disambiguated_name: val.disname };
        if ( val.geotag == null && val.dim == "Where" )
        {
			//performance note: cur.count() will cause the cursor to get all results, which is slow.. could do something like cur.next != null and cur.next == null
			//that might be faster (i.e. check if cur[0] exists and that cur[1] does not exist (only 1 result))
        	//attempt to lookup geo in feature.geo table
        	//attempt1 look on searchterm, if only 1 match.. jackpot
        	var firsttry = k.index.substring(0,k.index.lastIndexOf("/")); //this should always be in the form of ent/type
			var cur = db.geo.find({"geoindex":{$exists:true},"search_field":firsttry.toLowerCase()}); //search /^term$/i
			if ( cur.count() == 1 )
			{
				//found one! set loc to this
				var temp = cur.next();
				val.geotag = temp.geoindex;
				if ( temp.city != null )	val.ontology_type = "city";
				else if ( temp.region != null )	val.ontology_type = "countrysubsidiary";
				else if ( temp.country != null )	val.ontology_type = "country";
				else	val.ontology_type = "point";
			}
			else
			{
				//cases i've seen and cover here:
				//REMEMBER: to search on the primary term in search_term (i.e. cannot do region:new jersey,country:united states because you will get every city in new jersey returning)
				//case 1 city,region,country e.g. blacksburg,virginia,united states
				//case 2 region,country e.g. new jersey,united states
				var secondtry = firsttry.split("\\s*,\\s*");				
				if ( secondtry.length > 2 ) //has 3 terms or more, try to do city,region,country
				{
					var cur2 = db.geo.find({"geoindex":{$exists:true},"search_field":secondtry[0].toLowerCase(),"region":{$regex:'^' + secondtry[1] + '$', $options: 'i'},"country":{$regex:'^' + secondtry[2] + '$', $options: 'i'}});
					if (cur2.count() == 1 ) //do we want to use anything matching here? even if there are more than 1 hit on city,region,country it's unlikely they are far apart?
					{
						//found a match set loc to this
						val.geotag = cur2.next().geoindex;
						val.ontology_type = "city";
					}
				}
				else if ( secondtry.length > 1 ) //has 2 terms, try region,country
				{
					var cur2 = db.geo.find({"geoindex":{$exists:true},"search_field":secondtry[0].toLowerCase(),"country":{$regex:'^' + secondtry[1] + '$', $options: 'i'}});
					if (cur2.count() == 1 ) //do we want to use anything matching here? even if there are more than 1 hit on city,region,country it's unlikely they are far apart?
					{
						//found a match set loc to this
						val.geotag = cur2.next().geoindex;
						val.ontology_type = "countrysubsidiary";
					}
				}
			}
        }
        //add loc to set
        if ( val.geotag != null)
        {
        	setjson.geotag = val.geotag;
        }
        if ( val.ontology_type != null )
        {
        	setjson.ontology_type = val.ontology_type;
        }
        var json = { $set : setjson,
      		  $addToSet: { alias: { $each: val.alias } }
    	};
        if (null != val.linkdata) {
	        if ( val.linkdata.length > 0) {
	        	(json["$addToSet"]).linkdata = { $each: val.linkdata };
	        }
        }
        db.entity.update({ index: k.index, communityId: k.comm }, 
         		json, true, false);
        
        return { dc: null };
}

print("Entity feature completion, starting... " + Date());
db = db.getMongo().getDB( "feature" );
db.entity.ensureIndex({"index":1}); // (in case it's been dropped rather than just emptied...)
res = db.tmpCalcFreqCounts.mapReduce( m3, r3, { out: "tmpCalcFreqCounts3" } );

//Tidy up:
db.tmpCalcFreqCounts.drop();
db.tmpCalcFreqCounts3.drop();

print("Batch update completed... " + Date());
