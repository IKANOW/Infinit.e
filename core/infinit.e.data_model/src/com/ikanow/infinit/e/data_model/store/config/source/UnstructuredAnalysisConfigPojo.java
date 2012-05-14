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

/**
 * UnstructuredAnalysisPojo
 */
public class UnstructuredAnalysisConfigPojo 
{
	public enum Context{
		All,Header,Body,Footer,First; // (First means before any text cleasing, otherwise same as "all")
	}
		
	private String headerRegEx = null;  // (optional specifies a regex to find the header of a document)
	private String headerRexExFlags = null; // (very optional, allows fields to be specified for the above regex)
	private String footerRegEx = null; // (optional specifies a regex to find the footer of a document)
	private String footerRegExFlags = null;  // (very optional, allows fields to be specified for the above regex)
	
	private List<metaField> meta = null;	
	private List<SimpleTextCleanserPojo> simpleTextCleanser = null;
	
	public static class metaField{
		
		public String fieldName;
		public Context context;
		public String script; // (or regex for scriptlang=="regex")
		public String scriptlang; // (currently "javascript" or "regex", defaults to "javascript")
		public String replace; // (currently only used in "regex" mode)
		public Integer groupNum; // (currently only used in "regex" mode)
		public String flags; // (currently only used in "regex" mode)
		
		public metaField() {}
		
		// (default constructor uses regex)
		public metaField(String fieldName, Context context, int groupNum, String regex, String replace)
		{
			this.fieldName = fieldName;
			this.context = context;
			this.script = regex;
			this.scriptlang = "regex";
			this.groupNum = groupNum;
			this.replace = replace;
		}
		public metaField(String fieldName, Context context, String script, String scriptlang)
		{
			this.fieldName = fieldName;
			this.context = context;
			this.script = script;
			this.scriptlang = scriptlang;
		}
	}
	
	public void AddMetaField(String fieldName, Context context, String script, String scriptlang)
	{
		if (null == meta)
			meta = new ArrayList<metaField>();
			
		meta.add(new metaField(fieldName, context, script, scriptlang));
	}

	public void AddMetaField(String fieldName, Context context, int groupNum, String regex)
	{
		AddMetaField(fieldName, context, groupNum, regex, null);
	}
	
	public void AddMetaField(String fieldName, Context context, int groupNum, String regex, String replace)
	{
		if (null == meta)
			meta = new ArrayList<metaField>();
			
		meta.add(new metaField(fieldName, context,groupNum, regex, replace));
	}
	
	public List<metaField> getMeta()
	{
		return meta;
	}
	
	/**
	 * @return the headerRegEx
	 */
	public String getHeaderRegEx() {
		return headerRegEx;
	}
	/**
	 * @param headerRegEx the headerRegEx to set
	 */
	public void setHeaderRegEx(String headerRegEx) {
		this.headerRegEx = headerRegEx;
	}
	/**
	 * @return the footerRegEx
	 */
	public String getFooterRegEx() {
		return footerRegEx;
	}
	/**
	 * @param footerRegEx the footerRegEx to set
	 */
	public void setFooterRegEx(String footerRegEx) {
		this.footerRegEx = footerRegEx;
	}
	
	
	public void setSimpleTextCleanser(List<SimpleTextCleanserPojo> simpleTextCleanser) {
		this.simpleTextCleanser = simpleTextCleanser;
	}

	public List<SimpleTextCleanserPojo> getSimpleTextCleanser() {
		return simpleTextCleanser;
	}

	public void setHeaderRexExFields(String headerRexExFields) {
		this.headerRexExFlags = headerRexExFields;
	}

	public String getHeaderRexExFlags() {
		return headerRexExFlags;
	}

	public void setFooterRegExFlags(String footerRegExFields) {
		this.footerRegExFlags = footerRegExFields;
	}

	public String getFooterRegExFlags() {
		return footerRegExFlags;
	}
}
