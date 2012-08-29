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

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileNotFoundException;

import java.net.HttpURLConnection;
import java.net.URL;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import com.ikanow.infinit.e.data_model.InfiniteEnums;
import com.ikanow.infinit.e.data_model.InfiniteEnums.ExtractorDocumentLevelException;
import com.ikanow.infinit.e.harvest.utils.PropertiesManager;

public class AlchemyAPI_JSON {

    private String _apiKey;
    private String _requestUri = "http://access.alchemyapi.com/calls/";
    private static long nGetTextRequests = 0;
	public static long getTextRequests() { return nGetTextRequests; }
    private static long nGetExtractRequests = 0;
	public static long getExtractRequests() { return nGetExtractRequests; }    
	public static void transactionLimitExceeded() { nGetTextRequests--; }
    
	private boolean sentimentEnabled = true;
	public void setSentimentEnabled(boolean b) { sentimentEnabled = b; }
	
 // Initialize the Logger
	private static final Logger logger = Logger.getLogger(AlchemyAPI_JSON.class);
	
//////////////////////////////////////////////////////////////////////////////////////////
	
// Configuration:	

    private AlchemyAPI_JSON() {
    }

    static public AlchemyAPI_JSON GetInstanceFromProperties()
    {
    	AlchemyAPI_JSON api = new AlchemyAPI_JSON();
    	
        try 
        {
        	api.SetAPIKey(new PropertiesManager().getExtractorKey("AlchemyApi"));
		} 
        catch (Exception e) 
        {
        	// If an exception occurs log the error
			logger.error("Exception Message: " + e.getMessage(), e);
			return null;
		} 

        return api;
    }

    static public AlchemyAPI_JSON GetInstanceFromFile(String keyFilename)
        throws FileNotFoundException, IOException
    {
        AlchemyAPI_JSON api = new AlchemyAPI_JSON();
        api.LoadAPIKey(keyFilename);

        return api;
    }

    static public AlchemyAPI_JSON GetInstanceFromString(String apiKey)
    {
        AlchemyAPI_JSON api = new AlchemyAPI_JSON();
        api.SetAPIKey(apiKey);

        return api;
    }

    public void LoadAPIKey(String filename) throws IOException, FileNotFoundException
    {
        if (null == filename || 0 == filename.length())
            throw new IllegalArgumentException("Empty API key file specified.");

        File file = new File(filename);
        FileInputStream fis = new FileInputStream(file);

        BufferedReader breader = new BufferedReader(new InputStreamReader(fis));

        _apiKey = breader.readLine().replaceAll("\\n", "").replaceAll("\\r", "");

        fis.close();
        breader.close();

        if (null == _apiKey || _apiKey.length() < 5)
            throw new IllegalArgumentException("Too short API key.");
    }

    public void SetAPIKey(String apiKey) {
        _apiKey = apiKey;

        if (null == _apiKey || _apiKey.length() < 5)
            throw new IllegalArgumentException("Too short API key.");
    }

    public void SetAPIHost(String apiHost) {
        if (null == apiHost || apiHost.length() < 2)
            throw new IllegalArgumentException("Too short API host.");

        _requestUri = "http://" + apiHost + ".alchemyapi.com/calls/";
    }

//////////////////////////////////////////////////////////////////////////////////////////   
    
// Top level calls  
    
    // 1] Get entities from URL
    
    public String URLGetRankedNamedEntities(String url)
        throws IOException, SAXException,
               ParserConfigurationException, XPathExpressionException, ExtractorDocumentLevelException
    {
        CheckURL(url);
        
        AlchemyAPI_NamedEntityParams params = new AlchemyAPI_NamedEntityParams();
        params.setUrl(url);
        params.setOutputMode(AlchemyAPI_NamedEntityParams.OUTPUT_JSON);
        params.setShowSourceText(true);
        if (sentimentEnabled) {
        	params.setCustomParameters("sentiment", "1");
        }

        return POST("URLGetRankedNamedEntities", "url", params);
    }
    
     // 2] Get entities from text
    
    public String TextGetRankedNamedEntities(String text)
    throws IOException, SAXException,
           ParserConfigurationException, XPathExpressionException, ExtractorDocumentLevelException
	{
	    return TextGetRankedNamedEntities(text, new AlchemyAPI_NamedEntityParams());
	}
	public String TextGetRankedNamedEntities(String text, AlchemyAPI_NamedEntityParams params)
	throws IOException, SAXException,
	       ParserConfigurationException, XPathExpressionException, ExtractorDocumentLevelException
	{
		CheckText(text);
	
		params.setText(text);
		params.setOutputMode(AlchemyAPI_NamedEntityParams.OUTPUT_JSON);
        if (sentimentEnabled) {
        	params.setCustomParameters("sentiment", "1");
        }
	
		nGetExtractRequests++;		
		return POST("TextGetRankedNamedEntities", "text", params);
	}

	// 3] Get text
	
	public String URLGetText(String url)
    throws IOException, SAXException,
           ParserConfigurationException, XPathExpressionException, ExtractorDocumentLevelException
	{
	    return URLGetText(url, new AlchemyAPI_TextParams());
	}
    
    public String URLGetText(String url, AlchemyAPI_TextParams params )
    throws IOException, SAXException,
           ParserConfigurationException, XPathExpressionException, ExtractorDocumentLevelException
	{
	    CheckURL(url);
	    
	    params.setUrl(url);
	
	    nGetTextRequests++;
	    return GET("URLGetText", "url", params);
	}
    
    // 4a] Get keywords from text
    
