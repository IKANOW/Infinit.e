x = {
		"_id" : ObjectId("504f63b5e4b0ced482f8b490"),
        "jobtitle" : "HadoopJavascriptTemplate",
        "jobdesc" : "The template for Hadoop Javascript Prototyping Engine",
        "submitterID" : ObjectId("4e3706c48d26852237078005"),
        "communityIds" : [
                ObjectId("4c927585d591d31d7b37097a")
        ],
        "jarURL" : "file:///opt/infinite-home/lib/plugins/infinit.e.hadoop.prototyping_engine.jar",
        "inputCollection" : "doc_metadata.metadata",
        "outputCollection" : "504f63b5e4b0ced482f8b490_1",
        "nextRunTime" : NumberLong("4070908800000"),
        "jobidN" : 0,
        "scheduleFreq" : "NONE",
        "firstSchedule" : ISODate("2099-01-01T00:00:00Z"),
        "timesRan" : 0,
        "timesFailed" : 0,
        "isCustomTable" : false,
        "mapper" : "com.ikanow.infinit.e.utility.hadoop.HadoopPrototypingTool$JavascriptMapper",
        "reducer" : "com.ikanow.infinit.e.utility.hadoop.HadoopPrototypingTool$JavascriptReducer",
        "combiner" : "com.ikanow.infinit.e.utility.hadoop.HadoopPrototypingTool$JavascriptCombiner",
        "query" : "{ }",
        "outputKey" : "com.mongodb.hadoop.io.BSONWritable",
        "outputValue" : "com.mongodb.hadoop.io.BSONWritable",
        "mapProgress" : 1,
        "reduceProgress" : 1,
        "appendResults" : false,
        "appendAgeOutInDays" : 0,
        "jobDependencies" : [ ],
        "waitingOn" : [ ],
        "isUpdatingOutput" : false,
        "outputCollectionTemp" : "504f63b5e4b0ced482f8b490_2",
        "arguments" : "function map(key, val) {\r\n  emit( {'id': val._id}, val );\r\n} \r\nfunction reduce(key, vals) { \r\n   for (x in vals) {\r\n      emit( key, vals[x] ); \r\n      break;\r\n   }\r\n}\r\ncombine = reduce;\r\n",
        "exportToHdfs" : false
};
db.customlookup.insert(x);

