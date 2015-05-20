package com.ikanow.infinit.e.utility.archivetools;

import java.util.Random;

/**
 * Created by Mike Grill on 3/17/15.
 * Simple functions used through this application
 */
public class Common {

    /**
     * Generate a random string of specified length
     * @param length Length of random string
     * @return Random string
     */
    public static String randomValue( int length ){
        return randomValue( length, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789" );
    }

    /**
     * Generate a random string of specified size using specified characters
     * @param length Length of random string
     * @param set A string to be used as a pool of allowed characters
     * @return Random string
     */
    private static String randomValue( int length, String set){
        StringBuilder sb = new StringBuilder();
        // NOTE: Usually this should be a field rather than a method
        // variable so that it is not re-seeded every call.
        Random rand = new Random();
        for( int i = 0; i < length; i++){
            sb.append(set.charAt(rand.nextInt(set.length())));
        }
        return sb.toString();
    }

}
