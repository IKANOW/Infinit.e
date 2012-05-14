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
package com.ikanow.infinit.e.data_model.api.knowledge;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ikanow.infinit.e.data_model.api.BaseApiPojo;


public class DimensionListPojo extends BaseApiPojo {
	public List<SearchSuggestPojo> dimensions = new ArrayList<SearchSuggestPojo>();
	transient private Set<String> currentSuggestions = new HashSet<String>();
	
	public void addSearchSuggestPojo(SearchSuggestPojo suggestion)
	{
		String suggestionhash = new StringBuffer(suggestion.getValue()).append(suggestion.getType()).toString().toLowerCase();
		if ( !currentSuggestions.contains(suggestionhash) )
		{
			currentSuggestions.add(suggestionhash);
			dimensions.add(suggestion);
		}
	}
	public void reset() {
		dimensions.clear();
		currentSuggestions.clear();
	}
}
