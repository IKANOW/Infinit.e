#!/bin/bash
###########################################################################
# Description: If EC2 cluster_name Key is set, the value will be set to 
# the elastic.cluster variable in the properties file. Then, the EC2
# cluster_name Value will be set to the elastic.cluster variable value
# in the properties file.
###########################################################################
API_PROPERTY_FILE='/opt/infinite-home/config/infinite.api.properties'
SERVICE_PROPERTY_FILE='/opt/infinite-home/config/infinite.service.properties'
PROP_VAR_NAME='elastic.cluster'
PROP_VAR_ESC='elastic\.cluster'
EC2_VAR_NAME='cluster-name'

if [[ ! -f "$API_PROPERTY_FILE" || ! -f "$SERVICE_PROPERTY_FILE" ]]; then
	echo -e "\a**Properties file prerequisites not met"
	
else
	this_instance=$(curl -s http://169.254.169.254/1.0/meta-data/instance-id)
	
	ec2Value=$(aws dtags --filter "resource-id=$this_instance" --filter "key=$EC2_VAR_NAME" --simple | gawk '{ print $4 }' | tr "," "\n")
	
	if [ $(echo $ec2Value) ]; then
		echo "$EC2_VAR_NAME EC2 value found: $ec2Value"
		sed -i "s/\($PROP_VAR_NAME=\).*/\1$ec2Value/g" $API_PROPERTY_FILE
		echo "$API_PROPERTY_FILE updated"
		sed -i "s/\($PROP_VAR_NAME=\).*/\1$ec2Value/g" $SERVICE_PROPERTY_FILE
		echo "$SERVICE_PROPERTY_FILE updated"
	else
		echo "$EC2_VAR_Name EC2 value not found."
	fi
	
	propValue=$(sed -rn 's/^'$PROP_VAR_ESC'=(.*)/\1/p' $API_PROPERTY_FILE)
	
	if [ $(echo $propValue) ]; then
		echo "$PROP_VAR_NAME variable value found: $propValue"
		aws ctags $this_instance --tag $EC2_VAR_NAME=$propValue >/dev/null
		echo "$EC2_VAR_NAME EC2 value set"
	else
		echo "$PROP_VAR_NAME variable value not found in $API_PROPERTY_FILE."
	fi
fi

