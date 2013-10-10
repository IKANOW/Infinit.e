/////////////////////////////////////////////////////////////////////
//
// MongoDB map/reduce script to aggregate entity fields over time
//

oneday = 24*3600;
today = Math.floor((new Date().getTime()/1000)/oneday)*oneday;
yesterday = today - oneday; 
// (ie from yesterday)
my_query = { _id: { $gte: new ObjectId(yesterday.toString(16)+"0000000000000000"), $lt: new ObjectId(today.toString(16)+"0000000000000000") } };
//TO RUN ON ENTIRE DATASET
//my_query = { _id: { $lt: new ObjectId(yesterday.toString(16)+"0000000000000000") } };

map = function() {
	
	oneday = 24*3600*1000;
	var thisYear = this.publishedDate.getFullYear();
	var today = Math.floor(this.publishedDate.getTime()/oneday)*oneday;
	if (null != this.entities) {
		for (ent in this.entities) {
			var entity = this.entities[ent];
			var key = { i: entity.index, s: this.sourceKey, c: this.communityId, y: thisYear };
			var value = {};
			var valueEl = { c: 1 };
			value[today] = valueEl;
			if ((null != entity.sentiment) && (0 != entity.sentiment)) {
				value["cs"] = 1; // (just for debug)
				if (entity.sentiment > 0) {
					valueEl["p"] = entity.sentiment;
					valueEl["cp"] = 1;
				}
				else { // negative
					valueEl["n"] = entity.sentiment;					
					valueEl["cn"] = 1;
				}
			}
			emit(key, value);			
		}
	}
}

reduce = function(key, values) {
	
	var toemit = {};
	
	for (val in values) {
		var value = values[val]; // yearly container
		//(debug counter)
		if (null != value.cs) {
			if (null != toemit.cs) { 
				toemit.cs += value.cs;
			}
			else {
				toemit.cs = value.cs;
			}
		}
		//(end debug counter)
		for (day in value) {
			if (day == "cs") {
				continue;
			}
			var valday = value[day]; 
			var currval = toemit[day]; // daily container
			if (null == currval) { // first daily container for this year
				toemit[day] = valday;
			}
			else { // combine daily containers
				currval.c += valday.c;
				if (null != valday.cp) { // positive sentiment, if it exists
					if (null != currval.cp) {
						currval.cp += valday.cp;
						currval.p += valday.p;
					}
					else {
						currval.cp = valday.cp;
						currval.p = valday.p;						
					}//(end copy/combine positive sentiment)
				}
				if (null != valday.cn) { // negative sentiment, if it exists
					if (null != currval.cn) {
						currval.n += valday.n;
						currval.cn += valday.cn;
					}
					else {
						currval.n = valday.n;
						currval.cn = valday.cn;						
					}//(end copy/combine positive sentiment)
				}
			}
		}
	}
	return toemit;
}

db = db.getMongo().getDB("feature");
db.temporal.ensureIndex({"_id.i":1,"_id.s":1})
db = db.getMongo().getDB("doc_metadata");
print("Temporal Aggregation Starting: " + new Date());
db.metadata.mapReduce(map, reduce, { out: { reduce: "temporal", db: "feature", nonAtomic: true, sharded: true }, query: my_query} )
print("Temporal Aggregation Ending: " + new Date());
