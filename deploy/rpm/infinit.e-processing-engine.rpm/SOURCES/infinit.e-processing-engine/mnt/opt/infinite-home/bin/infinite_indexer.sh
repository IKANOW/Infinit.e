#!/bin/bash
SECURITY_POLICY="/opt/infinite-home/bin/security.policy"
CLASSPATH="infinit.e.mongo-indexer.jar:infinit.e.harvest.library.jar:infinit.e.data_model.jar:infinit.e.processing.generic.library.jar:extractors/*:infinit.e.core.server.jar:unbundled/*:es-libs/*"

(cd /opt/infinite-home/lib; java -cp $CLASSPATH -Djava.security.policy=$SECURITY_POLICY com.ikanow.infinit.e.utility.MongoIndexerMain "$@")
