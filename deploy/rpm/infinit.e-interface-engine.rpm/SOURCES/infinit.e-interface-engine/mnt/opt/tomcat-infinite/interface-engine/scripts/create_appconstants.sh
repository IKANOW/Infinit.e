#!/bin/bash
###########################################################################

# Utility functions

function getParam { # $1=param string, $2=file
	TMP=`grep "$1" $2 | sed s/"^[^=]*="// | sed s/"^\s*"// | sed s/"\s*$"//`
	echo $TMP
}

###########################################################################
# create_appconstants.sh
###########################################################################
PROPERTY_CONFIG_FILE='/opt/infinite-install/config/infinite.configuration.properties'
CONSTANTS_TEMPLATE='/opt/tomcat-infinite/interface-engine/templates/AppConstants.js.TEMPLATE'
CONSTANTS_CONF_LOCATION='/opt/tomcat-infinite/interface-engine/conf/AppConstants.js'

###########################################################################
# Get properties from infinite.configuration.properties and create
# AppConstants.js from AppConstants.js.TEMPLATE
###########################################################################
# Get the properties from the property file
APP_SAAS=$(getParam 				"^app.saas=" $PROPERTY_CONFIG_FILE)
END_POINT_URL=$(getParam			"^ui.end.point.url=" $PROPERTY_CONFIG_FILE)
DOMAIN_URL=$(getParam				"^ui.domain.url=" $PROPERTY_CONFIG_FILE)
GOOGLE_MAPS_API_KEY=$(getParam		"^google.maps.api.key=" $PROPERTY_CONFIG_FILE)
ACCESS_TIMEOUT=$(getParam			"^access.timeout=" $PROPERTY_CONFIG_FILE)
FORGOT_PASSWORD_URL=$(getParam		"^ui.forgot.password=" $PROPERTY_CONFIG_FILE)
LOGOUT_URL=$(getParam				"^ui.logout=" $PROPERTY_CONFIG_FILE)
	
cp $CONSTANTS_TEMPLATE $CONSTANTS_CONF_LOCATION
sed -i "s|END_POINT_URL|$END_POINT_URL|g" $CONSTANTS_CONF_LOCATION
sed -i "s|DOMAIN_URL|$DOMAIN_URL|g" $CONSTANTS_CONF_LOCATION
sed -i "s|GOOGLE_MAPS_API_KEY|$GOOGLE_MAPS_API_KEY|g" $CONSTANTS_CONF_LOCATION
sed -i "s|ACCESS_TIMEOUT|$ACCESS_TIMEOUT|g" $CONSTANTS_CONF_LOCATION
# Next 2 params depend on whether system is SaaS or not:
# (revert to non-SaaS if param=="")
if [ "$APP_SAAS" = "true" ]; then
	if [ "$FORGOT_PASSWORD_URL" != "" ]; then
		sed -i "s|.*forgotUrl.*app.saas=false.*|//\0|" $CONSTANTS_CONF_LOCATION
		sed -i "s|FORGOT_PASSWORD_URL|$FORGOT_PASSWORD_URL|" $CONSTANTS_CONF_LOCATION
	else
		sed -i "s|.*forgotUrl.*app.saas=true.*|//\0|" $CONSTANTS_CONF_LOCATION
	fi 
	if [ "$LOGOUT_URL" != "" ]; then
		sed -i "s|.*logoutURL.*app.saas=false.*|//\0|" $CONSTANTS_CONF_LOCATION
		sed -i "s|LOGOUT_URL|$LOGOUT_URL|" $CONSTANTS_CONF_LOCATION
	else
		sed -i "s|.*logoutURL.*app.saas=true.*|//\0|" $CONSTANTS_CONF_LOCATION
	fi 
else
	sed -i "s|.*app.saas=true.*|//\0|" $CONSTANTS_CONF_LOCATION
fi

chown tomcat.tomcat $CONSTANTS_CONF_LOCATION
