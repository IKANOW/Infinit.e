package com.ikanow.infinit.e.data_model.store.social.cookies;

import java.util.Date;
import java.util.List;

import org.bson.types.ObjectId;

import com.google.gson.reflect.TypeToken;
import com.ikanow.infinit.e.data_model.store.BaseDbPojo;

public class CookiePojo extends BaseDbPojo
{
	// Standard static function for readability
	@SuppressWarnings("unchecked")
	static public TypeToken<List<CookiePojo>> listType() { return new TypeToken<List<CookiePojo>>(){}; }
	
	private ObjectId _id = null;
	private ObjectId profileId = null;
	private ObjectId cookieId = null;
	private Date startDate = null;
	private Date lastActivity = null;
	
	public void set_id(ObjectId _id) {
		this._id = _id;
	}
	public ObjectId get_id() {
		return _id;
	}
	public void setProfileId(ObjectId profileId) {
		this.profileId = profileId;
	}
	public ObjectId getProfileId() {
		return profileId;
	}
	public void setCookieId(ObjectId cookieId) {
		this.cookieId = cookieId;
	}
	public ObjectId getCookieId() {
		return cookieId;
	}
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}
	public Date getStartDate() {
		return startDate;
	}
	public void setLastActivity(Date lastActivity) {
		this.lastActivity = lastActivity;
	}
	public Date getLastActivity() {
		if (null == lastActivity) {
			this.updateActivity();
		}
		return lastActivity;
	}
	public void updateActivity() {
		this.lastActivity = new Date();
	}
	
}
