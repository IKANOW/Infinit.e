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
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.xml.parsers.ParserConfigurationException;
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
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.DomSerializer;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.XPatherException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ikanow.infinit.e.data_model.InfiniteEnums.ExtractorDocumentLevelException;
import com.ikanow.infinit.e.data_model.store.config.source.SimpleTextCleanserPojo;
import com.ikanow.infinit.e.data_model.InfiniteEnums;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.config.source.UnstructuredAnalysisConfigPojo;
import com.ikanow.infinit.e.data_model.store.config.source.UnstructuredAnalysisConfigPojo.Context;
import com.ikanow.infinit.e.data_model.store.config.source.UnstructuredAnalysisConfigPojo.metaField;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.harvest.HarvestContext;
import com.ikanow.infinit.e.harvest.HarvestController;
import com.ikanow.infinit.e.harvest.extraction.document.file.XmlToMetadataParser;
import com.ikanow.infinit.e.harvest.extraction.text.legacy.TextExtractorTika;
import com.ikanow.infinit.e.harvest.utils.HarvestExceptionUtils;
import com.ikanow.infinit.e.harvest.utils.PropertiesManager;
import com.mongodb.BasicDBList;

/**
 * UnstructuredAnalysisHarvester
 */
public class UnstructuredAnalysisHarvester {
	// Configuration
	private Set<Integer> sourceTypesCanHarvest = new HashSet<Integer>();

	// Per-source state
	private Pattern headerPattern = null;
	private Pattern footerPattern = null;
	private UnstructuredAnalysisConfigPojo savedUap = null;

	// Javascript handling, if needed
	private ScriptEngineManager factory = null;
	private ScriptEngine engine = null;
	private static String parsingScript = null;

	// Using Tika to process documents:
	TextExtractorTika tikaExtractor = null;
	
	private HarvestContext _context = null;
	private Logger logger = Logger
			.getLogger(UnstructuredAnalysisHarvester.class);

	// (some web scraping may be needed)
	private long nBetweenDocs_ms = -1;
	// (set this in execute harvest - makes it easy to only set once in the per doc version called in bulk from the SAH)

	// Ensure we don't get long list of duplicates for commonly occurring words
	private HashSet<String> regexDuplicates = null;
	private HtmlCleaner cleaner = null;
	
	//if the sah already init'd an engine we'll just use it
	private ScriptEngine _sahEngine = null; 
	private JavascriptSecurityManager securityManager = null;
	
