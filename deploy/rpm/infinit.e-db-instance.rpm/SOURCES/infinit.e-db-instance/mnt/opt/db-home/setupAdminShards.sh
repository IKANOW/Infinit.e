#!/bin/sh
################################################################################
# This script is intended to set up Infinit.e's Mongo DB for the Initial Install
################################################################################
PROPERTY_CONFIG_FILE='/opt/infinite-install/config/infinite.configuration.properties'
ADMIN_EMAIL=`grep "^admin.email=" $PROPERTY_CONFIG_FILE | sed s/'admin.email='// | sed s/' '//g`
if [ "$ADMIN_EMAIL" == "" ]; then
	ADMIN_EMAIL=infinite_default@ikanow.com
fi
ADMIN_PWD=`grep "^admin.password=" $PROPERTY_CONFIG_FILE | sed s/'admin.password='// | sed s/' '//g`
TEST_USER_EMAIL=`grep "^test.user.email=" $PROPERTY_CONFIG_FILE | sed s/'test.user.email='// | sed s/' '//g`
if [ "$TEST_USER_EMAIL" == "" ]; then
	TEST_USER_EMAIL=test_user@ikanow.com
fi

SERVICE_PROPERTY_FILE='/opt/infinite-home/config/infinite.service.properties'
DB_SHARDED=`grep "^db.sharded=" $SERVICE_PROPERTY_FILE | sed s/'db.sharded='// | sed s/' '//g`

cur_date=$(date +%Y-%m-%dT%TZ)
subscrip_date=$(date +'%b %e, %Y %l:%M:%S %p')

IS_MONGOS=$(infdb is_mongos)
echo "This is a MongoS: $IS_MONGOS"
IS_DBINSTANCE=$(infdb is_dbinstance)
echo "This is a DB instance: $IS_DBINSTANCE"

################################################################################
# Start - Implement sharding if specified in infinite.service.properties
if [[ $IS_MONGOS == "true" && $DB_SHARDED == "1" ]]; then
	echo "Setup Sharding on this MongoS"

	mongo <<EOF
use admin;
db.runCommand( { enablesharding : "feature" } );
db.runCommand( { shardcollection : "feature.entity", key : {index : 1} } );
db.runCommand( { shardcollection : "feature.association", key : {index: 1} } );
db.runCommand( { enablesharding : "doc_content" } );
db.runCommand( { shardcollection : "doc_content.gzip_content", key : {sourceKey:1, url:1} } );
db.runCommand( { enablesharding : "doc_metadata" } );
db.runCommand( { shardcollection : "doc_metadata.metadata", key : {_id :1} } );
exit
EOF

fi
# End - Implement sharding

		
################################################################################
# Start - Add administrative and test accounts to the DB
if [ $IS_DBINSTANCE == "true" ]; then
	echo "Adding administrative and test account plus default widgets to the DB"
	
	ADMIN_PWD_ENC=$(echo -n $ADMIN_PWD  | sha256sum | xxd -r -p | base64)
	TEST_USER_PWD_ENC=$(echo -n $TEST_USER_PWD  | sha256sum | xxd -r -p | base64)
	
	mongo <<EOF
