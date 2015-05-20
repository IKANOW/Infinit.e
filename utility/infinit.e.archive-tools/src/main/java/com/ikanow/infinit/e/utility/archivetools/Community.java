package com.ikanow.infinit.e.utility.archivetools;

import com.google.common.io.Files;
import com.ikanow.infinit.e.utility.archivetools.exception.FileFoundException;
import com.mongodb.MongoException;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.bson.BSON;
import org.bson.BSONObject;
import org.bson.BasicBSONDecoder;
import org.bson.types.BasicBSONList;
import org.bson.types.ObjectId;

import java.io.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Michael Grill  on 3/10/15.
 * This object manages the process of archiving a community and associated data.
 */
public class Community extends MongoCli {

    /**
     * Mongorestore 3.x+ supports pipes. < 3.x we need to use temp files :(
     */
    protected boolean _usePipes = false;

    public Community(MongoCli props) {
        super(props);
    }

    /**
     * No filter version of archive collection.
     *
     * @param zipOutputStream Destination zip file output stream
     * @param database Mongo database to use
     * @param collection Mongo collection to archive
     * @param q A query to limit results
     * @return The number of documents archived.
     */
    private int archiveCollection(final ZipArchiveOutputStream zipOutputStream, String database, String collection, String q) {
        return archiveCollection(zipOutputStream, database, collection, q, new BsonProcessor(){
            @Override
            public BSONObject prepareBsonObject(BSONObject record) {
                return record;
            }
        });
    }

