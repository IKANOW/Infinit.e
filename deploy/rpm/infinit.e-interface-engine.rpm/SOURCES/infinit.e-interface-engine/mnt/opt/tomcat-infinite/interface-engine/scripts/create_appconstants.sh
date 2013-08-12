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
SERVERCONF_TEMPLATE='/opt/tomcat-infinite/interface-engine/templates/server.xml.TEMPLATE'
SERVERCONF_CONF_LOCATION='/opt/tomcat-infinite/interface-engine/conf/server.xml'

###########################################################################
# Get properties from infinite.configuration.properties and create
# AppConstants.js from AppConstants.js.TEMPLATE
###########################################################################
# Get the properties from the property file
APP_SAAS=$(getParam 					"^app.saas=" $PROPERTY_CONFIG_FILE)
END_POINT_URL=$(getParam				"^ui.end.point.url=" $PROPERTY_CONFIG_FILE)
DOMAIN_URL=$(getParam					"^ui.domain.url=" $PROPERTY_CONFIG_FILE)
GOOGLE_MAPS_API_KEY=$(getParam			"^google.maps.api.key=" $PROPERTY_CONFIG_FILE)
ACCESS_TIMEOUT=$(getParam				"^access.timeout=" $PROPERTY_CONFIG_FILE)
FORGOT_PASSWORD_URL=$(getParam			"^ui.forgot.password=" $PROPERTY_CONFIG_FILE)
LOGOUT_URL=$(getParam					"^ui.logout=" $PROPERTY_CONFIG_FILE)
EXTERNAL_SEARCH_NAME=$(getParam			"^ui.externalsearch.name=" $PROPERTY_CONFIG_FILE)
EXTERNAL_SEARCH_URL=$(getParam			"^ui.externalsearch.url=" $PROPERTY_CONFIG_FILE)
LOGO_URL=$(getParam						"^ui.logo.url=" $PROPERTY_CONFIG_FILE)
#(enterprise constants)
CASE_MANAGER_API_URL=$(getParam			"^app.case.api.url=" $PROPERTY_CONFIG_FILE)
CASE_MANAGER_URL=$(getParam				"^app.case.url=" $PROPERTY_CONFIG_FILE)
	
cp $CONSTANTS_TEMPLATE $CONSTANTS_CONF_LOCATION
if [ "$END_POINT_URL" != "" ]; then
	sed -i "s|END_POINT_URL|$END_POINT_URL|g" $CONSTANTS_CONF_LOCATION
	#(else set automatically below)
fi
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
# If no end point specified then default it to current location:
sed -i "s|\"END_POINT_URL\"|'http://'+document.location.hostname+':'+(document.location.port==''?80:document.location.port)+'/api/'|" $CONSTANTS_CONF_LOCATION

if [ "$EXTERNAL_SEARCH_NAME" = "" ]; then
	EXTERNAL_SEARCH_NAME="google" #(default)
fi
sed -i "s|EXTERNAL_SEARCH_NAME|$EXTERNAL_SEARCH_NAME|" $CONSTANTS_CONF_LOCATION

# External URL is a bit more complicated because you can return javascript (fine provided you use '' and not "")
if [ "$EXTERNAL_SEARCH_URL" = "" ]; then
	sed -i "s|.*EXTERNAL_SEARCH_URL.*|//\0|" $CONSTANTS_CONF_LOCATION
else	
	sed -i "s|EXTERNAL_SEARCH_URL|$EXTERNAL_SEARCH_URL|" $CONSTANTS_CONF_LOCATION
	sed -i "s|.*ui.logo.url=default.*|//\0|" $CONSTANTS_CONF_LOCATION
fi

#Logo (can be empty)
sed -i "s|LOGO_URL|$LOGO_URL|" $CONSTANTS_CONF_LOCATION

#(enterprise constants)
#(case app server)
if [ "$CASE_MANAGER_API_URL" = "local" ]; then
	sed -i "s|\"CASE_MANAGER_API_URL\"|'http://'+document.location.hostname+':'+(document.location.port==''?80:document.location.port)+'/caseserver/'|" $CONSTANTS_CONF_LOCATION
else
	sed -i "s|CASE_MANAGER_API_URL|$CASE_MANAGER_API_URL|g" $CONSTANTS_CONF_LOCATION
fi
#(case manager gui)
if [ "$CASE_MANAGER_URL" = "local" ]; then
	sed -i "s|\"CASE_MANAGER_URL\"|'http://'+document.location.hostname+':8090/casemanager/'|" $CONSTANTS_CONF_LOCATION
else
	sed -i "s|CASE_MANAGER_URL|$CASE_MANAGER_URL|g" $CONSTANTS_CONF_LOCATION
fi

chown tomcat.tomcat $CONSTANTS_CONF_LOCATION

###########################################################################
# Get properties from infinite.configuration.properties and create
# AppConstants.js from AppConstants.js.TEMPLATE
###########################################################################
cp $SERVERCONF_TEMPLATE $SERVERCONF_CONF_LOCATION

SSL_PASSPHRASE=$(getParam	"^ssl.passphrase=" $PROPERTY_CONFIG_FILE)

if [ "$SSL_PASSPHRASE" != "" ] && [ -f /usr/share/tomcat6/tomcat.keystore ]; then
	sed -i "s|SSL_PASSPHRASE|$SSL_PASSPHRASE|" $SERVERCONF_CONF_LOCATION
	sed -i '/__INF_SSL_ENABLED/d' $SERVERCONF_CONF_LOCATION
fi

chown tomcat.tomcat $SERVERCONF_CONF_LOCATION
