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

// INITIALIZATION

var infiniteJsConnector = infiniteJsConnector || {

	_flashMovie: null,

	getParentId: function() {
        var parentDoc = window;
        while(parentDoc !== parentDoc.parent)
        {
            parentDoc = parentDoc.parent;
        }
        parentDoc = parentDoc.document;
        var iFrames = parentDoc.getElementsByTagName('iframe');
        return iFrames[0].getAttribute("id");
	},

	init: function () {

		if (null == infiniteJsConnector._flashMovie) {
		    // OK for some reason, Chrome puts scroll bars up so we'll remove them...
		    // the sizes all look fine, so no idea why..
		    var id = '#' + infiniteJsConnector.getParentId().substring(7);
		    $(window.parent.document).find(id).css("overflow", "hidden");
		
		    if (document.getElementById) {
		    	infiniteJsConnector._flashMovie = parent.document.getElementById("Index");
		    }
		}
	},
	
	// CALLBACKS
	onNewDocumentSet: function() {},	
	//ACCESSORS

	getAssociations: function(maxAssociations)
	{
		var associations = [];
		try {
			var associationsStr = infiniteJsConnector._flashMovie.getAssociations(maxAssociations);
			if (null != associationsStr) {
				associations = JSON.parse(associationsStr);
			}
		}
		catch (e) {
			//alert("getAssociationsJS: " + e);
		}
		return associations;
	}
}

