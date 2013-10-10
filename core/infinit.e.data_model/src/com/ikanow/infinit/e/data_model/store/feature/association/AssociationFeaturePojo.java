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
package com.ikanow.infinit.e.data_model.store.feature.association;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;

import com.google.gson.reflect.TypeToken;
import com.ikanow.infinit.e.data_model.store.BaseDbPojo;

public class AssociationFeaturePojo extends BaseDbPojo
{
	// Standard static function for readability
	@SuppressWarnings("unchecked")
	static public TypeToken<List<AssociationFeaturePojo>> listType() { return new TypeToken<List<AssociationFeaturePojo>>(){}; }

	// Misc access constants:
	final public static int entity_MAXSIZE = 256; // (in feature/index restrict quotation lengths)
	final public static int entity_MAXFIELDS = 512; // (in feature/index restrict number of quotations)

	private String index; 
	final public static String index_ = "index";
	private String entity1_index;
	final public static String entity1_index_ = "entity1_index";
	private Set<String> entity1;
	final public static String entity1_ = "entity1";
	private String entity2_index;
	final public static String entity2_index_ = "entity2_index";
	private Set<String> entity2;
	final public static String entity2_ = "entity2";
	private String verb_category;
	final public static String verb_category_ = "verb_category";
	private Set<String> verb;
	final public static String verb_ = "verb";
	private String geo_index;
	final public static String geotag_ = "geotag";
	private String assoc_type;
	final public static String assoc_type_ = "assoc_type";

	private Long doccount = 0L;
	final public static String doccount_ = "doccount";
	private String db_sync_time = null;
	final public static String db_sync_time_ = "db_sync_time";
	private Long db_sync_doccount = 0L;	
	final public static String db_sync_doccount_ = "db_sync_doccount";
	private Double db_sync_prio = null; // Sparse field that indicates when an update is needed, and how badly
	final public static String db_sync_prio_ = "db_sync_prio";	
	private ObjectId communityId = null; 
	final public static String communityId_ = "communityId";
		// (note will be Set<String> as far as the API is concerned, once this object is available)	
	
	public ObjectId getCommunityId() {
		return communityId;
	}
	public void setCommunityId(ObjectId communityId) {
		this.communityId = communityId;
	}
	public void setEntity1(Set<String> entity1) {
		this.entity1 = entity1;
	}
	public void addEntity1(String ent1)
	{
		if ( this.entity1 == null )
			this.entity1 = new HashSet<String>();
		this.entity1.add(ent1);
	}
	public Set<String> getEntity1() {
		return this.entity1;
	}
	public void setEntity2(Set<String> entity2) {
		this.entity2 = entity2;
	}
	public void addEntity2(String ent2)
	{
		if ( this.entity2 == null )
			this.entity2 = new HashSet<String>();
		this.entity2.add(ent2);
	}
	public Set<String> getEntity2() {
		return this.entity2;
	}
	public void addVerb(String verb)
	{
		if ( this.verb == null )
			this.verb = new HashSet<String>();
		this.verb.add(verb);
	}
	public Set<String> getVerb() {
		return this.verb;
	}
	public void setIndex(String index) {
		this.index = index;
	}
	public String getIndex() {
		return index;
	}
	public void setEntity1_index(String entity1_index) {
		this.entity1_index = entity1_index;
	}
	public String getEntity1_index() {
		return entity1_index;
	}
	public void setEntity2_index(String entity2_index) {
		this.entity2_index = entity2_index;
	}
	public String getEntity2_index() {
		return entity2_index;
	}
	public void setVerb_category(String verb_category) {
		this.verb_category = verb_category;
	}
	public String getVerb_category() {
		return verb_category;
	}
	public String getGeo_index() {
		return geo_index;
	}
	public void setGeo_index(String geo_index) {
		this.geo_index = geo_index;
	}
	public void setDoccount(Long doccount) {
		this.doccount = doccount;
	}
	public Long getDoccount() {
		return doccount;
	}
	public void setDb_sync_time(String db_sync_time) {
		this.db_sync_time = db_sync_time;
	}
	public String getDb_sync_time() {
		return db_sync_time;
	}
	public void setDb_sync_doccount(Long db_sync_doccount) {
		this.db_sync_doccount = db_sync_doccount;
	}
	public Long getDb_sync_doccount() {
		return db_sync_doccount;
	}	
	public String getAssociation_type() {
		return assoc_type;
	}
	public void setAssociation_type(String assoc_type) {
		this.assoc_type = assoc_type;
	}
	public void setDb_sync_prio(Double db_sync_prio) {
		this.db_sync_prio = db_sync_prio;
	}
	public Double getDb_sync_prio() {
		return db_sync_prio;
	}
}
