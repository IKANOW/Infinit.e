/*******************************************************************************
 * Copyright 2012 The Infinit.e Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
document.addEventListener("DOMContentLoaded", init );

//add any necessary init handlers and such
function init()
{
	var apiinput = document.getElementById("apiInput");
	apiinput.addEventListener("change",setManagerLink);
	var saveButton = document.getElementById("saveButton");
	saveButton.addEventListener("click",save_options_and_login);
	restore_options();	
	setManagerLink();
}

function setManagerLink()
{
	var apiinput = document.getElementById("apiInput");
	var sourcelink = document.getElementById("sourceManagerLink");
	var apilink = apiinput.value
	if ( apilink != null && apilink.length > 4 && apilink.substring(apilink.length-4) == "api/" )
	{
		apilink = apilink.substring(0,apilink.length-4)
		sourcelink.href = apilink + "manager/sources.jsp";
	}
}

// Saves options to localStorage.
function save_options_and_login() 
{
	var userinput = document.getElementById("usernameInput");
	var passinput = document.getElementById("passwordInput");
	var timeoutinput = document.getElementById("timeoutInput");
	var apiinput = document.getElementById("apiInput");
  	localStorage["username"] = userinput.value;
  	localStorage["password"] = passinput.value;
  	localStorage["timeout"] = timeoutinput.value;
  	setAPI(apiinput.value);
  	
  	// Update status to let user know options were saved.
  	var status = document.getElementById("status");
  	status.innerHTML = "Login Saved.";
  	setTimeout(function() {
    	status.innerHTML = "";
  	}, 2000);
  	
  	//send login
  	loginInfinite();
}

// Restores select box state to saved value from localStorage.
function restore_options() 
{
  	var username = localStorage.username;
  	var password = localStorage.password;
  	var timeout = localStorage.timeout;
  	var api = getAPI();
  	
  	if (username) 
	{
		var userinput = document.getElementById("usernameInput");
		userinput.value = username;
  	}
  	if (password)
  	{
  		var passinput = document.getElementById("passwordInput");
  		passinput.value = password;
  	}
  	var timeoutinput = document.getElementById("timeoutInput");
  	if (timeout)
  	{
  		timeoutinput.value = timeout;
  	}  	
  	else
  	{  		
  		timeoutinput.value = "60000";
  	}
  	if ( api )
  	{
  		var apiinput = document.getElementById("apiInput");
  		apiinput.value = api;
  	}
}

function loginInfinite()
{
	var username = localStorage.username;
	var password = localStorage.password;
	if ( username == undefined || password == undefined )
	{
		//show login popup
		var div = document.createElement("div");
		
		var span1 = document.createElement("span");
		span1.appendChild(document.createTextNode('Please fill in the login settings found '));
		div.appendChild(span1);
		
		var anchor = document.createElement("a");
		anchor.setAttribute("href","options.html");
		anchor.setAttribute("target","new");		
		anchor.appendChild(document.createTextNode('here'));			
		div.appendChild(anchor);		
		
		postToStatus(div);	
	}
	else
	{
		//test hasher
		var hash = CryptoJS.SHA256(password);
		var encode = encodeURIComponent( hash.toString(CryptoJS.enc.Base64));
		//send credentials
		sendHttpRequestToInfinite("auth/login/"+username+"/"+encode, loginHandler);
	}
}

function loginHandler(message, httprequest)
{
	//if login was successful, we need to do our work, otherwise print error?
	var json = JSON.parse(message);
	if ( json.response.success )
	{		
		chrome.browserAction.setTitle({"title":"Ikanow Source: Select Source"});
		window.location = "sourceSelection.html";	
	}
	else
	{
		var div = document.createElement("div");
		
		var span1 = document.createElement("span");
		span1.setAttribute("class","message");
		span1.appendChild(document.createTextNode('Failed to login, are your username/password correct?'));
		div.appendChild(span1);
		
		postToStatus(div);		
	}
}

function postToStatus(elem)
{
	var status = document.getElementById("status1");
	status.appendChild(elem);
}