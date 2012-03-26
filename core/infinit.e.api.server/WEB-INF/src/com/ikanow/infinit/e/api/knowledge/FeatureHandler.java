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
