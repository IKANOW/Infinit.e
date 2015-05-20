package com.ikanow.infinit.e.utility.archivetools;

import org.bson.BSONObject;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Mike Grill on 3/13/15.
 * Wrapper for mongodb command line tools.
 */
public class MongoCli extends Cli {

    private String _hostname = "localhost";
    private String _username = "";
    private String _password = "";
    private int _port = 27017;

    public MongoCli(){
        this(null, null, null, null);
    }

    /**
     * Create a new MongoCli object, inherit values from another MongoCli Object
     * @param props MongoCli object to inherit config from
     */
    public MongoCli( MongoCli props ){
        this._hostname = props.getHostname();
        this._username = props.getUsername();
        this._password = props.getPassword();
        this._port = props.getPort();
    }

    /**
     * Create a new ArchiveTools application instance.
     * @param mongoHostname Mongo server hostname or IP address, 127.0.0.1 by default.
     * @param mongoUsername If mongo security is enabled, the username for authentication
     * @param mongoPassword The password for the mongo auth user if required. ( default: null )
     * @param mongoPort Mongo server port number, optional - 27017 by default
     */
    public MongoCli( String mongoHostname, String mongoUsername, String mongoPassword, Integer mongoPort ){
        this._hostname = mongoHostname != null ? mongoHostname : this._hostname;
        this._username = mongoUsername != null ? mongoUsername : this._username;
        this._password = mongoPassword != null ? mongoPassword : this._password;
        this._port = mongoPort != null ? mongoPort : this._port;
    }

    public String getHostname() {
        return _hostname;
    }
    public String getUsername(){
        return _username;
    }
    public String getPassword(){
        return this._password;
    }
    public Integer getPort(){
        return _port;
    }

    /**
     * Generate a query to select a community record.
     * @param communityId ID of community to search for.
     * @return String query
     */
    protected String getCommunityCollectionQuery( String communityId ){
        return String.format("{_id:ObjectId(\"%s\")}", communityId);
    }

    /**
     * Generate a query to select a community's documents.
     * @param communityId ID of community
     * @return String query
     */
    protected String getGzipContentCollectionQuery( String communityId ){
        return String.format("{communityId:ObjectId(\"%s\")}", communityId);
    }

    /**
     * Generate a query to select a community's document metadata.
     * @param communityId ID of community
     * @return String query
     */
    protected String getDocMetadataCollectionQuery( String communityId ){
        return String.format("{communityId:ObjectId(\"%s\")}", communityId);
    }

    /**
     * Generate a query to select a community's sources.
     * @param communityId ID of community.
     * @return String query.
     */
    protected String getSourceCollectionQuery( String communityId ){
        return String.format("{communityIds:ObjectId(\"%s\")}", communityId);
    }

    /**
     * Use mongodump to fetch a community object.
     * @param communityId Community ID to fetch
     * @return BSONObject of community record.
     */
    protected BSONObject getCommunityBSON(String communityId){

        Process dumpProcess = mongodump("social", "community", getCommunityCollectionQuery(communityId));
        ProcessResult dumpResult = getProcessResult(dumpProcess);

        if( dumpResult.getExitCode() > 0){
            System.err.printf("[MongoCli->getCommunityBSON] Mongodump exited with error(%d): %s%n",
                    dumpResult.getExitCode(), dumpResult.getStdErr() );
        }

        if( dumpResult.hasStdOut()){
            return org.bson.BSON.decode(dumpResult.getStdOut());
        }

        return null;
    }

    /**
     * Generate a mongodump command
     * @param db Database to use
     * @param collection Collection to dump
     * @param q Query to select items
     * @return Process of mongodump
     */
    protected Process mongodump( String db, String collection, String q ){

        //Base command
        ArrayList<String> cmd = new ArrayList<>();
        cmd.add("mongodump");
        cmd.add("--db");
        cmd.add(db);
        cmd.add("--collection");
        cmd.add(collection);
        cmd.add("--out");
        cmd.add("-");
        cmd.add("-q");
        cmd.add(q);

        if( null != this.getHostname() && !this.getHostname().isEmpty() ){
            cmd.add("--host");
            cmd.add(this.getHostname());
        }

        if( null != this.getUsername() && !this.getUsername().isEmpty() ){
            cmd.add("--username");
            cmd.add(this.getUsername());
        }

        if( null != this.getPassword() && !this.getPassword().isEmpty() ){
            cmd.add("--password");
            cmd.add(this.getPassword());
        }

        if( null != this.getPort() ){
            cmd.add("--port");
            cmd.add(this.getPort().toString());
        }

        String[] cmdArr = cmd.toArray(new String[cmd.size()]);
        return getProcess(cmdArr);
    }

    protected String getMongoDbVersion(){

        //Base command
        ArrayList<String> cmd = new ArrayList<>();
        cmd.add("mongo");
        cmd.add("--quiet");
        cmd.add("--eval");
        cmd.add("db.version()");
        String[] cmdArr = cmd.toArray(new String[cmd.size()]);

        ProcessResult pr = null;
        try {
            pr = new ProcessResult(getProcess(cmdArr));
            return new String(pr.getStdOut());
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }

        return "";
    }

    protected String getMongoRestoreVersion(){

        //Base command
        ArrayList<String> cmd = new ArrayList<>();
        cmd.add("mongorestore");
        cmd.add("--version");
        String[] cmdArr = cmd.toArray(new String[cmd.size()]);

        ProcessResult pr = null;
        try {
            pr = new ProcessResult(getProcess(cmdArr));
            String cliOutput = new String(pr.getStdOut());
            return cliOutput.replaceAll("[^0-9]*([0-9\\.]*) *?","$1");
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }

        return "";
    }

    /**
     * Prepare a mongorestore command to restore a mongo collection from a BSON file ( or named pipe )
     * @param db Database to restore in to
     * @param collection Collection to restore
     * @param bsonFilename Filename or named pipe of BSON data.
     * @return Process of mongo restore.
     */
    protected Process mongorestore( String db, String collection, String bsonFilename ){

        //Base command
        ArrayList<String> cmd = new ArrayList<>();
        cmd.add("mongorestore");
        cmd.add("--db");
        cmd.add(db);
        cmd.add("--collection");
        cmd.add(collection);
        cmd.add(bsonFilename);

        String[] cmdArr = cmd.toArray(new String[cmd.size()]);
        return getProcess(cmdArr);
    }

}
