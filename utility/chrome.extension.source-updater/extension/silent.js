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
chrome.tabs.onUpdated.addListener(tabUpdated);
chrome.tabs.onActivated.addListener(tabActivated);

function tabActivated(activeInfo)
{
	chrome.tabs.get(activeInfo.tabId, getTab);
}

function getTab(tab)
{
	checkTab(tab.url);	
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

function tabUpdated(tabId, changeInfo, tab)
{
	//only check if its the active tab
	if ( tab.active )
	{
		checkTab(changeInfo.url);
	}
}

function checkTab(url)
{
	//check if this tab is part of the current urls, if it is, turn the bulb blue
	if ( url != null )
	{
		var source = JSON.parse( localStorage["source"] );
		if ( hasUrl(url, source) )
		{
			chrome.browserAction.setTitle({"title":"Ikanow Source: This page is already in the source"});
			chrome.browserAction.setIcon({"path":"assets/icons/lightbulbcheck.png"});
		}
		else
		{
			chrome.browserAction.setTitle({"title":"Ikanow Source: Click to add this page to source"});
			chrome.browserAction.setIcon({"path":"assets/icons/lightbulbadd.png"});		 
		}
	}
}