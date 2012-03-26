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
package com.ikanow.infinit.e.data_model.store.social.gui;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;

import com.google.gson.reflect.TypeToken;
import com.ikanow.infinit.e.data_model.store.BaseDbPojo;


public class UIModulePojo extends BaseDbPojo
{
	// Standard static function for readability
	@SuppressWarnings("unchecked")
	static public TypeToken<List<UIModulePojo>> listType() { return new TypeToken<List<UIModulePojo>>(){}; }
	
	private ObjectId _id = null; // (optional, filled in by system if missing)
	private String swf = null; // (obsolete field, ignored by the system)
	private String url = null; // (mandatory)
	private String title = null; // (mandatory)
	private String description = null; // (mandatory)
	private Date created = null; // (user values ignored, filled in by the system)
	private Date modified = null; // (user values ignored, filled in by the system)
	private String version = null; // (mandatory)
	private String author = null; // (mandatory)
	private String imageurl = null; // (mandatory)
	private Boolean approved = null; // (optional, defaults to true)
	private String[] searchterms = null; // (optional)
	private Boolean debug = null; // (optional - debug objects won't appear in release versions of the UM GUI)
	private Set<ObjectId> communityIds = null; // (optional - if specified, restricts access to users belonging to one of the spec'd communities)
	
	public void set_id(ObjectId _id) {
		this._id = _id;
	}
	public ObjectId get_id() {
		return _id;
	}
	public void setSwf(String swf) {
		this.swf = swf;
	}
	public String getSwf() {
		return swf;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getUrl() {
		return url;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getTitle() {
		return title;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getDescription() {
		return description;
	}
	public void setCreated(Date created) {
		this.created = created;
	}
	public Date getCreated() {
		return created;
	}
	public void setModified(Date modified) {
		this.modified = modified;
	}
	public Date getModified() {
		return modified;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public String getVersion() {
		return version;
	}
	public void setAuthor(String author) {
		this.author = author;
	}
	public String getAuthor() {
		return author;
	}
	public void setApproved(boolean approved) {
		this.approved = approved;
	}
	public boolean isApproved() {
		return approved;
	}
	public void setImageurl(String imageurl) {
		this.imageurl = imageurl;
	}
	public String getImageurl() {
		return imageurl;
	}
	public void updateSearchField()
	{
		String searchString = title + " " + description + " " + author;
		//break searchString into token
		searchterms = searchString.split(" ");
		
	}
	public void setSearchterms(String[] searchterms) {
		this.searchterms = searchterms;
	}
	public String[] getSearchterms() {
		return searchterms;
	}
	public Boolean getDebug() {
		return debug;
	}
	public void setDebug(Boolean debug) {
		this.debug = debug;
	}
	public Set<ObjectId> getCommunityIds() {
		return communityIds;
	}
	public void setCommunityIds(Set<ObjectId> communityIds) {
		this.communityIds = communityIds;
	}
	public void addToCommunityIds(ObjectId communityId) {
		if (null == this.communityIds) {
			this.communityIds = new HashSet<ObjectId>();
		}
		this.communityIds.add(communityId);
	}
}
