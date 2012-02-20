
#Fail function
fail() {
	echo
	echo "Test $1 failed: $2"
	exit -1
}

# Common params

ADMIN_USER="admin_infinite@ikanow.com"
ADMIN_PASS="ADMIN_PASSWORD"
INFHOST="10.0.2.2:8184"

####################################################3

#1. INITIALIZE:

echo "1] Login as admin and Initialize"

rm -f cookies_*.txt
curl -s -c cookies_admin.txt -XGET "http://$INFHOST/auth/login/$ADMIN_USER/$ADMIN_PASS" | \
	grep -q '{"response":{"action":"Login","success":true' || fail "1" "Admin login"
echo
sleep 1

#Delete old users (so test initialized):
curl -s -b cookies_admin.txt -XGET "http://$INFHOST/social/person/delete/user1_test" > /dev/null
curl -s -b cookies_admin.txt -XGET "http://$INFHOST/social/person/delete/test2@test.com" > /dev/null
curl -s -b cookies_admin.txt -XGET "http://$INFHOST/social/person/delete/test3@test.com" > /dev/null
curl -s -b cookies_admin.txt -XGET "http://$INFHOST/social/person/delete/user3_test.com" > /dev/null
curl -s -b cookies_admin.txt -XGET "http://$INFHOST/social/person/delete/jillsmith@ikanow.com" > /dev/null

echo "[1] passed succesfully"

####################################################3

#2. USER CREATION

echo "2] User creation tests"

#2a: Create user with both WPUserID and email (encoded password)

