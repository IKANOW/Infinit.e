package com.ikanow.infinit.e.harvest.extraction.document.file;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
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

	public XmlToMetadataParser()
	{
		levelOneFields = new ArrayList<String>();
		ignoreFields = new ArrayList<String>();
	}

	/**
	 * @param levelOneFields the levelOneField to set
	 * @param ignoreFields the ignoreFields to set
	 */
	public XmlToMetadataParser(List<String> levelOneFields, List<String> ignoreFields, String XmlSourceName, String XmlPrimaryKey, Boolean XmlPreserveCase)
	{
		this();
		if (null != XmlPreserveCase) {
			this.bPreserveCase = XmlPreserveCase;
		}
		this.XmlSourceName = XmlSourceName;
		this.PKElement = XmlPrimaryKey;
		setLevelOneField(levelOneFields);
		setIgnoreField(ignoreFields);
	}

	/**
	 * @param levelOneFields the levelOneFields to set
	 */
	public void setLevelOneField(List<String> levelOneFields) {
		this.levelOneFields = levelOneFields;
	}

	/**
	 * @param ignoreFields the ignoreFields to set
	 */
	public void setIgnoreField(List<String> ignoreFields) {
		this.ignoreFields = ignoreFields;
	}

	/**
	 * Converts a JsonObject to a LinkedHashMap.
	 * @param json  JSONObject to convert
	 */
	@SuppressWarnings("static-access")
	private LinkedHashMap<String,Object> convertJsonObjectToLinkedHashMap(JSONObject json)
	{
		LinkedHashMap<String,Object> list = new LinkedHashMap<String,Object>();
		for (String names: json.getNames(json))
		{
			JSONObject rec = null;
			JSONArray jarray = null;
			try {
				jarray = json.getJSONArray(names);
				list.put(names, handleJsonArray(jarray));
			} catch (JSONException e2) {
				try {
					rec = json.getJSONObject(names);
					list.put(names, convertJsonObjectToLinkedHashMap(rec));
				} catch (JSONException e) {
					try {
						list.put(names,json.getString(names));
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

	
	private Object[] handleJsonArray(JSONArray jarray)
	{
		Object o[] = new Object[jarray.length()];
		for (int i = 0; i < jarray.length(); i++)
		{
			JSONObject tem;
			try {
				tem = jarray.getJSONObject(i);
				o[i] = convertJsonObjectToLinkedHashMap(tem);
			} catch (JSONException e) {
				try {
					o[i] = jarray.getString(i);
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
	@SuppressWarnings("static-access")
	public List<DocumentPojo> parseDocument(XMLStreamReader reader) throws XMLStreamException {
		DocumentPojo doc = new DocumentPojo();
		List<DocumentPojo> docList = new ArrayList<DocumentPojo>();
		boolean justIgnored = false;
		boolean hitIdentifier = false;

		while (reader.hasNext()) {
			int eventCode = reader.next();

			switch (eventCode)
			{
			case(XMLStreamReader.START_ELEMENT):
				String tagName = reader.getLocalName();
			if (null == levelOneFields || levelOneFields.size() == 0) {
				levelOneFields = new ArrayList<String>();
				levelOneFields.add(tagName);
				doc = new DocumentPojo();
				sb.delete(0, sb.length());
				justIgnored = false;
			}
			else if (levelOneFields.contains(tagName)){
				sb.delete(0, sb.length());
				doc = new DocumentPojo();
				justIgnored = false;
			}
			else if (ignoreFields.contains(tagName))
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


			break;
			case (XMLStreamReader.CHARACTERS):
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

			break;
			case (XMLStreamReader.END_ELEMENT):
				hitIdentifier = !reader.getLocalName().equalsIgnoreCase(PKElement);
			if (!ignoreFields.contains(reader.getLocalName())){
				if (levelOneFields.contains(reader.getLocalName())) {
					JSONObject json;
					try {
						json = XML.toJSONObject(sb.toString());
						for (String names: json.getNames(json))
						{
							JSONObject rec = null;
							JSONArray jarray = null;

							try {
								jarray = json.getJSONArray(names);
								doc.addToMetadata(names, handleJsonArray(jarray));
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
					sb.delete(0, sb.length());
					docList.add(doc);
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
			break;

			}
		}
		return docList;
	}

}
