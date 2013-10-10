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

function init()
{
	chrome.browserAction.setTitle({"title":"Ikanow Source: Select Source"});
	//go get a users available sources
	sendHttpRequestToInfinite("config/source/user", sourceHandler);
	var createSourceButton = document.getElementById("createSourceButton");
	createSourceButton.addEventListener("click",create_new_source);	
	var sourceList = document.getElementById("sourceList");
	sourceList.addEventListener("change",source_list_change);
}

function sourceHandler(message, httprequest)
{
	//if login was successful, we need to do our work, otherwise print error?
	var json = JSON.parse(message);
	if ( json.response.success )
	{		
		add_sources(json.data);
	}
}

function add_sources(sources)
{
	sources.sort(function(a,b){return (a.title.toLowerCase().trim() < b.title.toLowerCase().trim()) ? -1 : 1;});
	var select = document.getElementById("sourceList");
	for ( var i = 0; i < sources.length; i++)
	{
		var source = sources[i];
		var option = document.createElement("option");
		option.innerHTML = source.title;
		option.data = source;
		select.appendChild(option);
	}
}

function source_list_change()
{
	var select = document.getElementById("sourceList");
	var selectedItem = select.options[select.selectedIndex].data;
	var json = JSON.stringify(selectedItem);
	localStorage["source"] = json;
	chrome.browserAction.setIcon({"path":"assets/icons/lightbulbadd.png"});
	chrome.browserAction.setTitle({"title":"Ikanow Source: Click to add this page to source"});
	chrome.browserAction.setPopup({"popup":"sourceAdder.html"});
	//show user they are good
	var status = document.getElementById("status");
	/*while ( status.hasChildNodes() )
	{
		status.removeChild(status.lastChild);
	}*/
	status.innerHTML = "Source '"+selectedItem.title+"' selected successfully.  Use the Ikanow button on a page you wish to add.";
	
	/*var br = document.createElement("br");
	document.body.appendChild(br);
	var para = document.createElement("p");
	para.innerHTML = "Source '"+selectedItem.title+"' selected successfully.  Use the Ikanow button on a page you wish to add."; 			
	document.body.appendChild(para);*/
}

function create_new_source()
{
	window.location="addSource.html";
}