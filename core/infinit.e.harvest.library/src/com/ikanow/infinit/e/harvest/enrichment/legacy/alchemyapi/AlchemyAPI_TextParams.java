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
package com.ikanow.infinit.e.harvest.enrichment.legacy.alchemyapi;


public class AlchemyAPI_TextParams extends AlchemyAPI_Params{
	private Boolean useMetaData;
	private Boolean extractLinks;
	
	public boolean isUseMetaData() {
		return useMetaData;
	}
	
	public void setUseMetaData(boolean useMetaData) {
		this.useMetaData = useMetaData;
	}
	
	public boolean isExtractLinks() {
		return extractLinks;
	}
	
	public void setExtractLinks(boolean extractLinks) {
		this.extractLinks = extractLinks;
	}
	
	public String getParameterString(){
		String retString = super.getParameterString();
		
		if(useMetaData!=null) retString+="&useMetaData="+(useMetaData?"1":"0");
		if(extractLinks!=null) retString+="&extractLinks="+(extractLinks?"1":"0");
		
		return retString;
	}
}
