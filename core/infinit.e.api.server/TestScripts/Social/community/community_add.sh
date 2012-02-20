################################################################################
# community_add.sh
################################################################################
# Description:
# 
# Params
# $1 = API Server
# $2 = username
# $3 = password
# $4 = community name
# $5 = community description
# $6 = community tags
# $7 = parent id
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
NAME=${4// /%20}
echo 'Name: '$NAME
DESC=${5// /%20}
echo 'Description: '$DESC
TAGS=${6// /%20}
echo 'Tags: '$TAGS
echo 'ParentId: '$7
echo ''
read -p "Press any key to continue... " -n1 -s

echo ''
echo 'Command:'
echo curl -XGET -b cookie.txt $1/social/community/add/$NAME/$DESC/$TAGS/$7/
echo ''
echo 'Response:'
curl -XGET -b cookie.txt $1/social/community/add/$NAME/$DESC/$TAGS/$7/
echo ''
echo ''











