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
	public String XmlAttributePrefix = null; // (default: null - if enabled, attributes are converted into tags with this prefix)
	
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
