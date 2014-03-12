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
package com.ikanow.infinit.e.data_model.custom;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.lang.StringEscapeUtils;
import org.bson.BSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import com.ikanow.infinit.e.data_model.store.config.source.SourceFileConfigPojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.mongodb.BasicDBObject;

// (taken from com.ikanow.infinit.e.harvest.extraction.document.file.XmlToMetadataParser)

public class InfiniteFileInputXmlParser implements InfiniteFileInputParser {

	private List<String> levelOneFields;
	private List<String> ignoreFields;
	private String XmlSourceName;
	private String PKElement;
	private String AttributePrefix;
	private boolean bPreserveCase = false;
	
	private XMLStreamReader _xmlStreamReader = null;	
	private StringBuffer _sb = null;
	@SuppressWarnings("unused")
	private int _oneUp = 1;
	
	// Interface
	
	@Override
	public InfiniteFileInputParser initialize(InputStream inStream, SourceFileConfigPojo fileConfig) throws IOException {
		
		// Processing logic
		
		levelOneFields = new ArrayList<String>();
		ignoreFields = new ArrayList<String>();
		if (null != fileConfig.XmlPreserveCase) {
			this.bPreserveCase = fileConfig.XmlPreserveCase;
		}
		XmlSourceName = fileConfig.XmlSourceName;
		PKElement = fileConfig.XmlPrimaryKey;
		setLevelOneField(fileConfig.XmlRootLevelValues);
		setIgnoreField(fileConfig.XmlIgnoreValues);
		AttributePrefix = fileConfig.XmlAttributePrefix;
		
		_sb = new StringBuffer();
		
		// Input stream -> reader
		
		XMLInputFactory factory = XMLInputFactory.newInstance();
		factory.setProperty(XMLInputFactory.IS_COALESCING, true);
		factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		try {
			_xmlStreamReader = factory.createXMLStreamReader(inStream);
		} 
		catch (XMLStreamException e) {
			throw new IOException(e);
		}
		
		return this;		
	}

	@Override
	public void close() {
		try {
			if (null != _xmlStreamReader) _xmlStreamReader.close();
		}
		catch (Exception e) {} // just carry on
	}

	///////////////////////////////////
	
	// Interface/main processing logic
	
	@Override
	public BSONObject getNextRecord() throws IOException {
		boolean justIgnored = false;
		boolean hitIdentifier = false;

		BSONObject currObj = null;
		
		StringBuffer fullText = new StringBuffer();
		
		try {
			while (_xmlStreamReader.hasNext()) {
				int eventCode = _xmlStreamReader.next();

				switch (eventCode)
				{
				case(XMLStreamReader.START_ELEMENT):
				{
					String tagName = _xmlStreamReader.getLocalName();
				
					if (null == levelOneFields || levelOneFields.size() == 0) {
						levelOneFields = new ArrayList<String>();
						levelOneFields.add(tagName);
						currObj = new BasicDBObject();
						_sb.delete(0, _sb.length());
						fullText.setLength(0);
						justIgnored = false;
					}
					else if (levelOneFields.contains(tagName)){
						_sb.delete(0, _sb.length());
						currObj = new BasicDBObject();
						fullText.setLength(0);
						justIgnored = false;
					}
					else if ((null != ignoreFields) && ignoreFields.contains(tagName))
					{
						justIgnored = true;
					}
					else{
						if (this.bPreserveCase) {
							_sb.append("<").append(tagName).append(">");					
						}
						else {
							_sb.append("<").append(tagName.toLowerCase()).append(">");
						}
						justIgnored = false;
					}
					if (null != currObj) {
						fullText.append("<").append(tagName);
						for (int ii = 0; ii < _xmlStreamReader.getAttributeCount(); ++ii) {
							fullText.append(" ");
							fullText.append(_xmlStreamReader.getAttributeLocalName(ii)).append("=\"").append(_xmlStreamReader.getAttributeValue(ii)).append('"');
						}
						fullText.append(">");
					}//TESTED
					
					hitIdentifier = tagName.equalsIgnoreCase(PKElement);
					
					if (!justIgnored && (null != this.AttributePrefix)) { // otherwise ignore attributes anyway
						int nAttributes = _xmlStreamReader.getAttributeCount();
						StringBuffer sb2 = new StringBuffer();
						for (int i = 0; i < nAttributes; ++i) {
							sb2.setLength(0);
							_sb.append('<');
							
							sb2.append(this.AttributePrefix);
							if (this.bPreserveCase) {
								sb2.append(_xmlStreamReader.getAttributeLocalName(i).toLowerCase());
							}
							else {
								sb2.append(_xmlStreamReader.getAttributeLocalName(i));
							}
							sb2.append('>');
							
							_sb.append(sb2);
							_sb.append("<![CDATA[").append(_xmlStreamReader.getAttributeValue(i).trim()).append("]]>");
							_sb.append("</").append(sb2);
						}
					}
				}
				break;
				
				case (XMLStreamReader.CHARACTERS):
				{
					if (null != currObj) {
						fullText.append(_xmlStreamReader.getText());
					}//TESTED
					
					if(_xmlStreamReader.getText().trim().length()>0 && justIgnored == false)
						_sb.append("<![CDATA[").append(_xmlStreamReader.getText().trim()).append("]]>");
					if(hitIdentifier)
					{
						String tValue = _xmlStreamReader.getText().trim();
						if (null != XmlSourceName){
							if (tValue.length()> 0){
								currObj.put(DocumentPojo.url_, XmlSourceName + tValue);
							}
						}
					}
				}
				break;
				case (XMLStreamReader.END_ELEMENT):
				{
					if (null != currObj) {
						fullText.append("</").append(_xmlStreamReader.getLocalName()).append(">");
					}//TESTED
					
					hitIdentifier = !_xmlStreamReader.getLocalName().equalsIgnoreCase(PKElement);
					if ((null != ignoreFields) && !ignoreFields.contains(_xmlStreamReader.getLocalName())){
						if (levelOneFields.contains(_xmlStreamReader.getLocalName())) {
							try {
								JSONObject json = XML.toJSONObject(_sb.toString());
								BasicDBObject xmlMetadata = convertJsonObjectToBson(json); 
								currObj.put(DocumentPojo.metadata_, new BasicDBObject("xml", Arrays.asList(xmlMetadata)));	
							} 
							catch (JSONException e) {
								e.printStackTrace();
							}
							currObj.put(DocumentPojo.fullText_, fullText.toString());
							_sb.delete(0, _sb.length());
							_oneUp++;
							return currObj;
						}
						else{
							if (this.bPreserveCase) {
								_sb.append("</").append(_xmlStreamReader.getLocalName()).append(">");						
							}
							else {
								_sb.append("</").append(_xmlStreamReader.getLocalName().toLowerCase()).append(">");
							}

						}
					}
				} // (end case)
				break;
				} // (end switch)
			}
		} catch (XMLStreamException e) {
			// Don't throw exception, just recover and move onto next split			
			//throw new IOException("record " + _oneUp + ": " + e.getMessage(), e);
		}
		return null;
	}

