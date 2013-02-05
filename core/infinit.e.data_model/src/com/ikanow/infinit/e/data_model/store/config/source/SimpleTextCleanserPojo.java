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

public class SimpleTextCleanserPojo 
{
	private String field = null;
	private String regEx = null; // OBSOLETED, remove once posible
	private String script = null;
	private String scriptlang = null; // (defaults to "regex", can also be "xpath", "javascript")
	private String replacement = null; // (ignored for javascript)
	private String flags = null; // (same flags as corresponding metadata, plus "+" to concatenate multiple entries) 
	
	public void setField(String field) {
		this.field = field;
	}
	public String getField() {
		return field;
	}
	
	public void setScript(String script) {
		this.script = script;
	}
	public String getScript() {
		if (null == regEx) {
			return script;
		}
		return regEx;
	}
	
	public void setReplacement(String replacement) {
		this.replacement = replacement;
	}
	public String getReplacement() {
		return replacement;
	}
	public String getFlags() {
		return flags;
	}
	public void setFlags(String flags) {
		this.flags = flags;
	}
	public void setScriptlang(String scriptlang) {
		this.scriptlang = scriptlang;
	}
	public String getScriptlang() {
		return scriptlang;
	}

}
