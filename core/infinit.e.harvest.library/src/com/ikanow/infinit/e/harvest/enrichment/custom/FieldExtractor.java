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

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.log4j.Logger;

/**
 *  Utility class to extract fields within objects.
 * @author jfreydank
 *
 */
public class FieldExtractor {
	private static Pattern SUBSTITUTION_PATTERN = Pattern.compile("\\$([a-zA-Z._0-9]+)|\\$\\{([^}]+)\\}");

	private static final Logger logger = Logger.getLogger(FieldExtractor.class);

	/**
	 * extractValue
	 * Takes string in the form of: node1.node2.fieldName and returns
	 * @param fieldLocation
	 * @return value if nested field
	 */
	@SuppressWarnings("rawtypes")
	public static String extractValue(String fieldLocation,Object target) 
	{
		String retVal = null;
		try
		{
			if(fieldLocation!=null){
			// Strip out $ chars if present and then split on '.' 
			// to get the JSON node hierarchy and field name
			String[] field = fieldLocation.replace("$", "").split("\\.");
			//String fieldName = fieldLocation.replace("$", "");
			//retVal = BeanUtils.getNestedProperty(target,fieldName);
				Object attributeValue = target;
				if(attributeValue instanceof List){							
					attributeValue = ((List) attributeValue).get(0);	
				}else
				if(attributeValue instanceof Object[]){							
					attributeValue = ((Object[]) attributeValue)[0];	
				}
				for ( int i = 0; i < field.length ; i++ ) 
				{
					String fieldName = field[i];
					if(attributeValue!=null){
						if(attributeValue instanceof Map){
							attributeValue = ((Map) attributeValue).get(fieldName);							
						}else{
							Class<?> clazz = attributeValue.getClass();
							try {
								// this should work for regular objects having getters and setters, e.g. ChangeAwareDocumentPojo
								attributeValue = PropertyUtils.getSimpleProperty(attributeValue, fieldName);							
							} catch (Exception e) {
								// DEBUG ignore exception on purpose, comment in for debugging
								//logger.debug("extractValue caught exception getting value using PropertyUtils class:"+clazz+":"+fieldName,e);
								
								try {									
									Field reflectionField = clazz.getDeclaredField(fieldName);
									reflectionField.setAccessible(true);
									attributeValue = reflectionField.get(attributeValue);
								} catch (Exception e2) {
									logger.error("extractValue caught exception getting value using reflection on field:"+clazz+":"+fieldName,e2);
								} // e2 
							}// e
						}
						// post processing so we can extract the next 
						if(attributeValue instanceof List){							
							attributeValue = ((List) attributeValue).get(0);	
						}else
						if(attributeValue instanceof Object[]){					
							attributeValue = ((Object[]) attributeValue)[0];	
						}
					} //=null
				} //for			
			return (String)attributeValue;
			}
		}
		catch (Exception e)
		{
			// This can happen as part of normal logic flow
			logger.error("extractValue Exception: ",e);
		}
		return retVal;
	}
	
	/**
	 * getStringFromJsonField
	 * Takes string in the form of: node1.nodeN.fieldName and returns
	 * the value contained in the JSON for that field as an String
	 * Note: supports leading $s in the field name, $s get stripped
	 * out in getValueFromJsonField
	 * @param fieldLocation
	 * @return Object
	 */
	public static String getStringFromDocumentField(Object docPojo, String fieldLocation, String value) 
	{
		try
		{
			if ((null != value) && fieldLocation.equalsIgnoreCase("value")) // ($value when iterating)
			{
				return value;
			}//TESTED			
			String retVal = (String)FieldExtractor.extractValue(fieldLocation,docPojo);
			return retVal;
		}
		catch (Exception e)
		{
			return null;
		}
	}
	
	/**
	 * getFormattedTextFromField
	 * Accepts a string value that can contain a combination of literal text and
	 * names of fields in the JSON document that need to be retrieved into the 
	 * literal text, i.e.:
	 * 'On $metadata.reportdatetime MPD reported that a $metadata.offense occurred.'
	 * @param v - origString
	 * @return String
	 */
	public static String getFormattedTextFromField(Object docPojo,String origString, String value)
	{
		// legacy special case
		if (origString.isEmpty()) { 
			origString = "$value";
		}
		// Don't bother running the rest of the code if there are no replacements to make (i.e. does not have $)
		if (!origString.contains("$")) {
			return origString;
		}
		
		StringBuffer sb = new StringBuffer();
		Matcher m = SUBSTITUTION_PATTERN.matcher(origString);
		int ncurrpos = 0;
		
		// Iterate over each match found within the string and concatenate values together:
		// string literal value + JSON field (matched pattern) retrieved
		while (m.find()) 
		{
		   int nnewpos = m.start();
		   sb.append(origString.substring(ncurrpos, nnewpos));
		   ncurrpos = m.end();
		   
		   // Retrieve the field information matched with the RegEx
		   String match = (m.group(1) != null) ? m.group(1): m.group(2);
		   String sreplace;
		   if ((null != match) && match.equals("$")) { // $ escaping via ${$}
			   sreplace = "$";
		   }//TESTED
		   else {
			   // Retrieve the data from the JSON field and append
			   sreplace = getStringFromDocumentField(docPojo,match, value);
		   }
		   if (null == sreplace) {
			   return null;
		   }
		   sb.append( sreplace );
		}
		sb.append(origString.substring(ncurrpos));
		return sb.toString();
	}
	

}
