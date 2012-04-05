################################################################################
# share_add_json.sh
################################################################################
# Description:
# Params
# $1 = API Server
# $2 = username
# $3 = password
# $4 = type
# $5 = title
# $6 = description
################################################################################
echo ''
echo 'Log in to Infinit.e and get a cookie'
echo curl -XGET -c cookie.txt  $1/auth/login/$2/$3
echo ''
curl -XGET -c cookie.txt  $1/auth/login/$2/$3
echo ''

################################################################################
# Escape spaces with %20
TYPE=${4// /%20}
TITLE=${5// /%20}
DESC=${6// /%20}

################################################################################
# Call via post, send file
echo 'Command:'
echo curl -XPOST -b cookie.txt  $1/social/share/add/json/$TYPE/$TITLE/$DESC/ -d @json.txt
curl -XPOST -b cookie.txt  $1/social/share/add/json/$TYPE/$TITLE/$DESC/ -d @json.txt
echo ''
