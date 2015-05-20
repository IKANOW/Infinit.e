package com.ikanow.infinit.e.utility.archivetools;

import com.google.common.io.ByteStreams;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Mike Grill on 3/19/15.
 * This is just a few simple command line interactions mainly to b
 */
public abstract class Cli {

    /**
     * Create a randomly named pipe in the systems temp directory
     * Currently only supports POSIX systems
     * @param tempDir Folder to place named pipe
     * @return Absolute path to the named pipe.
     */
    protected String mkfifo( String tempDir ){

        //Generate a random temp filename
        String fifoName = mktemp( tempDir );

        try{

            ArrayList<String> cmd = new ArrayList<>();
            cmd.add("mkfifo");
            cmd.add(fifoName);

            //Attempt to run mkfifo and ignore the result.
            String[] cmdArr = cmd.toArray(new String[cmd.size()]);
            getProcessResult(getProcess(cmdArr));

            //Return the new named pipe's absolute path
            return fifoName;

        } catch (Exception ignored){}

        return null;
    }

    /**
     * Create a randomly named file.
     * @return Random filename inside tempdir
     */
    protected String mktemp(String tempDir){
        String randomName = Common.randomValue(10);
        return String.format("%s/%s.bson", tempDir, randomName);
    }

    /**
     * Safely start a process from a command
     * @param cmd Command line command to run
     * @return Command line process on success, null on failure.
     */
    protected Process getProcess(String[] cmd){
        System.out.println("[getProcess->getProcess] cmd: " + StringUtils.join(cmd, " "));
        try {
            ProcessBuilder builder = new ProcessBuilder(cmd);
            return builder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * This will hang if the process run generates too much stderr output.
     * A solution would be to have two threads, one for each stream so neither
     * should become full and blocking.
     * @param p Process from getProcess
     * @return Instance of ProcessResult or null on failure.
     */
    protected ProcessResult getProcessResult(Process p){
        //System.out.println("[getProcess->getProcessResult]");
        try {
            return new ProcessResult( p );
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
