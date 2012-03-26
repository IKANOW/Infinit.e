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

public class SimpleTextCleanserPojo 
{
	private String field = null;
	private String regEx = null;
	private String replacement = null;
	private String flags = null;
	
	public void setField(String field) {
		this.field = field;
	}
	public String getField() {
		return field;
	}
	
	public void setRegEx(String regEx) {
		this.regEx = regEx;
	}
	public String getRegEx() {
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

}
