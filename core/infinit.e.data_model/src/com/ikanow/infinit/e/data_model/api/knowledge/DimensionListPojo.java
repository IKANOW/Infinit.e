/*******************************************************************************
 * Copyright 2012, The Infinit.e Open Source Project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
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
