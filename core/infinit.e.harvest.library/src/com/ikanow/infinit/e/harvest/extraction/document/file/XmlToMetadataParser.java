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
package com.ikanow.infinit.e.harvest.extraction.document.file;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.lang.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;

public class XmlToMetadataParser {

	StringBuffer sb = new StringBuffer();
	boolean bPreserveCase = false;
	public List<String> levelOneFields;
	public List<String> ignoreFields;
	public String XmlSourceName;
	public String PKElement;
	public String AttributePrefix;
	private int nMaxDocs = Integer.MAX_VALUE;
	private int nCurrDocs = 0;
	private boolean _bPrefixMode = false; // (if true then includes prefix in its matching)

	// Track approximate memory usage
	private long _memUsage = 0;	
	public long getMemUsage() {
		return _memUsage*2; // (2x for string->byte)
	}
	
	public XmlToMetadataParser()
	{
		levelOneFields = new ArrayList<String>();
		ignoreFields = new ArrayList<String>();
		_bPrefixMode = false;
	}

	/**
	 * @param levelOneFields the levelOneField to set
	 * @param ignoreFields the ignoreFields to set
	 */
	public XmlToMetadataParser(List<String> levelOneFields, List<String> ignoreFields, 
								String XmlSourceName, String XmlPrimaryKey, String XmlAttributePrefix, Boolean XmlPreserveCase, int nMaxDocs)
	{
		this();
		if (nMaxDocs > 0) {
			this.nMaxDocs = nMaxDocs;
		}
		if (null != XmlPreserveCase) {
			this.bPreserveCase = XmlPreserveCase;
		}
		this.XmlSourceName = XmlSourceName;
		if (null == XmlSourceName) this.XmlSourceName = "";
		this.PKElement = XmlPrimaryKey;
		setLevelOneField(levelOneFields);
		setIgnoreField(ignoreFields);
		AttributePrefix = XmlAttributePrefix;
	}

	/**
	 * @param levelOneFields the levelOneFields to set
	 */
	public void setLevelOneField(List<String> levelOneFields) {
		if (null != levelOneFields) {
			this.levelOneFields = levelOneFields;
			
			for (String s: levelOneFields) {
				if (s.contains(":")) {
					_bPrefixMode = true;
				}
			}//TESTED (by hand)
		}
	}

