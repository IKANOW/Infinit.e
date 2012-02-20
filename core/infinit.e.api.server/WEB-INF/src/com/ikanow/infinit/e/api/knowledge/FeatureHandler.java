package com.ikanow.infinit.e.api.knowledge;

import com.ikanow.infinit.e.data_model.api.ResponsePojo;

public class FeatureHandler {

	public ResponsePojo suggestAlias(String entity, String updateItem, String cookieLookup) 
	{
		ResponsePojo rp = new ResponsePojo();
		return rp;
	}
	
	/**
	 * Cases:
	 * Case1: Both ent and alias exist in gaz (same or diff entries)
	 * Case2: 1 or the other exist in gaz
	 * Case3: Neither exist in gaz already
	 * 
	 * @param updateItem
	 * @return
	 */
	public ResponsePojo approveAlias(String updateItem) 
	{
		ResponsePojo rp = new ResponsePojo();
		return rp;
	}
	
	public ResponsePojo declineAlias(String updateItem) 
	{
		ResponsePojo rp = new ResponsePojo();
		return rp;
	}
	
	public ResponsePojo allAlias(String cookieLookup) 
	{
		ResponsePojo rp = new ResponsePojo();
		return rp;
	}		
	
	public ResponsePojo getEntityFeature(String updateItem) 
	{
		ResponsePojo rp = new ResponsePojo();
		return rp;
	}		
}
