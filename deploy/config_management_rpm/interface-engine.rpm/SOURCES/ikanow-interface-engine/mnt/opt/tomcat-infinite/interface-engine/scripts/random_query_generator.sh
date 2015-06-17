#!/bin/bash
#
# Usage random_query_generator.sh <user> <pass> <REST-ENDPOINT>
#

USER=$1
PASS=$2
USERPASS="$USER/$PASS"
INFINITE_URL=$3

if [ $# -lt 3 ]; then
	echo "Usage random_query_generator.sh <user> <pass> <REST-ENDPOINT>"
fi

EVENT_FILENAME="./event_list.txt"
ENTITY_FILENAME="./entity_list.txt"
TEXT_FILENAME="./word_list.txt"

# Sleep (100ms, 10s)
SHORT_SLEEP=100000
LONG_SLEEP=10000000

COMMUNITY="4c927585d591d31d7b37097a"

HTTPGET="wget -q -O /dev/null --keep-session-cookies --save-cookies=cookies_$USER.txt --load-cookies=cookies_$USER.txt"
#Debug version:
#HTTPGET="wget -qO- --keep-session-cookies --save-cookies=cookies_$USER.txt --load-cookies=cookies_$USER.txt"

###########################

# get_random_object <file to get from>, returns the entry

function get_random_object { 
	LINES=`wc -l $1 | gawk '{ print $1 }'`
	RANDOM_LINE=$[ ( ($RANDOM*32768 + $RANDOM) % $LINES )  + 1 ]
	RANDOM_OBJECT=`cat $1 | tail -n -$RANDOM_LINE | head -n 1 | sed s/"[^.a-zA-Z0-9 &[]_\"\'*]"/" "/g`
}
#(tested)

# get_logic_operator - used to generate queries as "and" 60% "or" 30% "and not" 10%

function get_logic_operator {
	DICE=$[ ( ($RANDOM*32768 + $RANDOM) % 100 ) ]
	if [ $DICE -lt 60 ]; then
		LOGIC_OPERATOR="&logic=\"1 and 2\""
	elif [ $DICE -lt 90 ]; then
		LOGIC_OPERATOR="&logic=\"1 or 2\""
	else
		LOGIC_OPERATOR="&logic=\"1 and not 2\""
	fi
}
#(tested)

# get_output_options - for the moment, just return a fixed field of GUI defaults

function get_output_options {
#For debugging
#	OUTOPT1='&output.aggregation.geoNumReturn=5&output.aggregation.timesInterval="1y"
#	OUTOPT2='&output.aggregation.entsNumReturn=5&output.aggregation.eventsNumReturn=5&output.aggregation.factsNumReturn=5'
#	OUTOPT3='&output.aggregation.sourceMetadata=2&output.aggregation.sources=2&output.docs.enable=false'
	OUTOPT1='&output.aggregation.geoNumReturn=1000&output.aggregation.timesInterval="1w"'
	OUTOPT2='&output.aggregation.entsNumReturn=250&output.aggregation.eventsNumReturn=100&output.aggregation.factsNumReturn=100'
	OUTOPT3='&output.aggregation.sourceMetadata=20&output.aggregation.sources=20'
	OUTPUT_OPTIONS="${OUTOP1}${OUTOPT2}${OUTOPT3}"
}

# do_entity_search_suggest <word>

function do_entity_search_suggest {
	# Loop over first 15 chars
	for i in `seq 1 15`; do
		# Issue search Suggest for incomplete word
		WORD_FRAGMENT=`echo $1 | sed "s/\(.\{$i\}\).*/\1/" | sed s/"[/ ]"/","/g`		
		$HTTPGET "http://$INFINITE_URL/knowledge/searchSuggest/$WORD_FRAGMENT/$COMMUNITY"
		#(test code)
		#echo "$HTTPGET \"http://$INFINITE_URL/knowledge/searchSuggest/$WORD_FRAGMENT/$COMMUNITY\""
		usleep $SHORT_SLEEP
	done
}
#tested

# query_request <query-string> [<logic>]- actually makes the query then sleeps

function query_request {
	get_output_options
	$HTTPGET "http://$INFINITE_URL/knowledge/query/${COMMUNITY}?${1}${2}${OUTPUT_OPTIONS}"
	#(test code)
	#echo "$HTTPGET \"http://$INFINITE_URL/knowledge/query/${COMMUNITY}?${1}${2}${OUTPUT_OPTIONS}\""
	usleep $LONG_SLEEP
}
#(tested)

###########################

function generate_event_query {
	get_random_object "$EVENT_FILENAME"
	query_request "$RANDOM_OBJECT"
}
#(tested)

function generate_entity_query {
	get_random_object "$ENTITY_FILENAME"
	do_entity_search_suggest "$RANDOM_OBJECT"
	query_request "qt[0].entity=\"$RANDOM_OBJECT\""
}
#(tested)

function generate_text_query {
	get_random_object "$TEXT_FILENAME"
	query_request "qt[0].etext=\"$RANDOM_OBJECT\""
}
#(tested)

###########################

# generate_entity_and_text_query

function generate_entity_and_text_query {
	get_random_object "$TEXT_FILENAME"
	WORD="$RANDOM_OBJECT"
	get_random_object "$ENTITY_FILENAME"
	ENTITY="$RANDOM_OBJECT"
	get_logic_operator
	do_entity_search_suggest "$ENTITY"
	query_request "qt[0].entity=\"$ENTITY\"&qt[1].etext=\"$WORD\"" "$LOGIC_OPERATOR"
}
#(tested)

# generate_event_and_text_query

function generate_event_and_text_query {
	get_random_object "$TEXT_FILENAME"
	WORD="$RANDOM_OBJECT"
	get_random_object "$EVENT_FILENAME"
	EVENT="$RANDOM_OBJECT"
	get_logic_operator
	query_request "$EVENT&qt[1].entity=\"$ENTITY\"" "$LOGIC_OPERATOR"
}
#(tested)

function login {
	$HTTPGET "http://$INFINITE_URL/auth/login/$USERPASS"
}
#(tested)

function test_code {
	echo
	get_random_object $TEXT_FILENAME
	echo $RANDOM_OBJECT
	get_random_object $ENTITY_FILENAME
	echo $RANDOM_OBJECT
	get_random_object $EVENT_FILENAME
	echo $RANDOM_OBJECT

	get_logic_operator
	echo $LOGIC_OPERATOR
	get_logic_operator
	echo $LOGIC_OPERATOR
	get_logic_operator
	echo $LOGIC_OPERATOR

	get_output_options
	echo $OUTPUT_OPTIONS

	do_entity_search_suggest "online complaint assistant/position"
	do_entity_search_suggest "http://alex /test.co1234567"
	
	generate_text_query	
	generate_event_query
	generate_entity_query

	generate_entity_and_text_query
	generate_event_and_text_query	

	exit 0	
}

##############################################################

# MAIN LOGIC

# 1, Login.

login

#Test code:
#test_code
#exit 0

# 2. Loop forever
while [ 1 ]; do
	# A representative mix of activities

	generate_entity_query
	generate_text_query
	generate_entity_and_text_query
	generate_entity_query

	generate_event_query

	generate_text_query
	generate_entity_query
	generate_entity_and_text_query
	generate_text_query

	generate_event_and_text_query

done
