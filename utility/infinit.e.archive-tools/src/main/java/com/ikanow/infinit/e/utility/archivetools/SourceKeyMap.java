package com.ikanow.infinit.e.utility.archivetools;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;

import java.io.*;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 * Created by Mike Grill on 3/25/15.
 *
 * SourceKeyMap manages a relation of a sourceKey to a new SourceKey.
 */
public class SourceKeyMap {

    private String uuid;
    private HashMap<String,String> keyMap;

    public SourceKeyMap(){
        this.keyMap = new HashMap<>();
        this.uuid = new ObjectId().toString();
        System.out.printf("[SourceKeyMap->SourceKeyMap] Ready. UUID: %s%n", uuid);
    }

    /**
     * Add a key to the map, no known replacement value
     * @param oldKey Old SourceKey to be mapped.
     * @return Already mapped value or original value if no map exists.
     */
    public String mapKey(String oldKey){
        return mapKey(oldKey,null);
    }

    /**
     * Add a SourceKey to the map.
     * @param oldKey Original sourceKey
     * @param newKey Replacement key
     * @return Mapped value or original value if mapped value is 'empty'.
     */
    public String mapKey(String oldKey, String newKey){

        //Haven't seen this sourceKey yet?
        if( !keyMap.containsKey(oldKey) ){

            //Store the key
            keyMap.put(oldKey, newKey);

            return getMappedSourceKey( oldKey );
        }

        //We already know about oldKey, is a newKey is here we'll update and use that
        //Otherwise we'll use the current map
        if( newKey != null){
            keyMap.put(oldKey, newKey);
        }

        return getMappedSourceKey( oldKey );
    }

    /**
     * Get the mapped value of a source key, otherwise a unique version of sourceKey
     * @param sourceKey SourceKey to check for a map
     * @return mapped value of a source key, otherwise a unique version of sourceKey
     */
    public String getMappedSourceKey(String sourceKey){

        //If the current mapped value isn't null use that
        String mappedValue = keyMap.get(sourceKey);
        if( mappedValue != null ){
            return mappedValue;
        }

        //Otherwise generate a unique one by appending the UUID
        return String.format("%s.%s",sourceKey, uuid);
    }

    /**
     * Read the contents of a saved keyMap csv
     * @param csvStream InputStream from Csv file.
     * @return True on success, false on failure.
     */
    public boolean readCsv( InputStream csvStream ){

        System.out.println("[SourceKeyMap->readCsv] Reading from input stream");

        try {
            BufferedReader csvBuffer = new BufferedReader( new InputStreamReader(csvStream));
            String line;
            String fields[];
            while( null != (line = csvBuffer.readLine()) ){

                //Decode the line
                fields = StringUtils.split(line, ',' );         //Split row
                fields = StringUtils.stripAll( fields, "\"");   // Trim wrapping " from elements

                //Skip header row
                if( fields.length > 0 && fields[0].equals( "original")){
                    System.out.printf("[SourceKeyMap->readCsv]     Skipping header%n");
                    continue;
                }

                //Add the map
                switch( fields.length){
                    case 0:
                        System.out.printf("[SourceKeyMap->readCsv]     Bad row: %s%n", line);
                        break;

                    case 1:
                        mapKey(fields[0]);
                        break;

                    default:
                        mapKey(fields[0], fields[1]);
                        break;
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.printf("[SourceKeyMap->readCsv] Map prepared. %d items.%n", keyMap.size());
        return true;
    }

    /**
     * Generate a csv header
     * @return CSV Header line
     */
    private String _csvHeader(){
        return "\"original\",\"replacement\"\n";
    }

    /**
     * Generate a csv row from a map entry
     * @param record Map entry
     * @return CSV Line
     */
    private String _csvRow(Entry<String,String> record){
        String mappedValue = record.getValue();
        return ( null == mappedValue) ?
             String.format("\"%s\",%n", record.getKey()) :
             String.format("\"%s\",\"%s\"%n", record.getKey(), mappedValue);
    }

    /**
     * Write a csv of this map's contents for later use to a zip file, using csvFileName
     * @param zipOutputStream Zip file output stream
     * @param csvFileName Filename to use for CSV within Zip.
     * @return true on success, false on failure.
     */
    public boolean writeCsv(ZipArchiveOutputStream zipOutputStream, String csvFileName ){

        try {

            //Create zip entry
            ZipArchiveEntry zeContent = new ZipArchiveEntry(csvFileName);

            //Write the entry to the zip output stream
            zipOutputStream.putArchiveEntry(zeContent);

            //Write the entry contents
            writeCsv(zipOutputStream);

            //Close the entry
            zipOutputStream.closeArchiveEntry();

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }


        return true;
    }

    /**
     * Write a csv of this map's contents for later use to an output stream
     * @param outputStream Output stream to write CSV contents
     * @throws IOException
     */
    public void writeCsv(OutputStream outputStream) throws IOException {
        outputStream.write( _csvHeader().getBytes() );
        for(Entry<String,String> entry : keyMap.entrySet()){
            outputStream.write( _csvRow( entry ).getBytes() );
        }
    }

}
