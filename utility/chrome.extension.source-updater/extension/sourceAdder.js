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

//this will fire when the popup opens, we will send off a 
//login to make sure we stay active
function init()
{
	chrome.browserAction.setTitle({"title":"Ikanow Source: Click to add this page to source"});
	var username = localStorage.username;
	var password = localStorage.password;
	var hash = CryptoJS.SHA256(password);
	var encode = encodeURIComponent( hash.toString(CryptoJS.enc.Base64) );
	sendHttpRequestToInfinite("auth/login/"+username+"/"+encode, loginHandler);
}

function loginHandler(message, httprequest)
{
	var json = JSON.parse(message);
	if ( json.response.success )
	{
		//get current tab
		chrome.tabs.getSelected(onCurrentTab);
	}
	else
	{
		var para = document.createElement("p");
		para.innerHTML = "Failed to connect to Infinit.e, did you change user/password?"; 			
		document.body.appendChild(para);
	}
}

function onCurrentTab(tab) 
{
	var url = tab.url;
	var source = JSON.parse( localStorage["source"] );
	//got url, check if its already in source, if not add it
	if ( hasUrl(url, source) )
	{
		var para = document.createElement("p");	
		para.innerHTML = tab.url + " is already in " + source.title;
		document.body.appendChild(para);  
	}
	else
	{
		var title = null;
		if ( !isRSS(url) )
		{
			title = tab.title;
		}
		var description = "(no description)";
		addUrlToSource(url,title,description,source);		 
	}
}

function hasUrl(url, source)
{
	//look at source.rss.extraUrls[]
	if ( source != null )
	{
		if ( source.rss != null )
		{
			var sourceUrls = source.rss.extraUrls;
			if ( sourceUrls != null)
			{
				//check if url is in here, if not add it				
				for (var i=0;i < sourceUrls.length; i++ )
				{
					var urlObject = sourceUrls[i];
					if ( urlObject.url == url )
					{
						return true;
					}
				}
				return false;
			}
		}
	}
	return false;
}

function isRSS(url)
{
	//TODO (INF-2528): apparently this breaks everything?
	//get the extension of the page if there is one
//	var urlpatt = new RegExp("/rss.[a-z]+(\\?|$)","i");
//	if (urlpatt.test(url)) {
//		return true;
//	}
		
	var extIndex = url.lastIndexOf(".");
	if ( extIndex > -1 )
	{
		var ext = url.substr( extIndex+1 );
		var patt = new RegExp("^(rss|xml|atom)$","i");
		return patt.test(ext);
	}
	return false;
}

function addUrlToSource(url, title, description, source)
{		
	if ( source.rss.extraUrls == null )
	{
		source.rss.extraUrls = new Array();
	}
	var json = {"url":url,"description":description};
	if ( title != null )
	{
		json.title = title;
		var para = document.createElement("p");	
		para.innerHTML = "Attempting to add a single page.";
		document.body.appendChild(para);
	}
	else
	{
		var para = document.createElement("p");	
		para.innerHTML = "Attempting to add an RSS feed.";
		document.body.appendChild(para);
	}
	source.rss.extraUrls.push(json);
	
	var json = JSON.stringify(source);
	sendHttpRequestToInfinite("config/source/save/"+source.communityIds[0], sourceAddHandler, json);
}

function sourceAddHandler(message, httprequest)
{
	var json = JSON.parse(message);
	if ( json.response.success )
	{
		localStorage["source"] = JSON.stringify(json.data);
		var para = document.createElement("p");	
		para.innerHTML = "Source Added Successfully!";
		document.body.appendChild(para);
		chrome.browserAction.setTitle({"title":"Ikanow Source: This page is already in the source"});
		chrome.browserAction.setIcon({"path":"assets/icons/lightbulbcheck.png"});
	}
	else
	{
		var para = document.createElement("p");
		para.innerHTML = "Failed to Add Source: " + json.response.message; 			
		document.body.appendChild(para);
	}
}