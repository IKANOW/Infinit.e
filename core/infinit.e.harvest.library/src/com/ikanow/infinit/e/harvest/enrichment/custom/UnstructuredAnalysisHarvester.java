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
package com.ikanow.infinit.e.harvest.enrichment.custom;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptContext;
import javax.script.ScriptException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.bson.types.ObjectId;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.DomSerializer;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.gson.stream.JsonReader;
import com.ikanow.infinit.e.data_model.Globals;
import com.ikanow.infinit.e.data_model.InfiniteEnums.ExtractorDocumentLevelException;
import com.ikanow.infinit.e.data_model.store.config.source.SimpleTextCleanserPojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePipelinePojo.ManualTextExtractionSpecPojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePipelinePojo.MetadataSpecPojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourceRssConfigPojo;
import com.ikanow.infinit.e.data_model.store.config.source.UnstructuredAnalysisConfigPojo;
import com.ikanow.infinit.e.data_model.store.config.source.UnstructuredAnalysisConfigPojo.Context;
import com.ikanow.infinit.e.data_model.store.config.source.UnstructuredAnalysisConfigPojo.metaField;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.data_model.utils.IkanowSecurityManager;
import com.ikanow.infinit.e.harvest.HarvestContext;
import com.ikanow.infinit.e.harvest.HarvestController;
import com.ikanow.infinit.e.harvest.enrichment.script.CompiledScriptFactory;
import com.ikanow.infinit.e.harvest.enrichment.script.CompiledScriptWrapperUtility;
import com.ikanow.infinit.e.harvest.extraction.document.file.JsonToMetadataParser;
import com.ikanow.infinit.e.harvest.extraction.document.file.XmlToMetadataParser;
import com.ikanow.infinit.e.harvest.extraction.text.legacy.TextExtractorTika;
import com.ikanow.infinit.e.harvest.utils.HarvestExceptionUtils;
import com.ikanow.infinit.e.harvest.utils.PropertiesManager;
import com.ikanow.infinit.e.harvest.utils.ProxyManager;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

/**
 * UnstructuredAnalysisHarvester
 */
public class UnstructuredAnalysisHarvester {
	
	///////////////////////////////////////////////////////////////////////////////////////////
	private CompiledScriptFactory compiledScriptFactory = null;

	// NEW PROCESSING PIPELINE INTERFACE

	//TODO (INF-1922): Handle headers and footers
	public void setContext(HarvestContext context) {
		_context = context;
		securityManager = _context.getSecurityManager();
	} 
	
	// Transform the doc's text (go get it if necessary)
	
	public String doManualTextEnrichment(DocumentPojo doc, List<ManualTextExtractionSpecPojo> textExtractors, SourceRssConfigPojo feedConfig) throws IOException {
		String cachedFullText = null;
		// Map to the legacy format and then call the legacy code 
		ArrayList<SimpleTextCleanserPojo> mappedTextExtractors = new ArrayList<SimpleTextCleanserPojo>(textExtractors.size());
		for (ManualTextExtractionSpecPojo textExtractor: textExtractors) {
			if (DocumentPojo.fullText_.equalsIgnoreCase(textExtractor.fieldName)) {
				boolean fullTextNeeded = (null == doc.getFullText()); // (check here so we can cache it)
				if (fullTextNeeded) {
					getRawTextFromUrlIfNeeded(doc, feedConfig);				
						// (if transforming full text then grab the raw body from the URL if necessary)
					cachedFullText = doc.getFullText();
				}//TESTED (by hand)
			}			
			SimpleTextCleanserPojo mappedTextExtractor = new SimpleTextCleanserPojo();
			mappedTextExtractor.setField(textExtractor.fieldName);
			mappedTextExtractor.setFlags(textExtractor.flags);
			mappedTextExtractor.setScript(textExtractor.script);
			mappedTextExtractor.setScriptlang(textExtractor.scriptlang);
			mappedTextExtractor.setReplacement(textExtractor.replacement);
			mappedTextExtractors.add(mappedTextExtractor);
		}
		this.cleanseText(mappedTextExtractors, doc);
		
		return cachedFullText;		
	}
	//TESTED (fulltext_regexTests.json)
	
	public void processMetadataChain(DocumentPojo doc, List<MetadataSpecPojo> metadataFields, SourceRssConfigPojo feedConfig, HashSet<String> unstoredFields) throws IOException
	{		
		// Map metadata list to a legacy meta format (they're really similar...)
		UnstructuredAnalysisConfigPojo.metaField mappedEl = new UnstructuredAnalysisConfigPojo.metaField();
		boolean textSet = false;
		
		for (MetadataSpecPojo meta: metadataFields) {
			mappedEl.fieldName = meta.fieldName;
			mappedEl.context = Context.All;
			mappedEl.flags = meta.flags;
			if (null == mappedEl.flags) {
				mappedEl.flags = "";
			}
			boolean scriptLangNeedsText = doesScriptLangNeedText(meta.scriptlang);			
				// (js you can operate only on metadata, other languages you need the text ...
				//  actually that is not true, because of chaining ugh - we'll just ignore that for now)
			
			if (scriptLangNeedsText || (null == mappedEl.flags) || mappedEl.flags.isEmpty() || mappedEl.flags.contains("t")) {
				if (!textSet) {
					getRawTextFromUrlIfNeeded(doc, feedConfig);				
					textSet = true;
				}
			}//TESTED (content_needed_test)
			// DO not remove until unless you want to debug for 2 days
			//ScriptUtil.printEngineState(compiledScriptFactory);

			mappedEl.scriptlang = meta.scriptlang;
			mappedEl.script = meta.script;
			mappedEl.replace = meta.replace;
			mappedEl.groupNum = null;
			//(no group num - just use replace, and flags "o" for xpath/gN:-1)

			// Storage of fields:
			if ((null != meta.store) && !meta.store) {
				unstoredFields.add(meta.fieldName); 
			}
			else if ((null != unstoredFields) && unstoredFields.contains(meta.fieldName)) {
				unstoredFields.remove(meta.fieldName);
			}
			this.processMeta(doc, mappedEl, doc.getFullText(), null, null);						
		} // metaSpecFields
		//TESTED (storageSettings_advanced.json)
	}
	//TESTED (fulltext_regexTests.json, storageSettings_advanced.json)
	
	///////////////////////////////////////////////////////////////////////////////////////////
	
	// PROCESSING PIPELINE - UTILITIES
	
