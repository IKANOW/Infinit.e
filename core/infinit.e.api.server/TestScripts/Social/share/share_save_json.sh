################################################################################
# share_save_json.sh
################################################################################
# Description:
# 
# Params
# $1 = API Server
# $2 = username
# $3 = password
# $4 = share id
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
SHAREID=$4
TYPE=${5// /%20}
TITLE=${6// /%20}
DESC=${7// /%20}

################################################################################
# Write the JSON to temp file
echo $8 > tmp.json
echo ''

################################################################################
# Call via post, send file and then delete the temp file
echo 'Command:'
echo curl -XPOST -b cookie.txt  $1/social/share/save/json/$SHAREID/$TYPE/$TITLE/$DESC/ -d @tmp.json
curl -XPOST -b cookie.txt  $1/social/share/save/json/$SHAREID/$TYPE/$TITLE/$DESC/ -d @tmp.json
rm tmp.json
echo ''
echo ''