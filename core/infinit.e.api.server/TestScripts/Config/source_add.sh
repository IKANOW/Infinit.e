################################################################################
# source_add.sh
################################################################################
# Description:
# /config/source/add/{sourceurl}/{sourcetitle}/{sourcedesc}/{extracttype}/{sourcetags}/{mediatype}/{groupid}
# Params
# $1 = API Server
# $2 = username
# $3 = password
# $4 = url
# $5 = title (name)
# $6 = desc
# $7 = extract type
# $8 = tags
# $9 = mediatype
# $10 = groupid
################################################################################
echo ''
echo 'Log in to Infinit.e and get a cookie'
echo curl -XGET -c cookie.txt  $1/auth/login/$2/$3
echo ''
curl -XGET -c cookie.txt  $1/auth/login/$2/$3
echo ''
echo ''

################################################################################
# Escape spaces with %20 for name, description, tags
NAME=${5// /%20}
echo 'Name: '$NAME
DESC=${6// /%20}
echo 'Description: '$DESC
TAGS=${8// /%20}
echo 'Tags: '$TAGS
echo ''
echo ''

echo 'Command:'
echo curl -XGET -b cookie.txt $1/config/source/add/$4/$NAME/$DESC/$7/$TAGS/$9/$10
echo ''
echo 'Response:'
curl -XGET -b cookie.txt $1/config/source/add/$4/$NAME/$DESC/$7/$TAGS/$9/$10
echo ''
echo ''