	/**
	 * Default Constructor
	 */
	public UnstructuredAnalysisHarvester() {
		sourceTypesCanHarvest.add(InfiniteEnums.UNSTRUCTUREDANALYSIS);
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
								tikaExtractor.extractText(d);
							}
						}
						else {
							URL url = new URL(d.getUrl());
							URLConnection urlConnect = url.openConnection();
							if ((null != source.getRssConfig()) && (null != source.getRssConfig().getUserAgent())) {
								urlConnect.setRequestProperty("User-Agent", source.getRssConfig().getUserAgent());
							}// TESTED
							
							InputStream urlStream = null;
							try {
								urlStream = urlConnect.getInputStream();
							}
							catch (Exception e) { // Try one more time, this time exception out all the way
								urlStream = urlConnect.getInputStream();					 
							}
							
							d.setFullText(new Scanner(urlStream,"UTF-8").useDelimiter("\\A").next());
						}
						bFetchedUrl = true;
						
					} catch (Exception e) { // Failed to get full text twice, remove doc
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
					logger.error("processBody1: " + e.getMessage(), e);
				}

				try {
					if (uap.getSimpleTextCleanser() != null) {
						cleanseText(source, d);
					}
				} catch (Exception e) {
					this._context.getHarvestStatus().logMessage("cleanseText: " + e.getMessage(), true);
					logger.error("cleanseText: " + e.getMessage(), e);
				}

				try {
					processHeader(headerPattern, d, meta, source, uap);
					processFooter(footerPattern, d, meta, source, uap);
					
				} catch (Exception e) {
					this._context.getHarvestStatus().logMessage("header/footerPattern: " + e.getMessage(), true);
					logger.error("header/footerPattern: " + e.getMessage(), e);
				}
				try {
					processBody(d, meta, false, source, uap);
					
				} catch (Exception e) {
					this._context.getHarvestStatus().logMessage("processBody2: " + e.getMessage(), true);
					logger.error("processBody2: " + e.getMessage(), e);
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
		UnstructuredAnalysisConfigPojo uap = source.getUnstructuredAnalysisConfig();

		int nChanges = 0;
		if (null != doc.getMetaData()) {
			nChanges = doc.getMetaData().size();
		}
		boolean bFetchedUrl = false;
		if (bGetRawDoc) {
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
				
					URL url = new URL(doc.getUrl());
					URLConnection urlConnect = url.openConnection();
					if ((null != source.getRssConfig()) && (null != source.getRssConfig().getUserAgent())) {
						urlConnect.setRequestProperty("User-Agent", source.getRssConfig().getUserAgent());
					}// TESTED
					
					InputStream urlStream = null;
					try {
						urlStream = urlConnect.getInputStream();
					}
					catch (Exception e) { // Try one more time, this time exception out all the way
						url = new URL(doc.getUrl());
						urlConnect = url.openConnection();
						if ((null != source.getRssConfig()) && (null != source.getRssConfig().getUserAgent())) {
							urlConnect.setRequestProperty("User-Agent", source.getRssConfig().getUserAgent());
						}// TESTED
						urlStream = urlConnect.getInputStream();					 
					}
					doc.setFullText(new Scanner(urlStream,"UTF-8").useDelimiter("\\A").next());
				}
				bFetchedUrl = true;
				
			} catch (Exception e) { // Failed to get full text twice... remove doc and carry on
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
				this._context.getHarvestStatus().logMessage("processBody1: " + e.getMessage(), true);
				logger.error("processBody1: " + e.getMessage(), e);
			}
			try {
				if (uap.getSimpleTextCleanser() != null) {
					cleanseText(source, doc);
				}
			} catch (Exception e) {
				this._context.getHarvestStatus().logMessage("cleanseText: " + e.getMessage(), true);
				logger.error("cleanseText: " + e.getMessage(), e);
			}
			try {
				processHeader(headerPattern, doc, meta, source, uap);
				processFooter(footerPattern, doc, meta, source, uap);
				
			} catch (Exception e) {
				this._context.getHarvestStatus().logMessage("header/footerPattern: " + e.getMessage(), true);
				logger.error("header/footerPattern: " + e.getMessage(), e);
			}
			try {
				processBody(doc, meta, false, source, uap);
				
			} catch (Exception e) {
				this._context.getHarvestStatus().logMessage("processBody2: " + e.getMessage(), true);
				logger.error("processBody2: " + e.getMessage(), e);
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
						this.processMeta(f, m, headerText, source, uap);
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
						this.processMeta(f, m, footerText, source, uap);
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
	private void processBody(DocumentPojo f, List<metaField> meta, boolean bPreCleansing, SourcePojo source, UnstructuredAnalysisConfigPojo uap)
	{
		if (null != meta) {
			for (metaField m : meta) {
				if ((bPreCleansing && (m.context == Context.First))
						|| (!bPreCleansing && (m.context == Context.Body || m.context == Context.All))) {
					String toProcess = f.getBody();
					if (toProcess == null)
						toProcess = f.getDescription();

					if (null != toProcess) {
						this.processMeta(f, m, toProcess, source, uap);
					}
				}
			}
		}
	}

	/**
	 * processMeta - handle an individual field
	 */
	private void processMeta(DocumentPojo f, metaField m, String text, SourcePojo source, UnstructuredAnalysisConfigPojo uap) {

		if ((null == m.scriptlang) || m.scriptlang.equalsIgnoreCase("regex")) {

			Pattern metaPattern = createRegex(m.script, m.flags);
			Matcher matcher = metaPattern.matcher(text);

			StringBuffer prefix = new StringBuffer(m.fieldName).append(':');
			int nFieldNameLen = m.fieldName.length() + 1;

			try {
				LinkedList<String> Llist = null;
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
					prefix.setLength(nFieldNameLen);
					prefix.append(toAdd);
					String dupCheck = prefix.toString();

					if (!regexDuplicates.contains(dupCheck)) {
						Llist.add(toAdd);
						regexDuplicates.add(dupCheck);
					}
				}
				if (null != Llist) {
					f.addToMetadata(m.fieldName, Llist.toArray());
				}
			} catch (Exception e) {
				this._context.getHarvestStatus().logMessage("processMeta1: " + e.getMessage(), true);
			}
		} 
		else if (m.scriptlang.equalsIgnoreCase("javascript")) 
		{
			if (null == f.getMetadata()) {
				f.setMetadata(new LinkedHashMap<String, Object[]>());
			}
			//set the script engine up if necessary
			if ( null == engine )
			{
				//use the passed in sah one if possible
				if ( null != this.get_sahEngine())
				{
					engine = this.get_sahEngine();
				}
				else if (null == factory)  //otherwise create our own
				{
					//set up the security manager
					securityManager = new JavascriptSecurityManager();	
					
					factory = new ScriptEngineManager();
					engine = factory.getEngineByName("JavaScript");		
					//grab any json cache and make it available to the engine
					try
					{
						if (null != uap.getCaches()) {
							CacheUtils.addJSONCachesToEngine(uap.getCaches(), engine, source.getCommunityIds(), _context);
						}
					}
					catch (Exception ex)
					{
						_context.getHarvestStatus().logMessage("JSONcache: " + ex.getMessage(), true);						
						logger.error("JSONcache: " + ex.getMessage(), ex);
					}
				}
				//once engine is created, do some initialization
				if ( null != engine )
				{
					// Script code embedded in source
					String script = (uap.getScript() != null) ? uap.getScript(): null;
					
					// scriptFiles - can contain String[] of script files to import into the engine
					String[] scriptFiles = (uap.getScriptFiles() != null) ? uap.getScriptFiles(): null;
					
			        // Pass scripts into the engine
			        try 
			        {
			        	// Eval script passed in s.script
			        	if (script != null) securityManager.eval(engine, script);
			        	
			        	// Retrieve and eval script files in s.scriptFiles
			        	if (scriptFiles != null)
			        	{
			        		for (String file : scriptFiles)
			        		{
			        			securityManager.eval(engine, JavaScriptUtils.getJavaScriptFile(file));
			        		}
			        	}
					} 
			        catch (ScriptException e) 
					{
						this._context.getHarvestStatus().logMessage("ScriptException: " + e.getMessage(), true);						
						logger.error("ScriptException: " + e.getMessage(), e);
					}
			        
					if (null == parsingScript) 
					{
						parsingScript = JavaScriptUtils.generateParsingScript();
					}
					try 
					{
						securityManager.eval(engine, parsingScript);						
					} 
					catch (ScriptException e) { // Just do nothing and log
						e.printStackTrace();
						logger.error(e.getMessage());
					}
				}
			}
			
			try 
			{
				// Javascript: the user passes in 
				Object[] currField = f.getMetadata().get(m.fieldName);
				if (null == m.flags) {
					if (null == currField) {
						engine.put("text", text);
						engine.put("_iterator", null);
					}
					//(otherwise will just pass the current fields in there)
				}
				else { // flags specified
					if (m.flags.contains("t")) { // text
						engine.put("text", text);							
					}
					if (m.flags.contains("d")) { // entire document
						GsonBuilder gb = new GsonBuilder();
						Gson g = gb.create();	
						JSONObject document = new JSONObject(g.toJson(f));
				        engine.put("document", document);
				        securityManager.eval(engine, JavaScriptUtils.initScript);			        						
					}
					if (m.flags.contains("m")) { // metadata
						GsonBuilder gb = new GsonBuilder();
						Gson g = gb.create();	
						JSONObject iterator = new JSONObject(g.toJson(f.getMetadata()));
						engine.put("_metadata", iterator);
						securityManager.eval(engine, JavaScriptUtils.iteratorMetaScript);
					}
				}//(end flags processing)
				
				if (null != currField) {
					f.getMetadata().remove(m.fieldName);
					
					GsonBuilder gb = new GsonBuilder();
					Gson g = gb.create();	
					JSONArray iterator = new JSONArray(g.toJson(currField));
					engine.put("_iterator", iterator);
					securityManager.eval(engine, JavaScriptUtils.iteratorDocScript);		        	
				}
				//TESTED (handling of flags, and replacing of existing fields, including when field is null but specified)

				Object returnVal = securityManager.eval(engine, m.script);

				if (null != returnVal) {
					if (returnVal instanceof String) { // The only easy case
						Object[] array = new Object[1];
						array[0] = returnVal;
						f.addToMetadata(m.fieldName, array);
					} else { // complex object or array - in either case the engine turns these into
								// internal.NativeArray or internal.NativeObject
						
						BasicDBList outList = JavaScriptUtils.parseNativeJsObject(returnVal, engine);												
						f.addToMetadata(m.fieldName, outList.toArray());
					}
				}
			} catch (ScriptException e) {

				_context.getHarvestStatus().logMessage(HarvestExceptionUtils.createExceptionMessage(e).toString(), true);

				// Just do nothing and log
				// e.printStackTrace();
				logger.error(e.getMessage());
			} catch (JSONException e) {
				
				_context.getHarvestStatus().logMessage(HarvestExceptionUtils.createExceptionMessage(e).toString(), true);

				// Just do nothing and log
				// e.printStackTrace();
				logger.error(e.getMessage());
			}
		} else if (m.scriptlang.equalsIgnoreCase("xpath")) {

			try {
				createHtmlCleanerIfNeeded();

				TagNode node = cleaner.clean(new ByteArrayInputStream(text.getBytes()));
				
				//NewCode : Only use html cleaner for cleansing
				//use JAXP for full Xpath lib
				Document doc = new DomSerializer(new CleanerProperties()).createDOM(node);
				

				String xpath = m.script;

				String extraRegex = extractRegexFromXpath(xpath);

				if (extraRegex != null)
					xpath = xpath.replace("regex(" + extraRegex + ")", "");
				
				XPath xpa = XPathFactory.newInstance().newXPath();
				NodeList res = (NodeList)xpa.evaluate(xpath, doc, XPathConstants.NODESET);
				
				if (res.getLength() > 0)
				{
					StringBuffer prefix = new StringBuffer(m.fieldName)
					.append(':');
					int nFieldNameLen = m.fieldName.length() + 1;
					ArrayList<Object> Llist = new ArrayList<Object>(res.getLength());
					boolean bConvertToObject = ((m.groupNum != null) && (m.groupNum == -1));
					for (int i= 0; i< res.getLength(); i++)
					{
						Node info_node = res.item(i);
						if (bConvertToObject) {
							// Try to create a JSON object out of this
							StringWriter writer = new StringWriter();
							try {
								Transformer transformer = TransformerFactory.newInstance().newTransformer();
								transformer.transform(new DOMSource(info_node), new StreamResult(writer));
							} catch (TransformerException e1) {
								continue;
							}

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
									regexDuplicates.add(dupCheck);
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
										regexDuplicates.add(dupCheck);
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

			} catch (IOException ioe) {
				_context.getHarvestStatus().logMessage(HarvestExceptionUtils.createExceptionMessage(ioe).toString(), true);

				// Just do nothing and log
				logger.error(ioe.getMessage());
			} catch (ParserConfigurationException e1) {
				_context.getHarvestStatus().logMessage(HarvestExceptionUtils.createExceptionMessage(e1).toString(), true);
				// Just do nothing and log
				logger.error(e1.getMessage());
			} catch (XPathExpressionException e1) {
				_context.getHarvestStatus().logMessage(HarvestExceptionUtils.createExceptionMessage(e1).toString(), true);
				// Just do nothing and log
				logger.error(e1.getMessage());
			}
		}
		// (don't currently support other script types)
	}

	private static String extractRegexFromXpath(String original_xpath) {
		Pattern addedRegex = createRegex("regex\\((.*)\\)", null);
		Matcher matcher = addedRegex.matcher(original_xpath);
		boolean matchFound = matcher.find();

		if (matchFound) {
			try {
				return matcher.group(1);
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
	private void cleanseText(SourcePojo source, DocumentPojo document)
	{
		List<SimpleTextCleanserPojo> simpleTextCleanser = source
				.getUnstructuredAnalysisConfig().getSimpleTextCleanser();
		
		// Store these since can re-generate them by concatenation
		StringBuffer fullTextBuilder = null;
		StringBuffer descriptionBuilder = null;
		StringBuffer titleBuilder = null;
		// (note no support for metadata concatenation, replace only)
		
		// Iterate over the cleanser functions that need to run on each feed
		for (SimpleTextCleanserPojo s : simpleTextCleanser) {
			boolean bConcat = (null != s.getFlags()) && s.getFlags().contains("+");
			
			if (s.getField().equalsIgnoreCase("fulltext")) {
				if (null != document.getFullText()) {
					StringBuffer myBuilder = fullTextBuilder;
					
					if ((!bConcat) && (null != myBuilder) && (myBuilder.length() > 0)) {
						document.setFullText(myBuilder.toString());
						myBuilder.setLength(0);	
					} //TESTED
					
					String res = cleanseField(document.getFullText(),
												s.getScriptlang(), s.getScript(), s.getFlags(),
												s.getReplacement());					
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
				if (null != document.getDescription()) {
					StringBuffer myBuilder = descriptionBuilder;
					
					if ((!bConcat) && (null != myBuilder) && (myBuilder.length() > 0)) {
						document.setDescription(myBuilder.toString());
						myBuilder.setLength(0);	
					} //TESTED
					
					String res = cleanseField(document.getDescription(),
												s.getScriptlang(), s.getScript(), s.getFlags(),
												s.getReplacement());
					
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
				if (null != document.getTitle()) {
					StringBuffer myBuilder = titleBuilder;
					
					if ((!bConcat) && (null != myBuilder) && (myBuilder.length() > 0)) {
						document.setTitle(myBuilder.toString());
						myBuilder.setLength(0);	
					} //TESTED
					
					String res = cleanseField(document.getTitle(),
												s.getScriptlang(), s.getScript(), s.getFlags(),
												s.getReplacement());
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
									s.getReplacement());
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
									String flags, String replaceWith) 
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
		} else if (scriptLang.equalsIgnoreCase("xpath")) {
			
			try {
				createHtmlCleanerIfNeeded();

				TagNode node = cleaner.clean(new ByteArrayInputStream(field
						.getBytes()));

				String xpath = script;

				if (xpath.startsWith("/html/body/")) {
					xpath = xpath.replace("/html/body/", "//body/");
				} else if (xpath.startsWith("/html[1]/body[1]/")) {
					xpath = xpath.replace("/html[1]/body[1]/", "//body/");
				}

				Object[] data_nodes = node.evaluateXPath(xpath);

				if (0 == data_nodes.length) { // No match, just return "", unlike regex we don't want anything if we don't match...
					return "";
				}
				else if (1 == data_nodes.length) {
					TagNode info_node = (TagNode) data_nodes[0];
					
					if ((null != flags) && flags.contains("H")) { // HTML decode
						return StringEscapeUtils.unescapeHtml(info_node.getText().toString());
					}
					else {
						return info_node.getText().toString();						
					}					
				}
				else if (data_nodes.length > 0) {
					StringBuffer sb = new StringBuffer();

					// Multiple matches are return by a tab-delmited String
					for (Object o : data_nodes) {
						TagNode info_node = (TagNode) o;
						if (sb.length() > 0) {
							sb.append('\t');
						}
						if ((null != flags) && flags.contains("H")) { // HTML decode
							sb.append(StringEscapeUtils.unescapeHtml(info_node.getText().toString()).trim());
						}
						else {
							sb.append(info_node.getText().toString().trim());
						}
					}
					return sb.toString();
				}

			} catch (IOException e) {
				_context.getHarvestStatus().logMessage(HarvestExceptionUtils.createExceptionMessage(e).toString(), true);
			} 
			catch (XPatherException e) {
				_context.getHarvestStatus().logMessage(HarvestExceptionUtils.createExceptionMessage(e).toString(), true);
			}

		}
		return field;
	}

	private static Pattern createRegex(String regEx, String flags) {
		int nflags = Pattern.DOTALL; // ('d', by default though)

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
				case 'D':
					nflags ^= Pattern.DOTALL;
					break; // (ie negate DOTALL)
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

	public void set_sahEngine(ScriptEngine _sahEngine) {
		this._sahEngine = _sahEngine;
	}

	public ScriptEngine get_sahEngine() {
		return _sahEngine;
	}	
	public void set_sahSecurity(JavascriptSecurityManager _securityManager) {
		this.securityManager = _securityManager;
	}

	public JavascriptSecurityManager get_sahSecurity() {
		return securityManager;
	}	
}
