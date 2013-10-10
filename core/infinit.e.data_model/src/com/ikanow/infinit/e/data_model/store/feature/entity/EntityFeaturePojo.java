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
package com.ikanow.infinit.e.data_model.store.feature.entity;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;

import com.google.gson.reflect.TypeToken;
import com.ikanow.infinit.e.data_model.store.BaseDbPojo;
import com.ikanow.infinit.e.data_model.store.document.EntityPojo.Dimension;
import com.ikanow.infinit.e.data_model.store.document.GeoPojo;

public class EntityFeaturePojo  extends BaseDbPojo
{
	// Standard static function for readability
	@SuppressWarnings("unchecked")
	static public TypeToken<List<EntityFeaturePojo>> listType() { return new TypeToken<List<EntityFeaturePojo>>(){}; }
	
	// NOTE DON'T CHANGE THIS STRUCTURE WITHOUT ALSO CHANGING EntityFeaturePojoIndexMap
	
	private ObjectId _id = null;
	final public static String _id_ = "_id";
	private String disambiguated_name = null;
	final public static String disambiguated_name_ = "disambiguated_name";
	private String type = null;
	final public static String type_ = "type";
	private Dimension dimension = null;
	final public static String dimension_ = "dimension";
	private Set<String> alias = null;
	final public static String alias_ = "alias";
	private Long totalfreq = 0L;
	final public static String totalfreq_ = "totalfreq";
	private Long doccount = 0L;
	final public static String doccount_ = "doccount";
	private String index = null; 
	final public static String index_ = "index";
	private String db_sync_time = null;
	final public static String db_sync_time_ = "db_sync_time";
	private Long db_sync_doccount = 0L;
	final public static String db_sync_doccount_ = "db_sync_doccount";
	private Double db_sync_prio = null; // Sparse field that indicates when an update is needed, and how badly
	final public static String db_sync_prio_ = "db_sync_prio";	
	private ObjectId communityId = null; // (note will be Set<String> as far as the API is concerned, once this object is available)
	final public static String communityId_ = "communityId";
	private GeoPojo geotag = null;
	final public static String geotag_ = "geotag";
	private String ontology_type = null;
	final public static String ontology_type_ = "ontology_type";
	private Set<String> linkdata = null; // Set of semantic references
	final public static String linkdata_ = "linkdata";
	
	public void setDisambiguatedName(String dname) {
		this.disambiguated_name = dname;
		if (null != this.type) {
			this.index = new StringBuffer(dname).append('/').append(this.type).toString().toLowerCase();
		}
	}
	public String getDisambiguatedName() {
		return disambiguated_name;
	}
	public void setIndex(String index) {
		this.index = index;
	}
	public String getIndex() {
		return index;
	}
	public void setType(String type) {
		this.type = type;
		if (null != this.disambiguated_name) {
			this.index = new StringBuffer(disambiguated_name).append('/').append(this.type).toString().toLowerCase();
		}
	}
	public String getType() {
		return type;
	}
	public void setDimension(Dimension dimension) {
		this.dimension = dimension;
	}
	public Dimension getDimension() {
		return dimension;
	}
	public void addAlias(String alias) 
	{
		if ( this.alias == null )
			this.alias = new HashSet<String>();
		this.alias.add(alias);
	}
	public Set<String> getAlias() {
		return alias;
	}
	public void setCommunityId(ObjectId communityId) 
	{
		this.communityId = communityId;
	}
	public ObjectId getCommunityId() {
		return communityId;
	}
	public void setTotalfreq(long totalfreq) {
		this.totalfreq = totalfreq;
	}
	public void setTotalfreq(Long totalfreq) {
		this.totalfreq = totalfreq;
	}
	public long getTotalfreq() {
		return totalfreq;
	}
	public void setDoccount(long doccount) {
		this.doccount = doccount;
	}
	public void setDoccount(Long doccount) {
		this.doccount = doccount;
	}
	public long getDoccount() {
		return this.doccount;
	}
	public long getDbSyncDoccount() {
		return this.db_sync_doccount;
	}
	public void setDbSyncDoccount(Long db_sync_doccount) {
		this.db_sync_doccount = db_sync_doccount;
	}
	public String getDbSyncTime() {
		return this.db_sync_time;
	}
	public void setDbSyncTime(String db_sync_time) {
		this.db_sync_time = db_sync_time;
	}
	public void addAllAlias(Set<String> alias2) {
		this.alias = new HashSet<String>();
		this.alias.addAll(alias2);		
	}
	public void set_id(ObjectId _id) {
		this._id = _id;
	}
	public ObjectId get_id() {
		return _id;
	}
	public void setGeotag(GeoPojo geotag) {
		this.geotag = geotag;
	}
	public GeoPojo getGeotag() {
		return geotag;
	}
	public Set<String> getSemanticLinks() {
		return linkdata;
	}
	public void addToSemanticLinks(Collection<String> linkdata) {
		if (null == this.linkdata) {
			this.linkdata = new HashSet<String>();
		}
		this.linkdata.addAll(linkdata);
	}
	public void setOntology_type(String ontology_type) {
		this.ontology_type = ontology_type;
	}
	public String getOntology_type() {
		return ontology_type;
	}
	public void setDb_sync_prio(Double db_sync_prio) {
		this.db_sync_prio = db_sync_prio;
	}
	public Double getDb_sync_prio() {
		return db_sync_prio;
	}
}
