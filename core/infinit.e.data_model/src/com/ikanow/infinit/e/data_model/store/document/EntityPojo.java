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
package com.ikanow.infinit.e.data_model.store.document;

import java.util.List;

import com.ikanow.infinit.e.data_model.utils.ContentUtils;

// This is for the entity in the document (FeedPojo), not the standalone GazateerPojo

public class EntityPojo 
{
	//*** IMPORTANT: don't add to this list without considering the ES mapping in EntityPojoIndexMap
	
	private String entid;
	final public static String entid_ = "entid";
	private String disambiguated_name;
	final public static String disambiguated_name_ = "disambiguated_name";
	final public static String docQuery_disambiguated_name_ = "entities.disambiguated_name";
	private String index;
	final public static String index_ = "index";
	final public static String docQuery_index_ = "entities.index";
	private String actual_name;
	final public static String actual_name_ = "actual_name";
	private String type;
	final public static String type_ = "type";
	private Double relevance;
	final public static String relevance_ = "relevance";
	private Long frequency; 
	final public static String frequency_ = "frequency";
	private Long totalfrequency = -1L;
	final public static String totalfrequency_ = "totalfrequency";
	final public static String docUpdate_totalfrequency_ = "entities.$.totalfrequency";
	private Long doccount = 0L;
	final public static String doccount_ = "doccount";
	final public static String docUpdate_doccount_ = "entities.$.doccount";
	private GeoPojo geotag;
	final public static String geotag_ = "geotag";
	public enum Dimension { What, When, Where, Who };
	private Dimension dimension; 
	final public static String dimension_ = "dimension";
	private List<String> linkdata;
	final public static String linkdata_ = "linkdata";
	private Double sentiment;
	final public static String sentiment_ = "sentiment";
	private String ontology_type = null; //for classifying using ontologies, currently only geo
	final public static String ontology_type_ = "ontology_type";
	
	// The following fields are calculated on a per query basis but are not currently ever stored:
	private Double significance; // (the document significance - not very interesting compared to...)
	final public static String significance_ = "significance";
	private Double datasetSignificance; // (the (query) dataset significance)
	final public static String datasetSignificance_ = "datasetSignificance";
	private Double queryCoverage; // (the % of documents containing this entity)
	final public static String queryCoverage_ = "queryCoverage";
	private Double averageFrequency; // (the average frequency across all documents)
	final public static String averageFreq_ = "averageFreq";
	private Double positiveSentiment; // (the sum of all the positive sentiment values)
	final public static String positiveSentiment_ = "positiveSentiment";
	private Double negativeSentiment; // (the sum of all the negative sentiment values)
	final public static String negativeSentiment_ = "negativeSentiment";
	private Long sentimentCount; // (the total number of documents in the (query) dataset containing a sentiment value)
	final public static String sentimentCount_ = "sentimentCount";
	
	// Get/Sets
	
	public String getEntid() {
		return entid;
	}
	public void setEntid(String entid) {
		this.entid = entid;
	}
	public String getDisambiguatedName() {
		return disambiguated_name;
	}
	public void setDisambiguatedName(String disambiguous_name) {
		
		this.disambiguated_name = ContentUtils.stripDiacritics(disambiguous_name);
		if (null != type) {
			this.index = new StringBuffer(disambiguous_name).append('/').append(type).toString().toLowerCase();
		}
	}
	public String getIndex() {
		return index;
	}
	public void setIndex(String index) {
		this.index = ContentUtils.stripDiacritics(index).toLowerCase();
	}
	public String getActual_name() {
		return actual_name;
	}
	public void setActual_name(String actual_name) {
		this.actual_name = actual_name;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
		if (null != disambiguated_name) {
			this.index = new StringBuffer(disambiguated_name).append('/').append(type).toString().toLowerCase();
		}
	}
	public Double getRelevance() {
		return relevance;
	}
	public void setRelevance(Double relevance) {
		this.relevance = relevance;
	}
	public Long getFrequency() {
		return frequency;
	}
	public void setFrequency(Long frequency) {
		this.frequency = frequency;
	}
	public Long getTotalfrequency() {
		return totalfrequency;
	}
	public void setTotalfrequency(Long totalfrequency) {
		this.totalfrequency = totalfrequency;
	}
	public Long getDoccount() {
		return doccount;
	}
	public void setDoccount(Long doccount) {
		this.doccount = doccount;
	}
	public GeoPojo getGeotag() {
		return geotag;
	}
	public void setGeotag(GeoPojo geotag) {
		this.geotag = GeoPojo.cleanseBadGeotag(geotag);
	}
	public Dimension getDimension() {
		return dimension;
	}
	public void setDimension(Dimension dimension) {
		this.dimension = dimension;
	}
	public List<String> getSemanticLinks() {
		return linkdata;
	}
	public void setSemanticLinks(List<String> linkdata) {
		this.linkdata = linkdata;
	}
	public Double getSentiment() {
		return sentiment;
	}
	public void setSentiment(Double sentiment) {
		if ((null != sentiment) && (Math.abs(sentiment) <= 1.0e7)) { 
			// (for some reason seeing the occasional corrupt sentiment from entity extractors, here's a handy central spot to sanity check)
			this.sentiment = sentiment;
		}
	}
	public void setOntology_type(String ontology_type) {
		this.ontology_type = ontology_type;
	}
	public String getOntology_type() {
		return ontology_type;
	}
	
	// Query (transient) fields
	
	public Double getSignificance() {
		return significance;
	}
	public void setSignificance(Double significance) {
		this.significance = significance;
	}
	public Double getDatasetSignificance() {
		return datasetSignificance;
	}
	public void setDatasetSignificance(Double datasetSignificance) {
		this.datasetSignificance = datasetSignificance;
	}
	public Double getQueryCoverage() {
		return queryCoverage;
	}
	public void setQueryCoverage(Double queryCoverage) {
		this.queryCoverage = queryCoverage;
	}
	public Double getAverageFrequency() {
		return averageFrequency;
	}
	public void setAverageFrequency(Double averageFrequency) {
		this.averageFrequency = averageFrequency;
	}
	public Double getPositiveSentiment() {
		return positiveSentiment;
	}
	public void setPositiveSentiment(Double positiveSentiment) {
		this.positiveSentiment = positiveSentiment;
	}
	public Double getNegativeSentiment() {
		return negativeSentiment;
	}
	public void setNegativeSentiment(Double negativeSentiment) {
		this.negativeSentiment = negativeSentiment;
	}
	public Long getSentimentCount() {
		return sentimentCount;
	}
	public void setSentimentCount(Long sentimentCount) {
		this.sentimentCount = sentimentCount;
	}
	
}
