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

	public XmlToMetadataParser()
	{
		levelOneFields = new ArrayList<String>();
		ignoreFields = new ArrayList<String>();
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

	/**
	 * Converts a JsonObject to a LinkedHashMap.
	 * @param json  JSONObject to convert
	 */
	static public LinkedHashMap<String,Object> convertJsonObjectToLinkedHashMap(JSONObject json)
	{
		return convertJsonObjectToLinkedHashMap(json, false);
	}
	static public LinkedHashMap<String,Object> convertJsonObjectToLinkedHashMap(JSONObject json, boolean bHtmlUnescape)
	{
		LinkedHashMap<String,Object> list = new LinkedHashMap<String,Object>();
		String[] names = JSONObject.getNames(json);
		if (null == names) { // (empty object)
			return null;
		}
		for (String name: names)
		{
			JSONObject rec = null;
			JSONArray jarray = null;
			try {
				jarray = json.getJSONArray(name);
				list.put(name, handleJsonArray(jarray, bHtmlUnescape));
			} catch (JSONException e2) {
				try {
					rec = json.getJSONObject(name);
					list.put(name, convertJsonObjectToLinkedHashMap(rec, bHtmlUnescape));
				} catch (JSONException e) {
					try {
						if (bHtmlUnescape) {
							list.put(name,StringEscapeUtils.unescapeHtml(json.getString(name)));
						}
						else {
							list.put(name,json.getString(name));
						}
					} catch (JSONException e1) {
						e1.printStackTrace();
					}
				}
			}
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
			JSONObject tem;
			try {
				tem = jarray.getJSONObject(i);
				o[i] = convertJsonObjectToLinkedHashMap(tem, bHtmlUnescape);
			} catch (JSONException e) {
				try {
					if (bHtmlUnescape) {
						o[i] = StringEscapeUtils.unescapeHtml(jarray.getString(i));
					}
					else {
						o[i] = jarray.getString(i);						
					}
				} catch (JSONException e1) {
					e1.printStackTrace();
				}
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

		StringBuffer fullText = new StringBuffer();
		
		while (reader.hasNext()) {
			int eventCode = reader.next();

			switch (eventCode)
			{
			case(XMLStreamReader.START_ELEMENT):
			{
				String tagName = reader.getLocalName();
			
				if (null != doc) {
					fullText.append("<").append(tagName);
					for (int ii = 0; ii < reader.getAttributeCount(); ++ii) {
						fullText.append(" ");
						fullText.append(reader.getAttributeLocalName(ii)).append("=\"").append(reader.getAttributeValue(ii)).append('"');
					}
					fullText.append(">");
				}//TESTED
				
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
						sb.append("<").append(tagName).append(">");					
					}
					else {
						sb.append("<").append(tagName.toLowerCase()).append(">");
					}
					justIgnored = false;
				}
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
				if (null != doc) {
					fullText.append("</").append(reader.getLocalName()).append(">");
				}//TESTED
				
				hitIdentifier = !reader.getLocalName().equalsIgnoreCase(PKElement);
				if ((null != ignoreFields) && !ignoreFields.contains(reader.getLocalName())){
					if (levelOneFields.contains(reader.getLocalName())) {
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
