################################################################################
# community_update.sh
################################################################################
# Description:
# 
# Params
# $1 = API Server
# $2 = username
# $3 = password
# $4 = id
# $5 = json
################################################################################
echo ''
echo 'Log in to Infinit.e and get a cookie'
echo curl -XGET -c cookie.txt  $1/auth/login/$2/$3
echo ''
curl -XGET -c cookie.txt  $1/auth/login/$2/$3
echo ''
echo ''

echo ''
echo 'Command:'
echo curl -XPOST -b cookie.txt $1/social/community/update/$4/ -d '{ "json": "$5" }'
echo ''
echo 'Response:'
curl -XPOST -b cookie.txt $1/social/community/update/$4/ -d '{ "json": "$5" }'
echo ''
echo ''











