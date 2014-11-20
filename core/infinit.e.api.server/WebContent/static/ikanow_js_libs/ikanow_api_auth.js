var api_key;
var api_url;
var login_callback;

//e.g. localhost:8080/api/
function setIKANOWApiUrl(url)
{
	api_url = url;
}

function getIKANOWApiUrl()
{
	return api_url;
}

//will retrieve the api key then call the callback function w/ it as a param (or null if we fail)
function getIKANOWApiKey(callback)
{
	login_callback = callback;
	//case 1, the local var is set, just return the key
	if ( api_key )
	{
		callback( api_key );
	}
	
	//case 2, try to get key from localstorage, test if its valid
	var temp_api_key = localStorage.getItem("IKANOW_API_KEY");
	if ( temp_api_key )
	{
		//test its valid
		$.get(api_url + "auth/keepalive?infinite_api_key=" + temp_api_key, function(data)
		{
			if ( data.response.success === true )
			{
				api_key = temp_api_key;
				callback( api_key );
			}
			else
			{
				openLoginPopup();
			}
		});		
	}
	else
	{
		//case 3, we don't have a valid key, popup login dialog		
		openLoginPopup();
	}	
}

function openLoginPopup(callback)
{
    //have to load window by hand otherwise they can't talk to each other (cross origin frame)
	//window.open(api_url + "auth/ui/popup","myWindow", "width=500,height=300");
    var popupRef = window.open("","myWindow", "width=500,height=300");
	popupRef.document.write("<style>\nhtml {\n	font-size: 10pt;\n}\nlabel \n{\n	display: block;\n	color: #555;\n}\n.cf:before,\n.cf:after \n{\n    content: \"\"; \n    display: table;\n}\n\n.cf:after \n{\n    clear: both;\n}\n.cf \n{\n    *zoom: 1;\n}\n.loginform \n{\n	width: 410px;\n	margin: 50px auto;\n	padding: 25px;\n	background-color: rgba(250,250,250,0.5);\n	border-radius: 5px;\n    border: 1px solid rgba(0, 0, 0, 0.3);\n}\n.loginform ul \n{\n	padding: 0;\n	margin: 0;\n}\n.loginform li \n{\n	display: inline;\n	float: left;\n}\n.loginform input:not([type=submit]) {\n	padding: 5px;\n	margin-right: 10px;\n	border: 1px solid rgba(0, 0, 0, 0.3);\n	border-radius: 3px;\n	box-shadow: inset 0px 1px 3px 0px rgba(0, 0, 0, 0.1), \n				0px 1px 0px 0px rgba(250, 250, 250, 0.5) ;\n}\n.loginform input[type=submit] \n{\n	border: 1px solid rgba(0, 0, 0, 0.3);		\n	color: #000;\n	padding: 5px 15px;\n	margin-right: 0;\n	margin-top: 15px;\n	border-radius: 3px;\n}\n</style>\n<section class=\"loginform cf\" >\n	<form id=\"inf_login_form\" action=\""+ api_url +"auth/login\">\n		<h1>IKANOW Login</h1>\n		<input type=\"hidden\" name=\"return_tmp_key\" value=\"true\" />\n		<ul>			\n			<li ><label for=\"usermail\">Email</label>  <input id=\"username\" type=\"email\" name=\"username\" placeholder=\"yourname@email.com\" required></li>  \n			<li ><label for=\"password\">Password</label>  <input type=\"password\" name=\"password\" placeholder=\"password\" required></li>  			\n			<li><input type=\"submit\" value=\"Login\"></li>			\n		</ul>\n	</form>\n</section>\n<script src=\"http://code.jquery.com/jquery-1.11.0.min.js\"></script>\n<script src=\"http://crypto-js.googlecode.com/svn/tags/3.1.2/build/rollups/sha256.js\"></script>\n<script src=\"http://crypto-js.googlecode.com/svn/tags/3.1.2/build/components/enc-base64-min.js\"></script>\n<script>\n	//this script block is after my html block so inf_login_form is ready when this tries to execute\n	//CANNOT PUT BEFORE\n	var inf_form = document.getElementById(\"inf_login_form\");\n	inf_form.addEventListener(\"submit\", validateForm, false);	\n\n	function validateForm(event)\n	{\n		event.preventDefault();\n		//encode the password using https://code.google.com/p/crypto-js/	\n		var password = inf_form.password.value;\n		var encrypt_password = encodeURIComponent( CryptoJS.SHA256(password).toString(CryptoJS.enc.Base64) );		\n		\n		inf_form.password.value=encrypt_password;\n		\n		//TODO send ajax request instead of form submission/redir\n		var login_url = window.opener.getIKANOWApiUrl() + \"auth/login?return_tmp_key=true&username=\"+$(\"#username\").val()+\"&password=\"+encrypt_password+\"&infinite_api_key=none\";\n		$.get(login_url, \n		function(data)\n		{\n			if ( data.response.success == true)\n			{\n				CloseMySelf(data.data);\n			}\n			else\n			{\n				alert(\"login failed\");\n			}\n		});\n	}\n	\n	function CloseMySelf(api_key)\n	{\n		try\n		{\n			window.opener.HandlePopupResult(api_key);\n		}\n		catch (err)\n		{\n			console.log(\"error sending to parent: \" + err);\n		}\n		window.close();\n		return false;\n	}\n</script>");
	//window.opener.setEndpoint(api_url);
	//window.setTimeout(setPopupEndpoint, 1000);
}

function HandlePopupResult(result)
{
	//write key to localStorage
	if ( result )
	{
		result = "tmp:" + result;
		localStorage.setItem("IKANOW_API_KEY", result);
	}	
	login_callback(result);
}