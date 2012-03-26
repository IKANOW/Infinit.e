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
