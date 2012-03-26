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

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.log4j.Logger;

import com.ikanow.infinit.e.data_model.store.config.source.SimpleTextCleanserPojo;
import com.ikanow.infinit.e.data_model.InfiniteEnums;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.config.source.UnstructuredAnalysisConfigPojo;
import com.ikanow.infinit.e.data_model.store.config.source.UnstructuredAnalysisConfigPojo.Context;
import com.ikanow.infinit.e.data_model.store.config.source.UnstructuredAnalysisConfigPojo.metaField;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.harvest.HarvestContext;
import com.ikanow.infinit.e.harvest.utils.HarvestExceptionUtils;
import com.ikanow.infinit.e.harvest.utils.PropertiesManager;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

/**
 * UnstructuredAnalysisHarvester
 */
public class UnstructuredAnalysisHarvester 
{
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
	
	private HarvestContext _context = null;
	private Logger logger = Logger.getLogger(UnstructuredAnalysisHarvester.class);
	
	//(some web scraping may be needed)
	private long nBetweenDocs_ms = -1;
		// (set this in execute harvest - makes it easy to only set once in the per doc version
		//  called in bulk from the SAH)
	
	// Ensure we don't get long list of duplicates for commonly occurring words
	private HashSet<String> regexDuplicates = null;
	
	/**
	 * Default Constructor
	 */
	public UnstructuredAnalysisHarvester()
	{			
		sourceTypesCanHarvest.add(InfiniteEnums.UNSTRUCTUREDANALYSIS);
	}

	/**
	 * executeHarvest(SourcePojo source, List<DocumentPojo> feeds)
	 * 
	 * @param source
	 * @param feeds
	 * @return List<DocumentPojo>
	 */
	public List<DocumentPojo> executeHarvest(HarvestContext context, SourcePojo source, List<DocumentPojo> documents) 
	{
		if (-1 == nBetweenDocs_ms) {
			PropertiesManager props = new PropertiesManager();
			nBetweenDocs_ms = props.getWebCrawlWaitTime();
			// Can override the default (feed) wait time from within the source (eg for sites that we know 
			// don't get upset about getting hammered)
			if (null != source.getRssConfig()) {
				if (null != source.getRssConfig().getWaitTimeOverride_ms()) {
					nBetweenDocs_ms = source.getRssConfig().getWaitTimeOverride_ms();
				}
			}
		}
		_context = context;
		UnstructuredAnalysisConfigPojo uap = source.getUnstructuredAnalysisConfig();

		if (uap != null)
		{
			boolean bGetRawDoc = source.getExtractType().equalsIgnoreCase("feed");		
			
			String headerRegEx = uap.getHeaderRegEx();
			String footerRegEx = uap.getFooterRegEx();
			List<metaField> meta= uap.getMeta();

			if (headerRegEx != null)
				headerPattern = createRegex(headerRegEx, uap.getHeaderRexExFlags());
			if (footerRegEx != null)
				footerPattern = createRegex(footerRegEx, uap.getFooterRegExFlags());

			Iterator<DocumentPojo> it = documents.iterator();
			while (it.hasNext()) {
				DocumentPojo d = it.next();
				regexDuplicates = new HashSet<String>();
				
				// For feeds, may need to go get the document text manually, it's a bit horrible since
				// obviously may then go get the data again for full text extraction
				boolean bFetchedUrl = false;
				if (bGetRawDoc && (null == d.getFullText())) {
					try {
						URL url = new URL(d.getUrl());
						InputStream urlStream = url.openStream();
						d.setFullText(new Scanner(urlStream).useDelimiter("\\A").next());
						bFetchedUrl = true;
					}
					catch (Exception e) { // Failed to get full text just carry on, nothing to be done here
						continue;
					}
				}
				long nTime_ms = System.currentTimeMillis();
				//^^^ (end slight hack to get raw text to the UAH for RSS feeds)

				try {
					processBody(d, meta, true);
				}
				catch (Exception e) {
					this._context.getHarvestStatus().logMessage("processBody1: " + e.getMessage(), true);						
					logger.error("processBody1: " + e.getMessage(), e);
				}

				try {
					if (uap.getSimpleTextCleanser() != null)
					{
						cleanseText(source, d);
					}
				}
				catch (Exception e) {
					this._context.getHarvestStatus().logMessage("cleanseText: " + e.getMessage(), true);						
					logger.error("cleanseText: " + e.getMessage(), e);
				}

				try {
					processHeader(headerPattern, d, meta);
					processFooter(footerPattern, d, meta);
				}
				catch (Exception e) {
					this._context.getHarvestStatus().logMessage("header/footerPattern: " + e.getMessage(), true);						
					logger.error("header/footerPattern: " + e.getMessage(), e);
				}
				try {
					processBody(d, meta, false);
				}
				catch (Exception e) {
					this._context.getHarvestStatus().logMessage("processBody2: " + e.getMessage(), true);						
					logger.error("processBody2: " + e.getMessage(), e);
				}
				
				if (it.hasNext() && bFetchedUrl) {
					nTime_ms = nBetweenDocs_ms - (System.currentTimeMillis() - nTime_ms); // (ie delay time - processing time)
					if (nTime_ms > 0) {
						try { Thread.sleep(nTime_ms); } catch (InterruptedException e) {}
					}
				} //(end politeness delay for URL getting from a single source (likely site)
			}
			return documents;
		}
		return new ArrayList<DocumentPojo>();
	}

	

