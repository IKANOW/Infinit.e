/*******************************************************************************
 * Copyright 2015, The Infinit.e Open Source Project.
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
package com.ikanow.infinit.e.harvest.enrichment.custom;

import org.json.JSONArray;
import org.json.JSONObject;

public class JsonExtractor {
	/**
	 * getValueFromJsonField
	 * Takes string in the form of: node1.node2.fieldName and returns
	 * the value contained in the JSON for that field as an Object
	 * Note: supports leading $s in the field name
	 * @param fieldLocation
	 * @return
	 */
	public Object getValueFromJsonField(String fieldLocation,JSONObject iterator,JSONObject document) 
	{
		try
		{
			// Strip out $ chars if present and then split on '.' 
			// to get the JSON node hierarchy and field name
			String[] field = fieldLocation.replace("$", "").split("\\.");

			StringBuffer node = new StringBuffer();
			// JSON node = all strings in field[] (except for the last string in the array)
			// concatenated together with the '.' char
			if (field.length > 1)
			{
				for ( int i = 0; i < field.length - 1; i++ ) 
				{
					if (node.length() > 0) node.append(".");
					node.append(field[i]);
				}
			}

			// The field name is the final value in the array
			String fieldName = field[field.length - 1];
			return getValueFromJson(node.toString(), fieldName,iterator,document);
		}
		catch (Exception e)
		{
			// This can happen as part of normal logic flow
			//logger.error("getValueFromJsonField Exception: " + e.getMessage());
			return null;
		}
	}
	
	
	
	
	/**
	 * getValueFromJson(String node, String field)
	 * Attempts to retrieve a value from the node/field and return
	 * and object containing the value to be converted by calling method
	 * @param node
	 * @param field
	 * @return Object o
	 */
	public Object getValueFromJson(String node, String field,JSONObject iterator,JSONObject document)
	{
		//JSONObject json = (_iterator != null) ? _iterator : _document;		
		JSONObject json = (iterator != null) ? iterator : document;		
		Object o = null;
		try
		{
			if (node.length() > 1)
			{
				// (removed the [] case, you'll need to do that with scripts unless you want [0] for every field)
				
				// Mostly standard case $metadata(.object).+field
				if (node.indexOf('.') > -1) {
					String node_fields[] = node.split("\\.");
					JSONObject jo = json; 
					for (String f: node_fields) {
						Object testJo = jo.get(f); 
						if (testJo instanceof JSONArray) {
							jo = ((JSONArray)testJo).getJSONObject(0);
						}
						else {
							jo = (JSONObject)testJo;
						}
					}
					Object testJo = jo.get(field); 
					if (testJo instanceof JSONArray) {
						o = ((JSONArray)testJo).getString(0);
					}
					else {
						o = testJo;
					}
				}
				// Standard case - $metadata.field
				else
				{
					JSONObject jo = json.getJSONObject(node);
					Object testJo = jo.get(field);
					if (testJo instanceof JSONArray)
					{
						o = ((JSONArray)testJo).getString(0);
					}
					else
					{
						o = testJo;
					}
				}
			}
			else
			{
				Object testJo = json.get(field);
				if (testJo instanceof JSONArray)
				{
					o = ((JSONArray)testJo).getString(0);
				}
				else
				{
					o = testJo;
				}
			}
		}
		catch (Exception e) 
		{
			// This can happen as part of normal logic flow
			//logger.error("getValueFromJson Exception: " + e.getMessage());
			return null;
		}
		return o;
	}


}
