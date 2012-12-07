#!/bin/bash

###########################################################################

# Utility functions

function getParam { # $1=param string, $2=file
	TMP=`grep "$1" $2 | sed s/"^[^=]*="// | sed s/"^\s*"// | sed s/"\s*$"//`
	echo $TMP
}

function setParam { # $1=variable name, $2 variable value, $3=file
        if [ "$2" = "" ]; then
                # Just sub out parameter:
                sed -i "s|.*$1.*|#\0|" $3
        else
                if `echo $2 | grep -v -q -F '|'`; then
                        sed  -i "s|$1|$2|g" $3
                elif `echo $2 | grep -v -q -F '%'`; then
                        sed  -i "s%$1%$2%g" $3
                fi
        fi
}

###########################################################################
# rewrite_property_files.sh
########################################################################### 
PROPERTY_CONFIG_FILE='/opt/infinite-install/config/infinite.configuration.properties'
API_PROPERTY_TEMPLATE='/opt/infinite-home/config/infinite.api.properties.TEMPLATE'
API_PROPERTY_FILE='/opt/infinite-home/config/infinite.api.properties'
SERVICE_PROPERTY_TEMPLATE='/opt/infinite-home/config/infinite.service.properties.TEMPLATE'
SERVICE_PROPERTY_FILE='/opt/infinite-home/config/infinite.service.properties'
PYTHON_CONFIG_TEMPLATE='/opt/infinite-home/config/python.cfg.TEMPLATE'
PYTHON_CONFIG_FILE='/opt/infinite-home/config/python.cfg'
AWSSECRET_FILE='/root/.awssecret'
S3CFG_FILE='/root/.s3cfg'

###########################################################################
# Make sure the infinite.configuration.properties file are there before proceeding
if [[ ! -f "$PROPERTY_CONFIG_FILE" ]]; then
	echo -e "\a**Properties file prerequisites not met"
	
