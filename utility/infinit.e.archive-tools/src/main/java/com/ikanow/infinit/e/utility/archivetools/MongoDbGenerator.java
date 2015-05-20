package com.ikanow.infinit.e.utility.archivetools;

import com.mongodb.*;
import org.bson.types.ObjectId;

import java.net.UnknownHostException;

/**
 * Created by Mike Grill on 3/13/15.
 * A tool to generate data to the collections used in communities.
 * This was only created to assist in local testing / development
 * and should never be used on a production system.
 */
public class MongoDbGenerator extends MongoCli {

    MongoClient mongoClient;

    public MongoDbGenerator( MongoCli props ){
        super(props);

        try {
            mongoClient = new MongoClient();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

    }


    /**
     * Generate a random DB Object for testing
     * Each object will have a random new ID as well as the following properties
     *      {
     *          communityId: ${communityId},
     *          communityIds: [ ObjectID(${communityId}) ],
     *      }

     * @param communityId Community ID of simulation
     * @param propertyCount # of properties to create for this object
     * @param keySize String length of property keys
     * @param valueSize String length of property values.
     * @return New DBObject with random data and injected community IDs
     */
    private DBObject randomObject( String communityId, int propertyCount, int keySize, int valueSize ){

        //Create new DBObject
        BasicDBObject tmpObject = new BasicDBObject();

        //Append communityId: property
        tmpObject.append("communityId", new ObjectId(communityId));

        //Append communityIds list property
        BasicDBList cids = new BasicDBList();
        cids.add( new ObjectId(communityId) );
        tmpObject.append( "communityIds", cids );

        //Add ${propertyCount} random properties
        for( int propertyX = 0; propertyX < propertyCount; propertyX++ ) {
            tmpObject.append(Common.randomValue(keySize), Common.randomValue(valueSize));
        }
        return tmpObject;
    }

    private DBObject getTestingCommunity(){

        DB mongoDb = mongoClient.getDB("social");
        if( null == mongoDb) {
            System.err.println("[MongoDbGenerator->getTestingCommunity] Could not select database 'social'");
            return null;
        }

        DBCollection collection = mongoDb.getCollection("community");
        if( null == collection) {
            System.err.println("[MongoDbGenerator->getTestingCommunity] Could not open collection 'community'");
            return null;
        }

        BasicDBList testingTags = new BasicDBList();
        testingTags.add("testing-community");

        DBObject testingCommunity = collection.findOne(new BasicDBObject("tags", testingTags));
        if( null == testingCommunity ){
            //No existing tst community found. make one.
            DBObject newCommunity = new BasicDBObject("tags", testingTags);
            WriteResult res = collection.insert(newCommunity);
            testingCommunity = collection.findOne(new BasicDBObject("tags", testingTags));
        }

        return testingCommunity;
    }

    public boolean fillCollection(String communityId, String db, String collection, int items){

        try {
            DB mongoDb = mongoClient.getDB(db);
            if( !mongoDb.collectionExists(collection) ){
                mongoDb.createCollection(collection, null);
            }
            DBCollection mongoCollection = mongoDb.getCollection(collection);

            for( Integer docX = 0; docX < items; docX++ ) {
                WriteResult result = mongoCollection.insert(
                    randomObject( communityId, 10, 10, 100 )
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Fill the collections used by communities with garbage data. This is dangerous. and used for initial testing.
     * @return true on succcess false otherwise
     */
    public String fillCommunityCollections(){

        String communityId = "";

        //Add a testing community
        DBObject community = this.getTestingCommunity();

        if( null == community){
            System.out.println("[MongoDbGenerator->fillCommunityCollections] Could not get/create testing community from social/community.");
            return "";
        } else {
            System.out.printf("[MongoDbGenerator->fillCommunityCollections] CommunityID: %s%n", community.get("_id").toString() );
            communityId = community.get("_id").toString();
        }

        if( !this.fillCollection(communityId, "doc_content",   "gzip_content", 100000) ){


            return "";
        }
        if( !this.fillCollection(communityId, "doc_metadata",  "metadata",     100000) ){


            return "";
        }
        if(!this.fillCollection(communityId, "ingest",        "source",       2)){


            return "";
        }

        return communityId;
    }

}
