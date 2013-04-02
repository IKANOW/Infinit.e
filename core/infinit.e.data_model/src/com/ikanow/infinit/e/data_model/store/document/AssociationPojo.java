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

import com.google.gson.reflect.TypeToken;
import com.ikanow.infinit.e.data_model.store.BaseDbPojo;
import com.ikanow.infinit.e.data_model.utils.ContentUtils;

// This is for events that live in the document (FeedPojo), not the "standalone" EventGazateerPojo

public class AssociationPojo extends BaseDbPojo
{
	// Standard static function for readability
	@SuppressWarnings("unchecked")
	static public TypeToken<List<AssociationPojo>> listType() { return new TypeToken<List<AssociationPojo>>(){}; }
	
	//*** IMPORTANT: don't add to this list without considering the ES mappings in EventPojoChildIndexMap and EventPojoIndexMap
	
	private String entity1 = null;
	final public static String entity1_ = "entity1";
	private String entity1_index = null;
	final public static String entity1_index_ = "entity1_index";
	private String verb = null;
	final public static String verb_ = "verb";
	private String verb_category = null;
	final public static String verb_category_ = "verb_category";
	private String entity2 = null;
	final public static String entity2_ = "entity2";
	private String entity2_index = null;
	final public static String entity2_index_ = "entity2_index";
	private String time_start = null; // (Standardize: ISO yyyy-MM-dd['T'HH:mm:ss], or "(freeform text)")
	final public static String time_start_ = "time_start";
	private String time_end = null; // (Standardize: ISO yyyy-MM-dd['T'HH:mm:ss], or "(freeform text)")
	final public static String time_end_ = "time_end";
	private GeoPojo geotag = null;
	final public static String geotag_ = "geotag";
	private String geo_index = null;
	final public static String geo_index_ = "geo_index";
	private String assoc_type = null;
	final public static String assoc_type_ = "assoc_type";
	private Double sentiment = null; // directed sentiment from ent1 to ent2
	final public static String sentiment_ = "sentiment";
	
	private String assoc_index = null; // (for counting on in facets)	
	final public static String assoc_index_ = "assoc_index";
	
	// The following fields are calculated on a per query basis but are not currently ever stored:
	// (in fact these fields are currently only generated for standalone events or aggregated events, not document children):	
	private Long doccount;
	final public static String doccount_ = "doccount";
	private Double assoc_sig;
	final public static String assoc_sig_ = "assoc_sig";
	// (these fields are currently only generates for aggregated events):
	private Double entity1_sig; // (query significance of entity1, if it's indexed)
	final public static String entity1_sig_ = "entity1_sig";
	private Double entity2_sig; // (query significance of entity2, if it's indexed)
	final public static String entity2_sig_ = "entity2_sig";
	private Double geo_sig; // (query significance of geo_index, if it exists)
	final public static String geo_sig_ = "geo_sig";
		
	// (Needed for some internal calculations)
	private transient String index = null;
	public String getIndex() { return index; }
	public void setIndex(String eventIndex) { index = eventIndex; }
	
	// Gets and Sets
	
	public String getEntity1() {
		return entity1;
	}
	public void setEntity1(String entity1) {
		this.entity1 = entity1;
	}
	public String getEntity1_index() {
		return entity1_index;
	}
	public void setEntity1_index(String entity1_index) {
		this.entity1_index = ContentUtils.stripDiacritics(entity1_index);
	}
	public String getVerb() {
		return verb;
	}
	public void setVerb(String verb) {
		this.verb = verb;
	}
	public String getVerb_category() {
		return verb_category;
	}
	public void setVerb_category(String verb_category) {
		this.verb_category = verb_category;
	}
	public String getEntity2() {
		return entity2;
	}
	public void setEntity2(String entity2) {
		this.entity2 = entity2;
	}
	public String getEntity2_index() {
		return entity2_index;
	}
	public void setEntity2_index(String entity2_index) {
		this.entity2_index = ContentUtils.stripDiacritics(entity2_index);
	}
	public String getTime_start() {
		return time_start;
	}
	public void setTime_start(String time_start) {
		this.time_start = time_start;
	}
	public String getTime_end() {
		return time_end;
	}
	public void setTime_end(String time_end) {
		this.time_end = time_end;
	}
	public GeoPojo getGeotag() {
		return geotag;
	}
	public void setGeotag(GeoPojo geotag) {
		this.geotag = GeoPojo.cleanseBadGeotag(geotag);
	}
	public String getGeo_index() {
		return geo_index;
	}
	public void setGeo_index(String geo_index) {
		this.geo_index = ContentUtils.stripDiacritics(geo_index);
	}
	public String getAssociation_type() {
		return assoc_type;
	}
	public void setAssociation_type(String assoc_type) {
		this.assoc_type = assoc_type;
	}
	public String getAssociation_index() {
		return assoc_index;
	}
	public void setAssociation_index(String assoc_index) {
		this.assoc_index = assoc_index;
	}
	// The following fields are calculated on a per query basis but are not currently ever stored:
	// Aggregated/standalone events only
	public Double getAssoc_sig() {
		return assoc_sig;
	}
	public void setAssoc_sig(Double assoc_sig) {
		this.assoc_sig = assoc_sig;
	}
	public Long getDoccount() {
		return doccount;
	}
	public void setDoccount(Long doccount) {
		this.doccount = doccount;
	}
	// Aggregated events only
	public Double getEntity1_sig() {
		return entity1_sig;
	}
	public void setEntity1_sig(Double entity1_sig) {
		this.entity1_sig = entity1_sig;
	}
	public Double getEntity2_sig() {
		return entity2_sig;
	}
	public void setEntity2_sig(Double entity2_sig) {
		this.entity2_sig = entity2_sig;
	}
	public Double getGeo_sig() {
		return geo_sig;
	}
	public void setGeo_sig(Double geo_sig) {
		this.geo_sig = geo_sig;
	}
	public void setSentiment(Double sentiment) {
		this.sentiment = sentiment;
	}
	public Double getSentiment() {
		return sentiment;
	}

}