###########################################################################
# Rewrite the API and Service property files using the settings in
# the infinite.configuration.properties file
else

	###########################################################################
	# Get values from infinite.configuration.properties for api and service.properties files
	###########################################################################
	APP_SAAS=$(getParam 						"^app.saas=" $PROPERTY_CONFIG_FILE)
	SAAS_TRUSTED_DNS=$(getParam 				"^app.saas.trusted.dns=" $PROPERTY_CONFIG_FILE)
	URL_ROOT=$(getParam 						"^url.root=" $PROPERTY_CONFIG_FILE)
	ACCESS_TIMEOUT=$(getParam 					"^access.timeout=" $PROPERTY_CONFIG_FILE)
	ACCESS_ALLOW_REGEX=$(getParam 				"^remote.access.allow=" $PROPERTY_CONFIG_FILE)
	ACCESS_DENY_REGEX=$(getParam 				"^remote.access.deny=" $PROPERTY_CONFIG_FILE)
	
	USE_AWS=$(getParam 							"^use.aws=" $PROPERTY_CONFIG_FILE)
	AWS_ACCESS_KEY=$(getParam 					"^aws.access.key=" $PROPERTY_CONFIG_FILE)
	AWS_SECRET_KEY=$(getParam 					"^aws.secret.key=" $PROPERTY_CONFIG_FILE)

	DB_SERVER=$(getParam 						"^db.server=" $PROPERTY_CONFIG_FILE)
	DB_PORT=$(getParam 							"^db.port=" $PROPERTY_CONFIG_FILE)
	DB_CONFIG_SERVERS=$(getParam 				"^db.config.servers=" $PROPERTY_CONFIG_FILE)
	DB_REPLICA_SETS=$(getParam 					"^db.replica.sets=" $PROPERTY_CONFIG_FILE)
	DB_SHARDED=$(getParam 						"^db.sharded=" $PROPERTY_CONFIG_FILE)
	DB_CLUSTER_SUBNET=$(getParam 				"^db.cluster.subnet=" $PROPERTY_CONFIG_FILE)
	DB_CAPACITY=$(getParam 						"^db.capacity=" $PROPERTY_CONFIG_FILE)
	DB_SHARDED=$(getParam 						"^db.sharded=" $PROPERTY_CONFIG_FILE)

	DISCOVERY_MODE=$(getParam 					"^elastic.node.discovery=" $PROPERTY_CONFIG_FILE)
	ELASTIC_URL=$(getParam 						"^elastic.url=" $PROPERTY_CONFIG_FILE)
	ELASTIC_CLUSTER=$(getParam 					"^elastic.cluster=" $PROPERTY_CONFIG_FILE)
	ELASTIC_NODES=$(getParam 					"^elastic.search.nodes=" $PROPERTY_CONFIG_FILE)
	ELASTIC_MIN_PEERS=$(getParam 				"^elastic.search.min_peers=" $PROPERTY_CONFIG_FILE)
	ELASTIC_MAX_REPLICAS=$(getParam 			"^elastic.max_replicas=" $PROPERTY_CONFIG_FILE)
	BOOTSTRAP_MLOCKALL=$(getParam 				"^bootstrap.mlockall=" $PROPERTY_CONFIG_FILE)
	
	HARVESTER_TYPES=$(getParam 					"^harvester.types=" $PROPERTY_CONFIG_FILE)
	HARVEST_FEED_WAIT=$(getParam 				"^harvest.feed.wait=" $PROPERTY_CONFIG_FILE)
	HARVESTER_USERAGENT=$(getParam 				"^harvest.feed.useragent=" $PROPERTY_CONFIG_FILE)
	HARVESTER_MINTIME_MS=$(getParam				"^harvest.mintime.ms=" $PROPERTY_CONFIG_FILE)
	HARVESTER_MINTIME_SOURCE_MS=$(getParam		"^harvest.source.mintime.ms=" $PROPERTY_CONFIG_FILE)
	HARVESTER_MAXDOCS_SOURCE=$(getParam 		"^harvest.maxdocs_persource=" $PROPERTY_CONFIG_FILE)
	HARVESTER_MAXSOURCES_HARVEST=$(getParam 	"^harvest.maxsources_perharvest=" $PROPERTY_CONFIG_FILE)
	HARVESTER_THREADS=$(getParam 				"^harvest.threads=" $PROPERTY_CONFIG_FILE)
	HARVESTER_SECURITY=$(getParam 				"^harvest.security=" $PROPERTY_CONFIG_FILE)
	HARVESTER_BATCH=$(getParam 					"^harvest.distribution.batch.harvest=" $PROPERTY_CONFIG_FILE)
	HARVEST_DISABLE_AGGREGATION=$(getParam 		"^harvest.disable_aggregation=" $PROPERTY_CONFIG_FILE)
	STORE_MAXCONTENT=$(getParam 				"^store.maxcontent=" $PROPERTY_CONFIG_FILE)

	MAIL_SERVER=$(getParam 						"^mail.server=" $PROPERTY_CONFIG_FILE)
	MAIL_USERNAME=$(getParam 					"^mail.username=" $PROPERTY_CONFIG_FILE)
	MAIL_PASSWORD=$(getParam 					"^mail.password=" $PROPERTY_CONFIG_FILE)
	TEST_TERMS=$(getParam 						"^api.search.test.terms=" $PROPERTY_CONFIG_FILE)
	EXPECTED_RESULTS=$(getParam 				"^api.search.expected.results=" $PROPERTY_CONFIG_FILE)
	MAIL_FROM=$(getParam 						"^log.files.mail.from=" $PROPERTY_CONFIG_FILE)
	MAIL_TO=$(getParam 							"^log.files.mail.to=" $PROPERTY_CONFIG_FILE)
	
	ALCHEMY_KEY=$(getParam 						"^extractor.key.alchemyapi=" $PROPERTY_CONFIG_FILE)
	OPENCALAIS_KEY=$(getParam 					"^extractor.key.opencalais=" $PROPERTY_CONFIG_FILE)
	DEFAULT_ENTITY_EXTRACTOR=$(getParam			 "^extractor.entity.default=" $PROPERTY_CONFIG_FILE)
	DEFAULT_TEXT_EXTRACTOR=$(getParam 			"^extractor.text.default=" $PROPERTY_CONFIG_FILE)
	ALCHEMY_POSTPROC_VAL=$(getParam 			"^app.alchemy.postproc=" $PROPERTY_CONFIG_FILE)
	
	HADOOP_DIR=$(getParam 						"^hadoop.configpath=" $PROPERTY_CONFIG_FILE)
	HADOOP_MAX_CONCURRENT=$(getParam 			"^hadoop.max_concurrent=" $PROPERTY_CONFIG_FILE)
	HADOOP_LOCALMODE=$(getParam		 			"^hadoop.local_mode=" $PROPERTY_CONFIG_FILE)
	
	###########################################################################
	# 
	###########################################################################
	GPG_PASSPHRASE=$(getParam 					"^s3.gpg.passphrase=" $PROPERTY_CONFIG_FILE)
	S3_URL=$(getParam 							"^s3.url=" $PROPERTY_CONFIG_FILE)
	CLUSTER_SUBNET=$(getParam 					"^db.cluster.subnet=" $PROPERTY_CONFIG_FILE)

	###########################################################################
	# Create infinite.service.properties from infinite.service.properties.TEMPLATE
	# and replace the placeholder values
	###########################################################################
	cp $SERVICE_PROPERTY_TEMPLATE $SERVICE_PROPERTY_FILE
	setParam USE_AWS "$USE_AWS" $SERVICE_PROPERTY_FILE
	setParam AWS_ACCESS_KEY "$AWS_ACCESS_KEY" $SERVICE_PROPERTY_FILE
	setParam AWS_SECRET_KEY "$AWS_SECRET_KEY" $SERVICE_PROPERTY_FILE
	setParam S3_URL "$S3_URL" $SERVICE_PROPERTY_FILE

	setParam URL_ROOT "$URL_ROOT" $SERVICE_PROPERTY_FILE

	setParam DB_SERVER "$DB_SERVER" $SERVICE_PROPERTY_FILE
	setParam DB_PORT "$DB_PORT" $SERVICE_PROPERTY_FILE
	setParam DB_CONFIG_SERVERS "$DB_CONFIG_SERVERS" $SERVICE_PROPERTY_FILE
	setParam DB_REPLICA_SETS "$DB_REPLICA_SETS" $SERVICE_PROPERTY_FILE
	setParam DB_CLUSTER_SUBNET "$DB_CLUSTER_SUBNET" $SERVICE_PROPERTY_FILE
	setParam DB_SHARDED "$DB_SHARDED" $SERVICE_PROPERTY_FILE
	setParam DB_CAPACITY "$DB_CAPACITY" $SERVICE_PROPERTY_FILE

	setParam DISCOVERY_MODE "$DISCOVERY_MODE" $SERVICE_PROPERTY_FILE
	setParam ELASTIC_URL "$ELASTIC_URL" $SERVICE_PROPERTY_FILE
	setParam ELASTIC_CLUSTER "$ELASTIC_CLUSTER" $SERVICE_PROPERTY_FILE
	setParam ELASTIC_NODES "$ELASTIC_NODES" $SERVICE_PROPERTY_FILE	
	setParam ELASTIC_MIN_PEERS "$ELASTIC_MIN_PEERS" $SERVICE_PROPERTY_FILE
	setParam ELASTIC_MAX_REPLICAS "$ELASTIC_MAX_REPLICAS" $SERVICE_PROPERTY_FILE
	setParam BOOTSTRAP_MLOCKALL "$BOOTSTRAP_MLOCKALL" $SERVICE_PROPERTY_FILE

	setParam HARVESTER_TYPES "$HARVESTER_TYPES" $SERVICE_PROPERTY_FILE
	setParam HARVEST_FEED_WAIT "$HARVEST_FEED_WAIT" $SERVICE_PROPERTY_FILE
	setParam HARVESTER_USERAGENT "$HARVESTER_USERAGENT" $SERVICE_PROPERTY_FILE
	setParam HARVESTER_MINTIME_MS "$HARVESTER_MINTIME_MS" $SERVICE_PROPERTY_FILE
	setParam HARVESTER_MINTIME_SOURCE_MS "$HARVESTER_MINTIME_SOURCE_MS" $SERVICE_PROPERTY_FILE
	setParam HARVESTER_MAXDOCS_SOURCE "$HARVESTER_MAXDOCS_SOURCE" $SERVICE_PROPERTY_FILE
	setParam HARVESTER_MAXSOURCES_HARVEST "$HARVESTER_MAXSOURCES_HARVEST" $SERVICE_PROPERTY_FILE
	setParam HARVESTER_THREADS "$HARVESTER_THREADS" $SERVICE_PROPERTY_FILE
	setParam HARVESTER_SECURITY "$HARVESTER_SECURITY" $SERVICE_PROPERTY_FILE
	setParam HARVESTER_BATCH "$HARVESTER_BATCH" $SERVICE_PROPERTY_FILE
	setParam HARVEST_DISABLE_AGGREGATION "$HARVEST_DISABLE_AGGREGATION" $SERVICE_PROPERTY_FILE
	setParam STORE_MAXCONTENT "$STORE_MAXCONTENT" $SERVICE_PROPERTY_FILE

	setParam MAIL_SERVER "$MAIL_SERVER" $SERVICE_PROPERTY_FILE
	setParam MAIL_USERNAME "$MAIL_USERNAME" $SERVICE_PROPERTY_FILE
	setParam MAIL_PASSWORD "$MAIL_PASSWORD" $SERVICE_PROPERTY_FILE
	setParam MAIL_FROM "$MAIL_FROM" $SERVICE_PROPERTY_FILE
	setParam MAIL_TO "$MAIL_TO" $SERVICE_PROPERTY_FILE

	setParam ALCHEMY_KEY "$ALCHEMY_KEY" $SERVICE_PROPERTY_FILE
	setParam OPENCALAIS_KEY "$OPENCALAIS_KEY" $SERVICE_PROPERTY_FILE
	setParam DEFAULT_ENTITY_EXTRACTOR "$DEFAULT_ENTITY_EXTRACTOR" $SERVICE_PROPERTY_FILE
	setParam DEFAULT_TEXT_EXTRACTOR "$DEFAULT_TEXT_EXTRACTOR" $SERVICE_PROPERTY_FILE
	setParam ALCHEMY_POSTPROC_VAL "$ALCHEMY_POSTPROC_VAL" $SERVICE_PROPERTY_FILE
	
	setParam HADOOP_DIR "$HADOOP_DIR" $SERVICE_PROPERTY_FILE
	setParam HADOOP_MAX_CONCURRENT "$HADOOP_MAX_CONCURRENT" $SERVICE_PROPERTY_FILE
	setParam HADOOP_LOCALMODE "$HADOOP_LOCALMODE" $SERVICE_PROPERTY_FILE
	
	chown tomcat.tomcat $SERVICE_PROPERTY_FILE
	################### End infinite.service.properties ###########################

	###########################################################################
	# Create infinite.api.properties from infinite.api.properties.TEMPLATE
	# and replace the placeholder values
	###########################################################################
	cp $API_PROPERTY_TEMPLATE $API_PROPERTY_FILE
	setParam APP_SAAS "$APP_SAAS" $API_PROPERTY_FILE
	setParam SAAS_TRUSTED_DNS "$SAAS_TRUSTED_DNS" $API_PROPERTY_FILE
	setParam URL_ROOT "$URL_ROOT" $API_PROPERTY_FILE
	setParam ACCESS_TIMEOUT "$ACCESS_TIMEOUT" $API_PROPERTY_FILE
	setParam ACCESS_ALLOW_REGEX "$ACCESS_ALLOW_REGEX" $API_PROPERTY_FILE
	setParam ACCESS_DENY_REGEX "$ACCESS_DENY_REGEX" $API_PROPERTY_FILE

	setParam DB_SERVER "$DB_SERVER" $API_PROPERTY_FILE
	setParam DB_PORT "$DB_PORT" $API_PROPERTY_FILE

	setParam ELASTIC_URL "$ELASTIC_URL" $API_PROPERTY_FILE
	setParam ELASTIC_CLUSTER "$ELASTIC_CLUSTER" $API_PROPERTY_FILE

	setParam HARVESTER_TYPES "$HARVESTER_TYPES" $API_PROPERTY_FILE
	setParam HARVEST_FEED_WAIT "$HARVEST_FEED_WAIT" $API_PROPERTY_FILE
	setParam HARVESTER_USERAGENT "$HARVESTER_USERAGENT" $API_PROPERTY_FILE
	setParam HARVESTER_MAXDOCS_SOURCE "$HARVESTER_MAXDOCS_SOURCE" $API_PROPERTY_FILE
	setParam HARVESTER_SECURITY "$HARVESTER_SECURITY" $API_PROPERTY_FILE

	setParam MAIL_SERVER "$MAIL_SERVER" $API_PROPERTY_FILE
	setParam MAIL_USERNAME "$MAIL_USERNAME" $API_PROPERTY_FILE
	setParam MAIL_PASSWORD "$MAIL_PASSWORD" $API_PROPERTY_FILE
	setParam TEST_TERMS "$TEST_TERMS" $API_PROPERTY_FILE
	setParam EXPECTED_RESULTS "$EXPECTED_RESULTS" $API_PROPERTY_FILE
	setParam MAIL_FROM "$MAIL_FROM" $API_PROPERTY_FILE
	setParam MAIL_TO "$MAIL_TO" $API_PROPERTY_FILE

	setParam ALCHEMY_KEY "$ALCHEMY_KEY" $API_PROPERTY_FILE
	setParam OPENCALAIS_KEY "$OPENCALAIS_KEY" $API_PROPERTY_FILE
	setParam DEFAULT_ENTITY_EXTRACTOR "$DEFAULT_ENTITY_EXTRACTOR" $API_PROPERTY_FILE
	setParam DEFAULT_TEXT_EXTRACTOR "$DEFAULT_TEXT_EXTRACTOR" $API_PROPERTY_FILE
	setParam ALCHEMY_POSTPROC_VAL "$ALCHEMY_POSTPROC_VAL" $API_PROPERTY_FILE
	chown tomcat.tomcat $API_PROPERTY_FILE
	################### End infinite.api.properties ###########################