	/**
	 * executeHarvest
	 * For single-feed calls (note exception handling happens in SAH)
	 * @param source
	 * @param doc
	 * @return
	 */
	public boolean executeHarvest(HarvestContext context, SourcePojo source, DocumentPojo doc, boolean bMoreDocs)
	{
		regexDuplicates = new HashSet<String>();
		
		if (-1 == nBetweenDocs_ms) {
			PropertiesManager props = new PropertiesManager();
			nBetweenDocs_ms = props.getWebCrawlWaitTime();
			// Can override the default (feed) wait time from within the source (eg for sites that we know 
			// don't get upset about getting hammered)
			if (null != source.getRssConfig()) {
				if (null != source.getRssConfig().getWaitTimeOverride_ms()) {
					nBetweenDocs_ms = source.getRssConfig().getWaitTimeOverride_ms();
				}
			}
		}
		_context = context;
		UnstructuredAnalysisConfigPojo uap = source.getUnstructuredAnalysisConfig();

		int nChanges = 0;
		if (null != doc.getMetaData()) {
			nChanges = doc.getMetaData().size();
		}
		boolean bFetchedUrl = false;
		if (null == doc.getFullText()) { // Go get data for the URL if it's an RSS feed
			boolean bGetRawDoc = source.getExtractType().equalsIgnoreCase("feed");		
			if (bGetRawDoc) {
				try {
					URL url = new URL(doc.getUrl());
					InputStream urlStream = url.openStream();
					doc.setFullText(new Scanner(urlStream).useDelimiter("\\A").next());
					bFetchedUrl = true;
				}
				catch (Exception e) { // Failed to get full text just carry on, nothing to be done here
					return false;
				}
			}			
		}
		long nTime_ms = System.currentTimeMillis();
		//^^^ (end slight hack to get raw text to the UAH for RSS feeds)		
		
		if (uap != null)
		{
			List<metaField> meta= uap.getMeta();
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
				processBody(doc, meta, true);
			}
			catch (Exception e) {
				this._context.getHarvestStatus().logMessage("processBody1: " + e.getMessage(), true);						
				logger.error("processBody1: " + e.getMessage(), e);
			}
			try {
				if (uap.getSimpleTextCleanser() != null)
				{
					cleanseText(source, doc);
				}
			}
			catch (Exception e) {
				this._context.getHarvestStatus().logMessage("cleanseText: " + e.getMessage(), true);						
				logger.error("cleanseText: " + e.getMessage(), e);
			}
			try {
				processHeader(headerPattern, doc, meta);
				processFooter(footerPattern, doc, meta);
			}
			catch (Exception e) {
				this._context.getHarvestStatus().logMessage("header/footerPattern: " + e.getMessage(), true);						
				logger.error("header/footerPattern: " + e.getMessage(), e);
			}
			try {
				processBody(doc, meta, false);
			}
			catch (Exception e) {
				this._context.getHarvestStatus().logMessage("processBody2: " + e.getMessage(), true);						
				logger.error("processBody2: " + e.getMessage(), e);
			}
		}
		if (bMoreDocs && bFetchedUrl) {
			nTime_ms = nBetweenDocs_ms - (System.currentTimeMillis() - nTime_ms); // (ie delay time - processing time)
			if (nTime_ms > 0) {
				try { Thread.sleep(nTime_ms); } catch (InterruptedException e) {}
			}
		} //(end politeness delay for URL getting from a single source (likely site)
		
