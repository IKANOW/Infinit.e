################################################################################
# Login_admin.sh
################################################################################
# Description:

# Params
# $1 = API Server
# $2 = username
# $3 = password
################################################################################
echo ''
echo 'Log in to Infinit.e and get a cookie'
echo curl -XGET -c cookie.txt  $1/auth/login/admin/$2/$3
echo ''
curl -XGET -c cookie.txt  $1/auth/login/admin/$2/$3
echo ''
echo ''

