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
import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;

import org.apache.log4j.Logger;



	public class AlchemyAPI_NamedEntityParams extends AlchemyAPI_Params{
		public static final String CLEANED_OR_RAW = "cleaned_or_raw";
		public static final String CLEANED = "cleaned";
		public static final String RAW = "raw";
		public static final String CQUERY = "cquery";
		public static final String XPATH = "xpath";

		private Boolean disambiguate;
		private Boolean linkedData;
		private Boolean coreference;
		private Boolean quotations;
		private String sourceText;
		private Boolean showSourceText;
		private String cQuery;
		private String xPath;
		private Integer maxRetrieve;
		private String baseUrl;
		
		// Initialize the Logger
		private static final Logger logger = Logger.getLogger(AlchemyAPI_NamedEntityParams.class);
		
		public boolean isDisambiguate() {
			return disambiguate;
		}
		public void setDisambiguate(boolean disambiguate) {
			this.disambiguate = disambiguate;
		}
		public boolean isLinkedData() {
			return linkedData;
		}
		public void setLinkedData(boolean linkedData) {
			this.linkedData = linkedData;
		}
		public boolean isCoreference() {
			return coreference;
		}
		public void setCoreference(boolean coreference) {
			this.coreference = coreference;
		}
		public boolean isQuotations() {
			return quotations;
		}
		public void setQuotations(boolean quotations) {
			this.quotations = quotations;
		}
		public String getSourceText() {
			return sourceText;
		}
		public void setSourceText(String sourceText) {
			if( !sourceText.equals(AlchemyAPI_NamedEntityParams.CLEANED) && !sourceText.equals(AlchemyAPI_NamedEntityParams.CLEANED_OR_RAW) 
					&& !sourceText.equals(AlchemyAPI_NamedEntityParams.RAW) && !sourceText.equals(AlchemyAPI_NamedEntityParams.CQUERY)
					&& !sourceText.equals(AlchemyAPI_NamedEntityParams.XPATH))
			{
				throw new RuntimeException("Invalid setting " + sourceText + " for parameter sourceText");
			}
			this.sourceText = sourceText;
		}
		public boolean isShowSourceText() {
			return showSourceText;
		}
		public void setShowSourceText(boolean showSourceText) {
			this.showSourceText = showSourceText;
		}
		public String getCQuery() {
			return cQuery;
		}
		public void setCQuery(String cQuery) {
			this.cQuery = cQuery;
		}
		public String getXPath() {
			return xPath;
		}
		public void setXPath(String xPath) {
			this.xPath = xPath;
		}
		public int getMaxRetrieve() {
			return maxRetrieve;
		}
		public void setMaxRetrieve(int maxRetrieve) {
			this.maxRetrieve = maxRetrieve;
		}
		public String getBaseUrl() {
			return baseUrl;
		}
		public void setBaseUrl(String baseUrl) {
			this.baseUrl = baseUrl;
		}
		
		public String getParameterString(){
			String retString = super.getParameterString();
			try{
				if(disambiguate!=null) retString+="&disambiguate="+(disambiguate ? "1":"0");
				if(linkedData!=null) retString+="&linkedData="+(linkedData?"1":"0");
				if(coreference!=null) retString+="&coreference="+(coreference?"1":"0");
				if(quotations!=null) retString+="&quotations="+(quotations?"1":"0");
				if(sourceText!=null) retString+="&sourceText="+sourceText;
				if(showSourceText!=null) retString+="&showSourceText="+(showSourceText?"1":"0");
				if(cQuery!=null) retString+="&cquery="+URLEncoder.encode(cQuery,"UTF-8");
				if(xPath!=null) retString+="&xpath="+URLEncoder.encode(xPath,"UTF-8");
				if(maxRetrieve!=null) retString+="&maxRetrieve="+maxRetrieve.toString();
				if(baseUrl!=null) retString+="&baseUrl="+URLEncoder.encode(baseUrl,"UTF-8");
			}
			catch(UnsupportedEncodingException e ){
				retString = "";
				// If an exception occurs log the error
				logger.error("Unsupported Encoding Exception Message: " + e.getMessage(), e);
			}
			return retString;
		}
		
	
		
	}
	


