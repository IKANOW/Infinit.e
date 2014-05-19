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

import java.util.List;

import com.google.gson.reflect.TypeToken;
import com.ikanow.infinit.e.data_model.store.document.GeoPojo;

public class SearchSuggestPojo 
{
	//@SuppressWarnings("unchecked")
	static public TypeToken<List<SearchSuggestPojo>> listType() { return new TypeToken<List<SearchSuggestPojo>>(){}; }
	
	private String dimension;
	private String value; 
	private String type;
	private GeoPojo geotag;
	private String ontology_type;
	private List<Object> linkdata; // this is actually a Link<String> but Java doesn't agree 
	private Double score;
	
	public String getDimension() {
		return dimension;
	}
	public void setDimension(String dimension) {
		this.dimension = dimension;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	
	public void setGeotag(GeoPojo geotag) {
		this.geotag = geotag;
	}
	public GeoPojo getGeotag() {
		return geotag;
	}
	
	public void setLocFromES(String latlng)
	{
		String[] loc = latlng.split(",");
		this.geotag = new GeoPojo(Double.parseDouble( loc[0]),Double.parseDouble( loc[1]));
	}
	public List<Object> getLinkdata() {
		return linkdata;
	}
	public void setLinkdata(List<Object> linkdata) {
		this.linkdata = linkdata;
	}
	public void setOntology_type(String ontology_type) {
		this.ontology_type = ontology_type;
	}
	public String getOntology_type() {
		return ontology_type;
	}
	public Double getScore() {
		return score;
	}
	public void setScore(Double score) {
		this.score = score;
	}
}
