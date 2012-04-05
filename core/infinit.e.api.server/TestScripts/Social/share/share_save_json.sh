################################################################################
# share_save_json.sh
################################################################################
# Description: 
# Params
# $1 = API Server
# $2 = username
# $3 = password
# $4 = share id
# $5 = type
# $6 = title
# $7 = description
################################################################################
echo ''
echo 'Log in to Infinit.e and get a cookie'
echo curl -XGET -c cookie.txt  $1/auth/login/$2/$3
echo ''
curl -XGET -c cookie.txt  $1/auth/login/$2/$3
echo ''

################################################################################
# Escape spaces with %20
SHAREID=$4
TYPE=${5// /%20}
TITLE=${6// /%20}
DESC=${7// /%20}

################################################################################
# Call via post, send file
echo 'Command:'
echo curl -XPOST -b cookie.txt  $1/social/share/update/json/$SHAREID/$TYPE/$TITLE/$DESC/ -d @share.json
curl -XPOST -b cookie.txt  $1/social/share/update/json/$SHAREID/$TYPE/$TITLE/$DESC/ -d @share.json
echo ''
