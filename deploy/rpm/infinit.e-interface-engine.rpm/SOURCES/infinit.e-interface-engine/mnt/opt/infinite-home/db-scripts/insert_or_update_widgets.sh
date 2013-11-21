#!/bin/sh

PROPERTY_CONFIG_FILE='/opt/infinite-install/config/infinite.configuration.properties'
ADMIN_EMAIL=`grep "^admin.email=" $PROPERTY_CONFIG_FILE | sed s/'admin.email='// | sed s/' '//g`
if [ "$ADMIN_EMAIL" == "" ]; then
	ADMIN_EMAIL=infinite_default@ikanow.com
fi

mongo <<EOF

use gui;
module={ "_id" : ObjectId("4e4bcd7addda4e72000eeb76"), "approved" : true, "author" : "$ADMIN_EMAIL", "created" : ISODate("2011-08-17T14:17:30.959Z"), "description" : "Displays details of currently selected documents", "imageurl" : "./InfiniteDocBrowserWidget/Thumbnail.png", "modified" : ISODate("2011-08-17T14:17:35.638Z"), "searchterms" : [ "Dev", "Details", "Displays", "details", "of", "currently", "selected", "documents", "ikanow" ], "swf" : "", "title" : "Doc Browser", "url" : "./InfiniteDocBrowserWidget/InfiniteDocBrowserWidget.swf", "version" : "0.1" };
db.modules.insert(module);
db.modules.update({"_id": module._id}, {"\$set": {"url":module.url, "imageurl":module.imageurl}}, false, false);
module={ "_id" : ObjectId("4e4bf8adbce84e723f3b297d"), "approved" : true, "author" : "$ADMIN_EMAIL", "created" : ISODate("2011-08-17T17:21:49.910Z"), "description" : "Shows entities and their significance in a bar chart", "imageurl" : "./InfiniteSignificanceWidget/Thumbnail.png", "modified" : ISODate("2011-08-17T17:21:49.910Z"), "searchterms" : [ 	"Dev", 	"Shows", 	"entities", 	"and", 	"their", 	"significance", 	"in", 	"a", 	"bar", 	"chart" ], "swf" : "", "title" : "Entity Significance", "url" : "./InfiniteSignificanceWidget/InfiniteSignificanceWidget.swf", "version" : "0.1" };
db.modules.insert(module);
db.modules.update({"_id": module._id}, {"\$set": {"url":module.url, "imageurl":module.imageurl}}, false, false);
module={ "_id" : ObjectId("4e4bf8f5bce84e72423b297d"), "approved" : true, "author" : "$ADMIN_EMAIL", "created" : ISODate("2011-08-17T17:23:01.616Z"), "description" : "Shows documents on a timeline", "imageurl" : "./InfiniteTimelineWidget/Thumbnail.png", "modified" : ISODate("2011-08-17T17:23:01.616Z"), "searchterms" : [ "Dev", "Shows", "documents", "on", "a", "timeline" ], "swf" : "", "title" : "Timeline", "url" : "./InfiniteTimelineWidget/InfiniteTimelineWidget.swf", "version" : "0.1" };
db.modules.insert(module);
db.modules.update({"_id": module._id}, {"\$set": {"url":module.url, "imageurl":module.imageurl}}, false, false);
module={ "_id" : ObjectId("4e4bf986bce84e72453b297d"), "approved" : true, "author" : "$ADMIN_EMAIL", "created" : ISODate("2011-08-17T17:25:26.507Z"), "description" : "Displays documents geospatially on Google Maps", "imageurl" : "./InfiniteMapWidget/Thumbnail.png", "modified" : ISODate("2011-08-17T17:25:26.507Z"), "searchterms" : [ 	"Dev", 	"Map", 	"Displays", 	"documents", 	"geospatially", 	"on", 	"Google", 	"Maps" ], "swf" : "", "title" : "Map", "url" : "./InfiniteMapWidget/InfiniteMapWidget.swf", "version" : "0.1" };
db.modules.insert(module);
db.modules.update({"_id": module._id}, {"\$set": {"url":module.url, "imageurl":module.imageurl}}, false, false);
module={ "_id" : ObjectId("4e4bf986bce84e72463b297d"), "approved" : true, "author" : "$ADMIN_EMAIL", "created" : ISODate("2011-08-17T17:25:26.507Z"), "description" : "Breakdown of query results by source, entity, and associations", "imageurl" : "./InfiniteQueryMetricsWidget/Thumbnail.png", "modified" : ISODate("2011-08-17T17:25:26.507Z"), "searchterms" : [ 	"Dev", 	"Map", 	"Displays", 	"documents", 	"geospatially", 	"on", 	"Google", 	"Maps" ], "swf" : "", "title" : "Query Metrics", "url" : "./InfiniteQueryMetricsWidget/InfiniteQueryMetricsWidget.swf", "version" : "0.1" };
db.modules.insert(module);
db.modules.update({"_id": module._id}, {"\$set": {"url":module.url, "imageurl":module.imageurl}}, false, false);
module={ "_id" : ObjectId("4e4bf9ecbce84e72483b297d"), "approved" : true, "author" : "$ADMIN_EMAIL", "created" : ISODate("2011-08-17T17:27:08.580Z"), "description" : "Link analysis graph showing events and facts in a web", "imageurl" : "./InfiniteEventGraphWidget/Thumbnail.png", "modified" : ISODate("2011-08-17T17:27:08.580Z"), "searchterms" : [ 	"Dev", 	"Event", 	"Graph", 	"Link", 	"analysis", 	"graph", 	"showing", 	"events", 	"and", 	"facts", 	"in", 	"a", 	"web" ], "swf" : "", "title" : "Event Graph", "url" : "./InfiniteEventGraphWidget/InfiniteEventGraphWidget.swf", "version" : "0.1" };
db.modules.insert(module);
db.modules.update({"_id": module._id}, {"\$set": {"url":module.url, "imageurl":module.imageurl}}, false, false);
module={ "_id" : ObjectId("4e4bfa4cbce84e724b3b297d"), "approved" : true, "author" : "$ADMIN_EMAIL", "created" : ISODate("2011-08-17T17:28:44.698Z"), "description" : "Bar chart showing sentiment in documents", "imageurl" : "./InfiniteSentimentWidget/Thumbnail.png", "modified" : ISODate("2011-08-17T17:28:44.698Z"), "searchterms" : [ 	"Dev", 	"Sentiment", 	"Bar", 	"chart", 	"showing", 	"sentiment", 	"in", 	"documents" ], "swf" : "", "title" : "Sentiment", "url" : "./InfiniteSentimentWidget/InfiniteSentimentWidget.swf", "version" : "0.1" };
db.modules.insert(module);
db.modules.update({"_id": module._id}, {"\$set": {"url":module.url, "imageurl":module.imageurl}}, false, false);
module={ "_id" : ObjectId("4e4bfa90bce84e724e3b297d"), "approved" : true, "author" : "$ADMIN_EMAIL", "created" : ISODate("2011-08-17T17:29:52.418Z"), "description" : "Shows events on a timeline", "imageurl" : "./InfiniteTimelineEventWidget/Thumbnail.png", "modified" : ISODate("2011-08-17T17:29:52.418Z"), "searchterms" : [ 	"Dev", 	"Event", 	"Timeline", 	"Shows", 	"events", 	"on", 	"a", 	"timeline" ], "swf" : "", "title" : "Event Timeline", "url" : "./InfiniteTimelineEventWidget/InfiniteTimelineEventWidget.swf", "version" : "0.1" };
db.modules.insert(module);
db.modules.update({"_id": module._id}, {"\$set": {"url":module.url, "imageurl":module.imageurl}}, false, false);

EOF
