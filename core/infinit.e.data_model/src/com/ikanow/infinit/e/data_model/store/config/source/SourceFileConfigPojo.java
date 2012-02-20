package com.ikanow.infinit.e.data_model.store.config.source;

import java.util.ArrayList;
import java.util.List;

public class SourceFileConfigPojo {

	public String username = null;
	public String password = null;
	public String domain = null;
	//for xml files
	public List<String> XmlRootLevelValues = null;
	public List<String> XmlIgnoreValues = null;
	public String XmlSourceName = null;
	public String XmlPrimaryKey = null;
	public Boolean XmlPreserveCase = null; // (default: false)
	
	public void addRootLevelValue(String value)
	{
		if (XmlRootLevelValues == null)
			XmlRootLevelValues = new ArrayList<String>();
		XmlRootLevelValues.add(value);
	}
	
	public void addIgnoreValue(String value)
	{
		if (XmlIgnoreValues == null)
			XmlIgnoreValues = new ArrayList<String>();
		XmlIgnoreValues.add(value);
	}
}