###########################################################################
# Write the python.cfg file
###########################################################################
PYTHON_CONFIG_TEXT="[config]
mail.to = $MAIL_TO
mail.from = $MAIL_FROM
"
echo "$PYTHON_CONFIG_TEXT" > $PYTHON_CONFIG_FILE
chown tomcat.tomcat $PYTHON_CONFIG_FILE

###########################################################################
# If use.aws=1 then we need to create .awssecret and .s3cfg files
	if [ $USE_AWS = "1" ]; then
		# Create/overwrite .awssecret file
		AWSSECRET_FILE_TEXT="$AWS_ACCESS_KEY
$AWS_SECRET_KEY"
		echo "$AWSSECRET_FILE_TEXT" > $AWSSECRET_FILE	
		chown tomcat.tomcat $AWSSECRET_FILE
		
		# Replace values in .s3cfg file
		setParam ACCESS_KEY "$AWS_ACCESS_KEY" $S3CFG_FILE
		setParam GPG_PASSPHRASE "$GPG_PASSPHRASE" $S3CFG_FILE
		setParam SECRET_KEY "$AWS_SECRET_KEY" $S3CFG_FILE
		chown tomcat.tomcat $S3CFG_FILE
	fi
fi

###########################################################################
# Finally: set cluster for AWS installs (harmless duplicate call when called from infinit.e-config RPM)
if [ "$USE_AWS" = "1" ]; then	
	echo "Set EC2 Cluster Name and Cluster Name in properties file"
	sh /opt/infinite-home/scripts/set_cluster.sh
fi
