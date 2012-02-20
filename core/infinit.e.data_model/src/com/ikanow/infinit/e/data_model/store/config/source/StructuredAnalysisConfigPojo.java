package com.ikanow.infinit.e.data_model.store.config.source;

import java.util.List;

/**
 * StructuredAnalysisPojo
 * @author cvitter
 */
public class StructuredAnalysisConfigPojo 
{
	
	// Private class variables
	private String title = null;
	private String description = null;
	private String url = null;
	private String publishedDate = null;
	private String scriptEngine = null;
	private String script = null;
	private String[] scriptFiles = null;
	private DocGeoSpecPojo docGeo = null; 
	private List<EntitySpecPojo> entities = null;
	private List<AssociationSpecPojo> associations = null;
		
	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}
	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}
	
	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}
	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	
	/**
	 * @param url the url to set
	 */
	public void setUrl(String url) {
		this.url = url;
	}
	/**
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}
	/**
	 * @param publishedDate the publishedDate to set
	 */
	public void setPublishedDate(String publishedDate) {
		this.publishedDate = publishedDate;
	}
	/**
	 * @return the publishedDate
	 */
	public String getPublishedDate() {
		return publishedDate;
	}

	/**
	 * @param scriptEngine the scriptEngine to set
	 */
	public void setScriptEngine(String scriptEngine) {
		this.scriptEngine = scriptEngine;
	}
	/**
	 * @return the scriptEngine
	 */
	public String getScriptEngine() {
		return scriptEngine;
	}

	/**
	 * @param script the script to set
	 */
	public void setScript(String script) {
		this.script = script;
	}
	/**
	 * @return the script
	 */
	public String getScript() {
		return script;
	}
	
	/**
	 * @param scriptFiles the scriptFiles to set
	 */
	public void setScriptFiles(String[] scriptFiles) {
		this.scriptFiles = scriptFiles;
	}
	/**
	 * @return the scriptFiles
	 */
	public String[] getScriptFiles() {
		return scriptFiles;
	}
	/**
	 * @param documentGeotag the documentGeotag to set
	 */
	public void setDocumentGeo(DocGeoSpecPojo documentGeo) {
		this.docGeo = documentGeo;
	}
	/**
	 * @return the documentGeotag
	 */
	public DocGeoSpecPojo getDocumentGeo() {
		return docGeo;
	}
	
	/**
	 * @param entities the entities to set
	 */
	public void setEntities(List<EntitySpecPojo> entities) {
		this.entities = entities;
	}
	/**
	 * @return the entities
	 */
	public List<EntitySpecPojo> getEntities() {
		return entities;
	}

	/**
	 * @param associations the associations to set
	 */
	public void setAssociations(List<AssociationSpecPojo> associations) {
		this.associations = associations;
	}
	/**
	 * @return the associations
	 */
	public List<AssociationSpecPojo> getAssociations() {
		return associations;
	}

	
	/**
	 * DocGeoSpecPojo
	 * @author cvitter
	 */
	public static class DocGeoSpecPojo
	{
		private String lat = null;
		private String lon = null;
		private String city = null;
		private String stateProvince = null;
		private String country = null;
		private String countryCode = null;
		
		/**
		 * @param lat the lat to set
		 */
		public void setLat(String lat) {
			this.lat = lat;
		}
		/**
		 * @return the lat
		 */
		public String getLat() {
			return lat;
		}
		/**
		 * @param lon the lon to set
		 */
		public void setLon(String lon) {
			this.lon = lon;
		}
		/**
		 * @return the lon
		 */
		public String getLon() {
			return lon;
		}
		/**
		 * @param city the city to set
		 */
		public void setCity(String city) {
			this.city = city;
		}
		/**
		 * @return the city
		 */
		public String getCity() {
			return city;
		}
		/**
		 * @param state the state to set
		 */
		public void setStateProvince(String stateProvince) {
			this.stateProvince = stateProvince;
		}
		/**
		 * @return the state
		 */
		public String getStateProvince() {
			return stateProvince;
		}
		/**
		 * @param country the country to set
		 */
		public void setCountry(String country) {
			this.country = country;
		}
		/**
		 * @return the country
		 */
		public String getCountry() {
			return country;
		}
		/**
		 * @param countryCode the countryCode to set
		 */
		public void setCountryCode(String countryCode) {
			this.countryCode = countryCode;
		}
		/**
		 * @return the countryCode
		 */
		public String getCountryCode() {
			return countryCode;
		}
	}
	
	
	/**
	 * EntitySpecPojo
	 * Based on EntityPojo, used as a template by the StructuredAnalysisHarvester to
	 * create entities within a feed 
	 * @author cvitter
	 */
	public static class EntitySpecPojo
	{
		private String iterateOver = null;
		private String disambiguated_name = null;
		private String actual_name = null;
		private String dimension = null;
		private String type = null;
		private String relevance = null;
		private String frequency = null;
		private GeoSpecPojo geotag = null;
		private String ontology_type = null;
		private Boolean useDocGeo = false;
		private String creationCriteriaScript = null;
		
		// One entity can be broken out into one or more entities use 
		// an embedded list of EntitySpecPojos
		private List<EntitySpecPojo> entities = null;
				
		/**
		 * @param iterateOver the iterateOver to set
		 */
		public void setIterateOver(String iterateOver) {
			this.iterateOver = iterateOver;
		}
		/**
		 * @return the iterateOver
		 */
		public String getIterateOver() {
			return iterateOver;
		}
		
		/**
		 * @param actual_name the actual_name to set
		 */
		public void setActual_name(String actual_name) {
			this.actual_name = actual_name;
		}
		/**
		 * @return the actual_name
		 */
		public String getActual_name() {
			return actual_name;
		}
		
		/**
		 * @param disambiguated_name the disambiguous_name to set
		 */
		public void setDisambiguated_name(String disambiguated_name) {
			this.disambiguated_name = disambiguated_name;
		}
		/**
		 * @return the disambiguous_name
		 */
		public String getDisambiguated_name() {
			return disambiguated_name;
		}
		
		/**
		 * @param dimension the dimension to set
		 */
		public void setDimension(String dimension) {
			this.dimension = dimension;
		}
		/**
		 * @return the dimension
		 */
		public String getDimension() {
			return dimension;
		}

		/**
		 * @param type the type to set
		 */
		public void setType(String type) {
			this.type = type;
		}
		/**
		 * @return the type
		 */
		public String getType() {
			return type;
		}
		
		/**
		 * @param relevance the relevance to set
		 */
		public void setRelevance(String relevance) {
			this.relevance = relevance;
		}
		/**
		 * @return the relevance
		 */
		public String getRelevance() {
			return relevance;
		}
		/**
		 * @param frequency the frequency to set
		 */
		public void setFrequency(String frequency) {
			this.frequency = frequency;
		}
		/**
		 * @return the frequency
		 */
		public String getFrequency() {
			return frequency;
		}
		/**
		 * @param geotag the geotag to set
		 */
		public void setGeotag(GeoSpecPojo geotag) {
			this.geotag = geotag;
		}
		/**
		 * @return the geotag
		 */
		public GeoSpecPojo getGeotag() {
			return geotag;
		}
		/**
		 * @param useDocGeo the useDocGeo to set
		 */
		public void setUseDocGeo(Boolean useDocGeo) {
			this.useDocGeo = useDocGeo;
		}
		/**
		 * @return the useDocGeo
		 */
		public Boolean getUseDocGeo() {
			return useDocGeo;
		}

		/**
		 * @param creationCriteriaScript the creationCriteriaScript to set
		 */
		public void setCreationCriteriaScript(String creationCriteriaScript) {
			this.creationCriteriaScript = creationCriteriaScript;
		}
		/**
		 * @return the creationCriteriaScript
		 */
		public String getCreationCriteriaScript() {
			return creationCriteriaScript;
		}
		/**
		 * @param entities the entities to set
		 */
		public void setEntities(List<EntitySpecPojo> entities) {
			this.entities = entities;
		}
		/**
		 * @return the entities
		 */
		public List<EntitySpecPojo> getEntities() {
			return entities;
		}
		/**
		 * @param ontological_type the ontological_type to set
		 */
		public void setOntology_type(String ontology_type) {
			this.ontology_type = ontology_type;
		}
		/**
		 * @return the ontological_type
		 */
		public String getOntology_type() {
			return ontology_type;
		}
	}
	
	
	/**
	 * AssociationSpecPojo 
	 * Based on AssociationPojo, used as a template by the StructuredAnalysisHarvester to
	 * create associations within a document 
	 * @author cvitter
	 */
	public static class AssociationSpecPojo
	{
		private String iterateOver = null;
		private String entity1 = null;
		private String entity1_index = null;
		private String entity2 = null;
		private String entity2_index = null;
		private String verb = null;
		private String verb_category = null;
		private String assoc_type = null; // (can override default in 3 ways, event/fact->summary or event<->fact)
		private String time_start = null; // (Standardize: ISO yyyy-MM-dd['T'HH:mm:ss], or "(<Free-text>)")
		private String time_end = null; // (Standardize: ISO yyyy-MM-dd['T'HH:mm:ss], or "(<Free-text>)")
		private String geo_index = null;
		private GeoSpecPojo geotag = null;
		private String creationCriteriaScript = null;
		
		// One association can be broken out into one or more associations using 
		// an embedded list of AssociationSpecPojos
		private List<AssociationSpecPojo> associations = null; 

		/**
		 * @param iterateOver the iterateOver to set
		 */
		public void setIterateOver(String iterateOver) {
			this.iterateOver = iterateOver;
		}
		/**
		 * @return the iterateOver
		 */
		public String getIterateOver() {
			return iterateOver;
		}		

		/**
		 * @param entity1 the entity1 to set
		 */
		public void setEntity1(String entity1) {
			this.entity1 = entity1;
		}
		/**
		 * @return the entity1
		 */
		public String getEntity1() {
			return entity1;
		}
		/**
		 * @param entity1_index the entity1_index to set
		 */
		public void setEntity1_index(String entity1_index) {
			this.entity1_index = entity1_index;
		}
		/**
		 * @return the entity1_index
		 */
		public String getEntity1_index() {
			return entity1_index;
		}
		/**
		 * @param entity2 the entity2 to set
		 */
		public void setEntity2(String entity2) {
			this.entity2 = entity2;
		}
		/**
		 * @return the entity2
		 */
		public String getEntity2() {
			return entity2;
		}
		/**
		 * @param entity2_index the entity2_index to set
		 */
		public void setEntity2_index(String entity2_index) {
			this.entity2_index = entity2_index;
		}
		/**
		 * @return the entity2_index
		 */
		public String getEntity2_index() {
			return entity2_index;
		}
		/**
		 * @param verb the verb to set
		 */
		public void setVerb(String verb) {
			this.verb = verb;
		}
		/**
		 * @return the verb
		 */
		public String getVerb() {
			return verb;
		}
		/**
		 * @param verb_category the verb_category to set
		 */
		public void setVerb_category(String verb_category) {
			this.verb_category = verb_category;
		}
		/**
		 * @return the verb_category
		 */
		public String getVerb_category() {
			return verb_category;
		}
		/**
		 * @param assoc_type the assoc_type to set
		 */
		public void setAssoc_type(String assoc_type) {
			this.assoc_type = assoc_type;
		}
		/**
		 * @return the assoc_type
		 */
		public String getAssoc_type() {
			return assoc_type;
		}
		/**
		 * @param time_start the time_start to set
		 */
		public void setTime_start(String time_start) {
			this.time_start = time_start;
		}
		/**
		 * @return the time_start
		 */
		public String getTime_start() {
			return time_start;
		}
		/**
		 * @param time_end the time_end to set
		 */
		public void setTime_end(String time_end) {
			this.time_end = time_end;
		}
		/**
		 * @return the time_end
		 */
		public String getTime_end() {
			return time_end;
		}
		/**
		 * @param geo_index the geo_index to set
		 */
		public void setGeo_index(String geo_index) {
			this.geo_index = geo_index;
		}
		/**
		 * @return the geo_index
		 */
		public String getGeo_index() {
			return geo_index;
		}
		/**
		 * @param geotag the geotag to set
		 */
		public void setGeotag(GeoSpecPojo geotag) {
			this.geotag = geotag;
		}

		/**
		 * @return the geotag
		 */
		public GeoSpecPojo getGeotag() {
			return geotag;
		}
		/**
		 * @param creationCriteriaScript the creationCriteriaScript to set
		 */
		public void setCreationCriteriaScript(String creationCriteriaScript) {
			this.creationCriteriaScript = creationCriteriaScript;
		}

		/**
		 * @return the creationCriteriaScript
		 */
		public String getCreationCriteriaScript() {
			return creationCriteriaScript;
		}

		/**
		 * @param associations the associations to set
		 */
		public void setAssociations(List<AssociationSpecPojo> associations) {
			this.associations = associations;
		}
		/**
		 * @return the associations
		 */
		public List<AssociationSpecPojo> getAssociations() {
			return associations;
		}
	}
	
	
	/**
	 * GeoSpecPojo
	 * Based on GeoPojo, used as a template by the StructuredAnalysisHarvester to
	 * create geoTags within entities and/or associations in a document 
	 * @author cvitter
	 */
	public static class GeoSpecPojo
	{
		private String lat = null;
		private String lon = null;
		
		/**
		 * @param latitude the latitude to set
		 */
		public void setLat(String latitude) 
		{
			this.lat = latitude;
		}
		/**
		 * @return the latitude
		 */
		public String getLat() {
			return lat;
		}
		
		/**
		 * @param longitude the longitude to set
		 */
		public void setLon(String longitude) 
		{
			this.lon = longitude;
		}
		/**
		 * @return the longitude
		 */
		public String getLon() {
			return lon;
		}		
		
	}

}