curl -s -b cookies_admin.txt -XPOST "http://$INFHOST/social/person/register" -d'
{
	"user": {
		"WPUserID":"user1_test",
		"firstname": "test",
		"lastname": "user1",
		"email":[ "test1@test.com" ]
	},
	"auth": {
		"password":"+HnVWFrttWiqqUIPZgbFwgg0SfPMqBsZw+5IGouxgJE="
	}
}
' | grep -q '{"response":{"action":"WP Register User","success":true' || fail "2a" "Register user" 
echo
sleep 1

#(check can login)
curl -s -c cookies_user1.txt -XGET "http://$INFHOST/auth/login/test1@test.com/%2BHnVWFrttWiqqUIPZgbFwgg0SfPMqBsZw%2B5IGouxgJE%3D" | \
	grep -q '{"response":{"action":"Login","success":true' || fail "2a" "User1 login" 
echo
sleep 1

#(check displayname)
curl -s -b cookies_user1.txt -XGET "http://$INFHOST/social/person/get" | \
	grep -q '"displayName":"test user1"' || fail "2a" "Display name"
echo
sleep 1

echo "Test 2a passed succesfully"

#2b: Create user with no WPUserID but an email success (decoded password)

curl -s -b cookies_admin.txt -XPOST "http://$INFHOST/social/person/register" -d'
{
	"user": {
		"lastname": "user2",
		"email":[ "test2@test.com" ]
	},
	"auth": {
		"password":"user2_test"
	}
}
' | grep -q '{"response":{"action":"WP Register User","success":true' || fail "2b" "Register user" 
echo
sleep 1

#(check can login)
curl -s -c cookies_user2.txt -XGET "http://$INFHOST/auth/login/test2@test.com/HY8NvDfrKINBsp8FV%2BGcZPUb6SwlehgyotdLSL8KqzM%3D" | \
	grep -q '{"response":{"action":"Login","success":true' || fail "2b" "User2 login" 
echo
sleep 1

#(check displayname)
curl -s -b cookies_user2.txt -XGET "http://$INFHOST/social/person/get" | \
	grep -q '"displayName":"user2"' || fail "2b" "Display name"
echo
sleep 1

#(check WPUserID)
curl -s -b cookies_user2.txt -XGET "http://$INFHOST/social/person/get" | \
	grep -q '"WPUserID":"test2@test.com"' || fail "2b" "WPUserID"
echo
sleep 1

echo "Test 2b passed succesfully"

#2c: Fails because no email

curl -s -b cookies_admin.txt -XPOST "http://$INFHOST/social/person/register" -d'
{
	"user": {
		"WPUserID":"user1_test",
		"lastname": "user3"
	},
	"auth": {
		"password":"user3_test"
	}
}
' | grep -q '{"response":{"action":"WP Register User","success":false' || fail "2c" "Register user (blank email)" 
echo
sleep 1

curl -s -b cookies_admin.txt -XPOST "http://$INFHOST/social/person/register" -d'
{
	"user": {
		"lastname": "user3",
		"email":[ ]
	},
	"auth": {
		"password":"user3_test"
	}
}
' | grep -q '{"response":{"action":"WP Register User","success":false' || fail "2c" "Register user (empty email)" 
echo
sleep 1

echo "Test 2c passed succesfully"

#2d: Fails because no lastname or firstname

curl -s -b cookies_admin.txt -XPOST "http://$INFHOST/social/person/register" -d'
{
	"user": {
		"email":[ "test3@test.com" ]
	},
	"auth": {
		"password":"user3_test"
	}
}
' | grep -q '{"response":{"action":"WP Register User","success":false' || fail "2d" "Register user (blank email)" 
echo
sleep 1

echo "Test 2d passed succesfully"

#2e: Fails because WPUserId exists

curl -s -b cookies_admin.txt -XPOST "http://$INFHOST/social/person/register" -d'
{
	"user": {
		"WPUserID":"user1_test",
		"firstname": "test",
		"lastname": "user3",
		"email":[ "test3@test.com" ]
	},
	"auth": {
		"password":"+HnVWFrttWiqqUIPZgbFwgg0SfPMqBsZw+5IGouxgJE="
	}
}
' | grep -q '{"response":{"action":"WP Register User","success":false' || fail "2e" "Register user (duplicate WPUserID)" 
echo
sleep 1

echo "Test 2e passed succesfully"

#2f: Fails because email exists

curl -s -b cookies_admin.txt -XPOST "http://$INFHOST/social/person/register" -d'
{
	"user": {
		"WPUserID":"user3_test",
		"firstname": "test",
		"lastname": "user3",
		"email":[ "test1@test.com" ]
	},
	"auth": {
		"password":"+HnVWFrttWiqqUIPZgbFwgg0SfPMqBsZw+5IGouxgJE="
	}
}
' | grep -q '{"response":{"action":"WP Register User","success":false' || fail "2f" "Register user (duplicate email)" 
echo
sleep 1

echo "Test 2f passed succesfully"

#2e: create a user using the legacy interface

USERJSON="%7B%22created%22%3A%22Oct%2021%2C%202011%2014%3A13%3A08%20PM%22%2C%22modified%22%3A%22Oct%2021%2C%202011%2014%3A13%3A08%20PM%22%2C%22firstname%22%3A%22jill%22%2C%22lastname%22%3A%22smith%22%2C%22phone%22%3A%225555555555%22%2C%22email%22%3A%5B%22jillsmith%40ikanow.com%22%5D%20%7D"
AUTHJSON="%7B%22username%22%3A%22jillsmith%40ikanow.com%22%2C%22password%22%3A%22%2BHnVWFrttWiqqUIPZgbFwgg0SfPMqBsZw%2B5IGouxgJE%3D%22%2C%22accountType%22%3A%22user%22%2C%22created%22%3A%22Oct%2021%2C%202011%2014%3A13%3A08%20PM%22%2C%22modified%22%3A%22Oct%2021%2C%202011%2014%3A13%3A08%20PM%22%20%7D"
curl -s -b cookies_admin.txt -XGET "http://$INFHOST/social/person/register?wpuser=${USERJSON}&wpauth=${AUTHJSON}" 

#(check can login)
curl -s -c cookies_jill.txt -XGET "http://$INFHOST/auth/login/jillsmith@ikanow.com/%2BHnVWFrttWiqqUIPZgbFwgg0SfPMqBsZw%2B5IGouxgJE%3D" | \
	grep -q '{"response":{"action":"Login","success":true' || fail "2e" "Jill Smith login" 
echo
sleep 1

echo "[2] passed succesfully"

####################################################3

#3. USER MODIFICATION

echo "3] User update tests"

#3a: Change the password of a user (ref via email) ... logged in as admin

curl -s -b cookies_admin.txt -XPOST "http://$INFHOST/social/person/update" -d'
{
	"user": {
		"email":[ "test2@test.com" ]
	},
	"auth": {
		"password":"n1ght1m3"
	}
}
' | grep -q '{"response":{"action":"WP Update User","success":true' || fail "3a" "Update password" 
echo
sleep 1

#(check can login with new password)
curl -s -c cookies_user2.txt -XGET "http://$INFHOST/auth/login/test2@test.com/lxjNCfljrF7JqeMkBH%2FCLnuh8pxliPD0YswAluUIy8A%3D" | \
	grep -q '{"response":{"action":"Login","success":true' || fail "3a" "User2 login" 
echo
sleep 1

echo "Test 3a passed succesfully"

#3b,c: Change the email of a user and display name  (ref via WPUserID) ... logged in as self

curl -s -b cookies_user1.txt -XPOST "http://$INFHOST/social/person/update" -d'
{
	"user": {
		"WPUserID":"user1_test",
		"firstname":"testing",
		"email":[ "testing1@test.com" ]
	},
	"auth": {
	}
}
' | grep -q '{"response":{"action":"WP Update User","success":true' || fail "3b,c" "Update display name and email" 
echo
sleep 1

#(check changes applied)
curl -s -b cookies_user1.txt -XGET "http://$INFHOST/social/person/get" | \
	grep -q '"displayName":"testing user1"' || fail "3b" "Display name"
echo
sleep 1

#(check changes applied)
curl -s -b cookies_user1.txt -XGET "http://$INFHOST/social/person/get" | \
	grep -q '"email":"testing1@test.com"' || fail "3c" "Email"
echo
sleep 1

#(check can -still- login)
curl -s -c cookies_user1.txt -XGET "http://$INFHOST/auth/login/testing1@test.com/%2BHnVWFrttWiqqUIPZgbFwgg0SfPMqBsZw%2B5IGouxgJE%3D" | \
	grep -q '{"response":{"action":"Login","success":true' || fail "3b,c" "User1 login" 
echo
sleep 1

echo "Tests 3b,c passed succesfully"

#3d: Change the password via the short cut interface ... logged in as admin (clear version)

curl -s -b cookies_admin.txt -XGET "http://$INFHOST/social/person/update/password/test2@test.com/user2_test"

#(check can -still- login)
curl -s -c cookies_user2.txt -XGET "http://$INFHOST/auth/login/test2@test.com/HY8NvDfrKINBsp8FV%2BGcZPUb6SwlehgyotdLSL8KqzM%3D" | \
	grep -q '{"response":{"action":"Login","success":true' || fail "3d" "User2 login" 
echo
sleep 1

echo "Test 3d passed succesfully"

#3e: Change the password via the short cut interface ... logged in as user (hashed version)

curl -s -b cookies_user2.txt -XGET "http://$INFHOST/social/person/update/password/test2@test.com/lxjNCfljrF7JqeMkBH%2FCLnuh8pxliPD0YswAluUIy8A%3D"

#(check can -still- login)
curl -s -c cookies_user2.txt -XGET "http://$INFHOST/auth/login/test2@test.com/lxjNCfljrF7JqeMkBH%2FCLnuh8pxliPD0YswAluUIy8A%3D" | \
	grep -q '{"response":{"action":"Login","success":true' || fail "3e" "User2 login" 
echo
sleep 1

echo "Test 3e passed succesfully"

#3f: Change the email via the short cut interface ... logged in as admin

curl -s -b cookies_admin.txt -XGET "http://$INFHOST/social/person/update/email/user1_test/test1@test.com"

#(check email)
curl -s -b cookies_user1.txt -XGET "http://$INFHOST/social/person/get" | \
	grep -q '"email":"test1@test.com"' || fail "3f" "Email address"
echo
sleep 1

echo "Test 3f passed succesfully"

#3g. Check wpupdate legacy interface

USERJSON="%7B%22created%22%3A%22Oct%2021%2C%202011%2014%3A13%3A08%20PM%22%2C%22modified%22%3A%22Oct%2021%2C%202011%2014%3A13%3A08%20PM%22%2C%22firstname%22%3A%22jilly%22%2C%22lastname%22%3A%22smyth%22%2C%22phone%22%3A%225555555555%22%2C%22email%22%3A%5B%22jillsmith%40ikanow.com%22%5D%20%7D"
AUTHJSON="%7B%22username%22%3A%22jillsmith%40ikanow.com%22%2C%22password%22%3A%22%2BHnVWFrttWiqqUIPZgbFwgg0SfPMqBszw%2B5IGouxgJE%3D%22%2C%22accountType%22%3A%22user%22%2C%22created%22%3A%22Oct%2021%2C%202011%2014%3A13%3A08%20PM%22%2C%22modified%22%3A%22Oct%2021%2C%202011%2014%3A13%3A08%20PM%22%20%7D"
curl -s -b cookies_admin.txt -XGET "http://$INFHOST/social/person/wpupdate?wpuser=${USERJSON}&wpauth=${AUTHJSON}" 

#(check can still login)
curl -s -c cookies_jill.txt -XGET "http://$INFHOST/auth/login/jillsmith@ikanow.com/%2BHnVWFrttWiqqUIPZgbFwgg0SfPMqBszw%2B5IGouxgJE%3D" | \
	grep -q '{"response":{"action":"Login","success":true' || fail "3f" "Jill Smith login" 
echo
sleep 1

#(check changes applied)
curl -s -b cookies_jill.txt -XGET "http://$INFHOST/social/person/get" | \
	grep -q '"displayName":"jilly smyth"' || fail "3g" "Display name"
echo
sleep 1

echo "Test 3g passed succesfully"

echo "[3] passed succesfully"

####################################################3

#4. USER DELETION

#4a. Trying deleting users logged in as non-admin

curl -s -b cookies_user2.txt -XGET "http://$INFHOST/social/person/delete/user1_test" \
	| grep -q '"success":false' || fail "4a" "Delete test1@test.com / user1_test as user2" 
echo
sleep 1

echo "Test 4a passed succesfully"

#4b. Delete the 2 users created in 2a and 2b

curl -s -b cookies_admin.txt -XGET "http://$INFHOST/social/person/delete/user1_test" \
	| grep -q '{"response":{"action":"WP Delete User","success":true' || fail "4b" "Delete test1@test.com / user1_test" 
echo
sleep 1

curl -s -XGET "http://$INFHOST/social/person/delete/test2@test.com?admuser=$ADMIN_USER&admpass=$ADMIN_PASS" \
	| grep -q '{"response":{"action":"WP Delete User","success":true' || fail "4b" "Delete test2@test.com / user2" 
echo
sleep 1

#Try a command to check that the cookies have been deleted

curl -s -b cookies_user2.txt -XGET "http://$INFHOST/social/person/get" | \
	grep -q '"success":false' || fail "4b" "User2 cookie not deleted"
echo
sleep 1

echo "Test 4b passed succesfully"

#4c. Try deleting a user that doesn't exist

#(See setup code)

echo "Test 4c passed succesfully"

#4d. Delete other users

curl -s -b cookies_admin.txt -XGET "http://$INFHOST/social/person/delete/jillsmith@ikanow.com" > /dev/null

echo "Test 4d passed succesfully"

echo "[4] passed succesfully"

####################################################3

#5. TIDY UP
curl -s -b cookies.admin -XGET "http://$INFHOST/auth/logout" | \
	grep -q '{"response":{"action":"Login","success":true' || fail "1" "Admin logout" >& /dev/null
echo

echo "[5] passed succesfully"