    public String TextGetRankedKeywords(String text)
    throws IOException, SAXException,
           ParserConfigurationException, XPathExpressionException, ExtractorDocumentLevelException
	{
	    return TextGetRankedKeywords(text, new AlchemyAPI_NamedEntityParams());
	}
	public String TextGetRankedKeywords(String text, AlchemyAPI_NamedEntityParams params)
	throws IOException, SAXException,
	       ParserConfigurationException, XPathExpressionException, ExtractorDocumentLevelException
	{
		CheckText(text);
	
		params.setText(text);
		params.setOutputMode(AlchemyAPI_NamedEntityParams.OUTPUT_JSON);
        if (sentimentEnabled) {
        	params.setCustomParameters("sentiment", "1");
        }
        // Default is normal, not sure which is best
        //params.setCustomParameters("keywordExtractMode", "strict");
	
		nGetExtractRequests++;		
		return POST("TextGetRankedKeywords", "text", params);
	}

    // 4b] Get keywords from URL

	public String URLGetRankedKeywords(String url)
	throws IOException, SAXException,
	ParserConfigurationException, XPathExpressionException, ExtractorDocumentLevelException
	{
		CheckURL(url);

		AlchemyAPI_NamedEntityParams params = new AlchemyAPI_NamedEntityParams();
		params.setUrl(url);
		params.setOutputMode(AlchemyAPI_NamedEntityParams.OUTPUT_JSON);
		params.setShowSourceText(true);
        if (sentimentEnabled) {
        	params.setCustomParameters("sentiment", "1");
        }

		return POST("URLGetRankedKeywords", "url", params);
	}
	
    // 5a] Get concepts from text
    
    public String TextGetRankedConcepts(String text)
    throws IOException, SAXException,
           ParserConfigurationException, XPathExpressionException, ExtractorDocumentLevelException
	{
	    return TextGetRankedConcepts(text, new AlchemyAPI_NamedEntityParams());
	}
	public String TextGetRankedConcepts(String text, AlchemyAPI_NamedEntityParams params)
	throws IOException, SAXException,
	       ParserConfigurationException, XPathExpressionException, ExtractorDocumentLevelException
	{
		CheckText(text);
	
		params.setText(text);
		params.setOutputMode(AlchemyAPI_NamedEntityParams.OUTPUT_JSON);
	
		nGetExtractRequests++;		
		return POST("TextGetRankedConcepts", "text", params);
	}

    // 5b] Get concepts from URL

	public String URLGetRankedConcepts(String url)
	throws IOException, SAXException,
	ParserConfigurationException, XPathExpressionException, ExtractorDocumentLevelException
	{
		CheckURL(url);

		AlchemyAPI_NamedEntityParams params = new AlchemyAPI_NamedEntityParams();
		params.setUrl(url);
		params.setOutputMode(AlchemyAPI_NamedEntityParams.OUTPUT_JSON);
		params.setShowSourceText(true);

		return POST("URLGetRankedConcepts", "url", params);
	}
	
	
	
//////////////////////////////////////////////////////////////////////////////////////////
	
// Utility functions, level 1

    private void CheckURL(String url) {
        if (null == url || url.length() < 10)
            throw new IllegalArgumentException("Enter an URL to analyze.");
    }
	private void CheckText(String text) {
        if (null == text || text.length() < 5)
            throw new IllegalArgumentException("Enter some text to analyze.");
    }
	
    
// Utility functions, level 2 
    
	// (only used for pure get text)
	
    private String GET(String callName, String callPrefix, AlchemyAPI_Params params)
    throws IOException, SAXException,
           ParserConfigurationException, XPathExpressionException, ExtractorDocumentLevelException
	{
	    StringBuilder uri = new StringBuilder();
	    uri.append(_requestUri).append(callPrefix).append('/').append(callName)
	       .append('?').append("apikey=").append(this._apiKey.trim()).append("&outputMode=json");
	    uri.append(params.getParameterString());
	
	    URL url = new URL(uri.toString());
	    HttpURLConnection handle = (HttpURLConnection) url.openConnection();
	    handle.setDoOutput(true);
	
	    return doRequest(handle);
	}
    
    // (Used for everything else)
    
    private String POST(String callName, String callPrefix, AlchemyAPI_Params params)
    throws IOException, SAXException,
           ParserConfigurationException, XPathExpressionException, ExtractorDocumentLevelException
	{
	    URL url = new URL(_requestUri + callPrefix + "/" + callName);
	
	    HttpURLConnection handle = (HttpURLConnection) url.openConnection();
	    handle.setDoOutput(true);
	
	    StringBuilder data = new StringBuilder();
	
	    data.append("apikey=").append(this._apiKey);
	    data.append(params.getParameterString());
	
	    handle.addRequestProperty("Content-Length", Integer.toString(data.length()));
	
	    DataOutputStream ostream = new DataOutputStream(handle.getOutputStream());
	    ostream.write(data.toString().getBytes());
	    ostream.close();
	
	    return doRequest(handle);
	}

// Utility functions level 3
    
    private String doRequest(HttpURLConnection handle)
    throws IOException, SAXException,
           ParserConfigurationException, XPathExpressionException, ExtractorDocumentLevelException
	{
    	String toReturn = "";
	    InputStream istream = handle.getInputStream();
	    try
	    {
	    	toReturn = convertStreamToString(istream);
	    }
	    catch (Exception e)
	    {
			throw new InfiniteEnums.ExtractorDocumentLevelException();
	    }
	    finally {
	    	istream.close();
	    	handle.disconnect();		
	    }
	    return toReturn;
	}
    
    public static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
          sb.append(line + "\n");
        }
        is.close();
        return sb.toString();
      }        
}
