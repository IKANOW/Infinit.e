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