	public void getRawTextFromUrlIfNeeded(DocumentPojo doc, SourceRssConfigPojo feedConfig) throws IOException {
		if (null != doc.getFullText()) { // Nothing to do
			return;
		}
		Scanner s = null;
		OutputStreamWriter wr = null;
		try {
			URL url = new URL(doc.getUrl());
			URLConnection urlConnect = null;
			String postContent = null;
			if (null != feedConfig) {
				urlConnect = url.openConnection(ProxyManager.getProxy(url, feedConfig.getProxyOverride()));
				if (null != feedConfig.getUserAgent()) {
					urlConnect.setRequestProperty("User-Agent", feedConfig.getUserAgent());
				}// TESTED (by hand)
				if (null != feedConfig.getHttpFields()) {
					for (Map.Entry<String, String> httpFieldPair: feedConfig.getHttpFields().entrySet()) {
						if (httpFieldPair.getKey().equalsIgnoreCase("content")) {
							postContent = httpFieldPair.getValue();
							urlConnect.setDoInput(true);
							urlConnect.setDoOutput(true);
						}
						else {
							urlConnect.setRequestProperty(httpFieldPair.getKey(), httpFieldPair.getValue());
						}
					}
				}//TESTED (by hand)
			}
			else {
				urlConnect = url.openConnection();				
			}
			InputStream urlStream = null;
			try {
				securityManager.setSecureFlag(true); // (disallow file/local URL access)
				if (null != postContent) {
					wr = new OutputStreamWriter(urlConnect.getOutputStream());
					wr.write(postContent.toCharArray());
					wr.flush();
				}//TESTED
				urlStream = urlConnect.getInputStream();
			}
			catch (SecurityException se) {
				throw se;
			}
			catch (Exception e) { // Try one more time, this time exception out all the way
				securityManager.setSecureFlag(false); // (some file stuff - so need to re-enable)
				if (null != feedConfig) {
					urlConnect = url.openConnection(ProxyManager.getProxy(url, feedConfig.getProxyOverride()));
					if (null != feedConfig.getUserAgent()) {
						urlConnect.setRequestProperty("User-Agent", feedConfig.getUserAgent());
					}// TESTED
					if (null != feedConfig.getHttpFields()) {
						for (Map.Entry<String, String> httpFieldPair: feedConfig.getHttpFields().entrySet()) {
							if (httpFieldPair.getKey().equalsIgnoreCase("content")) {
								urlConnect.setDoInput(true); // (need to do this again)
								urlConnect.setDoOutput(true);						
							}
							else {
								urlConnect.setRequestProperty(httpFieldPair.getKey(), httpFieldPair.getValue());														
							}
						}
					}//TESTED
				}
				else {
					urlConnect = url.openConnection();				
				}
				securityManager.setSecureFlag(true); // (disallow file/local URL access)
				if (null != postContent) {
					wr = new OutputStreamWriter(urlConnect.getOutputStream());
					wr.write(postContent.toCharArray());
					wr.flush();
				}//TESTED
				urlStream = urlConnect.getInputStream();
			}
			finally {
				securityManager.setSecureFlag(false); // (turn security check for local URL/file access off)
			}
			// Grab any interesting header fields
			Map<String, List<String>> headers = urlConnect.getHeaderFields();
			BasicDBObject metadataHeaderObj = null;
			for (Map.Entry<String, List<String>> it: headers.entrySet()) {
				if (null != it.getKey()) {
					if (it.getKey().startsWith("X-") || it.getKey().startsWith("Set-") || it.getKey().startsWith("Location")) {
						if (null == metadataHeaderObj) {
							metadataHeaderObj = new  BasicDBObject();
						}
						metadataHeaderObj.put(it.getKey(), it.getValue());
					}
				}
			}//TESTED
			// Grab the response code
			try {
				HttpURLConnection httpUrlConnect = (HttpURLConnection)urlConnect;
				int responseCode = httpUrlConnect.getResponseCode();
				if (200 != responseCode) {
					if (null == metadataHeaderObj) {
						metadataHeaderObj = new  BasicDBObject();
					}
					metadataHeaderObj.put("responseCode", String.valueOf(responseCode));
				}
			}//TESTED
			catch (Exception e) {} // interesting, not an HTTP connect ... shrug and carry on
			if (null != metadataHeaderObj) {
				doc.addToMetadata("__FEED_METADATA__", metadataHeaderObj);
			}//TESTED
			
			String contentType = urlConnect.getContentType();
			
			if ((null != contentType) && contentType.contains("html")) { // HTML
				s = new Scanner(urlStream, "UTF-8");
				s.useDelimiter("\\A");
				doc.setFullText(s.next());																			
			}
			else { // not HTML, send to tika instead
				if (null == _tika) {
					_tika = new Tika();
				}
				Metadata metadata = new Metadata();
				String text = _tika.parseToString(urlStream, metadata);
				doc.setFullText(text);					
				TextExtractorTika.addMetadata(doc, metadata);
			}//TESTED			
		}
		catch (MalformedURLException me) { // This one is worthy of a more useful error message
			throw new MalformedURLException(me.getMessage() + ": Likely because the document has no full text (eg JSON) and you are calling a contentMetadata block without setting flags:'m' or 'd'");
		} catch (TikaException e) {
			throw new MalformedURLException(e.getMessage() + ": tika error on binary data");
		}
		finally { //(release resources)
			if (null != s) {
				s.close();
			}
			if (null != wr) {
				wr.close();
			}
		}		
		
	}//TESTED (cut-and-paste from existing code, so new testing very cursory) 
	
	///////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////
	
	// LEGACY CODE - USE TO SUPPORT OLD CODE FOR NOW + AS UTILITY CODE FOR THE PIPELINE LOGIC
	
	// Per-source state
	private Pattern headerPattern = null;
	private Pattern footerPattern = null;
	private UnstructuredAnalysisConfigPojo savedUap = null;

	// Using Tika to process documents:
	TextExtractorTika tikaExtractor = null; // (cases where the user specifically asks for it)
	protected Tika _tika = null;  // (mutually exclusive case where we override because it's a binary file)
	
	private HarvestContext _context = null;
	private Logger logger = Logger.getLogger(UnstructuredAnalysisHarvester.class);

	// (some web scraping may be needed)
	private long nBetweenDocs_ms = -1;
	// (set this in execute harvest - makes it easy to only set once in the per doc version called in bulk from the SAH)

	// Ensure we don't get long list of duplicates for commonly occurring words
	private HashSet<String> regexDuplicates = null;
	private HtmlCleaner cleaner = null;
	
	//if the sah already init'd an engine we'll just use it
//	private ScriptEngine _sahEngine = null; 
	private IkanowSecurityManager securityManager = null;
	
	/**
	 * Default Constructor
	 */
	public UnstructuredAnalysisHarvester() {
	}

	// For harvest pipeline, just ensures duplicate map exists and is empty for each doc
	public void resetForNewDoc() {
		if ((null == regexDuplicates) || (!regexDuplicates.isEmpty())) {
			regexDuplicates = new HashSet<String>();
		}
	}
	
