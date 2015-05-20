#! /bin/bash
################################################################################
# write_es_yml_files.sh
################################################################################
SERVICE_PROPERTY_FILE='/opt/infinite-home/config/infinite.service.properties'
YML_TEMPLATE='/opt/elasticsearch-infinite/config/elasticsearch.yml.TEMPLATE'
TMP_YML_FILE='/opt/elasticsearch-infinite/config/elasticsearch-tmp.yml'

################################################################################
# Get config properties from infinite.service.properties
DISCOVERY_MODE=`grep "^elastic.node.discovery=" $SERVICE_PROPERTY_FILE | sed s/'elastic.node.discovery='// | sed s/"^\s*"// | sed s/"\s*$"//`
CLUSTER_NAME=`grep "^elastic.cluster=" $SERVICE_PROPERTY_FILE | sed s/'elastic.cluster='// | sed s/"^\s*"// | sed s/"\s*$"//`
AWS_KEY=`grep "^aws.access.key=" $SERVICE_PROPERTY_FILE | sed s/'aws.access.key='// | sed s/"^\s*"// | sed s/"\s*$"//`
SECRET_KEY=`grep "^aws.secret.key=" $SERVICE_PROPERTY_FILE | sed s/'aws.secret.key='// | sed s/"^\s*"// | sed s/"\s*$"//`
ELASTIC_NODES=`grep "^elastic.search.nodes=" $SERVICE_PROPERTY_FILE | sed s/'elastic.search.nodes='// | sed s/"^\s*"// | sed s/"\s*$"//`
BOOTSTRAP_MLOCKALL=`grep "^bootstrap.mlockall=" $SERVICE_PROPERTY_FILE | sed s/'bootstrap.mlockall='// | sed s/"^\s*"// | sed s/"\s*$"//`
MIN_NODES=`grep "^elastic.search.min_peers=" $SERVICE_PROPERTY_FILE | sed s/'elastic.search.min_peers='// | sed s/"^\s*"// | sed s/"\s*$"//`

################################################################################
# 

cp $YML_TEMPLATE $TMP_YML_FILE

if [ -f /opt/elasticsearch-infinite/config/INTERFACE_ONLY ]; then
	sed -i "s|IS_DATA_NODE|false|g" $TMP_YML_FILE
	sed -i "s|BOOTSTRAP_MLOCKALL|false|g" $TMP_YML_FILE
else
	sed -i "s|IS_DATA_NODE|true|g" $TMP_YML_FILE
	sed -i "s|BOOTSTRAP_MLOCKALL|$BOOTSTRAP_MLOCKALL|g" $TMP_YML_FILE
fi

sed -i "s|CLUSTER_NAME|$CLUSTER_NAME|g" $TMP_YML_FILE

sed -i "s|DISCOVERY_MODE|$DISCOVERY_MODE|g" $TMP_YML_FILE

sed -i "s|MIN_NODES|$MIN_NODES|g" $TMP_YML_FILE

if [ $DISCOVERY_MODE = "zen" ]; then
	sed -i "s|ELASTIC_NODES|$ELASTIC_NODES|g" $TMP_YML_FILE
	sed -i "s|.*cloud:.*|#\0|g" $TMP_YML_FILE
	sed -i "s|.*ec2.tag.*|#\0|g" $TMP_YML_FILE
	sed -i "s|.*AWS_KEY.*|#\0|g" $TMP_YML_FILE
	sed -i "s|.*SECRET_KEY.*|#\0|g" $TMP_YML_FILE
else
	# Get region:
	REGION=$(curl -s http://169.254.169.254/latest/meta-data/placement/availability-zone/ | sed -e 's/[^0-9]*$//')
	sed -i "s|AWS_REGION|$REGION|g" $TMP_YML_FILE
		
	sed -i "s|.*ELASTIC_NODES.*|#\0|g" $TMP_YML_FILE
	sed -i "s|AWS_KEY|$AWS_KEY|g" $TMP_YML_FILE
	sed -i "s|SECRET_KEY|$SECRET_KEY|g" $TMP_YML_FILE
fi

chown elasticsearch.elasticsearch $TMP_YML_FILE