		if (null != doc.getMetaData()) {
			if (nChanges != doc.getMetaData().size()) {
				return true;
			}
		}
		return false;
	}	
	
	/**
	 * processHeader
	 * @param headerPattern
	 * @param f
	 * @param meta
	 */
	private void processHeader(Pattern headerPattern, DocumentPojo f, List<metaField> meta)
	{
		if (headerPattern != null)
		{
			Matcher headerMatcher = headerPattern.matcher(f.getFullText());
			String headerText = null;
			while (headerMatcher.find()){
				if (headerMatcher.start() == 0)
				{
					headerText = headerMatcher.group(0);
					f.setHeaderEndIndex(headerText.length());
					for (int i = 1; i < headerMatcher.groupCount()+1; i++)
					{
						f.addToHeader(headerMatcher.group(i).trim());
					}
					break;
				}
			}

			if (null != headerText && null!=meta)
			{
				for (metaField m : meta)
				{
					if(m.context == Context.Header || m.context == Context.All)
					{
						this.processMeta(f, m, headerText);
					}
				}
			}
		}
	}

	
	/**
	 * processFooter
	 * @param footerPattern
	 * @param f
	 * @param meta
	 */
	private void processFooter(Pattern footerPattern, DocumentPojo f, List<metaField> meta)
	{

		if (footerPattern != null)
		{
			Matcher footerMatcher = footerPattern.matcher(f.getFullText());
			String footerText = null;
			while (footerMatcher.find()){
				footerText = footerMatcher.group(0);
				int docLength = f.getFullText().length();
				f.setFooterStartIndex(docLength-footerMatcher.group(0).length());
				for (int i = 1; i < footerMatcher.groupCount()+1; i++)
				{
					f.addToHeader(footerMatcher.group(i).trim());
				}
				break;
			}

			if (null != footerText && null != meta)
			{
				for (metaField m : meta)
				{
					if(m.context == Context.Footer || m.context == Context.All)
					{
						this.processMeta(f, m, footerText);
					}
				}
			}
		}
	}

	
	
	/**
	 * processBody
	 * @param f
	 * @param meta
	 */
	private void processBody(DocumentPojo f, List<metaField> meta, boolean bPreCleansing)
	{
		if (null != meta){
			for (metaField m : meta)
			{
				if( (bPreCleansing  && (m.context == Context.First)) ||
					(!bPreCleansing && (m.context == Context.Body || m.context == Context.All)))
				{
					String toProcess = f.getBody();
					if (toProcess == null)
						toProcess = f.getDescription();
					
					if (null != toProcess) {
						this.processMeta(f, m, toProcess);
					}
				}
			}
		}
	}

	/**
	 * processMeta - handle an individual field 
	 */
	private void processMeta(DocumentPojo f, metaField m, String text) {
		
		if (m.scriptlang.equalsIgnoreCase("regex")) {
		
			Pattern metaPattern = createRegex(m.script, m.flags);
			Matcher matcher = metaPattern.matcher(text);
	
			try {
				while(matcher.find())
				{
					if (null == m.groupNum) {
						m.groupNum = 0;
					}
					String toAdd = matcher.group(m.groupNum);
					if (null != m.replace) {
						toAdd = metaPattern.matcher(toAdd).replaceFirst(m.replace);
					}
					
					if (!regexDuplicates.contains(toAdd)) {
						addToMeta(f,m.fieldName, toAdd);
						regexDuplicates.add(toAdd);
					}
				}
			}
			catch (Exception e) {
				this._context.getHarvestStatus().logMessage("processMeta1: " + e.getMessage(), true);
			}
		}
		else if (m.scriptlang.equalsIgnoreCase("javascript")) {
			if (null == f.getMetadata()) {
				f.setMetadata(new LinkedHashMap<String,Object[]>());
			}
			if (null == factory) { 
				factory = new ScriptEngineManager();
				engine = factory.getEngineByName("JavaScript");
				if (null == parsingScript) {
					parsingScript = generateParsingScript();
				}
				try {
					engine.eval(parsingScript);
				} 
				catch (ScriptException e) { // Just do nothing and log
					e.printStackTrace();
					logger.error(e.getMessage());
				}				
			}
	        engine.put("text", text);
	        
	        try {
				Object returnVal = engine.eval(m.script);
				if (null != returnVal) {
					if (returnVal instanceof String) { // The only easy case
						Object[] array = new Object[1];
						array[0] = returnVal;
						f.addToMetadata(m.fieldName, array);
					}
					else {	// complex object or array - in either case the engine turns these into 
							// internal.NativeArray or internal.NativeObject
						engine.put("output", returnVal);
						BasicDBObject objFactory = new BasicDBObject();
						BasicDBList listFactory = new BasicDBList(); 
						BasicDBList outList = new BasicDBList();
						engine.put("objFactory", objFactory);
						engine.put("listFactory", listFactory);
						engine.put("outList", outList);
												
						engine.eval("s1(output);");
						
						f.addToMetadata(m.fieldName, outList.toArray());
					}
				}				
			} catch (ScriptException e) {
				
				_context.getHarvestStatus().logMessage(HarvestExceptionUtils.createExceptionMessage(e).toString(), true);
				
				// Just do nothing and log
				//e.printStackTrace();
				logger.error(e.getMessage());
			}
		}
		//(don't currently support other script types)
	}

	// First time through, need to generate a script to convert the native JS objects into something
	// we can parse (NativeObject and NativeArrays can't be handled at the "user level" annoyingly)
	
	private static String generateParsingScript() {
		StringBuffer sbSub1 = new StringBuffer();
		sbSub1.append("function s1(el) {").append('\n');
		sbSub1.append(   "if (el == null) {}").append('\n');
		sbSub1.append(   "else if (el instanceof Array) {").append('\n');
		sbSub1.append(      "s2(el, 1);").append('\n');
		sbSub1.append(   "}").append('\n');
		sbSub1.append(   "else if (typeof el == 'object') {").append('\n');
		sbSub1.append(      "outList.add(s3(el));").append('\n');
		sbSub1.append(   "}").append('\n');
		sbSub1.append(   "else {").append('\n');
		sbSub1.append(      "outList.add(el.toString());").append('\n');
		sbSub1.append(   "}").append('\n');
		sbSub1.append("}").append('\n');

		StringBuffer sbSub2 = new StringBuffer();
		sbSub2.append("function s2(el, master_list) {").append('\n');
		sbSub2.append(   "var list = (1 == master_list)?outList:listFactory.clone();").append('\n');
		sbSub2.append(   "for (var i = 0; i < el.length; ++i) {").append('\n');
		sbSub2.append(      "var subel = el[i];").append('\n');
		sbSub2.append(      "if (subel == null) {}").append('\n');
		sbSub2.append(      "else if (subel instanceof Array) {").append('\n');
		sbSub2.append(         "list.add(s2(subel, 0));").append('\n');
		sbSub2.append(      "}").append('\n');
		sbSub2.append(      "else if (typeof subel == 'object') {").append('\n');
		sbSub2.append(         "list.add(s3(subel));").append('\n');
		sbSub2.append(      "}").append('\n');
		sbSub2.append(      "else {").append('\n');
		sbSub2.append(         "list.add(subel.toString());").append('\n');
		sbSub2.append(      "}").append('\n');
		sbSub2.append(   "}").append('\n');
		sbSub2.append("return list; }").append('\n');
		
		StringBuffer sbSub3 = new StringBuffer();
		sbSub3.append("function s3(el) {").append('\n');
		sbSub3.append(   "var currObj = objFactory.clone()").append('\n');
		sbSub3.append(   "for (var prop in el) {").append('\n');
		sbSub3.append(      "var subel = el[prop];").append('\n');
		sbSub3.append(      "if (subel == null) {}").append('\n');
		sbSub3.append(      "else if (subel instanceof Array) {").append('\n');
		sbSub3.append(         "currObj.put(prop, s2(subel, 0));").append('\n');
		sbSub3.append(      "}").append('\n');
		sbSub3.append(      "else if (typeof subel == 'object') {").append('\n');
		sbSub3.append(         "currObj.put(prop, s3(subel));").append('\n');
		sbSub3.append(      "}").append('\n');
		sbSub3.append(      "else {").append('\n');
		sbSub3.append(         "currObj.put(prop, subel.toString());").append('\n');
		sbSub3.append(      "}").append('\n');
		sbSub3.append(   "}").append('\n');
		sbSub3.append("return currObj; }").append('\n');
		
		StringBuffer sbMain = new StringBuffer();
		sbMain.append(sbSub1);
		sbMain.append(sbSub2);
		sbMain.append(sbSub3);

		return sbMain.toString();
	}//TESTED (including null values, converts to string)
	
	/**
	 * addToMeta
	 * @param f
	 * @param fieldName
	 * @param toAdd
	 */
	public void addToMeta(DocumentPojo f,String fieldName, String toAdd)
	{
		LinkedHashMap<String, Object[]> metaData = f.getMetadata();
		if (null == metaData)
		{
			f.addToMetadata(fieldName, toAdd.trim());
		}
		else{
			Object[] o = f.getMetaData().get(fieldName);
			if (null == o)
				f.addToMetadata(fieldName, toAdd.trim());
			else
			{
				int size = o.length;
				Object[] newArray = new Object[size+1];
				for (int i = 0; i < size; i++)
				{
					newArray[i]=o[i];
				}
				newArray[size] = toAdd.trim();
				f.addToMetadata(fieldName, newArray);
			}
		}
	}
	
	/**
	 * cleanseText
	 * @param source
	 * @param documents
	 * @return
	 */
	private void cleanseText(SourcePojo source, DocumentPojo document)
	{
		List<SimpleTextCleanserPojo> simpleTextCleanser = source.getUnstructuredAnalysisConfig().getSimpleTextCleanser();
		// Iterate over the cleanser functions that need to run on each feed
		for (SimpleTextCleanserPojo s : simpleTextCleanser)
		{
			if (s.getField().equalsIgnoreCase("fulltext")) {
				if (null != document.getFullText()) {
					document.setFullText(cleanseField(document.getFullText(), s.getRegEx(), s.getFlags(), s.getReplacement()));
				}
			}
			else if (s.getField().equalsIgnoreCase("description")) {
				if (null != document.getDescription()) {
					document.setDescription(cleanseField(document.getDescription(), s.getRegEx(), s.getFlags(), s.getReplacement()));
				}
			}
			else if (s.getField().equalsIgnoreCase("title")) {
				if (null != document.getTitle()) {
					document.setTitle(cleanseField(document.getTitle(), s.getRegEx(), s.getFlags(), s.getReplacement()));
				}				
			}
			else if (s.getField().startsWith("metadata.")) {
				String metaField = s.getField().substring(9); // (9 for "metadata.")
				Object[] meta = document.getMetadata().get(metaField);
				if ((null != meta) && (meta.length > 0)) {
					Object[] newMeta = new Object[meta.length];
					for (int i = 0; i < meta.length; ++i) {
						Object metaValue = meta[i];
						if (metaValue instanceof String) {
							newMeta[i] = (Object)cleanseField((String)metaValue, s.getRegEx(), s.getFlags(), s.getReplacement());
						}
						else {
							newMeta[i] = metaValue;
						}
					}
					// Overwrite the old fields
					document.addToMetadata(metaField, newMeta);
				}
			}
			// This is sufficient fields for the moment
		}	
	}//TESTED
	
	
	/**
	 * cleanseField
	 * @param field
	 * @param regEx
	 * @param replaceWith
	 */
	private String cleanseField(String field, String regEx, String flags, String replaceWith)
	{
		if (null == flags) {
			return field.replaceAll(regEx, replaceWith);
		}
		else {
			return createRegex(regEx, flags).matcher(field).replaceAll(replaceWith);
		}
	}

	private static Pattern createRegex(String regEx, String flags) {
		int nflags = Pattern.DOTALL; // ('d', by default though)
		
		if (null != flags) {
			for (int i = 0; i < flags.length(); ++i) {
				char c = flags.charAt(i);
				switch (c) {
				case 'm':
					nflags |= Pattern.MULTILINE; break;
				case 'i':
					nflags |= Pattern.CASE_INSENSITIVE; break;
				case 'D':
					nflags ^= Pattern.DOTALL; break; // (ie negate DOTALL)
				case 'u':
					nflags |= Pattern.UNICODE_CASE; break;
				case 'n':
					nflags |= Pattern.UNIX_LINES; break;
				}
			}
		}
		return Pattern.compile(regEx, nflags);
	}
	
}