    /**
     * Archive a collection with a given filter.
     *
     * @param zipOutputStream Destination zip file output stream
     * @param database Mongo database to use
     * @param collection Mongo collection to archive
     * @param q A query to limit results
     * @param bsonFilter A callback to operate on each record before saving
     * @return The number of documents archived.
     */
    private int archiveCollection(final ZipArchiveOutputStream zipOutputStream, String database,
                                  String collection, String q, BsonProcessor bsonFilter) {

        System.out.printf("Querying mongo: %s.%s.find(%s)%n", database, collection, q);
        //Prepare zip entry
        ZipArchiveEntry zeContent = new ZipArchiveEntry(String.format("%s.bson", collection));
        try {
            zipOutputStream.putArchiveEntry(zeContent);
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }

        //Begin dump from mongo to zip
        Process dumpProcess = mongodump(database, collection, q);

        // Using the stdout from mongodump, begin decoding the BSON so we can alter the communityId fields
        BasicBSONDecoder bsonStream = new BasicBSONDecoder();
        InputStream mongoStream = dumpProcess.getInputStream();

        //Read until EOF
        int docCount = 0;
        try {

            //Old simple file -> zip ( no alterations )
            //org.apache.commons.io.IOUtils.copy(dumpProcess.getInputStream(), zipOutputStream);

            BSONObject tmpObject;
            while( null != ( tmpObject = bsonStream.readObject(mongoStream) ) ){
                tmpObject = bsonFilter.prepareBsonObject(tmpObject);
                zipOutputStream.write(BSON.encode(tmpObject));
                docCount++;
            }

            System.out.printf("Document count: %d%n", docCount);
        } catch (IOException ioe) {
            System.out.printf("Document count: %d%n", docCount);
        }

        try {
            dumpProcess.waitFor();
            zipOutputStream.closeArchiveEntry();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return docCount;
    }

    /**
     * Restore a collection from an input stream using a named pipe and mongorestore.
     * @param collectionInputStream InputStream of BSON archive.
     * @param database Mongo database to restore into
     * @param collection Mongo collection to restore into.
     * @param fifoName Name pipe path to pass to mongorestore
     * @param bsonFilter BsonProcessor instance to process records as they're inserted.
     * @return Number of documents processed / inserted.
     */
    private int restoreCollection(final InputStream collectionInputStream, String database,
                                  String collection, final String fifoName, final BsonProcessor bsonFilter) {

        System.out.printf("[Community->restoreCollection] Restoring: %s.%s%n", database, collection);

        final int docCount[] = new int[1];
        docCount[0] = 0;

        Thread outputThread = new Thread(new Runnable() {
            @Override
            public void run() {

                // Using the stdout from mongodump, begin decoding the BSON so we can alter the communityId fields
                BasicBSONDecoder bsonStream = new BasicBSONDecoder();
                OutputStream outputStream = null;

                try {
                    outputStream = new FileOutputStream(fifoName);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                if( null == outputStream){
                    return;
                }

                System.out.println("[Community->restoreCollection->outputThread] Decoding documents");

                //Read until EOF
                try {

                    BSONObject tmpObject;
                    while( null != ( tmpObject = bsonStream.readObject(collectionInputStream) ) ){
                        tmpObject = bsonFilter.prepareBsonObject(tmpObject);
                        outputStream.write(BSON.encode(tmpObject));
                        docCount[0]++;
                    }

                } catch (IOException ioe) {
                    System.out.printf("[Community->restoreCollection->outputThread] Document count: %d%n", docCount[0]);
                }

                try {
                    collectionInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    outputStream.flush();
                    outputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });

        //Use a new thread for pipes,
        //and generate the file before running mongo restore if not using pipes
        if( _usePipes ) {
            outputThread.start();
            System.out.println("[Community->restoreCollection] Output thread running, starting mongorestore");
        } else {
            System.out.println("[Community->restoreCollection] Writing temp bson file.");
            outputThread.run();
        }

        try {
            //Start mongorestore async - our named pipe should block mongorestore until we write to it.
            // we'll wait for mongo restore to finish after we've written all our BSON to the named pipe.
            Process restoreProcess = mongorestore(database, collection, fifoName);

            if( _usePipes ) {
                System.out.println("[Community->restoreCollection] Waiting for output thread to finish...");
                outputThread.join();
            }

            System.out.println("[Community->restoreCollection] Waiting for mongorestore to finish...");
            restoreProcess.waitFor();

        } catch (Exception e){
            e.printStackTrace();
        }

        return docCount[0];
    }

    /**
     * Archive a community and all associated data. Indexes are generated upon restoration.
     * @param communityId Community ID to archive.
     * @param zipFilename Destination zip file name
     * @return True on success, false on failure.
     * @throws FileFoundException
     */
    public boolean archive(String communityId, String zipFilename) throws FileFoundException {

        if (communityId == null) {
            throw new IllegalArgumentException("CommunityId is required");
        }

        //Make sure the file does not exist
        File inputFile = new File(zipFilename);
        if (inputFile.exists()) {
            throw new FileFoundException("Destination file already exists.");
        }

        try {

            //Prepare SourceKey Map
            final SourceKeyMap sourceKeyMap = new SourceKeyMap();

            //Prepare zip output
            File fZipFile = new File(zipFilename);
            BufferedOutputStream sZipOutputStream = new BufferedOutputStream(new FileOutputStream(fZipFile));
            ZipArchiveOutputStream zipOutputStream = new ZipArchiveOutputStream(sZipOutputStream);

            //Archive social.community object and prepare object before archiving
            archiveCollection(
                    zipOutputStream,
                    "social", "community",
                    getCommunityCollectionQuery(communityId),
                    new BsonProcessor() {
                        @Override
                        public BSONObject prepareBsonObject(BSONObject record) {
                            if( record.containsField("sourceKey") ) {
                                sourceKeyMap.mapKey((String)record.get("sourceKey"));
                            }
                            record.put("members", new BasicBSONList());
                            record.put("ownerDisplayName", "%owner_display_name%");
                            record.put("ownerId", "%owner_id%");
                            return record;
                        }
                    }
            );

            //Archive the community's documents
            archiveCollection(
                    zipOutputStream,
                    "doc_content", "gzip_content",
                    getGzipContentCollectionQuery(communityId),
                    new BsonProcessor() {
                        @Override
                        public BSONObject prepareBsonObject(BSONObject record) {
                            if( record.containsField("sourceKey") ) {
                                sourceKeyMap.mapKey((String)record.get("sourceKey"));
                            }
                            record.put("communityId", "%community_id%");
                            return record;
                        }
                    }
            );

            //Document the Metadata for community
            archiveCollection(
                    zipOutputStream,
                    "doc_metadata", "metadata",
                    getDocMetadataCollectionQuery(communityId),
                    new BsonProcessor() {
                        @Override
                        public BSONObject prepareBsonObject(BSONObject record) {
                            if( record.containsField("sourceKey") ) {
                                sourceKeyMap.mapKey((String)record.get("sourceKey"));
                            }
                            record.put("communityId", "%community_id%");
                            return record;
                        }
                    }
            );

            //Document Content for this community
            archiveCollection(
                    zipOutputStream,
                    "ingest", "source",
                    getSourceCollectionQuery(communityId),
                    new BsonProcessor() {
                        @Override
                        public BSONObject prepareBsonObject(BSONObject record) {
                            if( record.containsField("key") ) {
                                sourceKeyMap.mapKey((String)record.get("key"));
                            }
                            record.put("communityIds", new BasicBSONList());
                            record.put("ownerId", "%owner_id%");
                            return record;
                        }
                    }
            );

            //Create a sourceKey map inside the zip
            sourceKeyMap.writeCsv(zipOutputStream, "sourceKeyMap.csv");

            //Close output zip stream
            zipOutputStream.close();

            //Return successfully
            System.out.println("Archive complete.");
            return true;
        } catch (MongoException me) {
            System.err.println("Error running mongo command: " + me.toString());
        } catch (Exception err) {
            err.printStackTrace();
        }

        return false;
    }

    /**
     * Restore / Import a community of data from a zip archive created by this utility.
     * @param newCommunityId Community ID to place data within
     * @param inputFile Source ZipFile, File instance.
     * @return True on success, false on failure.
     * @throws FileNotFoundException
     */
    public boolean restore(final String newCommunityId, File inputFile) throws FileNotFoundException {

        System.out.printf("[Community->restore] %s -> %s%n", inputFile.getName(), newCommunityId);

        if (newCommunityId == null) {
            throw new IllegalArgumentException("newCommunityId is required");
        }

        //Make sure the file exists
        if (!inputFile.exists()) {
            throw new FileNotFoundException(inputFile.getAbsolutePath());
        }

        //Get community record
        BSONObject community = getCommunityBSON(newCommunityId);
        if( community == null ){
            System.out.printf("[Community->restore] Community %s does not exist.%n", newCommunityId);
            System.err.printf("[Community->restore] Community %s does not exist.%n", newCommunityId);
            return false;
        }

        try {

            String mongoVersion = getMongoRestoreVersion();
            if( null == mongoVersion ) {
                System.out.println("Cannot determine mongorestore version, attempting to continue using temp files.");
            } else {
                int mongoMajor = Integer.parseInt(mongoVersion.replaceAll("([0-9]).*", "$1"));
                if (mongoMajor > 2) {
                    System.out.println("Mongorestore major version > 2, using pipes.");
                    _usePipes = true;
                } else if (mongoMajor < 3) {
                    System.out.println("Mongorestore major version < 3, using temp files.");
                }
            }

            // Prepare Temp dir for mongorestore FIFO buffer
            // and Make a named pipe so we can use mongorestore
            File tempDir = Files.createTempDir();
            File tempFile; //Temp file may be a pipe or a file we re-create each collection

            //Prepare zip Input
            ZipFile zipFile = new ZipFile(inputFile);
            ZipArchiveInputStream zipInputStream = new ZipArchiveInputStream(
                    new BufferedInputStream(new FileInputStream(inputFile)));

            //System.out.println("[Community->restore] ZipArchiveInputStream ready");

            //Get a list of zip entries as a map
            // Key is the collection name, value is the ZipArchiveEntry needed to get an InputStream
            Enumeration<ZipArchiveEntry> zipEntries = zipFile.getEntries();
            Map<String,ZipArchiveEntry> zipCollections = new HashMap<>();
            while( zipEntries.hasMoreElements() ){
                ZipArchiveEntry zippedCollection = zipEntries.nextElement();
                zipCollections.put(zippedCollection.getName(), zippedCollection);
            }

            //Get values we need to fill in as we go
            final ObjectId ownerId = (ObjectId) community.get("ownerId");
            final BasicBSONList communityIdList = new BasicBSONList();
            communityIdList.add(new ObjectId(newCommunityId));

            //If there's a sourceKey map csv load it up
            final SourceKeyMap sourceKeyMap = new SourceKeyMap();
            if( zipCollections.containsKey("sourceKeyMap.csv") ) {
                sourceKeyMap.readCsv(zipFile.getInputStream(zipCollections.get("sourceKeyMap.csv")));
            }

            if( _usePipes ) {
                tempFile = new File(mkfifo(tempDir.getAbsolutePath()));
                System.out.printf("[Community->restore] named pipe ready: %s%n", tempFile.getAbsolutePath() );
            } else {
                tempFile = new File(mktemp(tempDir.getAbsolutePath()));
            }

            //Restore community documents
            restoreCollection(
                    zipFile.getInputStream(zipCollections.get("gzip_content.bson")),    // Zip source input stream
                    "doc_content", "gzip_content",                                      // collection info
                    tempFile.getAbsolutePath(),                                       // named pipe
                    new BsonProcessor() {                                  // per-record callback
                        @Override
                        public BSONObject prepareBsonObject(BSONObject record) {
                            record.put("communityId", new ObjectId(newCommunityId));
                            if( record.containsField("sourceKey") ){
                                record.put("sourceKey", sourceKeyMap.mapKey((String)record.get("sourceKey")));
                            }
                            return record;
                        }
                    }
            );

            // if we're not using pipes, then we need to cleanup temp files and make a new one.
            if( !_usePipes ){
                if( !tempFile.delete() ){
                    System.err.printf("[Community->restore] Could not clean up temp file: %s%n", tempFile.getAbsolutePath() );
                }
                tempFile = new File(mktemp(tempDir.getAbsolutePath()));
            }

            //Restore community document metadata
            restoreCollection(
                    zipFile.getInputStream(zipCollections.get("metadata.bson")), // Zip source input stream
                    "doc_metadata", "metadata",                                     // collection info
                    tempFile.getAbsolutePath(),                                        // named pipe
                    new BsonProcessor() {                                  // per-record callback
                        @Override
                        public BSONObject prepareBsonObject(BSONObject record) {
                            record.put("communityId", new ObjectId(newCommunityId));
                            if( record.containsField("sourceKey") ){
                                record.put("sourceKey", sourceKeyMap.mapKey((String)record.get("sourceKey")));
                            }
                            return record;
                        }
                    }
            );

            if( !_usePipes ){
                if( !tempFile.delete() ){
                    System.err.printf("[Community->restore] Could not clean up temp file: %s%n", tempFile.getAbsolutePath() );
                }
                tempFile = new File(mktemp(tempDir.getAbsolutePath()));
            }

            //Restore ingestion source(s)
            restoreCollection(
                    zipFile.getInputStream(zipCollections.get("source.bson")), // Zip source input stream
                    "ingest", "source",                                           // collection info
                    tempFile.getAbsolutePath(),                                      // named pipe
                    new BsonProcessor() {                                    // per-record callback
                        @Override
                        public BSONObject prepareBsonObject(BSONObject record) {

                            record.put("communityIds", communityIdList);
                            record.put("ownerId", ownerId);

                            //Alter source key
                            if( record.containsField("key") ){
                                record.put("key", sourceKeyMap.mapKey((String)record.get("key")));
                            }

                            return record;
                        }
                    }
            );


            //Cleanup
            zipInputStream.close();

            //Clean up temp
            if( !_usePipes ){
                if( !tempFile.delete() ){
                    System.err.printf("[Community->restore] Could not clean up temp file: %s%n", tempFile.getAbsolutePath() );
                }
            } else if( !tempFile.delete() ){
                System.err.printf("[Community->restore] Could not remove temp named pipe: %s%n", tempFile.getAbsolutePath());
            }

            if (!tempDir.delete()) {
                System.err.printf("[Community->restore] Could not remove temp directory: %s%n", tempDir.getAbsolutePath());
            }

            System.out.println("[Community->restore] Database records inserted. Indexing...");

            InfiniteCli icli = new InfiniteCli();
            if( !icli.indexCommunity(newCommunityId) ){
                System.err.println("Restore Failed. Indexer did not complete properly.");
                return false;
            } else {
                System.out.println("Restore complete.");
            }

            //Return successfully
            return true;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

}
