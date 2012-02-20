################################################################################
# share_add_json.sh
################################################################################
# Description:
# 
# Params
# $1 = API Server
# $2 = username
# $3 = password
# $4 = type
# $5 = title
# $6 = description
# $7 = json
################################################################################
echo ''
echo 'Log in to Infinit.e and get a cookie'
echo curl -XGET -c cookie.txt  $1/auth/login/$2/$3
echo ''
curl -XGET -c cookie.txt  $1/auth/login/$2/$3
echo ''
echo ''

################################################################################
# Escape spaces with %20
TYPE=${4// /%20}
TITLE=${5// /%20}
DESC=${6// /%20}

################################################################################
# Write the JSON to temp file
echo $7 > tmp.json
echo ''

################################################################################
# Call via post, send file and then delete the temp file
echo 'Command:'
echo curl -XPOST -b cookie.txt  $1/social/share/save/json/$TYPE/$TITLE/$DESC/ -d @tmp.json
curl -XPOST -b cookie.txt  $1/social/share/save/json/$TYPE/$TITLE/$DESC/ -d @tmp.json
rm tmp.json
echo ''
echo ''