################################################################################
# source_save.sh
################################################################################
# Description:
# 
# Params
# $1 = API Server
# $2 = username
# $3 = password
# $4 = communityid
################################################################################
echo ''
echo 'Log in to Infinit.e and get a cookie'
echo curl -XGET -c cookie.txt  $1/auth/login/$2/$3
echo ''
curl -XGET -c cookie.txt  $1/auth/login/$2/$3
echo ''
echo ''

################################################################################
# Call via post, send file and then delete the temp file
echo 'Command:'
echo curl -XPOST -b cookie.txt  $1/config/source/save/$4 -d @json.txt
curl -XPOST -b cookie.txt  $1/config/source/save/$4 -d @json.txt
echo ''
echo ''