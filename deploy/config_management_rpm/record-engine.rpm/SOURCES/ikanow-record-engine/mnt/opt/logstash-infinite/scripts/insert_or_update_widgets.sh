#!/bin/sh

PROPERTY_CONFIG_FILE='/opt/infinite-install/config/infinite.configuration.properties'
ADMIN_EMAIL=`grep "^admin.email=" $PROPERTY_CONFIG_FILE | sed s/'admin.email='// | sed s/' '//g`
if [ "$ADMIN_EMAIL" == "" ]; then
	ADMIN_EMAIL=infinite_default@ikanow.com
fi

mongo <<EOF

use gui;
module={ "_id" : ObjectId("4e3bcd7feeea4e72000eeb76"), "approved" : true, "author" : "$ADMIN_EMAIL", "created" : ISODate("2014-01-01T14:17:30.959Z"), "description" : "Connector to the Kibana record analyzer", "imageurl" : "/infinit.e.records/static/InfiniteKibanaConnector/Thumbnail.png", "modified" : ISODate("2014-01-01T14:17:30.959Z"), "searchterms" : [ "Connector", "Kibana", "Record", "Analyzer" ], "swf" : "", "title" : "Record Analyzer", "url" : "/infinit.e.records/static/InfiniteKibanaConnector/InfiniteKibanaConnector.swf", "version" : "0.1" };
db.modules.insert(module);
db.modules.update({"_id": module._id}, {"\$set": {"url":module.url, "imageurl":module.imageurl}}, false, false);
module={ "_id" : ObjectId("4e3bcd6feeea4e62000eeb67"), "approved" : true, "author" : "$ADMIN_EMAIL", "created" : ISODate("2015-11-01T14:17:30.959Z"), "description" : "Connector to the Kibana V2 analyzer", "imageurl" : "/infinit.e.records/static/InfiniteV2KibanaConnector/Thumbnail.png", "modified" : ISODate("2015-11-01T14:17:30.959Z"), "searchterms" : [ "Connector", "Kibana", "V2", "Analyzer" ], "swf" : "", "title" : "V2 Data Viewer", "url" : "/infinit.e.records/static/InfiniteV2KibanaConnector/InfiniteV2KibanaConnector.swf", "version" : "0.1" };
db.modules.insert(module);
db.modules.update({"_id": module._id}, {"\$set": {"url":module.url, "imageurl":module.imageurl}}, false, false);

EOF