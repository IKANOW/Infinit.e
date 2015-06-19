package com.ikanow.infinit.e.utility.archivetools;

import java.util.ArrayList;

/**
 * Created by Mike Grill on 3/19/15.
 * Wrapper for any Infinite shell scripts needed
 */
public class InfiniteCli extends Cli {

    /**
     * Run infinite_indexer.sh on a given community
     * @param communityId Community ID to index
     */
    public boolean indexCommunity( String communityId ){
        return indexCommunity(communityId, "/opt/infinite-home/bin");
    }

    /**
     * Run infinite_indexer.sh on a given community, with a given path to infinite_indexer.sh
     * @param communityId Community ID to index
     * @param indexerPath Absolute path to infinite_indexer.sh
     */
    public boolean indexCommunity( String communityId, String indexerPath ){
        //Base command
        ArrayList<String> cmd = new ArrayList<String>();
        cmd.add("sh");
        cmd.add( String.format( "%s/infinite_indexer.sh", indexerPath ));
        cmd.add("--doc");
        cmd.add("--rebuild");
        cmd.add("--query");
        cmd.add( String.format( "{\"communityId\":{\"$oid\":\"%s\"}}", communityId) );

        String[] cmdArr = cmd.toArray(new String[cmd.size()]);

        ProcessResult pr = getProcessResult(getProcess(cmdArr));

        if( pr.getExitCode() != 0 ){
            System.out.printf("[InfiniteCli->indexCommunity] Indexer failed (%d): %s%n", pr.getExitCode(), new String(pr.getStdErr()) );
            return false;
        }

        return true;
    }

}
