################################################################################
# community_get_private.sh
################################################################################
# Description:
# Login via the Infinit.e API
# Params
# $1 = API Server
# $2 = username
# $3 = password
################################################################################
echo ''
echo 'Log in to Infinit.e and get a cookie'
echo curl -XGET -c cookie.txt  $1/auth/login/$2/$3
echo ''
curl -XGET -c cookie.txt  $1/auth/login/$2/$3
echo ''
echo ''

echo 'Command:'
echo curl -XGET -b cookie.txt $1/social/community/getprivate/
echo 'Response:'
curl -XGET -b cookie.txt $1/social/community/getprivate/
echo ''
echo ''