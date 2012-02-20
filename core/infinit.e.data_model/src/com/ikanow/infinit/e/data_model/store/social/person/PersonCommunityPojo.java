package com.ikanow.infinit.e.data_model.store.social.person;

import org.bson.types.ObjectId;

import com.ikanow.infinit.e.data_model.store.BaseDbPojo;

public class PersonCommunityPojo extends BaseDbPojo 
{
	private ObjectId _id = null;
	private String name = null;
	
	public PersonCommunityPojo()
	{
		
	}
	
	public PersonCommunityPojo(ObjectId id, String _name)
	{
		_id = id;
		name = _name;
	}
	
	/**
	 * @param _id the _id to set
	 */
	public void set_id(ObjectId _id) {
		this._id = _id;
	}
	/**
	 * @return the _id
	 */
	public ObjectId get_id() {
		return _id;
	}
	
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}	
	
}