	/**
	 * executeHarvest(SourcePojo source, List<DocumentPojo> feeds)
	 * 
	 * @param source
	 * @param feeds
	 * @return List<DocumentPojo>
	 */
	public List<DocumentPojo> executeHarvest(HarvestController contextController, SourcePojo source, List<DocumentPojo> documents)
	{
		nBetweenDocs_ms = -1;
		// Can override the default (feed) wait time from within the source (eg
		// for sites that we know don't get upset about getting hammered)
		if (null != source.getRssConfig()) {
			if (null != source.getRssConfig().getWaitTimeOverride_ms()) {
				nBetweenDocs_ms = source.getRssConfig().getWaitTimeOverride_ms();
			}
		}
		if (-1 == nBetweenDocs_ms) {
			PropertiesManager props = new PropertiesManager();
			nBetweenDocs_ms = props.getWebCrawlWaitTime();
		}
		// TESTED: default and overridden values

		_context = contextController;
		securityManager = _context.getSecurityManager();
		UnstructuredAnalysisConfigPojo uap = source.getUnstructuredAnalysisConfig();

		if (uap != null) {
			boolean bGetRawDoc = source.getExtractType().equalsIgnoreCase("feed");

			String headerRegEx = uap.getHeaderRegEx();
			String footerRegEx = uap.getFooterRegEx();
			List<metaField> meta = uap.getMeta();

			if (headerRegEx != null)
				headerPattern = createRegex(headerRegEx, uap.getHeaderRegExFlags());
			if (footerRegEx != null)
				footerPattern = createRegex(footerRegEx, uap.getFooterRegExFlags());

			Iterator<DocumentPojo> it = documents.iterator();
			int nDocs = 0;
			while (it.hasNext()) {
				nDocs++;
				DocumentPojo d = it.next();
 				regexDuplicates = new HashSet<String>();
				cleaner = null;

				// For feeds, may need to go get the document text manually,
				// it's a bit horrible since
				// obviously may then go get the data again for full text
				// extraction
				boolean bFetchedUrl = false;
				if (bGetRawDoc && (null == d.getFullText())) {
					if (null == source.getRssConfig()) {
						source.setRssConfig(new SourceRssConfigPojo()); // (makes logic easier down the road)
					}
					// (first time through, sleep following a URL/RSS access)
					if ((1 == nDocs) && (null != source.getUrl())) { // (have already made a call to RSS (or "searchConfig" URL)
						try {
							Thread.sleep(nBetweenDocs_ms);
						} catch (InterruptedException e) {
						}
					}
					// TESTED (first time only, correct value after searchConfig override)

					try {
						if ((null != source.useTextExtractor()) && source.useTextExtractor().equalsIgnoreCase("tika")) {
							// Special case: if tika enabled then do that first
							if (null == tikaExtractor) {
								tikaExtractor = new TextExtractorTika();
							}
							tikaExtractor.extractText(d);
						}
						else {
							this.getRawTextFromUrlIfNeeded(d, source.getRssConfig());
						}
						bFetchedUrl = true;
						
					} catch (Exception e) { // Failed to get full text twice, remove doc
						if (e instanceof SecurityException) { // This seems worthy of actually logging, even though it's a lowly doc error
							contextController.getHarvestStatus().logMessage(e.getMessage(), true);
						}
						contextController.handleExtractError(e, source); //handle extractor error if need be				
						it.remove();
						d.setTempSource(null); // (can safely corrupt this doc since it's been removed)						
						continue;
					}
				}
				long nTime_ms = System.currentTimeMillis();
				// ^^^ (end slight hack to get raw text to the UAH for RSS feeds)

				try {
					processBody(d, meta, true, source, uap);
				} catch (Exception e) {
					this._context.getHarvestStatus().logMessage("processBody1: " + e.getMessage(), true);
					//DEBUG (don't output log messages per doc)
					//logger.error("processBody1: " + e.getMessage(), e);
				}

				try {
					if (uap.getSimpleTextCleanser() != null) {
						cleanseText(uap.getSimpleTextCleanser(), d);
					}
				} catch (Exception e) {
					this._context.getHarvestStatus().logMessage("cleanseText: " + e.getMessage(), true);
					//DEBUG (don't output log messages per doc)
					//logger.error("cleanseText: " + e.getMessage(), e);
				}

				try {
					processHeader(headerPattern, d, meta, source, uap);
					processFooter(footerPattern, d, meta, source, uap);
					
				} catch (Exception e) {
					this._context.getHarvestStatus().logMessage("header/footerPattern: " + e.getMessage(), true);
					//DEBUG (don't output log messages per doc)
					//logger.error("header/footerPattern: " + e.getMessage(), e);
				}
				try {
					processBody(d, meta, false, source, uap);
					
				} catch (Exception e) {
					logger.error("executeHarvest caught exception calling processBody:",e);
					this._context.getHarvestStatus().logMessage("processBody2: " + e.getMessage(), true);
					//DEBUG (don't output log messages per doc)
					//logger.error("processBody2: " + e.getMessage(), e);
				}

				if (it.hasNext() && bFetchedUrl) {
					nTime_ms = nBetweenDocs_ms
							- (System.currentTimeMillis() - nTime_ms); // (ie delay time - processing time)
					if (nTime_ms > 0) {
						try {
							Thread.sleep(nTime_ms);
						} catch (InterruptedException e) {
						}
					}
				} // (end politeness delay for URL getting from a single source (likely site)
			}
			return documents;
		}
		return new ArrayList<DocumentPojo>();
	}

