/*******************************************************************************
 * Copyright 2012 The Infinit.e Open Source Project.
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
package com.ikanow.infinit.e.data_model.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class GeoOntologyMapping {
	///////////////////////////////////////////////////////////////////////////////////////
	
	// Currently supported ontology types/mappings from known types
	
	private static final Pattern PATTERN_CONTINENT = Pattern.compile("continent", Pattern.CASE_INSENSITIVE);
	private static final Pattern PATTERN_COUNTRY = Pattern.compile("country", Pattern.CASE_INSENSITIVE);
	private static final Pattern PATTERN_COUNTRYSUBSIDIARY = Pattern.compile("provinceorstate|stateorcountry", Pattern.CASE_INSENSITIVE);
	private static final Pattern PATTERN_CITY = Pattern.compile("city", Pattern.CASE_INSENSITIVE);
	private static final Pattern PATTERN_GEOGRAPHICALREGION = Pattern.compile("naturalfeature|region|geographicalfeature", Pattern.CASE_INSENSITIVE);		
	
	private static Map<String,List<String>> ontMap = null;
	/**
	 * A temporary method that attempts to map a few known extraction types
	 * to a small subset of the opencyc ontology to get started with our ontology
	 * 
	 * @param docs Docs to update the entities on
	 */
	static public String mapEntityToOntology(String type)
	{		
		//attempt to map type to our small ontology subset
		if ( null == type )
			return ("point");		
		else if ( PATTERN_CONTINENT.matcher(type).find() )
			return ("continent");
		else if ( PATTERN_COUNTRY.matcher(type).find() )
			return ("country");
		else if ( PATTERN_COUNTRYSUBSIDIARY.matcher(type).find() )
			return ("countrysubsidiary");
		else if ( PATTERN_CITY.matcher(type).find() )
			return ("city");
		else if ( PATTERN_GEOGRAPHICALREGION.matcher(type).find() )
			return ("geographicalregion");
		else
			return ("point");
	}
	
	///////////////////////////////////////////////////////////////////////////////////////

	// Performance optimization for geo decay scoring
	
	private static String[] currentOntologyList = {"geographicalregion","continent","country","countrysubsidiary","city","point"}; //largest to smallest
	
	// Maps from one of our OpenCyc approved ontology types to a single char for performance in ES faceting
	public static char encodeOntologyCode(String ontology)
	{
		if (null == ontology) {
			return 'p';
		}
		else if (ontology.equalsIgnoreCase("continent")) {
			return 'C';
		}
		else if (ontology.equalsIgnoreCase("country")) {
			return 'c';
		}
		else if (ontology.equalsIgnoreCase("countrysubsidiary")) {
			return 'a'; // (administrative subdivision, meh)
		}
		else if (ontology.equalsIgnoreCase("city")) {
			return 'u'; // (for urban, meh)
		}
		else if (ontology.equalsIgnoreCase("geographicalregion")) {
			return 'g';
		}
		return 'p';
	}
	
	/**
	 * Converts a character code back into one of our OpenCyc ontology types
	 * Currently we accept the following:
	 * 
	 * p = point
	 * C = continent
	 * c = country;
	 * a = countrysubsidiary
	 * u = city;
	 * g = geographicalregion
	 * all others = point
	 * 
	 * Defaults to point if unknown (chars can't be null)
	 * 
	 * @param ontologycode character to convert into ontology type
	 * @return a string version of the ontology type, defaults to point
	 */
	public static String decodeOntologyCode(char ontologycode)
	{
		if ( ontologycode == 'p' )
			return "point";
		else if ( ontologycode == 'C' )
			return "continent";
		else if ( ontologycode == 'c' )
			return "country";
		else if ( ontologycode == 'a' )
			return "countrysubsidiary";
		else if ( ontologycode == 'u' )
			return "city";
		else if ( ontologycode == 'g' )
			return "geographicalregion";
		else 
			return "point";
	}
	
	
	
	//note in the future we talked about loading ont order via mongo and having a static
	//version that sat in the api and was updated every hour or so
	//this can be achieved be replacing what currentOntologyList holds and this code below should work just fine
	//the above code for decoding/encoding would have to change tho
	/**
	 * Returns all current ontology types.
	 * 
	 * @return List of ontology types.
	 */
	public static List<String> getOntologyList()
	{
		return getOntologyList(null);
	}
	
	/**
	 * Returns all current ontology types at or below the given startOntology level.
	 * 
	 * @param startOntology the ontology type to start at and only return ont types below this. 
	 * If null returns all ontology types.
	 * 
	 * @return all ontology types below startOntology, all if startOntology is null
	 */
	public static List<String> getOntologyList(String startOntology)
	{
		if ( ontMap == null )
			initOntMap();
		if ( startOntology == null )
			startOntology = currentOntologyList[0];
		return ontMap.get(startOntology.toLowerCase());		
	}
	
	/**
	 * Builds a map for every item in the ontology list
	 * This is an optomization so when we do a lookup
	 * we can just call ontMap.get(ontname) and already
	 * have the list built out. See getOntologyList
	 * 
	 */
	public static void initOntMap()
	{
		ontMap = new HashMap<String, List<String>>();
		for (int i = 0; i < currentOntologyList.length; i++ )
		{
			List<String> ontList = new ArrayList<String>();
			for ( int j = i; j < currentOntologyList.length; j++ )
			{
				ontList.add(currentOntologyList[j]);
			}
			ontMap.put(currentOntologyList[i], ontList);
		}
	}
	
}
