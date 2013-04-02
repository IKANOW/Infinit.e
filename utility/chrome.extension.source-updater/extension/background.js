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
iconClicked();
//chrome.browserAction.onClicked.addListener(iconClicked);


function iconClicked()
{
	//if logged in and source active, add current page to source
	sendHttpRequestToInfinite("auth/keepalive", cookieHandler);
}

function cookieHandler(message, httprequest)
{
	//if login was successful, we need to do our work, otherwise print error?
	var json = JSON.parse(message);
	if ( json.response.success )
	{		
		//still logged in, see if we have an active source and just use that
		if ( localStorage["source"] != null )
		{
			var para = document.createElement("p");
			para.innerHTML = "Logged in and have source, try to add this page to source"; 			
			document.body.appendChild(para);
			//have a source already so switch popup to source adder			
			chrome.browserAction.setIcon({"path":"assets/icons/lightbulbadd.png"});
			chrome.browserAction.setTitle({"title":"Ikanow Source: Click to add this page to source"});
			chrome.browserAction.setPopup({"popup":"sourceAdder.html"});
			window.location = "sourceAdder.html";
					
		}
		else
		{
			var para = document.createElement("p");
			para.innerHTML = "Logged in successfully, no active source."; 			
			document.body.appendChild(para);
			//skip to source selection page
			chrome.browserAction.setTitle({"title":"Ikanow Source: Select Source"});
			window.location = "sourceSelection.html";
		}
	}
	else
	{
		//clear any active source
		localStorage.removeItem("source");		
		//not logged in, open options page
		//chrome.tabs.create({"url":"options.html"});
		window.location = "options.html";
	}
}