	/**
	 * executeHarvest For single-feed calls (note exception handling happens in
	 * SAH)
	 * 
	 * @param source
	 * @param doc
	 * @return
	 * @throws ExtractorDocumentLevelException 
	 */
	public boolean executeHarvest(HarvestContext context, SourcePojo source, DocumentPojo doc, boolean bFirstTime, boolean bMoreDocs) throws ExtractorDocumentLevelException
	{		
		regexDuplicates = new HashSet<String>();
		cleaner = null;
		boolean bGetRawDoc = source.getExtractType().equalsIgnoreCase("feed")
								&& (null == doc.getFullText());
		// (ie don't have full text and will need to go fetch it from network)

		if (bFirstTime) {
			nBetweenDocs_ms = -1; // (reset eg bewteen searchConfig and SAH)
		}
		if ((-1 == nBetweenDocs_ms) && bGetRawDoc && (bMoreDocs || bFirstTime)) { // (don't bother if not using it...)
			// Can override the default (feed) wait time from within the source
			// (eg for sites that we know
			// don't get upset about getting hammered)
			if (null != source.getRssConfig()) {
				if (null != source.getRssConfig().getWaitTimeOverride_ms()) {
					nBetweenDocs_ms = source.getRssConfig().getWaitTimeOverride_ms();
				}
			}
			if (-1 == nBetweenDocs_ms) { // (ie not overridden so use default)
				PropertiesManager props = new PropertiesManager();
				nBetweenDocs_ms = props.getWebCrawlWaitTime();
			}
		} // TESTED (overridden and using system default)

		_context = context;
		securityManager = _context.getSecurityManager();
		UnstructuredAnalysisConfigPojo uap = source.getUnstructuredAnalysisConfig();

		int nChanges = 0;
		if (null != doc.getMetaData()) {
			nChanges = doc.getMetaData().size();
		}
		boolean bFetchedUrl = false;
		if (bGetRawDoc) {
			if (null == source.getRssConfig()) {
				source.setRssConfig(new SourceRssConfigPojo()); // (makes logic easier down the road)
			}
			try {
				// Workaround for observed twitter bug (first access after the
				// RSS was gzipped)
				if (bFirstTime) {
					// (first time through, sleep following a URL/RSS access)
					if (null != source.getUrl()) { // (have already made a call to RSS (or "searchConfig" URL)
						try {
							Thread.sleep(nBetweenDocs_ms);
						} catch (InterruptedException e) {
						}
					}
					// TESTED
				}
				
				if ((null != source.useTextExtractor()) && source.useTextExtractor().equalsIgnoreCase("tika")) {
					// Special case: if tika enabled then do that first
					if (null == tikaExtractor) {
						tikaExtractor = new TextExtractorTika();
						tikaExtractor.extractText(doc);
					}
				}
				else {
					getRawTextFromUrlIfNeeded(doc, source.getRssConfig());
				}
				bFetchedUrl = true;
				
			}
			catch (SecurityException e) { // This seems worthy of actually logging, even though it's a lowly doc error
				_context.getHarvestStatus().logMessage(e.getMessage(), true);
				throw new ExtractorDocumentLevelException(e.getMessage());
			}//TESTED
			catch (Exception e) { // Failed to get full text twice... remove doc and carry on
				throw new ExtractorDocumentLevelException(e.getMessage());
			}
		}
		long nTime_ms = System.currentTimeMillis();
		// ^^^ (end slight hack to get raw text to the UAH for RSS feeds)

		if (uap != null) {
			List<metaField> meta = uap.getMeta();
			if (savedUap != uap) {
				String headerRegEx = uap.getHeaderRegEx();
				String footerRegEx = uap.getFooterRegEx();

				if (headerRegEx != null)
					headerPattern = Pattern.compile(headerRegEx, Pattern.DOTALL);
				if (footerRegEx != null)
					footerPattern = Pattern.compile(footerRegEx, Pattern.DOTALL);

				savedUap = uap;
			}
			try {
				processBody(doc, meta, true, source, uap);
				
			} catch (Exception e) {
				logger.error("executeHarvest caught exception calling processBody:",e);
				this._context.getHarvestStatus().logMessage("processBody1: " + e.getMessage(), true);
				//DEBUG (don't output log messages per doc)
				//logger.error("processBody1: " + e.getMessage(), e);
			}
			try {
				if (uap.getSimpleTextCleanser() != null) {
					cleanseText(uap.getSimpleTextCleanser(), doc);
				}
			} catch (Exception e) {
				this._context.getHarvestStatus().logMessage("cleanseText: " + e.getMessage(), true);
				//DEBUG (don't output log messages per doc)
				//logger.error("cleanseText: " + e.getMessage(), e);
			}
			try {
				processHeader(headerPattern, doc, meta, source, uap);
				processFooter(footerPattern, doc, meta, source, uap);
				
			} catch (Exception e) {
				logger.error("executeHarvest caught exception calling processHeader/Footer:",e);
				this._context.getHarvestStatus().logMessage("header/footerPattern: " + e.getMessage(), true);
				//DEBUG (don't output log messages per doc)
				//logger.error("header/footerPattern: " + e.getMessage(), e);
			}
			try {
				processBody(doc, meta, false, source, uap);
				
			} catch (Exception e) {
				logger.error("executeHarvest caught exception calling processBody:",e);
				this._context.getHarvestStatus().logMessage("processBody2: " + e.getMessage(), true);
				//DEBUG (don't output log messages per doc)
				//logger.error("processBody2: " + e.getMessage(), e);
			}
		}
		if (bMoreDocs && bFetchedUrl) {
			nTime_ms = nBetweenDocs_ms - (System.currentTimeMillis() - nTime_ms); // (ie delay time - processing time)
			if (nTime_ms > 0) {
				try {
					Thread.sleep(nTime_ms);
				} catch (InterruptedException e) {
				}
			}
		} // (end politeness delay for URL getting from a single source (likely site)

		if (null != doc.getMetaData()) {
			if (nChanges != doc.getMetaData().size()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * processHeader
	 * 
	 * @param headerPattern
	 * @param f
	 * @param meta
	 */
	@SuppressWarnings("deprecation")
	private void processHeader(Pattern headerPattern, DocumentPojo f, List<metaField> meta, SourcePojo source, UnstructuredAnalysisConfigPojo uap)
	{
		if (headerPattern != null) {
			Matcher headerMatcher = headerPattern.matcher(f.getFullText());
			String headerText = null;
			while (headerMatcher.find()) {
				if (headerMatcher.start() == 0) {
					headerText = headerMatcher.group(0);
					f.setHeaderEndIndex(headerText.length());
					for (int i = 1; i < headerMatcher.groupCount() + 1; i++) {
						f.addToHeader(headerMatcher.group(i).trim());
					}
					break;
				}
			}
			if (null != headerText && null != meta) {
				for (metaField m : meta) {
					if (m.context == Context.Header || m.context == Context.All) {
						String text = null;
						try{
							text = f.getFullText();
							f.setFullText(headerText);
							this.processMeta(f, m, headerText, source, uap);
						}
						finally{
								f.setFullText(text);
						}
					}
				}
			}
		}
	}

	/**
	 * processFooter
	 * 
	 * @param footerPattern
	 * @param f
	 * @param meta
	 */
	@SuppressWarnings("deprecation")
	private void processFooter(Pattern footerPattern, DocumentPojo f, List<metaField> meta, SourcePojo source, UnstructuredAnalysisConfigPojo uap)
	{

		if (footerPattern != null) {
			Matcher footerMatcher = footerPattern.matcher(f.getFullText());
			String footerText = null;
			while (footerMatcher.find()) {
				footerText = footerMatcher.group(0);
				int docLength = f.getFullText().length();
				f.setFooterStartIndex(docLength - footerMatcher.group(0).length());
				for (int i = 1; i < footerMatcher.groupCount() + 1; i++) {
					f.addToHeader(footerMatcher.group(i).trim());
				}
				break;
			}

			if (null != footerText && null != meta) {
				for (metaField m : meta) {
					if (m.context == Context.Footer || m.context == Context.All) {
						// hack, put text into doc.Fulltect for engine and replace back afterwards
						String text = null;
						try{
							text = f.getFullText();
							f.setFullText(footerText);
							this.processMeta(f, m, footerText, source, uap);
						}finally{						
							f.setFullText(text);
						}
					}
				}
			}
		}
	}

	/**
	 * processBody
	 * 
	 * @param f
	 * @param meta
	 */
	@SuppressWarnings("deprecation")
	private void processBody(DocumentPojo f, List<metaField> meta, boolean bPreCleansing, SourcePojo source, UnstructuredAnalysisConfigPojo uap)
	{
		if (null != meta) {
			for (metaField m : meta) {
				if ((bPreCleansing && (m.context == Context.First))
						|| (!bPreCleansing && (m.context == Context.Body || m.context == Context.All))) {
					
					boolean scriptLangNeedsText = doesScriptLangNeedText(m.scriptlang);			
					// (js you can operate only on metadata, other languages you need the text ...
					//  actually that is not true, because of chaining ugh - we'll just ignore that for now)
				
					// If running with text (default) then need there to be non-null text
					String toProcess = null;
					if (scriptLangNeedsText || (null == m.flags) ||m.flags.isEmpty() || m.flags.contains("t")) {
						toProcess = f.getBody();
						if (toProcess == null)
							toProcess = f.getDescription();
						if (toProcess == null)
							continue;
					}
					// hack, put text into doc.Fulltect for engine and replace back afterwards
					String text = null;
					try{
						text = f.getFullText();
						f.setFullText(toProcess);
						this.processMeta(f, m, toProcess, source, uap);
					}finally{						
						f.setFullText(text);
					}
				}
			}
		}
	}

	/**
	 * processMeta - handle an individual field
	 */
	private void processMeta(DocumentPojo f, metaField m, String text, SourcePojo source, UnstructuredAnalysisConfigPojo uap) {

		boolean bAllowDuplicates = false;
		if ((null != m.flags) && m.flags.contains("U")) {
			bAllowDuplicates = true;
		}		
		if ((null == m.scriptlang) || m.scriptlang.equalsIgnoreCase("regex")) {

			Pattern metaPattern = createRegex(m.script, m.flags);

			int timesToRun = 1;
			Object[] currField = null;
			if ((null != m.flags) && m.flags.contains("c")) {
				currField = f.getMetadata().get(m.fieldName);
			}
			if (null != currField) { // chained metadata
				timesToRun = currField.length;
				text = (String)currField[0];
			}//TESTED

			Matcher matcher = metaPattern.matcher(text);
			LinkedList<String> Llist = null;
			
			for (int ii = 0; ii < timesToRun; ++ii) {
				if (ii > 0) { // (else either just text, or in the above "chained metadata" initialization above)
					text = (String)currField[ii];		
					matcher = metaPattern.matcher(text);
				}//TESTED
			
				StringBuffer prefix = new StringBuffer(m.fieldName).append(':');
				int nFieldNameLen = m.fieldName.length() + 1;
	
				try {
					while (matcher.find()) {
						if (null == Llist) {
							Llist = new LinkedList<String>();
						}
						if (null == m.groupNum) {
							m.groupNum = 0;
						}
						String toAdd = matcher.group(m.groupNum);
						if (null != m.replace) {
							toAdd = metaPattern.matcher(toAdd).replaceFirst(
									m.replace);
						}
						if ((null != m.flags) && m.flags.contains("H"))	{
							toAdd = StringEscapeUtils.unescapeHtml(toAdd);
						}
						prefix.setLength(nFieldNameLen);
						prefix.append(toAdd);
						String dupCheck = prefix.toString();
	
						if (!regexDuplicates.contains(dupCheck)) {
							Llist.add(toAdd);
							if (!bAllowDuplicates) {
								regexDuplicates.add(dupCheck);
							}
						}
					}
				} catch (Exception e) {
					this._context.getHarvestStatus().logMessage("processMeta1: " + e.getMessage(), true);
				}
			}//(end metadata chaining handling)
			if (null != Llist) {
				if (null != currField) { // (overwrite)
					f.getMetadata().put(m.fieldName, Llist.toArray());
				}
				else {
					f.addToMetadata(m.fieldName, Llist.toArray());
				}
			}//TESTED
		} 
		else if (m.scriptlang.equalsIgnoreCase("javascript")) 
		{
			if (null == f.getMetadata()) {
				f.setMetadata(new LinkedHashMap<String, Object[]>());
			}
			//set the script engine up if necessary
			if ((null != source) && (null != uap)) {
				//(these are null if called from new processing pipeline vs legacy code)
				intializeScriptEngine(source, uap,compiledScriptFactory);
			}
			
			try 
			{
				CompiledScriptWrapperUtility.initializeDocumentPojoInEngine(compiledScriptFactory, f);
				// DO not remove until unless you want to debug for 2 days
				//ScriptUtil.printEngineState(compiledScriptFactory);
				
				//TODO (INF-2488): in new format, this should only happen in between contentMeta blocks/docs
				// (also should be able to use SAH _document object I think?)
				
				// Javascript: the user passes in 
				Object[] currField = f.getMetadataReadOnly().get(m.fieldName);
				if ((null == m.flags) || m.flags.isEmpty()) {
					if (null == currField) {
						// for legacy reasons text is not coming from documentPojo.text here
						compiledScriptFactory.getScriptContext().setAttribute("text", text, ScriptContext.ENGINE_SCOPE);
						compiledScriptFactory.getScriptContext().setAttribute("_iterator", null, ScriptContext.ENGINE_SCOPE);
					}
					//(otherwise will just pass the current fields in there)
				}
				else { // flags specified
					if (m.flags.contains("t")) { // text
						compiledScriptFactory.getScriptContext().setAttribute("text", text, ScriptContext.ENGINE_SCOPE);
					}
				}//(end flags processing)
				
				if (null != currField) {
					f.getMetadata().remove(m.fieldName);
					compiledScriptFactory.executeCompiledScript(JavaScriptUtils.getIteratorOm2js(),"iteratorPojo", currField);
				}
				//TESTED (handling of flags, and replacing of existing fields, including when field is null but specified)

				// DO not remove until unless you want to debug for 2 days
				//ScriptUtil.printEngineState(compiledScriptFactory);
				Object returnVal = compiledScriptFactory.executeCompiledScript(m.script);				        
				if (null != returnVal) {
					if (returnVal instanceof String) { // The only easy case
						Object[] array = new Object[1];
						if ((null != m.flags) && m.flags.contains("H"))	{
							returnVal = StringEscapeUtils.unescapeHtml((String)returnVal);
						}
						array[0] = returnVal;
						f.addToMetadata(m.fieldName, array);
					} else { // complex object or array - in either case the engine turns these into
								// internal.NativeArray or internal.NativeObject
						
						BasicDBList outList = JavaScriptUtils.parseNativeJsObjectCompiled(returnVal, compiledScriptFactory);
						// set metadata to object inside engine
						f.addToMetadata(m.fieldName, outList.toArray());
					}
				}
			} catch (ScriptException e) {

				_context.getHarvestStatus().logMessage(HarvestExceptionUtils.createExceptionMessage(e).toString(), true);

				// Just do nothing and log
				// e.printStackTrace();
				//DEBUG (don't output log messages per doc)
				//logger.error(e.getMessage());
			} catch (Exception e) {
				
				_context.getHarvestStatus().logMessage(HarvestExceptionUtils.createExceptionMessage(e).toString(), true);

				// Just do nothing and log
				// e.printStackTrace();
				//DEBUG (don't output log messages per doc)
				//logger.error(e.getMessage());
			}
		} else if (m.scriptlang.equalsIgnoreCase("xpath")) {

			String xpath = m.script;
			
			try {
				createHtmlCleanerIfNeeded();

				int timesToRun = 1;
				Object[] currField = null;
				if ((null != m.flags) && m.flags.contains("c")) {
					currField = f.getMetadata().get(m.fieldName);
				}
				if (null != currField) { // chained metadata
					f.getMetadata().remove(m.fieldName); // (so will add to the end)
					timesToRun = currField.length;
					text = (String)currField[0];
				}//TESTED

				for (int ii = 0; ii < timesToRun; ++ii) {
					if (ii > 0) { // (else either just text, or in the above "chained metadata" initialization above)
						text = (String)currField[ii];						
					}//TESTED
					
					TagNode node = cleaner.clean(new ByteArrayInputStream(text.getBytes()));
					
					//NewCode : Only use html cleaner for cleansing
					//use JAXP for full Xpath lib
					Document doc = new DomSerializer(new CleanerProperties()).createDOM(node);
					
	
					String extraRegex = extractRegexFromXpath(xpath);
	
					if (extraRegex != null)
						xpath = xpath.replace(extraRegex, "");
					
					XPath xpa = XPathFactory.newInstance().newXPath();
					NodeList res = (NodeList)xpa.evaluate(xpath, doc, XPathConstants.NODESET);
					
					if (res.getLength() > 0)
					{
						if ((null != m.flags) && (m.flags.contains("o"))) { // "o" for object
							m.groupNum = -1; // (see bConvertToObject below)
						}
						StringBuffer prefix = new StringBuffer(m.fieldName).append(':');
						int nFieldNameLen = m.fieldName.length() + 1;
						ArrayList<Object> Llist = new ArrayList<Object>(res.getLength());
						boolean bConvertToObject = ((m.groupNum != null) && (m.groupNum == -1));
						boolean convertToXml =  ((null != m.flags) && (m.flags.contains("x")));
						for (int i= 0; i< res.getLength(); i++)
						{
							Node info_node = res.item(i);
							if ((null != m.flags) && (m.flags.contains("g"))) {
								Llist.add(parseHtmlTable(info_node, m.replace));
							}
							else if (bConvertToObject || convertToXml) {
								// Try to create a JSON object out of this
								StringWriter writer = new StringWriter();
								try {
									Transformer transformer = TransformerFactory.newInstance().newTransformer();
									transformer.transform(new DOMSource(info_node), new StreamResult(writer));
								} catch (TransformerException e1) {
									continue;
								}
	
								if (bConvertToObject) {
									try {
										JSONObject subObj = XML.toJSONObject(writer.toString());
										if (xpath.endsWith("*"))  { // (can have any number of different names here)
											Llist.add(XmlToMetadataParser.convertJsonObjectToLinkedHashMap(subObj));
										}//TESTED
										else {
											String[] rootNames = JSONObject.getNames(subObj);
											if (1 == rootNames.length) {
												// (don't think it can't be any other number in fact)
												subObj = subObj.getJSONObject(rootNames[0]);
											}
											boolean bUnescapeHtml = ((null != m.flags) && m.flags.contains("H"));
											Llist.add(XmlToMetadataParser.convertJsonObjectToLinkedHashMap(subObj, bUnescapeHtml));										
										}//TESTED
									}
									catch (JSONException e) { // Just carry on
										continue;
									}
									//TESTED
								}
								else { // leave in XML form
									Llist.add(writer.toString().substring(38)); // +38: (step over <?xml version="1.0" encoding="UTF-8"?>)
								}//TESTED (xpath_test.json)
							}
							else { // Treat this as string, either directly or via regex
								String info = info_node.getTextContent().trim();
								if (extraRegex == null || extraRegex.isEmpty()) {
									prefix.setLength(nFieldNameLen);
									prefix.append(info);
									String dupCheck = prefix.toString();
		
									if (!regexDuplicates.contains(dupCheck)) {
										if ((null != m.flags) && m.flags.contains("H")) {
											info = StringEscapeUtils.unescapeHtml(info);
										}
										Llist.add(info);
										if (!bAllowDuplicates) {
											regexDuplicates.add(dupCheck);
										}
									}
								} 
								else { // Apply regex to the string
									Pattern dataRegex = createRegex(extraRegex, m.flags);
									Matcher dataMatcher = dataRegex.matcher(info);
									boolean result = dataMatcher.find();
									while (result) {
										String toAdd;
										if (m.groupNum != null)
											toAdd = dataMatcher.group(m.groupNum);
										else
											toAdd = dataMatcher.group();
										prefix.setLength(nFieldNameLen);
										prefix.append(toAdd);
										String dupCheck = prefix.toString();
		
										if (!regexDuplicates.contains(dupCheck)) {
											if ((null != m.flags) && m.flags.contains("H")) {
												toAdd = StringEscapeUtils.unescapeHtml(toAdd);
											}
											Llist.add(toAdd);
											if (!bAllowDuplicates) {
												regexDuplicates.add(dupCheck);
											}
										}
		
										result = dataMatcher.find();
									}
								}//(regex vs no regex)
							}//(end string vs object)
						}
						if (Llist.size() > 0) {
							f.addToMetadata(m.fieldName, Llist.toArray());
						}
					}
				}//(end loop over metadata objects if applicable)

			} catch (IOException ioe) {
				_context.getHarvestStatus().logMessage(HarvestExceptionUtils.createExceptionMessage(ioe).toString(), true);

				// Just do nothing and log
				//DEBUG (don't output log messages per doc)
				//logger.error(ioe.getMessage());
			} catch (ParserConfigurationException e1) {
				_context.getHarvestStatus().logMessage(HarvestExceptionUtils.createExceptionMessage(e1).toString(), true);
				// Just do nothing and log
				//DEBUG (don't output log messages per doc)
				//logger.error(e1.getMessage());
			} catch (XPathExpressionException e1) {
				_context.getHarvestStatus().logMessage("Error evaluating xpath expression: " +  xpath, true);
			}
		}
		else if (m.scriptlang.equalsIgnoreCase("stream")) { // XML or JSON streaming interface
			// which one?
			try {
				boolean json = false;
				boolean xml = false;
				for (int i = 0; i < 128; ++i) {
					if ('<' == text.charAt(i)) {
						xml = true;
						break;
					}
					if ('{' == text.charAt(i) || '[' == text.charAt(i)) {
						json = true;
						break;
					}
					if (!Character.isSpaceChar(text.charAt(i))) {
						break;
					}
				}//TESTED (too many spaces: meta_stream_test, test4; incorrect chars: test3, xml: test1, json: test2)
				
				boolean textNotObject = m.flags == null || !m.flags.contains("o");
				
				List<DocumentPojo> docs = new LinkedList<DocumentPojo>();
				List<String> levelOneFields = null;
				if (null != m.script) {
					levelOneFields = Arrays.asList(m.script.split("\\s*,\\s*"));
					if ((1 == levelOneFields.size()) && levelOneFields.get(0).isEmpty()) {
						// convert [""] to null
						levelOneFields = null;
					}
				}//TESTED (json and xml)
				
				if (xml) {
					XmlToMetadataParser parser = new XmlToMetadataParser(levelOneFields, null, null, null, null, null, Integer.MAX_VALUE);
					XMLInputFactory factory = XMLInputFactory.newInstance();
					factory.setProperty(XMLInputFactory.IS_COALESCING, true);
					factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
					XMLStreamReader reader = null;
					try {							
						reader = factory.createXMLStreamReader(new ByteArrayInputStream(text.getBytes()));
						docs = parser.parseDocument(reader, textNotObject);
					}
					finally {
						if (null != reader) reader.close();
					}
				}//TESTED (meta_stream_test, test1)
				if (json) {
					JsonReader jsonReader = null;
					try {					
						JsonToMetadataParser parser = new JsonToMetadataParser(null, levelOneFields, null, null, Integer.MAX_VALUE);
						jsonReader = new JsonReader(new InputStreamReader(new ByteArrayInputStream(text.getBytes()), "UTF-8"));
						jsonReader.setLenient(true);
						docs = parser.parseDocument(jsonReader, textNotObject);
					}
					finally {
						if (null != jsonReader) jsonReader.close();
					}
				}//TESTED (meta_stream_test test2)
				
				if (!docs.isEmpty()) {
					ArrayList<String> Llist = null;					
					ArrayList<Object> LlistObj = null;					
					if (textNotObject) {
						Llist = new ArrayList<String>(docs.size());
					}
					else {
						LlistObj = new ArrayList<Object>(docs.size());
					}
					for (DocumentPojo doc: docs) {
						if ((null != doc.getFullText()) || (null != doc.getMetadata())) {							
							if (textNotObject) {
								Llist.add(doc.getFullText());
							}//TESTED
							else if (xml) {
								LlistObj.add(doc.getMetadata());								
							}//TESTED
							else if (json) {
								Object o = doc.getMetadata();
								if (null != o) {
									o = doc.getMetadata().get("json");									
									if (o instanceof Object[]) {
										LlistObj.addAll(Arrays.asList((Object[])o));
									}
									else if (null != o) {
										LlistObj.add(o);
									}//TESTED
								}
							}//TESTED
						}
					}//TESTED
					if ((null != Llist) && !Llist.isEmpty()) {
						f.addToMetadata(m.fieldName, Llist.toArray());
					}//TESTED
					if ((null != LlistObj) && !LlistObj.isEmpty()) {
						f.addToMetadata(m.fieldName, LlistObj.toArray());
					}//TESTED
					
				}//TESTED (meta_stream_test test1,test2)
			}//(end try)
			catch (Exception e) { // various parsing errors
				_context.getHarvestStatus().logMessage(HarvestExceptionUtils.createExceptionMessage(e).toString(), true);				
			}
		}//TESTED (meta_stream_test)
		
		// (don't currently support other script types)
	}

	private static String extractRegexFromXpath(String original_xpath) {
		Pattern addedRegex = Pattern.compile("regex\\(.*\\)\\s*$", Pattern.MULTILINE | Pattern.DOTALL); 
		Matcher matcher = addedRegex.matcher(original_xpath);
		boolean matchFound = matcher.find();

		if (matchFound) {
			try {
				return matcher.group();
			} catch (Exception e) {
				return null;
			}
		}
		return null;

	}

	/**
	 * cleanseText
	 * 
	 * @param source
	 * @param documents
	 * @return
	 */
	private void cleanseText(List<SimpleTextCleanserPojo> simpleTextCleanser, DocumentPojo document)
	{
		// Store these since can re-generate them by concatenation
		StringBuffer fullTextBuilder = null;
		StringBuffer descriptionBuilder = null;
		StringBuffer titleBuilder = null;
		// (note no support for metadata concatenation, replace only)
		
		// Iterate over the cleanser functions that need to run on each feed
		for (SimpleTextCleanserPojo s : simpleTextCleanser) {
			boolean bConcat = (null != s.getFlags()) && s.getFlags().contains("+");
			
			boolean bUsingJavascript = ((null != s.getScriptlang()) && s.getScriptlang().equalsIgnoreCase("javascript"));
			if (s.getField().equalsIgnoreCase("fulltext")) {
				if ((null != document.getFullText()) || bUsingJavascript) {
					StringBuffer myBuilder = fullTextBuilder;
					
					if ((!bConcat) && (null != myBuilder) && (myBuilder.length() > 0)) {
						document.setFullText(myBuilder.toString());
						myBuilder.setLength(0);	
					} //TESTED
					
					String res = cleanseField(document.getFullText(),
												s.getScriptlang(), s.getScript(), s.getFlags(),
												s.getReplacement(), document);					
					if (bConcat) {
						if (null == myBuilder) {
							fullTextBuilder = myBuilder = new StringBuffer();
						}
						myBuilder.append(res).append('\n');
					}
					else {
						document.setFullText(res);
					}					
				}
			} //TESTED
			else if (s.getField().equalsIgnoreCase("description")) {
				if ((null != document.getDescription()) || bUsingJavascript) {
					StringBuffer myBuilder = descriptionBuilder;
					
					if ((!bConcat) && (null != myBuilder) && (myBuilder.length() > 0)) {
						document.setDescription(myBuilder.toString());
						myBuilder.setLength(0);	
					} //TESTED
					
					String res = cleanseField(document.getDescription(),
												s.getScriptlang(), s.getScript(), s.getFlags(),
												s.getReplacement(), document);
					
					if (bConcat) {
						if (null == myBuilder) {
							descriptionBuilder = myBuilder = new StringBuffer();
						}
						myBuilder.append(res).append('\n');
					}
					else {
						document.setDescription(res);
					}					
				}
			} //TESTED
			else if (s.getField().equalsIgnoreCase("title")) {
				if ((null != document.getTitle()) || bUsingJavascript) {
					StringBuffer myBuilder = titleBuilder;
					
					if ((!bConcat) && (null != myBuilder) && (myBuilder.length() > 0)) {
						document.setTitle(myBuilder.toString());
						myBuilder.setLength(0);	
					} //TESTED
					
					String res = cleanseField(document.getTitle(),
												s.getScriptlang(), s.getScript(), s.getFlags(),
												s.getReplacement(), document);
					if (bConcat) {
						if (null == myBuilder) {
							titleBuilder = myBuilder = new StringBuffer();
						}
						myBuilder.append(res).append('\n');
					}
					else {
						document.setTitle(res);
					}					
				}
			} //TESTED
			else if (s.getField().startsWith("metadata.")) {
				// (note no support for metadata concatenation, replace only)
				String metaField = s.getField().substring(9); // (9 for"metadata.")
				Object[] meta = document.getMetadata().get(metaField);
				if ((null != meta) && (meta.length > 0)) {
					Object[] newMeta = new Object[meta.length];
					for (int i = 0; i < meta.length; ++i) {
						Object metaValue = meta[i];
						if (metaValue instanceof String) {
							newMeta[i] = (Object) cleanseField(
									(String) metaValue, s.getScriptlang(),
									s.getScript(), s.getFlags(),
									s.getReplacement(), document);
						} else {
							newMeta[i] = metaValue;
						}
					}
					// Overwrite the old fields
					document.addToMetadata(metaField, newMeta);
				}
			}			
			// This is sufficient fields for the moment
			
		} // (end loop over fields)
		
		// Handle any left over cases:
		if ((null != fullTextBuilder) && (fullTextBuilder.length() > 0)) {
			document.setFullText(fullTextBuilder.toString());
		} //TESTED
		if ((null != descriptionBuilder) && (descriptionBuilder.length() > 0)) {
			document.setDescription(descriptionBuilder.toString());
		} //TESTED
		if ((null != titleBuilder) && (titleBuilder.length() > 0)) {
			document.setTitle(titleBuilder.toString());
		} //TESTED
		
	}// TESTED

	/**
	 * cleanseField
	 * 
	 * @param field
	 * @param script
	 * @param replaceWith
	 */
	private String cleanseField(String field, String scriptLang, String script,
									String flags, String replaceWith, DocumentPojo f) 
	{
		if ((null == scriptLang) || scriptLang.equalsIgnoreCase("regex")) {
			if (null == flags) {
				return field.replaceAll(script, replaceWith);
			} 
			else {
				if (flags.contains("H")) { // HTML decode
					return StringEscapeUtils.unescapeHtml(createRegex(script,flags).matcher(field).replaceAll(replaceWith));
				} else {
					return createRegex(script, flags).matcher(field).replaceAll(replaceWith);
				}
			}
		} 
		else if (scriptLang.equalsIgnoreCase("xpath")) {
			
			try {
				createHtmlCleanerIfNeeded();

				TagNode node = cleaner.clean(new ByteArrayInputStream(field.getBytes()));

				Document doc = new DomSerializer(new CleanerProperties()).createDOM(node);
				XPath xpa = XPathFactory.newInstance().newXPath();				
				
				NodeList res = (NodeList)xpa.evaluate(script, doc, XPathConstants.NODESET);

				if (0 == res.getLength()) { // No match, just return "", unlike regex we don't want anything if we don't match...
					return "";					
				}
				else {
					StringBuffer sb = new StringBuffer();
					for (int i= 0; i< res.getLength(); i++) {
						if (0 != i) {
							sb.append('\n');
						}
						Node info_node = res.item(i);

						if ((null != flags) && flags.contains("H")) { // HTML decode
							sb.append(StringEscapeUtils.unescapeHtml(info_node.getTextContent().trim()));
						}
						else if ((null != flags) && flags.contains("x")) { // Leave as XML string 
							StringWriter writer = new StringWriter();
							try {
								Transformer transformer = TransformerFactory.newInstance().newTransformer();
								transformer.transform(new DOMSource(info_node), new StreamResult(writer));
								sb.append(writer.toString().substring(38)); // (step over <?xml etc?> see under metadata field extraction
							} 
							catch (TransformerException e1) { // (do nothing just skip)
							}
						}
						else {
							sb.append(info_node.getTextContent().trim());						
						}										
					}										
					return sb.toString();
				}//TESTED (xpath_test: object - multiple and single, text)
				
			} catch (IOException e) {
				_context.getHarvestStatus().logMessage(HarvestExceptionUtils.createExceptionMessage(e).toString(), true);
			} 
			catch (XPathExpressionException e) {
				_context.getHarvestStatus().logMessage(HarvestExceptionUtils.createExceptionMessage(e).toString(), true);
			} 
			catch (ParserConfigurationException e) {
				_context.getHarvestStatus().logMessage(HarvestExceptionUtils.createExceptionMessage(e).toString(), true);
			}
		}
		else if (scriptLang.equalsIgnoreCase("javascript")) {
			try {
				SourcePojo src = f.getTempSource();
				intializeScriptEngine(src, src.getUnstructuredAnalysisConfig(),compiledScriptFactory);

				// Setup input:
				if (null == flags) {
					flags = "t";
				}
				if (flags.contains("t")) { // text
					compiledScriptFactory.getScriptContext().setAttribute("text", field, ScriptContext.ENGINE_SCOPE);
				}
				Object returnVal = compiledScriptFactory.executeCompiledScript(script);				        

				field = (String) returnVal; // (If not a string or is null then will exception out)
				if ((null != flags) && flags.contains("H") && (null != field)) { // HTML decode
					field = StringEscapeUtils.unescapeHtml(field);
				}
			}
			catch (Exception e) {
				_context.getHarvestStatus().logMessage(HarvestExceptionUtils.createExceptionMessage(e).toString(), true);

				// Just do nothing and log
				// e.printStackTrace();
				//DEBUG (don't output log messages per doc)
				//logger.error(e.getMessage());
			}
		}
		return field;
	}
	
	// Handles parsing of HTML tables to Objects that can be easily printed as JSON. (flag = g)
	// 1] No Replace Value - The first row of the table will be set as the headers
	// 2] Replace Value = "[]" - Headers will be set to the column count number (beginning with 0) eg "0","1"
	// 3a] Replace Value = "[one,two,three]" - The provided headers will be set as the headers
	// 3b] Replace Values set, but more data columns than values provided - Additional columns that were not
	//		specified will be assigned it's column count number. eg "specified","1","2"
	// 4] Replace Value = "[one,null,three]" - Columns specified as null in the provided header will be skipped.
	//		eg "one","three"
	
	private static HashMap<String, Object> parseHtmlTable(Node table_node, String replaceWith)
	{
		if (table_node.getNodeName().equalsIgnoreCase("table") && table_node.hasChildNodes())
		{
			Node topNode = table_node;
			
			boolean tbody = table_node.getFirstChild().getNodeName().equalsIgnoreCase("tbody");
			
			if (tbody)
				topNode = table_node.getFirstChild();
			
			if (topNode.hasChildNodes())
			{
				NodeList rows = topNode.getChildNodes();
				
				List<String> headers = null;
				ArrayList<HashMap<String, String>> data = null;
				int headerLength = 0;
				boolean[] skip = null;
				
				if (null != replaceWith)
				{
					if (replaceWith.equals("[]")){
						headers = new ArrayList<String>();
						headerLength = 0;
					} // TESTED (by eye - 2)
					else
					{
						//Remove square brackets
						if(replaceWith.startsWith("[") && replaceWith.endsWith("]"))
							replaceWith = replaceWith.substring(1, replaceWith.length()-1);
						//Turn the provided list of headers into a list object
						headers = Arrays.asList(replaceWith.split("\\s*,\\s*"));
						headerLength = headers.size();
						skip = new boolean[headerLength];
						for(int h = 0; h < headerLength; h++)
						{
							String val = headers.get(h);
							if (val.length() == 0 || val.equalsIgnoreCase("null"))
								skip[h] = true;
							else
								skip[h] = false;
						}
						
					}// TESTED (by eye - 3a)
				}
				
				//traverse rows
				for(int i = 0; i < rows.getLength(); i++)
				{
					Node row = rows.item(i);
					if (row.getNodeName().equalsIgnoreCase("tr") || row.getNodeName().equalsIgnoreCase("th"))
					{
						//If the header value has not been set, the first row will be set as the headers
						if (null == headers)
						{
							//Traverse through cells
							headers = new ArrayList<String>();
							if (row.hasChildNodes())
							{
								NodeList cells = row.getChildNodes();
								headerLength = cells.getLength();
								skip = new boolean[headerLength];
								for (int j = 0; j < headerLength; j++)
								{
									headers.add(cells.item(j).getTextContent());
									skip[j] = false;
								}
							} // TESTED (by eye - 1)
						} 
						else
						{
							if (null == data)
							{
								data = new ArrayList<HashMap<String,String>>();
							}
							if (row.hasChildNodes())
							{
								HashMap<String,String> cellList = new HashMap<String,String>();
								NodeList cells = row.getChildNodes();
								for (int j = 0; j < cells.getLength(); j++)
								{
									// Skip Code (TESTED by eye - 4)
									if (headerLength == 0 || (j < headerLength && skip[j] == false))
									{
										String key = Integer.toString(j); // TESTED (by eye - 3b)
										if (j < headerLength)
											key = headers.get(j);
	
										cellList.put(key, cells.item(j).getTextContent());
									}
								}
								data.add(cellList);
							}
							
						}
					}
				}
				//Create final hashmap containing attributes
				HashMap<String,Object> table_attrib = new HashMap<String, Object>();
				
				NamedNodeMap nnm = table_node.getAttributes();
				for (int i = 0; i < nnm.getLength(); i++)
				{
					Node att = nnm.item(i);
					table_attrib.put(att.getNodeName(), att.getNodeValue());
				}
				table_attrib.put("table", data);
				
				//TESTED (by eye) attributes added to table value
				// eg: {"id":"search","cellpadding":"1","table":[{"Status":"B","two":"ONE6313" ......
				
				return table_attrib;
			}			
		}
		return null;
	}

	private static Pattern createRegex(String regEx, String flags) {
		int nflags = 0; 

		if (null != flags) {
			for (int i = 0; i < flags.length(); ++i) {
				char c = flags.charAt(i);
				switch (c) { 
				case 'm':
					nflags |= Pattern.MULTILINE;
					break;
				case 'i':
					nflags |= Pattern.CASE_INSENSITIVE;
					break;
				case 'd':
					nflags |= Pattern.DOTALL;
					break; 
				case 'u':
					nflags |= Pattern.UNICODE_CASE;
					break;
				case 'n':
					nflags |= Pattern.UNIX_LINES;
					break;
				}
			}
		}
		return Pattern.compile(regEx, nflags);
	}

	// Utility to minimise number of times the cleaner is created
	
	private void createHtmlCleanerIfNeeded()
	{
		if (null == cleaner) {
			cleaner = new HtmlCleaner();
			CleanerProperties props = cleaner.getProperties();
			props.setAllowHtmlInsideAttributes(true);
			props.setAllowMultiWordAttributes(true);
			props.setRecognizeUnicodeChars(true);
			props.setOmitComments(true);
			props.setTreatUnknownTagsAsContent(false);
			props.setTranslateSpecialEntities(true);
			props.setTransResCharsToNCR(true);
			props.setNamespacesAware(false);
		}		
	}


	public void intializeScriptEngine(SourcePojo source, UnstructuredAnalysisConfigPojo uap, CompiledScriptFactory compiledScriptFactory) {
		if (this.compiledScriptFactory == null) {
			this.compiledScriptFactory = compiledScriptFactory;
			try {
				// COMPILED_SCRIPT initialization
				this.compiledScriptFactory.executeCompiledScript(CompiledScriptFactory.GLOBAL);
			} catch (Exception e) {
				this._context.getHarvestStatus().logMessage("ScriptException (globals): " + e.getMessage(), true);
			}
		
			if ((null != source) && (uap!=null)) {
				loadLookupCaches(uap.getCaches(), source.getCommunityIds(), source.getOwnerId());
			}
		}
	}// TESTED (legacy + imports_and_lookup_test.json + imports_and_lookup_test_uahSah.json)
	
	//////////////////////////////////////////////////////
	
	// Utilities that in legacy mode are called from the initializeScriptEngine, but can be called
	// standalone in the pipelined mode:
	
	public void loadLookupCaches(Map<String, ObjectId> caches, Set<ObjectId> communityIds, ObjectId sourceOwnerId)
	{
		try
		{
			if (null != caches) {
				List<String> errs = CacheUtils.addJSONCachesToEngine(caches, compiledScriptFactory, communityIds, sourceOwnerId, _context);
				for (String err: errs) {
					_context.getHarvestStatus().logMessage(err, true);
				}				
			}
		}
		catch (Exception ex)
		{
			StringBuffer sb = new StringBuffer("JSONcache: ").append(ex.getMessage());
			Globals.populateStackTrace(sb, ex);
			_context.getHarvestStatus().logMessage(sb.toString(), true);
			//(no need to log this, appears in log under source -with URL- anyway):
			//logger.error(sb.toString());
		}
	}//TESTED (legacy + imports_and_lookup_test.json)
	
	public void loadGlobalFunctions(List<String> imports, String script) 
	{
		intializeScriptEngine(null,null,compiledScriptFactory);
	}//TESTED (legacy + imports_and_lookup_test.json)
	
	// UTILITY - CURRENTLY ONLY JS CAN SURVIVE WITHOUT TEXT...
	
	private static boolean doesScriptLangNeedText(String scriptLang) {
		return !scriptLang.equalsIgnoreCase("javascript");
	}//TESTED (content_needed_test)
}
