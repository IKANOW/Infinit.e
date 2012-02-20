################################################################################
# person_register.sh
################################################################################
# Description:
# 
# Params
# $1 = API Server
# $2 = username
# $3 = password
# $4 = password
################################################################################
echo ''
echo 'Log in to Infinit.e and get a cookie'
echo curl -XGET -c cookie.txt  "$1/auth/login/$2/$3"
echo ''
curl -XGET -c cookie.txt  $1/auth/login/$2/$3
echo ''
echo ''

curl -s -b cookie.txt -XPOST "$1/social/person/register" -d'
{
	"user": {
		"WPUserID":"nputzier@ikanow.com",
		"firstname": "Nathan",
		"lastname": "Putzier",
		"email":[ "nputzier@ikanow.com" ]
	},
	"auth": {
		"password":"rxDiN6OGsmz4twX3ewFaPzPUqq5AOj6TFKsivGHYriQ="
	}
}
'
echo ''
echo ''

