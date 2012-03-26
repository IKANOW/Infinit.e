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

import java.util.List;

import com.ikanow.infinit.e.data_model.store.document.GeoPojo;

public class SearchSuggestPojo 
{
	private String dimension;
	private String value; 
	private String type;
	private GeoPojo geotag;
	private String ontology_type;
	private List<Object> linkdata; // this is actually a Link<String> but Java doesn't agree 
	
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
}