	/**
	 * @param ignoreFields the ignoreFields to set
	 */
	public void setIgnoreField(List<String> ignoreFields) {
		if (null != ignoreFields) {
			this.ignoreFields = ignoreFields;
			
			if (!_bPrefixMode) {
				for (String s: ignoreFields) {
					if (s.contains(":")) {
						_bPrefixMode = true;
					}
				}
			}//TESTED (by hand)
		}
	}

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
	static public LinkedHashMap<String,Object> convertJsonObjectToLinkedHashMap(JSONObject json)
	{
		return convertJsonObjectToLinkedHashMap(json, false);
	}
	static public LinkedHashMap<String,Object> convertJsonObjectToLinkedHashMap(JSONObject json, boolean bHtmlUnescape)
	{
		int length = json.length();
		LinkedHashMap<String,Object> list = new LinkedHashMap<String,Object>(capacity(length));
		String[] names = JSONObject.getNames(json);
		if (null == names) { // (empty object)
			return null;
		}
		for (String name: names)
		{
			try {
				Object unknownType =  json.get(name);
				if (unknownType instanceof JSONArray) {
					list.put(name, handleJsonArray((JSONArray)unknownType, bHtmlUnescape));
				}
				else if (unknownType instanceof JSONObject) {
					list.put(name, convertJsonObjectToLinkedHashMap((JSONObject)unknownType, bHtmlUnescape));
				}
				else {
					if (bHtmlUnescape) {
						list.put(name,StringEscapeUtils.unescapeHtml(unknownType.toString()));
					}
					else {
						list.put(name,unknownType.toString());
					}
				} 
			}
			catch (JSONException e2) { }
		}
		if (list.size() > 0)
		{
			return list;
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
					o[i] = convertJsonObjectToLinkedHashMap((JSONObject)unknownType, bHtmlUnescape);					
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

	/**
	 * Parses XML and returns a new feed with the resulting HashMap as Metadata
	 * @param reader XMLStreamReader using Stax to avoid out of memory errors
	 * @return List of Feeds with their Metadata set
	 */
	public List<DocumentPojo> parseDocument(XMLStreamReader reader) throws XMLStreamException {
		return parseDocument(reader, false);		
	}//TESTED (used by FileHarvester in this form, UAH::meta (stream) with textOnly==true below)
	public List<DocumentPojo> parseDocument(XMLStreamReader reader, boolean textOnly) throws XMLStreamException {
		DocumentPojo doc = new DocumentPojo();
		List<DocumentPojo> docList = new ArrayList<DocumentPojo>();
		boolean justIgnored = false;
		boolean hitIdentifier = false;
		nCurrDocs = 0;
		_memUsage = 0;

		StringBuffer fullText = new StringBuffer();
		
		while (reader.hasNext()) {
			int eventCode = reader.next();

			switch (eventCode)
			{
			case(XMLStreamReader.START_ELEMENT):
			{				
				String tagName = reader.getLocalName();
				if (_bPrefixMode && (null != reader.getPrefix())) {
					tagName = reader.getPrefix() + ":" + tagName;
				}//TESTED (by hand)
			
				if (null == levelOneFields || levelOneFields.size() == 0) {
					levelOneFields = new ArrayList<String>();
					levelOneFields.add(tagName);
					doc = new DocumentPojo();
					sb.delete(0, sb.length());
					fullText.setLength(0);
					justIgnored = false;
				}
				else if (levelOneFields.contains(tagName)){
					sb.delete(0, sb.length());
					doc = new DocumentPojo();
					fullText.setLength(0);
					justIgnored = false;
				}
				else if ((null != ignoreFields) && ignoreFields.contains(tagName))
				{
					justIgnored = true;
				}
				else{
					if (this.bPreserveCase) {
						sb.append("<").append(reader.getLocalName()).append(">");					
					}
					else {
						sb.append("<").append(reader.getLocalName().toLowerCase()).append(">");
					}
					justIgnored = false;
				}
				if (null != doc) {
					fullText.append("<").append(reader.getLocalName());
					for (int ii = 0; ii < reader.getAttributeCount(); ++ii) {
						fullText.append(" ");
						fullText.append(reader.getAttributeLocalName(ii)).append("=\"").append(reader.getAttributeValue(ii)).append('"');
					}
					fullText.append(">");
				}//TESTED
				
				hitIdentifier = tagName.equalsIgnoreCase(PKElement);
				
				if (!justIgnored && (null != this.AttributePrefix)) { // otherwise ignore attributes anyway
					int nAttributes = reader.getAttributeCount();
					StringBuffer sb2 = new StringBuffer();
					for (int i = 0; i < nAttributes; ++i) {
						sb2.setLength(0);
						sb.append('<');
						
						sb2.append(this.AttributePrefix);
						if (this.bPreserveCase) {
							sb2.append(reader.getAttributeLocalName(i).toLowerCase());
						}
						else {
							sb2.append(reader.getAttributeLocalName(i));
						}
						sb2.append('>');
						
						sb.append(sb2);
						sb.append("<![CDATA[").append(reader.getAttributeValue(i).trim()).append("]]>");
						sb.append("</").append(sb2);
					}
				}
			}
			break;
			
			case (XMLStreamReader.CHARACTERS):
			{
				if (null != doc) {
					fullText.append(reader.getText());
				}//TESTED
				
				if(reader.getText().trim().length()>0 && justIgnored == false)
					sb.append("<![CDATA[").append(reader.getText().trim()).append("]]>");
				if(hitIdentifier)
				{
					String tValue = reader.getText().trim();
					if (null != XmlSourceName){
						if (tValue.length()> 0){
							doc.setUrl(XmlSourceName + tValue);
						}
					}
				}
			}
			break;
			case (XMLStreamReader.END_ELEMENT):
			{
				String tagName = reader.getLocalName();
				if (_bPrefixMode && (null != reader.getPrefix())) {
					tagName = reader.getPrefix() + ":" + tagName;
				}//TESTED (by hand)			
				
				if (null != doc) {
					if (this.bPreserveCase) {
						fullText.append("</").append(reader.getLocalName()).append(">");
					}
					else {
						fullText.append("</").append(reader.getLocalName()).append(">");						
					}
				}//TESTED						
				
				hitIdentifier = !tagName.equalsIgnoreCase(PKElement);
				if ((null != ignoreFields) && !ignoreFields.contains(tagName)){
					if (levelOneFields.contains(tagName)) {						
						if (null == doc) {
							// Uh oh, something has gone very wrong here, we've found an end element
							// but we didn't find a start element
							if (null != reader.getPrefix()) {
								throw new RuntimeException("XML parse error, found end without start: " + reader.getPrefix() + ':' + reader.getLocalName() + "; prefix mode = " + _bPrefixMode);								
							}
							else {
								throw new RuntimeException("XML parse error, found end without start: " + reader.getLocalName() + "; prefix mode = " + _bPrefixMode);
							}
						}//TESTED
						JSONObject json;
						if (!textOnly) {
							try {
								json = XML.toJSONObject(sb.toString());
								for (String names: JSONObject.getNames(json))
								{
									JSONObject rec = null;
									JSONArray jarray = null;
		
									try {
										jarray = json.getJSONArray(names);
										doc.addToMetadata(names, handleJsonArray(jarray, false));
									} catch (JSONException e) {
										try {
											rec = json.getJSONObject(names);
											doc.addToMetadata(names, convertJsonObjectToLinkedHashMap(rec));
										} catch (JSONException e2) {
											try {
												Object[] val = {json.getString(names)};
												doc.addToMetadata(names,val);
											} catch (JSONException e1) {
												e1.printStackTrace();
											}
										}
									}
								}
		
							} catch (JSONException e) {
								e.printStackTrace();
							}
						}
						doc.setFullText(fullText.toString());
						_memUsage += sb.length()*4L; // 3x - one for full text, 3x for the object + overhead
						sb.setLength(0);
						sb.delete(0, sb.length());
						
						docList.add(doc);
						doc = null;
						if (++nCurrDocs >= nMaxDocs) {
							return docList;
						}						
					}
					else{
						if (this.bPreserveCase) {
							sb.append("</").append(reader.getLocalName()).append(">");						
						}
						else {
							sb.append("</").append(reader.getLocalName().toLowerCase()).append(">");
						}
	
					}
				}
			} // (end case)
			break;
			} // (end switch)
		}
		return docList;
	}

}
