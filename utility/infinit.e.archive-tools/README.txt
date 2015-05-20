Archive-Tools at the moment supports archiving and restoration of
communities and their data.

To Compile:

    Requirements
    JDK 1.7+
    Maven

    mvn clean
    mvn package

    A Jar-with-dependencies will be created in the /target/ folder.

Known Limitations & Artifacts:

    Mongo 3.x+:
    Only support for POSIX systems with mkfifo available. Named pipes are used to
    communicate with mongo restore and skip writing large temp files.
    NOTE: Turns out mongorestore supports stdin so this could be altered to write
          directly to mongorestore's stdin instead of using a named pipe.

    Mongo < 3.x
    Temp files are used, no dependency on mkfifo

    /opt/infinite-home/bin/infinite_indexer.sh must be available.

    During the restoration process, a globally unique id is appended to all source keys associated
    with the community being restored. This UUID is also applied to the documents and their metadata.
    The UUID used is printed to the console (stdout) during restoration.

    During restoration, the ownerID of sources is altered to match the community chosen for restoration.

Archive Structure:

    Inside each zip file created will be 4 BSON files representing 4 different
    collections from the infinite mongo data store. Private information such as
    userIDs and userDisplayName are removed for privacy.

    In addition to the BSON files, there is a single CSV ( sourceKeyMap.csv ). This
    CSV contains all the sourceKeys found during archiving. You may edit this file
    ( and update the zip contents ) if you wish to replace any sourceKeys with another.
    The default behaviour is to use the original sourceKey and append ".{objectId}" to
    enforce uniqueness. Either method will globally replace sourceKeys with consistency
    during the restore process.

How To Use:

    To see a full list of flags use -h
    java --jar archive-tools-jar-with-dependencies.jar -h

    An action is required. Use -a or -r to archive or restore.


    To create an archive

    java --jar archive-tools-jar-with-dependencies.jar -a {communityId} -f {outDir} -H {mongoHost}

    A zip file will be created
    {outDir}/{communityId}-{unixTimestamp}.zip
    where {unixTimestamp} represents the start time of the archive process.


    To restore an archive

    Using the Infinite Manager, create a new community, and get the new community's ID.

    java --jar archive-tools-jar-with-dependencies.jar -r {newCommunityId} -f {zipFile} -H {mongoHost}

    After inserting the documents, the mongo indexer will be invoked to create indexes.
    Therefore /opt/infinite-home/bin/infinite_indexer.sh must be available.
