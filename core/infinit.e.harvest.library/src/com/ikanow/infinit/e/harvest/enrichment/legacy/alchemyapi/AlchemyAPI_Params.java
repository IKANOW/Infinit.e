package com.ikanow.infinit.e.harvest.enrichment.legacy.alchemyapi;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.log4j.Logger;



public class AlchemyAPI_Params {
	public static final String OUTPUT_XML = "xml";
	public static final String OUTPUT_RDF = "rdf";
	public static final String OUTPUT_JSON = "json";
	
	private String url;
	private String html;
	private String text;
	private String outputMode = OUTPUT_XML;
	private String customParameters;
	
	// Initialize the Logger
	private static final Logger logger = Logger.getLogger(AlchemyAPI_Params.class);
	
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getHtml() {
		return html;
	}
	public void setHtml(String html) {
		this.html = html;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public String getOutputMode() {
		return outputMode;
	}
	public void setOutputMode(String outputMode) {
		if( !outputMode.equals(AlchemyAPI_Params.OUTPUT_XML) && !outputMode.equals(OUTPUT_RDF) && !outputMode.equals(OUTPUT_JSON)) 
		{
			throw new RuntimeException("Invalid setting " + outputMode + " for parameter outputMode");
		}
		this.outputMode = outputMode;
	}
	public String getCustomParameters() {
		return customParameters;
	}
	
	public void setCustomParameters(String... customParameters) {
		StringBuilder data = new StringBuilder();
		try{
			for (int i = 0; i < customParameters.length; ++i) {
				data.append('&').append(customParameters[i]);
				if (++i < customParameters.length)
					data.append('=').append(URLEncoder.encode(customParameters[i], "UTF8"));
			}
		}
		catch(UnsupportedEncodingException e){
			this.customParameters = "";
			return;
		}
		this.customParameters = data.toString();
	}
	
	public String getParameterString(){
		String retString = "";
		try{
			if(url!=null) retString+="&url="+URLEncoder.encode(url,"UTF-8");
			if(html!=null) retString+="&html="+URLEncoder.encode(html,"UTF-8");
			if(text!=null) retString+="&text="+URLEncoder.encode(text,"UTF-8");
			if(customParameters!=null) retString+=customParameters;
			if(outputMode!=null) retString+="&outputMode="+outputMode;
		}
		catch(UnsupportedEncodingException e ){
			// If an exception occurs log the error
			logger.error("Unsupported Encoding Exception Message: " + e.getMessage(), e);
			retString = "";
		}
		return retString;
	}
}
