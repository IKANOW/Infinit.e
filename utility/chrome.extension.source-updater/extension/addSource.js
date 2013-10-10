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
	var createSourceButton = document.getElementById("createSourceButton");
	createSourceButton.addEventListener("click", create_new_source)
	var backButton = document.getElementById("backButton");
	backButton.addEventListener("click", back_button_click);
	var templateList = document.getElementById("templateList");
	templateList.addEventListener("change", template_list_change);
	
	//get person object so we can use community
	sendHttpRequestToInfinite("social/person/get/", personHandler);
	//add the templates via code so we can set extra attributes
	var select = document.getElementById("templateList");
	//none		
	var option1 = document.createElement("option");
	option1.innerHTML = "System Default";
	option1.data = {"textExtractor":"","extractor":""};
	select.appendChild(option1);
	//sentiment
	var option3 = document.createElement("option");
	option3.innerHTML = "Entities with Sentiment";
	option3.data = {"textExtractor":"null","extractor":"AlchemyAPI"};
	select.appendChild(option3);
	//statistical keywords and sentiment
	var option4 = document.createElement("option");
	option4.innerHTML = "Keywords with Sentiment";
	option4.data = {"textExtractor":"null","extractor":"AlchemyAPI-metadata"};
	select.appendChild(option4);
	//business oriented
	var option5 = document.createElement("option");
	option5.innerHTML = "Business Oriented Entities and Associations";
	option5.data = {"textExtractor":"AlchemyAPI","extractor":"OpenCalais"};
	select.appendChild(option5);
	//open source ents
	var option6 = document.createElement("option");
	option6.innerHTML = "Open Source Edition: Entities and Associations";
	option6.data = {"textExtractor":"boilerpipe","extractor":"OpenCalais"};
	select.appendChild(option6);
	//open source keyword
	var option7 = document.createElement("option");
	option7.innerHTML = "Open Source Edition: Keywords";
	option7.data = {"textExtractor":"boilerpipe","extractor":"textrank"};
	select.appendChild(option7);
}

function personHandler(message, httprequest)
{
	var json = JSON.parse(message);
	if ( json.response.success )
	{		
		var person = json.data;
		add_communities(person.communities);
	}
	else
	{
		var para = document.createElement("p");
		para.innerHTML = "Failed to get user object: " + json.response.message; 			
		document.body.appendChild(para);	
	}
}

function add_communities(communities)
{
	communities.sort(function(a,b){return (a._id.toLowerCase().trim() < b._id.toLowerCase().trim()) ? -1 : 1;});
	var select = document.getElementById("communityList");
	for ( var i = 0; i < communities.length; i++)
	{
		var community = communities[i];
		var option = document.createElement("option");
		option.innerHTML = community.name;
		option.data = community;
		select.appendChild(option);
	}
	select.selectedIndex = 0;
}

function template_list_change()
{
	var templateSelect = document.getElementById("templateList");
	var selectedItem = templateSelect.options[templateSelect.selectedIndex].data;
	var textExtractor = selectedItem.textExtractor;
	var entExtractor = selectedItem.extractor;
	//set the extractors to this templates defaults
	//set text
	var textSelect = document.getElementById("textList");
	for ( var i = 0; i < textSelect.length; i++)
	{		
		if ( textSelect[i].value == textExtractor )
		{
			textSelect.selectedIndex = i;
			break;
		}
	}
	//set ent
	var entSelect = document.getElementById("entList");
	for ( var i = 0; i < entSelect.length; i++)
	{		
		if ( entSelect[i].value == entExtractor )
		{
			entSelect.selectedIndex = i;
			break;
		}
	}
}

function create_new_source()
{	
	var source = getSourceFromTemplate();
	
	//set this source to active, it will save the first time a url is added
	var json = JSON.stringify(source);
	localStorage["source"] = json;
	chrome.browserAction.setIcon({"path":"assets/icons/lightbulbadd.png"});
	chrome.browserAction.setTitle({"title":"Ikanow Source: Click to add this page to source"});
	chrome.browserAction.setPopup({"popup":"sourceAdder.html"});
	//let user know they are good
	var br = document.createElement("br");
	document.body.appendChild(br);
	var para = document.createElement("p");
	para.innerHTML = "Source created successfully, it will not be saved until the first link is added.  Use the Ikanow button on a page you wish to add."; 			
	document.body.appendChild(para);
}

function getSourceFromTemplate()
{
	//get all the inputs and validate them
	var title = document.getElementById("titleInput").value;
	var desc = document.getElementById("descInput").value;
	var mediaType = document.getElementById("mediaInput").value;
	var select = document.getElementById("communityList");
	var selectedItem = select.options[select.selectedIndex].data;
	var communityID = selectedItem._id;
	var textSelect = document.getElementById("textList");
	var textExtractor = textSelect.options[textSelect.selectedIndex].value;
	var entSelect = document.getElementById("entList");
	var entExtractor = entSelect.options[entSelect.selectedIndex].value;
	var templateSelect = document.getElementById("templateList");
	var template = templateSelect.options[templateSelect.selectedIndex].value;
	var source = {"title":title,"description":desc,"extractType":"Feed","mediaType":mediaType,"rss":{"extraUrls":[]},"communityIds":[communityID]};
	if ( entExtractor != "null" && entExtractor != "")
	{
		source.useExtractor = entExtractor;
	}
	if ( textExtractor != "null" && textExtractor != "")
	{
		source.useTextExtractor = textExtractor;
	}
	
	return source;
}

function back_button_click()
{
	window.location="sourceSelection.html";
}