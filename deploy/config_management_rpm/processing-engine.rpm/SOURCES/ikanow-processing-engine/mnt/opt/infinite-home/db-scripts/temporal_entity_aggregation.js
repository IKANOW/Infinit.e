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

		try {
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
	                value.maxDay = today;
	                value.minDay = today;
	                value.lastUpdated=new Date();
	      			emit(key, value);
	      		}
	      	}
		}
		catch (e) {}
}

reduce = function(key, values) {

         	var toemit = {};
         	var minDay=null;
         	var maxDay=null;

         	toemit.lastUpdated = new Date();

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
         			if (day == "cs" || day=="maxDay" || day=="minDay" || day=="lastUpdated") {
         				continue;
         			}

         			if (minDay==null)
         				minDay=day;
         			else if (day < minDay)
         				minDay=day;

         			if (maxDay==null)
         				maxDay=day;
         			else if (day > maxDay)
         				maxDay = day;

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

         	toemit.minDay = minDay;
         	toemit.maxDay = maxDay;
         	return toemit;
}

db = db.getMongo().getDB("feature");
a = db.temporal.findOne();
if ((null != a) && ( (null == a.value) || (null == a.value.maxDay) )) {
    print("Temporal One time Structure change Starting: " + new Date());

	db.temporal.mapReduce(function() {

        var maxDay=null;
        var minDay=null;

        for (day in this.value) {
            if (day == "cs") {
		continue;
            }

            if (minDay==null)
		minDay=day;
            else if (day < minDay)
		minDay=day;

            if (maxDay==null)
		maxDay=day;
            else if (day > maxDay)
		maxDay = day;
	}

        this.value.maxDay=maxDay;
        this.value.minDay=minDay;
        this.value.lastUpdated=new Date();

        emit(this._id, this.value);
},
function(key, values) {
    return values[0];
},
{ out: { merge:"temporal",  db: "feature", nonAtomic: true, sharded: true }});

}
else
	print("No need to update Temporal structure");

db.temporal.ensureIndex({"_id.i":1,"_id.s":1})
db.temporal.ensureIndex({"_id.c":1,"value.maxDay":1, "value.minDay":1 })

db = db.getMongo().getDB("doc_metadata");
print("Temporal Aggregation Starting: " + new Date());
db.metadata.mapReduce(map, reduce, { out: { reduce: "temporal", db: "feature", nonAtomic: true, sharded: true }, query: my_query} )
print("Temporal Aggregation Ending: " + new Date());
