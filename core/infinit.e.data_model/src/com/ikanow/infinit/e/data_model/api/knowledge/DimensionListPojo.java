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
