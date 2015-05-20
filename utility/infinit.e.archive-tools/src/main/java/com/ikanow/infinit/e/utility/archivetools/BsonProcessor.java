package com.ikanow.infinit.e.utility.archivetools;

import org.bson.BSONObject;

/**
 * Created by Mike Grill  on 3/20/15.
 * Just an simple class to override for standard callbacks while processing BSON Objects
 */
public abstract class BsonProcessor {

    /**
     * This must be implemented by any filter extending this callback
     * @param record BSON Object to be prepared
     */
    public abstract BSONObject prepareBsonObject( BSONObject record );

}