use social;
community={"_id" : ObjectId("4c927585d591d31d7b37097a"),"created" : ISODate("$cur_date"),"modified" : ISODate("$cur_date"), "ownerId": ObjectId("4e3706c48d26852237078005"), "ownerDisplayName": "Admin Infinite", "name" : "Infinit.e System Community","description" : "Infinit.e System Community","isSystemCommunity" : true,"isPersonalCommunity" : false,"communityAttributes" : {"usersCanCreateSubCommunities" : {"type" : "boolean","value" : "false"},"registrationRequiresApproval" : {"type" : "boolean","value" : "true"},"isPublic" : {"type" : "boolean","value" : "true"},"usersCanSelfRegister" : {"type" : "boolean","value" : "false"}},"userAttributes" : {"publishCommentsPublicly" : {"type" : "boolean","defaultValue" : "false","allowOverride" : true},"publishQueriesToActivityFeed" : {"type" : "boolean","defaultValue" : "true","allowOverride" : true},"publishLoginToActivityFeed" : {"type" : "boolean","defaultValue" : "true","allowOverride" : true},"publishSharingToActivityFeed" : {"type" : "boolean","defaultValue" : "true","allowOverride" : true},"publishCommentsToActivityFeed" : {"type" : "boolean","defaultValue" : "true","allowOverride" : true}},"communityStatus" : "active","numberOfMembers" : 2};
db.community.insert(community);
person1={"_id" : ObjectId("4e3706c48d26852237078005"),"created" : ISODate("$cur_date"),"modified" : ISODate("$cur_date"),"accountStatus" : "active","email" : "$ADMIN_EMAIL","firstName" : "Admin","lastName" : "Infinite","displayName" : "Admin Infinite","phone" : "","communities" : [{"_id" : ObjectId("4e3706c48d26852237078005"), "name" : "Admin Infinite's Personal Community"}, {"_id" : ObjectId("4c927585d591d31d7b37097a"), "name" : "Infinit.e System Community"}],"WPUserID" : "174","SubscriptionID" : "189","SubscriptionTypeID" : "1","SubscriptionStartDate" : "$subscrip_date"};
db.person.insert(person1);
community={"_id" : ObjectId("4e3706c48d26852237078005"),"created" : ISODate("$cur_date"),"modified" : ISODate("$cur_date"),"name" : "Admin Infinite's Personal Community","description" : "Admin Infinite's Personal Community","isSystemCommunity" : false,"isPersonalCommunity" : true,"communityAttributes" : {"usersCanCreateSubCommunities" : {"type" : "boolean","value" : "false"},"registrationRequiresApproval" : {"type" : "boolean","value" : "false"},"isPublic" : {"type" : "boolean","value" : "false"},"usersCanSelfRegister" : {"type" : "boolean","value" : "false"}},"userAttributes" : {"publishCommentsPublicly" : {"type" : "boolean","defaultValue" : "false","allowOverride" : true},"publishQueriesToActivityFeed" : {"type" : "boolean","defaultValue" : "true","allowOverride" : true},"publishLoginToActivityFeed" : {"type" : "boolean","defaultValue" : "true","allowOverride" : true},"publishSharingToActivityFeed" : {"type" : "boolean","defaultValue" : "true","allowOverride" : true},"publishCommentsToActivityFeed" : {"type" : "boolean","defaultValue" : "true","allowOverride" : true}},"communityStatus" : "active","numberOfMembers" : 0};
db.community.insert(community);
var isUserPresent = db.community.findOne({"_id" : ObjectId("4c927585d591d31d7b37097a"), "members._id": ObjectId("4e3706c48d26852237078005")},{"_id":1});
if (null == isUserPresent) db.community.update({ "_id" : ObjectId("4c927585d591d31d7b37097a") }, { "\$push": { "members": { "_id": ObjectId("4e3706c48d26852237078005"), "email": "$ADMIN_EMAIL", "displayName": "Admin Infinite", "userType": "owner", "userStatus":"active" } } });
use security;
auth={"WPUserID" : null,"_id" : ObjectId("4ca4a7c5b94b6296f3469d36"),"accountStatus" : "ACTIVE","accountType" : "Admin","created" : ISODate("$cur_date"),"modified" : ISODate("$cur_date"),"password" : "$ADMIN_PWD_ENC","profileId" : ObjectId("4e3706c48d26852237078005"),"username" : "$ADMIN_EMAIL"};
db.authentication.insert(auth);
use social;
person2={"_id" : ObjectId("4e3706c48d26852237079004"),"created" : ISODate("$cur_date"),"modified" : ISODate("$cur_date"),"accountStatus" : "active","email" : "$TEST_USER_EMAIL","firstName" : "Test","lastName" : "Infinite", "displayName" : "Test User", "Account" : "Test Account","phone" : "","communities" : [{"_id" : ObjectId("4e3706c48d26852237079004"), "name" : "Test Account's Personal Community"}, {"_id" : ObjectId("4c927585d591d31d7b37097a"), "name" : "Infinit.e System Community"}],"WPUserID" : "001","SubscriptionID" : "001","SubscriptionTypeID" : "1","SubscriptionStartDate" : "$subscrip_date"};
db.person.insert(person2);
community={"_id" : ObjectId("4e3706c48d26852237079004"),"created" : ISODate("$cur_date"),"modified" : ISODate("$cur_date"),"name" : "Test Accounts's Personal Community","description" : "Test Accounts's Personal Community","isSystemCommunity" : false,"isPersonalCommunity" : true,"communityAttributes" : {"usersCanCreateSubCommunities" : {"type" : "boolean","value" : "false"},"registrationRequiresApproval" : {"type" : "boolean","value" : "false"},"isPublic" : {"type" : "boolean","value" : "false"},"usersCanSelfRegister" : {"type" : "boolean","value" : "false"}},"userAttributes" : {"publishCommentsPublicly" : {"type" : "boolean","defaultValue" : "false","allowOverride" : true},"publishQueriesToActivityFeed" : {"type" : "boolean","defaultValue" : "true","allowOverride" : true},"publishLoginToActivityFeed" : {"type" : "boolean","defaultValue" : "true","allowOverride" : true},"publishSharingToActivityFeed" : {"type" : "boolean","defaultValue" : "true","allowOverride" : true},"publishCommentsToActivityFeed" : {"type" : "boolean","defaultValue" : "true","allowOverride" : true}},"communityStatus" : "active","numberOfMembers" : 0};
db.community.insert(community);
var isUserPresent = db.community.findOne({"_id" : ObjectId("4c927585d591d31d7b37097a"), "members._id": ObjectId("4e3706c48d26852237079004")},{"_id":1});
if (null == isUserPresent) db.community.update({ "_id" : ObjectId("4c927585d591d31d7b37097a") }, { "\$push": { "members": { "_id": ObjectId("4e3706c48d26852237079004"), "email": "$TEST_USER_EMAIL", "displayName": "Test User", "userType": "member", "userStatus":"active" } } });
use security;
auth={"WPUserID" : null,"_id" : ObjectId("4ca4a7c5b94b6296f3468d35"),"accountStatus" : "ACTIVE","accountType" : "User","created" : ISODate("$cur_date"),"modified" : ISODate("$cur_date"),"password" : "$TEST_USER_PWD_ENC", "profileId" : ObjectId("4e3706c48d26852237079004"),"username" : "$TEST_USER_EMAIL"};
db.authentication.insert(auth);
EOF

fi	