	///////////////////////////////////
	
	// Other processing logic
	
	/**
	 * Converts a JsonObject to a LinkedHashMap.
	 * @param json  JSONObject to convert
	 */
	static private int capacity(int expectedSize) {
	    if (expectedSize < 3) {
	        return expectedSize + 1;
	    }
        return expectedSize + expectedSize / 3;
	}
	static public BasicDBObject convertJsonObjectToBson(JSONObject json)
	{
		return convertJsonObjectToBson(json, false);
	}
	static public BasicDBObject convertJsonObjectToBson(JSONObject json, boolean bHtmlUnescape)
	{
		int length = json.length();
		BasicDBObject dbo = new BasicDBObject(capacity(length));
		String[] names = JSONObject.getNames(json);
		if (null == names) { // (empty object)
			return null;
		}
		for (String name: names)
		{
			try {
				Object unknownType =  json.get(name);
				if (unknownType instanceof JSONArray) {
					dbo.put(name, handleJsonArray((JSONArray)unknownType, bHtmlUnescape));
				}
				else if (unknownType instanceof JSONObject) {
					dbo.put(name, convertJsonObjectToBson((JSONObject)unknownType, bHtmlUnescape));
				}
				else {
					if (bHtmlUnescape) {
						dbo.put(name,StringEscapeUtils.unescapeHtml(unknownType.toString()));
					}
					else {
						dbo.put(name,unknownType.toString());
					}
				} 
			}
			catch (JSONException e2) { }
		}
		if (dbo.size() > 0)
		{
			return dbo;
		}
		return null;
	}

	static private Object[] handleJsonArray(JSONArray jarray, boolean bHtmlUnescape)
	{
		Object o[] = new Object[jarray.length()];
		for (int i = 0; i < jarray.length(); i++)
		{
			try {
				Object unknownType =  jarray.get(i);
				if (unknownType instanceof JSONObject) {
					o[i] = convertJsonObjectToBson((JSONObject)unknownType, bHtmlUnescape);					
				}
				else if (unknownType instanceof JSONArray) {
					o[i] = handleJsonArray((JSONArray) unknownType, bHtmlUnescape);
				}
				else {
					if (bHtmlUnescape) {
						o[i] = StringEscapeUtils.unescapeHtml(unknownType.toString());
					}
					else {
						o[i] = unknownType.toString();						
					}					
				}				
				
			} catch (JSONException e) {
			}
		}
		return o;
	}


	///////////////////////////////////

	// Minor utility code:

	/**
	 * @param levelOneFields the levelOneFields to set
	 */
	public void setLevelOneField(List<String> levelOneFields) {
		if (null != levelOneFields) {
			this.levelOneFields = levelOneFields;
		}
	}

	/**
	 * @param ignoreFields the ignoreFields to set
	 */
	public void setIgnoreField(List<String> ignoreFields) {
		if (null != ignoreFields) {
			this.ignoreFields = ignoreFields;
		}
	}

	@Override
	public String getCanonicalExtension() {
		return ".xml";
	}
}
