#!/bin/bash
################################################################################
# Command line args
# 1: URL
# 
# 0. Setup command line
INFINITE_URL=$1
USERNAME=$2
USERPASS=$3
SERVERADDR=$(/sbin/ifconfig | grep -v  "127.0.0.1" | grep -P -m 1 -o "addr:[0-9.]+")

################################################################################
# 
API_PROPERTY_FILE='/opt/infinite-home/config/infinite.api.properties'
FROMUSER=`grep "^log.files.mail.from=" $API_PROPERTY_FILE | sed s/'log.files.mail.from='// | sed s/' '//g`
TOUSER=`grep "^log.files.mail.to=" $API_PROPERTY_FILE | sed s/'log.files.mail.to='// | sed s/' '//g`
SUBJECT='[ALERT] Unexpected Number of API Search Results '
EXPECTEDRESULTNUM=`grep "^api.search.expected.results=" $API_PROPERTY_FILE | sed s/'api.search.expected.results='// | sed s/' '//g`
SEARCHTERMS_RAW=`grep "^api.search.test.terms=" $API_PROPERTY_FILE | sed s/'api.search.test.terms='//`
        # (need to allow spaces here)
SEARCHTERMS=($SEARCHTERMS_RAW)
NUMSEARCHTERMS=${#SEARCHTERMS[@]}
if [ "$NUMSEARCHTERMS" == "-1" ]; then
	#Don't want to perform active monitoring
	exit
fi

################################################################################
#  No URL Specified, Exit Out
if [ -z "$INFINITE_URL" ]; then
        echo "<testapp> <url>"
        echo "Specify a URL"
        exit
fi

################################################################################
# Access - Setup username and password
if [ -z $USERNAME ]; then
        USERNAME=`grep "^test.user.email=" /opt/infinite-install/config/infinite.configuration.properties | sed s/[^=]*=//`
		if [ -z $USERNAME ]; then
	        USERNAME="test_user@ikanow.com"
		fi
        USERPASS=`grep "^test.user.password=" /opt/infinite-install/config/infinite.configuration.properties | sed s/[^=]*=//`
fi
PASSWORD=$(echo -n $USERPASS  | sha256sum | xxd -r -p | base64|sed s/[/]/'%2F'/g|sed s/[=]/'%3D'/g|sed s/[+]/'%2B'/g)
USERPASS="$USERNAME/$PASSWORD"

USER=`echo $USERPASS | sed s/"\/.*"//g`
GROUP='*'

################################################################################
#  Some commands:
LOGIN='auth/login'
SEARCH='knowledge/query'
LOGOUT='auth/logout'

HTTPGET="wget -q -O /dev/null --keep-session-cookies --save-cookies=cookies_$USER.txt --load-cookies=cookies_$USER.txt"
HTTPGET_LOG="wget -q -o /tmp/status.txt -O /tmp/apiResults.txt --keep-session-cookies --save-cookies=cookies_$USER.txt --load-cookies=cookies_$USER.txt"

################################################################################
# Log-in (try for ~3 minutes then error out)
rm -rf cookies_$USER.txt
for i in `seq 1 100`; do 
        $HTTPGET "http://$INFINITE_URL/$LOGIN/$USERPASS?override=false"
        if grep -q "infinitecookie" cookies_$USER.txt; then
                sleep 1
                break
        fi
        echo "(Failed login, backing off)"
        sleep 2
done

################################################################################
#  We've broken out the loop so we're just going to try an override if we're not logged in...
if ! grep -q "infinitecookie" cookies_$USER.txt; then
        echo "(Failed login, overriding existing logins)"
        $HTTPGET "http://$INFINITE_URL/$LOGIN/$USERPASS?override=true"
fi

################################################################################
# Choose Random Term to Search From Term Array
WORD=${SEARCHTERMS[($RANDOM%$NUMSEARCHTERMS)]}
echo "RANDOM WORD: $WORD"


################################################################################
#  Issue search request
$HTTPGET_LOG "http://$INFINITE_URL/$SEARCH/${GROUP}?qt[0].etext=$WORD&output.docs.metadata=false"

################################################################################
#  Search Number of Entries
stringToMatch='"title":' #Unique String that displays for every entry
COUNT=`grep -o $stringToMatch /tmp/apiResults.txt | wc -l`

################################################################################
#  Send Email if Results are not as Expected
if [ ! "$COUNT" -lt "$EXPECTEDRESULTNUM" ] 
then
        echo $COUNT$' Results Received, no Email Sent'

else 
		CLUSTER=$(grep "^elastic.cluster=" /opt/infinite-install/config/infinite.configuration.properties | sed s/[^=]*=//)
        echo "from: $FROMUSER " >> /tmp/tmpEmail.txt
        echo "subject: $SUBJECT"$"($COUNT) [IP:${SERVERADDR} HOST:$(hostname) CLUSTER:${CLUSTER}]"  >> /tmp/tmpEmail.txt
        echo "Search Term: $WORD" >> /tmp/tmpEmail.txt
        echo "Number of Results Returned: $COUNT" >> /tmp/tmpEmail.txt
        echo "Number of Results Expected:$EXPECTEDRESULTNUM" >> /tmp/tmpEmail.txt
        sendmail $TOUSER < /tmp/tmpEmail.txt
        echo $COUNT$' Results Recieved. Email Sent'
        rm /tmp/tmpEmail.txt
fi

echo "Done"

################################################################################
# Logout:
$HTTPGET "http://$INFINITE_URL/$LOGOUT"

################################################################################
# cleanup
#rm /tmp/apiResults.txt

