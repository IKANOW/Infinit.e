package com.ikanow.infinit.e.utility;

import com.ikanow.infinit.e.utility.archivetools.Community;
import com.ikanow.infinit.e.utility.archivetools.MongoCli;
import com.ikanow.infinit.e.utility.archivetools.MongoDbGenerator;
import com.ikanow.infinit.e.utility.archivetools.exception.FileFoundException;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Date;

/**
 * Created by Mike Grill on 3/12/15.
 *
 * Command line tools to create and restore archives of platform data.
 * Specifically ( and only ) communities for now.
 */
public class ArchiveTools extends MongoCli {

    /**
     * App Entry - ArchiveTools Class Below
     * @param args Application arguments
     */
    public static void main(String[] args) {

        CommandLineParser parser = new BasicParser();

        Options options = new Options();

        // Actions
        Option oActionCreate  = new Option("a", "archive",   true, "Archive community by community ID to <file>");
        Option oActionExtract = new Option("r", "restore",  true, "Restore from archive zip to community ID");
        //Option oActionTest    = new Option("t", "test",     true, "DANGER: Generate a set of test information. Generates garbage records for testing.");
        Option oFile          = new Option("f", "file",     true, "Path to input zip or output folder");

        //Mongo Connection
        Option oMongoHost     = new Option("H", "hostname", true, "Mongo instance hostname");
        Option oMongoUser     = new Option("u", "username", true, "Mongo connection user");
        Option oMongoPass     = new Option("p", "password", true, "Mongo user password");
        Option oMongoPort     = new Option("P", "port",     true, "Mongo server port");

        // Generic
        Option oHelp          = new Option("h", "help",     false, "Show this help");
        Option oVerbose       = new Option("v", "verbose",  false, "Association feature transfer");

        //Actions are grouped, either one is required. Not both.
        OptionGroup oAction = new OptionGroup();
        oAction.addOption(oActionCreate);
        oAction.addOption(oActionExtract);
        //oAction.addOption(oActionTest);
        oAction.setRequired(true);

        oFile.setRequired(true);

        options.addOptionGroup(oAction);
        options.addOption(oFile);
        options.addOption(oMongoHost);
        options.addOption(oMongoUser);
        options.addOption(oMongoPass);
        options.addOption(oMongoPort);
        options.addOption(oHelp);
        options.addOption(oVerbose);

        boolean result = false;
        try {

            CommandLine cli = parser.parse(options, args);

            //Just want help?
            if( cli.hasOption("help")){
                HelpFormatter hFormat = new HelpFormatter();
                hFormat.printHelp("java --jar jarFile.jar", options, true );
                System.exit(0);
            }

            String mongoHost = cli.getOptionValue("hostname");
            String mongoUser = cli.getOptionValue("username");
            String mongoPass = cli.getOptionValue("password");
            Integer mongoPort  = cli.hasOption("port") ? Integer.parseInt(cli.getOptionValue("port")) : null;

            ArchiveTools at = new ArchiveTools( mongoHost, mongoUser, mongoPass, mongoPort );

            //Archive?
            if( cli.hasOption("archive") ){
                result = at.createArchive(cli.getOptionValue("archive"), new File(cli.getOptionValue("file")));

            //Restore?
            }else if( cli.hasOption("restore") ){
                result = at.restoreArchive(cli.getOptionValue("restore"), new File(cli.getOptionValue("file")));
            }

            //Test
//            else if( cli.hasOption("test") ){
//                String testCommunityId = at.generateTestCommunity();
//                if( null != testCommunityId ) {
//                    result = true;
//                }
//                System.out.printf("Testing community id: %s%n", testCommunityId);
//            }

        } catch (ParseException e) {
            System.out.println(e.getMessage());
            HelpFormatter hFormat = new HelpFormatter();
            hFormat.printHelp("java --jar jarFile.jar", options, true );
            System.exit(1);
        } catch (Exception e){
            System.out.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        if(!result){
            System.out.println("Operation failed.");
            System.exit(1);
        }

    }


    /**
     * Create an ArchiveTools application instance.
     * @param host Mongo Host
     * @param user Mongo Auth user
     * @param pass Mongo Auth pass
     * @param port Mongo server port
     */
    public ArchiveTools( String host, String user, String pass, Integer port ){
        super(host, user, pass, port);
        System.out.println("[ArchiveTools] Init");
    }

    public boolean createArchive( String communityId, File outputDirectory ){

        System.out.printf("[ArchiveTools->createArchive] %s -> %s%n", communityId, outputDirectory);

        //For CREATE action, make sure output dir is kosher.
        try {
            Community c = new Community(this);
            String zipFilename = String.format("%s/%s-%s.zip",
                    outputDirectory.getAbsolutePath(), communityId, new Date().getTime() / 1000 );
            return c.archive( communityId, zipFilename );

        } catch (FileFoundException e) {
            e.printStackTrace();
        }

        return false;
    }


    public boolean restoreArchive( String communityId, File zipFilename ){

        System.out.printf("[ArchiveTools->restoreArchive] %s -> %s%n", zipFilename, communityId );

        //For CREATE action, make sure output dir is kosher.
        try {
            Community c = new Community(this);
            return c.restore(communityId, zipFilename);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Create a community and associated random data to be used for testing.
     * @return The Test community id
     */
    public String generateTestCommunity(){

        System.out.println("[ArchiveTools->generateTestCommunity]");

        //For CREATE action, make sure output dir is kosher.
        MongoDbGenerator c = new MongoDbGenerator(this);
        return c.fillCommunityCollections();
    }
}
