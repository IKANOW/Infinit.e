################################################################################
# Login_keepalive.sh
################################################################################
# Description:

# Params
# $1 = API Server
################################################################################
echo ''
echo 'Command'
echo curl -XGET -c cookie.txt  $1/auth/keepalive
echo ''
curl -XGET -c cookie.txt  $1/auth/keepalive
echo ''
echo ''

