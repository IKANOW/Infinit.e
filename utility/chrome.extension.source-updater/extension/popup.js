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
chrome.browserAction.onClicked.addListener(iconClicked);

function iconClicked()
{
	if ( localStorage["login"] == true )
	{
		//was first time, put up login message
		localStorage["login"] = false;
		var para = document.createElement("p");
		para.innerHTML = "Logged in successfully, click this Infinit.e icon on pages you want to add to your current source: " + localStorage["source"].title; 			
		document.body.appendChild(para);
	}
	else
	{
		//was subsequent time, try to add this page to source
		var para = document.createElement("p");
		para.innerHTML = "boo: " + localStorage["source"].title; 			
		document.body.appendChild(para);
		//chrome.tabs.getCurrent(onCurrentTab);
	}	
}

function onCurrentTab(tab) 
{
	var para = document.createElement("p");
	para.innerHTML = tab.url + " added to " + localStorage["source"].title;
	document.body.appendChild(para);  
}