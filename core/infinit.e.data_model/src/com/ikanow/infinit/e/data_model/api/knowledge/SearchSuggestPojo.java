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
