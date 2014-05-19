#!/bin/bash
CLASSPATH="infinit.e.data_model.jar:infinit.e.processing.generic.library.jar:infinit.e.mongo-indexer.jar:es-libs/*"

(cd /opt/infinite-home/lib; java -cp $CLASSPATH com.ikanow.infinit.e.utility.MongoIndexerMain "$@")
