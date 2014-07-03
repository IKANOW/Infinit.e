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
package com.ikanow.infinit.e.harvest.extraction.document.file;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;

import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.CompositeParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.bson.types.ObjectId;
import org.xml.sax.ContentHandler;
import org.apache.commons.codec.digest.DigestUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.ikanow.infinit.e.data_model.InfiniteEnums;
import com.ikanow.infinit.e.data_model.InfiniteEnums.ExtractorSourceLevelMajorException;
import com.ikanow.infinit.e.data_model.InfiniteEnums.HarvestEnum;
import com.ikanow.infinit.e.data_model.store.config.source.SourceFileConfigPojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourceFileConfigPojo.StreamingType;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePipelinePojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.data_model.store.document.GeoPojo;
import com.ikanow.infinit.e.harvest.HarvestContext;
import com.ikanow.infinit.e.harvest.extraction.document.HarvesterInterface;
import com.ikanow.infinit.e.harvest.extraction.document.DuplicateManager;
import com.ikanow.infinit.e.harvest.utils.AuthUtils;
import com.ikanow.infinit.e.harvest.utils.HarvestExceptionUtils;
import com.ikanow.infinit.e.harvest.utils.PropertiesManager;

public class FileHarvester implements HarvesterInterface {

	@SuppressWarnings("unused")
	private static final byte[] SP = "                                              ".getBytes();
	private int maxDepth;
	private Set<Integer> sourceTypesCanHarvest = new HashSet<Integer>();
	private int maxDocsPerCycle = Integer.MAX_VALUE;

	@SuppressWarnings("unused")
	private static final String TYPES[] = {
		"TYPE_COMM",
		"TYPE_FILESYSTEM",
		"TYPE_NAMED_PIPE",
		"TYPE_PRINTER",
		"TYPE_SERVER",
		"TYPE_SHARE",
		"TYPE_WORKGROUP"
	};

	private int errors = 0;
	
	// List of Feeds
	private List<DocumentPojo> files = null;
	private List<DocumentPojo> docsToAdd = null;
	private List<DocumentPojo> docsToUpdate = null;
	private List<DocumentPojo> docsToRemove = null;

	private boolean _deleteExistingFilesBySourceKey = false;
	private HashSet<String> sourceUrlsGettingUpdated = null; 
		// (tells us source URLs that are being deleted)
	
	private HarvestContext _context;
	
	// Some internal state
	private boolean _streaming = false; // (new mode, currently unused)
	private boolean _customJob = false; // (some logic is different)
	private Date _customLastRecordWritten = null;
	
	// Formatting office docs: allows HTML/XML output and to push options from the parsers into the tika instance
	private Tika _tika = null;
	ContentHandler _tikaOutputFormat = null;
	StringWriter _tikaXmlFormatWriter;
	ParseContext _tikaOutputParseContext = null;
	
	
	// Can specify regexes to select which files to ignore
	private Pattern includeRegex = null; // files only
	private Pattern excludeRegex = null; // files and paths
	
	// Security:
	private boolean harvestSecureMode = false;
	
	// Try to avoid blowing up the memory:
	private long _memUsage = 0;
	
	/**
	 * Get a specific doc to return the bytes for
	 * @throws Exception 
	 */
	public static byte[] getFile(String fileURL, SourcePojo source ) throws Exception
	{
		InputStream in = null;
		try 
		{
			InfiniteFile searchFile = searchFileShare( source, fileURL);
			
			if ( searchFile == null )
				return null;
			else
			{
				//found the file, return the bytes
				in = searchFile.getInputStream();
				if (null == in)
					return null;
				
				ByteArrayOutputStream buffer = new ByteArrayOutputStream();
				
				int read;
				byte[] data = new byte[16384];
				while ( (read = in.read(data, 0, data.length)) != -1 )
				{
					buffer.write(data,0,read);
				}
				buffer.flush();
				return buffer.toByteArray();
			}			
		} 
		catch (Exception e) 
		{
			throw e;
		}
		finally {
			if (null != in) {
				in.close();
			}
		}
	}
	
	/**
	 * Same as the traverse method but returns the InfiniteFile if it finds searchFile
	 * returns null otherwise
	 * 
	 * @param f
	 * @param source
	 * @param depth
	 * @param searchFile
	 * @return
	 * @throws SmbException
	 */
	
	private static InfiniteFile searchFileShare( SourcePojo source, String searchFile ) throws Exception 
	{
		// Made this synchronized to work around what looks like deadlock issue in code
		// This is undesirable and should be fixed once the underlying bug has been fixed
		// (note in practice this is only an issue for multiple threads going to the same domain)
		InfiniteFile f;
		synchronized (FileHarvester.class) {
			try {
				if (null != source.getProcessingPipeline()) { // new style...
					SourcePipelinePojo firstElement = source.getProcessingPipeline().iterator().next();
					source.setFileConfig(firstElement.file);
					source.setUrl(firstElement.file.getUrl());
				}//TESTED
				if (source.getUrl().startsWith("inf://")) { // Infinit.e share/custom object
					NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(source.getCommunityIds().iterator().next().toString(), source.getOwnerId().toString(), null);
					f = InfiniteFile.create(source.getUrl(), auth);
					
					if (f.isDirectory()) {
						InfiniteFile subs[] = f.listFiles();
						for (InfiniteFile sub: subs) {
							if (sub.isDirectory()) { // (can only nest once)
								InfiniteFile subs2[] = sub.listFiles();
								for (InfiniteFile sub2: subs2) {
									if (sub2.getUrlString().equals(searchFile)) {
										return sub2;
									}//TOTEST
								}								
							}//(end loop ove sub-dirs)
							else if (sub.getUrlString().equals(searchFile)) {
								return sub;
							}//TOTEST
						}//(end loop over dirs)
						
					}//TOTEST
					
				}//TODO (INF-2122): TOTEST
				else if( source.getFileConfig() == null || source.getFileConfig().password == null || source.getFileConfig().username == null)
				{
					f = InfiniteFile.create(searchFile);
				}
				else
				{
					if (source.getFileConfig().domain == null) {
						source.getFileConfig().domain = "";
					}
					NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(source.getFileConfig().domain, source.getFileConfig().username, source.getFileConfig().password);
					f = InfiniteFile.create(searchFile, auth);
				}
			}//TESTED
			catch (Exception e) {
	
				int nIndex = searchFile.lastIndexOf("/");
				searchFile = searchFile.substring(0, nIndex); // (ie not including the /)
				f = searchFileShare(source, searchFile);
				if (f.isDirectory()) {
					throw new MalformedURLException(searchFile + " is directory.");				
				}
			}//TESTED			
			return f;
		}
		// (End INF-1406 sync bug, see above explanation)
	} //TESTED
	
	
	/**
	 * Get the list of docs
	 * @return
	 * @throws Exception 
	 */
	private List<DocumentPojo> getFiles(SourcePojo source) throws Exception {
		InfiniteFile file = null;
		_deleteExistingFilesBySourceKey = false;
		try 
		{
			if (source.getUrl().startsWith("inf://")) { // Infinit.e share/custom object
				NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(Arrays.toString(source.getCommunityIds().toArray()), source.getOwnerId().toString(), null);
				file = InfiniteFile.create(source.getUrl(), auth);	

				if (source.getUrl().startsWith("inf://custom/")) {
					_customJob = true;
					// A few cases: 
					// 1] If first time, or source has completed:
					// Quick check of share/custom date vs last imported doc in this case:
					ObjectId customLastRecordId = null;
					if ((null == source.getHarvestStatus()) || (HarvestEnum.success == source.getHarvestStatus().getHarvest_status()))
					{					
						if (!_context.getDuplicateManager().needsUpdated_Url(new Date(file.getDate()), null, source)) {
							return files;
						}//TESTED			
						else {
							_customLastRecordWritten = _context.getDuplicateManager().getLastModifiedDate();
							customLastRecordId = _context.getDuplicateManager().getLastModifiedDocId();
							_context.getDuplicateManager().resetForNewSource();
								// (reset the saved state since I faked my harvest status)
							_deleteExistingFilesBySourceKey = true;
						}//TESTED
					}
					else { // 2] If in the middle of a multiple harvest cycle....
						// Specifically for custom, need to handle m/r changing ... we'll fake the harvest status
						// to force it to check the last doc's modified time vs the current file time...
						
						HarvestEnum saved = source.getHarvestStatus().getHarvest_status();
						source.getHarvestStatus().setHarvest_status(HarvestEnum.success);
						try {
							// Just doing this so I know if I need to delete everything and restart
							// (the trick here is that all files from a given custom run have the same date
							// (CustomMapReduceJobPojo.lastRunTime_)
							//  even in non-append mode ... so if the file time is different than the most recent doc then
							//  the job must have been re-run)
							if (_context.getDuplicateManager().needsUpdated_Url(new Date(file.getDate()), null, source)) {
								_deleteExistingFilesBySourceKey = true;								
							}
							_customLastRecordWritten = _context.getDuplicateManager().getLastModifiedDate();
							customLastRecordId = _context.getDuplicateManager().getLastModifiedDocId();
							_context.getDuplicateManager().resetForNewSource();
								// (reset the saved state since I faked my harvest status)
						}
						finally { // (rewrite original)
							source.getHarvestStatus().setHarvest_status(saved);
						}
					}//TESTED
					if (_streaming) { // Never delete files...
						_deleteExistingFilesBySourceKey = false;
					}//TESTED
					
					if (null == customLastRecordId) { // no docs, so no need for this
						// (or -in the case of distributed sources- the new harvest has already begun)
						_deleteExistingFilesBySourceKey = false;						
					}//TESTED

					// Incremental updates: never delete anything, only process new objects
					InternalInfiniteFile customHandle = (InternalInfiniteFile)file;
					if (customHandle.isIncremental()) {
						_deleteExistingFilesBySourceKey = false;
					}//TOTEST
					
					// Finally, if we wanted to delete the files then go ahead now:
					if (_deleteExistingFilesBySourceKey) {						
						// For now, support only "non-append" mode efficiently:
						// Always delete all the old docs, updated docs will work but inefficiently (will delete and re-create)
						DocumentPojo docRepresentingSrcKey = new DocumentPojo();
						if (null != source.getDistributionFactor()) {
							// If split across multiple docs then need a more expensive delete (note: still indexed)
							docRepresentingSrcKey.setId(customLastRecordId);
						}
						docRepresentingSrcKey.setCommunityId(source.getCommunityIds().iterator().next());
						docRepresentingSrcKey.setSourceKey(source.getKey());
						this.docsToRemove.add(docRepresentingSrcKey);						
					}//TESTED
				}
				else { // share - this is much simpler:
					if (!_context.getDuplicateManager().needsUpdated_Url(new Date(file.getDate()), null, source)) {
						return files;
					}//TESTED					
				}
				
			}//TESTED
			else if( source.getFileConfig() == null || source.getFileConfig().password == null || source.getFileConfig().username == null)
			{
				// Local file: => must be admin to continue
				if (harvestSecureMode) { // secure mode, must be admin
					if (source.getUrl().startsWith("file:")) {
						if (!AuthUtils.isAdmin(source.getOwnerId())) {
							throw new ExtractorSourceLevelMajorException("Permission denied");
						}
					}
				}//TODO (INF-2119): come up with something better than this...(this is at least consistent with SAH/UAH security, apart from allowing admin more rights)
				file = InfiniteFile.create(source.getUrl());
			}
			else
			{
				if (source.getFileConfig().domain == null) {
					source.getFileConfig().domain = "";
				}
				NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(source.getFileConfig().domain, source.getFileConfig().username, source.getFileConfig().password);
				file = InfiniteFile.create(source.getUrl(), auth);
			}
			traverse(file, source, maxDepth);
		} 
		catch (Exception e) {
			// If an exception here this is catastrophic, throw it upwards:
			errors++;
			throw e;
		}
		
		return files;
	}

	/**
	 * Constructor for processing doc information for a source
	 * @param maxDepth
	 */
	public FileHarvester()
	{
		sourceTypesCanHarvest.add(InfiniteEnums.FILES);
		maxDepth = 5;

		PropertiesManager pm = new PropertiesManager();
		maxDocsPerCycle = pm.getMaxDocsPerSource();
		harvestSecureMode = pm.getHarvestSecurity();
	}

	// Process the doc
	private void processFiles(SourcePojo source) throws Exception {

		// Can override system settings if less:
		if ((null != source.getThrottleDocs()) && (source.getThrottleDocs() < maxDocsPerCycle)) {
			maxDocsPerCycle = source.getThrottleDocs();
		}		
		sourceUrlsGettingUpdated = new HashSet<String>();
		LinkedList<String> duplicateSources = new LinkedList<String>(); 		
		try {			
			// Compile regexes if they are present
			if ((null != source.getFileConfig()) && (null != source.getFileConfig().pathInclude)) {
				includeRegex = Pattern.compile(source.getFileConfig().pathInclude, Pattern.CASE_INSENSITIVE);
			}
			if ((null != source.getFileConfig()) && (null != source.getFileConfig().pathExclude)) {
				excludeRegex = Pattern.compile(source.getFileConfig().pathExclude, Pattern.CASE_INSENSITIVE);				
			}
			if ((null != source.getFileConfig()) && (null != source.getFileConfig().maxDepth)) {
				this.maxDepth = source.getFileConfig().maxDepth;
			}
			
			// Process the fileshare
			getFiles(source);	
		}
		catch (Exception e) {
			// If an exception here this is catastrophic, throw it upwards:
			errors++;
			throw e;
		}

		try {			
			//Dedup code, ironically enough partly duplicated in parse(), probably unnecessarily
			DuplicateManager qr = _context.getDuplicateManager();
			for(DocumentPojo doc: files)
			{
				try {			
					duplicateSources.clear();
					if (null != doc.getSourceUrl()) { 

						boolean add = true;

						// However still need to check for duplicates so can update entities correctly (+maintain _ids, etc)
						// We only do this if the source URL changes (unless URL is taken from the object in which case all bets are off) 
						
						boolean sourceUrlUpdated = sourceUrlsGettingUpdated.contains(doc.getSourceUrl());
						if (!doc.getHasDefaultUrl() || sourceUrlUpdated) { // src URL for a given URL							
							// (only if the the sourceUrl is not new...)
							if (qr.isDuplicate_Url(doc.getUrl(), source, duplicateSources)) {
								doc.setUpdateId(qr.getLastDuplicateId()); // (set _id to doc we're going to replace)
								
								if (!sourceUrlUpdated && !_deleteExistingFilesBySourceKey) {
									// Here update instead so we delete the old doc and add the new one
									add = false;
									docsToUpdate.add(doc);
								}//TESTED
								else {
									// (else *still* don't add this to updates because we've added the source URL or source key to the delete list)
									// (hence approximate create with the updateId...)
									if (null != doc.getUpdateId()) {
										doc.setCreated(new Date(doc.getUpdateId().getTime()));
									}//TESTED									
								}//TESTED
							}
							//(note we don't get about duplicate sources in this case - just too complex+rare a case)
							
						}//TESTED (src url changing, different src url, non-default URL)
						
						// For composite files we (almost always) delete everything that already exists (via docsToRemove) and then add new docs
						if (add) {
							docsToAdd.add(doc);
						}						
						//TESTED
					}
					else if (qr.isDuplicate_Url(doc.getUrl(), source, duplicateSources)) {
						// Other files, if the file already exists then update it (essentially, delete/add)
						doc.setUpdateId(qr.getLastDuplicateId()); // (set _id to doc we're going to replace)
						docsToUpdate.add(doc);
					}
					else { // if duplicateSources is non-empty then this URL is a duplicate of one from a different source 
						if (!duplicateSources.isEmpty()) { 
							doc.setDuplicateFrom(duplicateSources.getFirst());
						}
						docsToAdd.add(doc);
					}
				}
				catch (Exception e) {
					errors++;
					_context.getHarvestStatus().logMessage(HarvestExceptionUtils.createExceptionMessage(e).toString(), true);
				}
			}
		}
		catch (Exception e) {
			// If an exception here this is catastrophic, throw it upwards:
			errors++;
			throw e;
		}
	}

	private void parse( InfiniteFile f, SourcePojo source ) throws MalformedURLException, URISyntaxException {

		//NOTE: we only ever break out of here because of max docs in standalone mode
		// (because we don't know how to continue reading)
		
		DocumentPojo doc = null;		
		//Determine File Extension
		String fileName = f.getName().toString();
		
		int mid= fileName.lastIndexOf(".");
		String extension = fileName.substring(mid+1,fileName.length()); 

		//Checked to save processing time
		long fileTimestamp = (f.getDate()/1000)*1000;
			// (ensure truncated to seconds, since some operation somewhere hear does this...)
		
		Date modDate = new Date(fileTimestamp);
		//XML Data gets placed into MetaData
		
		boolean bIsXml = false;
		boolean bIsJson = false;
		boolean bIsLineOriented = false;
		if ((null != source.getFileConfig()) && (null != source.getFileConfig().type)) {
			extension = source.getFileConfig().type;
		}
		bIsXml = extension.equalsIgnoreCase("xml");
		bIsJson = extension.equalsIgnoreCase("json");
		bIsLineOriented = extension.endsWith("sv");
		
		if (bIsXml || bIsJson || bIsLineOriented)
		{
			int debugMaxDocs =  Integer.MAX_VALUE; // by default don't set this, it's only for debug mode
			if (_context.isStandalone()) { // debug mode
				debugMaxDocs = maxDocsPerCycle; 
			}			
			
			//fast check to see if the file has changed before processing (or if it never existed)
			if(needsUpdated_SourceUrl(modDate, f.getUrlString(), source))
			{
				if (0 != modDate.getTime()) { // if it ==0 then sourceUrl doesn't exist at all, no need to delete
					// This file already exists - in normal/managed mode will re-create
					// In streaming mode, simple skip over
					if (_streaming) {
						return;
					}//TESTED
					
					DocumentPojo docRepresentingSrcUrl = new DocumentPojo();
					docRepresentingSrcUrl.setSourceUrl(f.getUrlString());
					docRepresentingSrcUrl.setSourceKey(source.getKey());
					docRepresentingSrcUrl.setCommunityId(source.getCommunityIds().iterator().next());
					sourceUrlsGettingUpdated.add(docRepresentingSrcUrl.getSourceUrl());
					this.docsToRemove.add(docRepresentingSrcUrl);
						// (can add documents with just source URL, are treated differently in the core libraries)					
				}
				
				SourceFileConfigPojo fileSystem = source.getFileConfig();
				if ((null == fileSystem) && (bIsXml || bIsJson)) {
					fileSystem = new SourceFileConfigPojo();
				}
				XmlToMetadataParser xmlParser = null;
				JsonToMetadataParser jsonParser = null;
				String urlType = extension;
				if (bIsXml) {
					xmlParser = new XmlToMetadataParser(fileSystem.XmlRootLevelValues, 
										fileSystem.XmlIgnoreValues, fileSystem.XmlSourceName, fileSystem.XmlPrimaryKey, 
										fileSystem.XmlAttributePrefix, fileSystem.XmlPreserveCase, debugMaxDocs);
				}//TESTED
				else if (bIsJson) {
					jsonParser = new JsonToMetadataParser(fileSystem.XmlSourceName, fileSystem.XmlRootLevelValues, fileSystem.XmlPrimaryKey, fileSystem.XmlIgnoreValues, debugMaxDocs);
				}//TESTED
				
				List<DocumentPojo> partials = null;
				try {
					if (bIsXml) {
						XMLStreamReader xmlStreamReader = null;
						XMLInputFactory factory = XMLInputFactory.newInstance();
						factory.setProperty(XMLInputFactory.IS_COALESCING, true);
						factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
						try {							
							xmlStreamReader = factory.createXMLStreamReader(f.getInputStream());
							partials = xmlParser.parseDocument(xmlStreamReader);
							_memUsage += xmlParser.getMemUsage();
						}
						finally {
							if (null != xmlStreamReader) xmlStreamReader.close();
						}
					}//TESTED
					else if (bIsJson) {
						JsonReader jsonReader = null;
						try {							
							jsonReader = new JsonReader(new InputStreamReader(f.getInputStream(), "UTF-8"));
							jsonReader.setLenient(true);
							partials = jsonParser.parseDocument(jsonReader);
							_memUsage += jsonParser.getMemUsage();
						}
						finally {
							if (null != jsonReader) jsonReader.close();
						}
					}//TESTED
					else if (bIsLineOriented) { // Just generate a document for every line
						
						BufferedReader lineReader = null;
						try {
							lineReader = new BufferedReader(new InputStreamReader(f.getInputStream(), "UTF-8"));
							CsvToMetadataParser lineParser = new CsvToMetadataParser(debugMaxDocs);
							partials = lineParser.parseDocument(lineReader, source);
							_memUsage += lineParser.getMemUsage();							
						}
						finally {
							if (null != lineReader) lineReader.close();
						}
					}//TESTED

					MessageDigest md5 = null; // (generates unique urls if the user doesn't below)
					try {
						md5 = MessageDigest.getInstance("MD5");
					} catch (NoSuchAlgorithmException e) {
						// Do nothing, unlikely to happen...
					}					
					int nIndex = 0;
					int numPartials = partials.size();					
					for (DocumentPojo doctoAdd : partials)
					{
						nIndex++;
						doctoAdd.setSource(source.getTitle());
						doctoAdd.setSourceKey(source.getKey());
						doctoAdd.setMediaType(source.getMediaType());
						doctoAdd.setModified(new Date(fileTimestamp));
						doctoAdd.setCreated(new Date());				
						
						if(null == doctoAdd.getUrl()) { // Can be set in the parser or here
							doctoAdd.setHasDefaultUrl(true); // (ie cannot occur in a different src URL)
							
							if (1 == numPartials) {
								String urlString = f.getUrlString();
								if (urlString.endsWith(urlType)) {
									doctoAdd.setUrl(urlString);
								}
								else {
									doctoAdd.setUrl(new StringBuffer(urlString).append('.').append(urlType).toString());
								}
								// (we always set sourceUrl as the true url of the file, so want to differentiate the URL with
								//  some useful information)
							}
							else if (null == doctoAdd.getMetadata()) { // Line oriented case
								doctoAdd.setUrl(new StringBuffer(f.getUrlString()).append("/").append(nIndex).append('.').append(urlType).toString());
							}
							else {
								if (null == md5) { // Will never happen, MD5 always exists
									doctoAdd.setUrl(new StringBuffer(f.getUrlString()).append("/").append(doctoAdd.getMetadata().hashCode()).append('.').append(urlType).toString());
								}
								else { // This is the standard call if the XML parser has not been configured to build the URL
									doctoAdd.setUrl(new StringBuffer(f.getUrlString()).append("/").append(DigestUtils.md5Hex(doctoAdd.getMetadata().toString())).append('.').append(urlType).toString());
								}
							}//TESTED
						}						
						doctoAdd.setTitle(f.getName().toString());
						doctoAdd.setPublishedDate(new Date(fileTimestamp));
						doctoAdd.setSourceUrl(f.getUrlString());

						// Always add to files because I'm deleting the source URL
						files.add(doctoAdd);						
					}//TESTED 
					
				} catch (XMLStreamException e1) {
					errors++;
					_context.getHarvestStatus().logMessage(HarvestExceptionUtils.createExceptionMessage(e1).toString(), true);
				} catch (FactoryConfigurationError e1) {
					errors++;
					_context.getHarvestStatus().logMessage(e1.getMessage(), true);
					
				} catch (IOException e1) {
					errors++;
					_context.getHarvestStatus().logMessage(HarvestExceptionUtils.createExceptionMessage(e1).toString(), true);
				}
				catch (Exception e1) {
					errors++;
					_context.getHarvestStatus().logMessage(HarvestExceptionUtils.createExceptionMessage(e1).toString(), true);					
				}
			}//(end if needs updated)
		}
		else //Tika supports Excel,Word,Powerpoint,Visio, & Outlook Documents
		{
			// (This dedup tells me if it's an add/update vs ignore - qr.isDuplicate higher up tells me if I need to add or update)
			if(needsUpdated_Url(modDate, f.getUrlString(), source))
			{

				Metadata metadata = null;
				InputStream in = null;
				try {

					doc = new DocumentPojo();
					
					// Create a tika object (first time only)
					if (null == _tika) {
						this.initializeTika(_context, source);
					}
					
					// BUGGERY
					// NEED TO LIKELY SET LIMIT TO BE 30MB or 50MB and BYPASS ANYTHING OVER THAT BELOW IS THE CODE TO DO THAT
					// tika.setMaxStringLength(30*1024*1024);
					// Disable the string length limit
					_tika.setMaxStringLength(-1);
					//input = new FileInputStream(new File(resourceLocation));
					// Create a metadata object to contain the metadata
					
					metadata = new Metadata();
					// Parse the file and get the text of the file
					doc.setSource(source.getTitle());
					doc.setSourceKey(source.getKey());
					doc.setMediaType(source.getMediaType());
					String fullText = "";
					
					in = f.getInputStream();
					try {
						if (null == _tikaOutputFormat) { // text only
							fullText = _tika.parseToString(in, metadata);
						}//TESTED
						else { // XML/HMTL
							_tika.getParser().parse(in, _tikaOutputFormat, metadata, _tikaOutputParseContext);
							fullText = _tikaXmlFormatWriter.toString();
							_tikaXmlFormatWriter.getBuffer().setLength(0);
						}//TESTED
					}
					finally {
						if (null != in) in.close();
					}
					int descCap = 500;
					doc.setFullText(fullText);
					if (descCap > fullText.length())
					{
						descCap = fullText.length();
					}
					doc.setDescription(fullText.substring(0,descCap));
					doc.setModified(new Date(fileTimestamp));
					doc.setCreated(new Date());
					doc.setUrl(f.getUrlString());
					doc.setTitle(f.getName().toString());
					doc.setPublishedDate(new Date(fileTimestamp));
					
					_memUsage += (250L*(doc.getFullText().length() + doc.getDescription().length()))/100L; // 25% overhead, 2x for string->byte
					
					// If the metadata contains a more plausible date then use that
					try {
						String title = metadata.get(Metadata.TITLE);
						if (null != title) {
							doc.setTitle(title);
						}
					}
					catch (Exception e) { // Fine just carry on						
					}
					try { 
						Date date = metadata.getDate(Metadata.CREATION_DATE); // MS Word
						if (null != date) { 
							doc.setPublishedDate(date);
						}
						else {
							date = metadata.getDate(Metadata.DATE); // Dublin
							if (null != date) {
								doc.setPublishedDate(date);
							}
							else {
								date = metadata.getDate(Metadata.ORIGINAL_DATE);
								if (null != date) {
									doc.setPublishedDate(date);
								}
							}
						}
					}
					catch (Exception e) { // Fine just carry on						
					}
					//TESTED
					
					// If the metadata contains a geotag then apply that:
					try {
						String lat = metadata.get(Metadata.LATITUDE);
						String lon = metadata.get(Metadata.LONGITUDE);
						if ((null != lat) && (null != lon)) {
							GeoPojo gt = new GeoPojo();
							gt.lat = Double.parseDouble(lat);
							gt.lon = Double.parseDouble(lon);
							doc.setDocGeo(gt);
						}
					}
					catch (Exception e) { // Fine just carry on						
					}
					
					// Save the entire metadata:
					doc.addToMetadata("_FILE_METADATA_", metadata);

					for(ObjectId communityId: source.getCommunityIds())
					{
						doc.setCommunityId(communityId);
					}
					files.add(doc);

					// Close the input stream
					in.close();
					in = null;

					//TESTED

				} catch (SmbException e) {
					errors++;
					_context.getHarvestStatus().logMessage(HarvestExceptionUtils.createExceptionMessage(e).toString(), true);
				} catch (MalformedURLException e) {
					errors++;
					_context.getHarvestStatus().logMessage(HarvestExceptionUtils.createExceptionMessage(e).toString(), true);
				} catch (UnknownHostException e) {
					errors++;
					_context.getHarvestStatus().logMessage(HarvestExceptionUtils.createExceptionMessage(e).toString(), true);
				}
				catch (IOException e) {
					errors++;
					_context.getHarvestStatus().logMessage(e.getMessage(), true);
				} catch (TikaException e) {
					errors++;
					_context.getHarvestStatus().logMessage(e.getMessage(), true);
				}
				catch (Exception e) {
					errors++;
					_context.getHarvestStatus().logMessage(HarvestExceptionUtils.createExceptionMessage(e).toString(), true);
				}
				finally { // Close the input stream if an error occurs
					if (null != in) {
						try {
							in.close();
						} catch (IOException e) {
							// All good, do nothing
						}
					}
				} // end exception handling
			} // end dedup check
		} // end XML vs "office" app
		
		//DEBUG
		//System.out.println("FILE=" + files.size() + " / MEM=" + _memUsage + " VS " + Runtime.getRuntime().totalMemory());
	}

	private void traverse( InfiniteFile f, SourcePojo source, int depth ) throws Exception {
		if( depth == 0 ) {
			return;
		}

		InfiniteFile[] l;
		try {
			// Made this synchronized to work around what looks like deadlock issue in code
			// This is undesirable and should be fixed once the underlying bug has been fixed
			// (note in practice this is only an issue for multiple threads going to the same domain)
			synchronized (FileHarvester.class) {
				if (_customJob && (null != _customLastRecordWritten)) {
					l = f.listFiles(_customLastRecordWritten);
				}
				else {
					l = f.listFiles();
				}
				
				for(int i = 0; l != null && i < l.length; i++ ) {
					if (null == l[i]) break; // (reached the end of the list)
				
					// Check what the deal with memory usage is:
					// (Allow 25% of current heap)
					if ((_memUsage*4) > Runtime.getRuntime().maxMemory()) {						
						source.setReachedMaxDocs();						
						break;
					}//TESTED
					
					// Check to see if the item is a directory or a file that needs to parsed
					// if it is a file then parse the sucker using tika 
					// if it is a directory then use recursion to dive into the directory
					if (files.size() >= this.maxDocsPerCycle) {
						source.setReachedMaxDocs();						
						break;
					}
					if( l[i].isDirectory() ) {
						// Directories: included unless explicity exclude:
						String path = l[i].getUrlPath();
						boolean bProcess = true;
						if (null != excludeRegex) {
							if (excludeRegex.matcher(path).matches()) {
								bProcess = false;
							}							
						}//TESTED
						if (bProcess) {
							traverse( l[i], source, depth - 1 );
							if (source.reachedMaxDocs()) { // (express elevator back to recursion root)
								return;
							}
						}
					}
					else {
						boolean bProcess = true;
						// Files: check both include and exclude and distribution logic
						String path = l[i].getUrlPath();
						
						// Intra-source distribution logic:
						if ((null != source.getDistributionTokens()) && (null != source.getDistributionFactor())) {
							int split = Math.abs(path.hashCode()) % source.getDistributionFactor();
							if (!source.getDistributionTokens().contains(split)) {
								bProcess = false;
							}
						}//TESTED
						
						if (bProcess && (null != includeRegex)) {
							if (!includeRegex.matcher(path).matches()) {
								bProcess = false;
							}
						}
						if (bProcess && (null != excludeRegex)) {
							if (excludeRegex.matcher(path).matches()) {
								bProcess = false;
							}							
						}//TESTED
						if (bProcess) {
							parse( l[i], source);
								// (Adds to this.files)
							
							// If we've got here, check what we should do with the file
							if (!_context.isStandalone()) {
								if ((null != source.getFileConfig()) && (null != source.getFileConfig().renameAfterParse)) {
									try {
										if (source.getFileConfig().renameAfterParse.isEmpty() || source.getFileConfig().renameAfterParse.equals(".")) 
										{ // delete it
											l[i].delete();
										}//TESTED
										else {
											l[i].rename(createNewName(l[i], source.getFileConfig().renameAfterParse));
										}//TESTED
									}
									catch (IOException e) { // doesn't seem worth bombing out but should error
										_context.getHarvestStatus().logMessage(HarvestExceptionUtils.createExceptionMessage(e).toString(), true);									
									}
								}//TESTED
							}
						}//(not excluded)
					}//(file not directory)
				}//(end loop over directory files)
			}
			// (End INF-1406 sync bug, see above explanation)

		} catch (Exception e) {
			if (maxDepth == depth) { // Top level error, abandon ship
				errors++;
				throw e;
			}
			else { // Already had some luck with this URL keep going			
				errors++;
				_context.getHarvestStatus().logMessage(HarvestExceptionUtils.createExceptionMessage(e).toString(), true);
			}
		}
	}


	private boolean needsUpdated_SourceUrl(Date mod, String sourceUrl, SourcePojo source)
	{
		try {					
			DuplicateManager qr = _context.getDuplicateManager();
			return qr.needsUpdated_SourceUrl(mod, sourceUrl, source);
		} 
		catch (Exception e) {
			// Do nothing
		} 
		return false;
	}

	private boolean needsUpdated_Url(Date mod, String url, SourcePojo source)
	{
		try {					
			DuplicateManager qr = _context.getDuplicateManager();

			return qr.needsUpdated_Url(mod, url, source);
		} 
		catch (Exception e) {
			// Do nothing
		} 
		return false;
	}
	@Override
	public boolean canHarvestType(int sourceType) {
		return sourceTypesCanHarvest.contains(sourceType);
	}

	@Override
	public void executeHarvest(HarvestContext context, SourcePojo source, List<DocumentPojo> toAdd, List<DocumentPojo> toUpdate, List<DocumentPojo> toRemove) {
				
		_context = context;
		if (_context.isStandalone()) {
			maxDocsPerCycle = _context.getStandaloneMaxDocs();
		}
		try 
		{
			// Defaults to some "normal" mode that involves trying to spot existing files that have been modified and re-creating their harvested docs
			// In streaming mode it will just skip over those files and carry on
			// (It should be particularly useful for custom mode, can just re-run the same job on he last day's data and the source will keep adding them)
			if ((null != source.getFileConfig()) && (null != source.getFileConfig().mode) 
					&& (StreamingType.streaming == source.getFileConfig().mode))
			{
				_streaming = true;
			}
			
			//logger.debug("Source: " + source.getUrl());

			//create new list for files
			this.files = new LinkedList<DocumentPojo>();
			this.docsToAdd = toAdd;
			this.docsToUpdate = toUpdate;
			this.docsToRemove = toRemove;
			processFiles(source);
			
			//harvested "successfully", post in mongo
			String logMsg = (0 == errors)?(""):(new StringBuffer().append(errors).append(" file error(s).").toString());
			_context.getHarvestStatus().update(source, new Date(), HarvestEnum.in_progress, logMsg, false, false);	
		}
		catch (Exception e)
		{
			errors++;
			_context.getHarvestStatus().update(source,new Date(),HarvestEnum.error,e.getMessage(), true, false);
		}		
		finally {
			// (ie these can be deleted once the harvest is complete)
			this.files = null;
			this.docsToAdd = null;
			this.docsToUpdate = null;
			this.docsToRemove = null;			
		}
	}
	
	// Renaming utility
	
	private static String createNewName(InfiniteFile subFile, String replacement) throws MalformedURLException, UnsupportedEncodingException, URISyntaxException {
		String path = subFile.getUrlString(); // (currently the entire string)
		String name = subFile.getName();
		int startOfName = path.lastIndexOf(name);
		return replacement.replace("$name", name).replace("$path", path.substring(0, startOfName - 1));
	}


	/////////////////////////////////////////////////////////////////////////////////////
	
	// Get tika options:
	// Bonus option output:xhtml|text
	// Example option: "application/pdf:{setEnableAutoSpace:false}", ie format is mediaType:JSON
	// where JSON is key/value pairs for the function name and the arg (only String, bool, int/long/double types are possible)
	
	private void initializeTika(HarvestContext context, SourcePojo source)
	{
		AutoDetectParser autoDetectParser = new AutoDetectParser();
		
		if (null != source.getFileConfig().XmlRootLevelValues) {
			for (String s: source.getFileConfig().XmlRootLevelValues) {
				int separator = s.indexOf(':');
				String jsonStr = s.substring(separator + 1);
				
				if (separator > 0) {
					String mediaType = s.substring(0, separator);
					if (mediaType.equalsIgnoreCase("output")) { //special case, just going to configure output
						if (jsonStr.equalsIgnoreCase("xml") || jsonStr.equalsIgnoreCase("xhtml")) {
							_tikaXmlFormatWriter = new StringWriter();
							_tikaOutputFormat = getTransformerHandler("xml", _tikaXmlFormatWriter);
							_tikaOutputParseContext = new ParseContext();
						}
						if (jsonStr.equalsIgnoreCase("html")) {
							_tikaXmlFormatWriter = new StringWriter();
							_tikaOutputFormat = getTransformerHandler("html", _tikaXmlFormatWriter);
							_tikaOutputParseContext = new ParseContext();
						}
						continue;
					}//TESTED
					
					// Try to get media type parser:
					
					Parser p = autoDetectParser.getParsers().get(MediaType.parse(mediaType));
					while (p instanceof CompositeParser) {
						p = ((CompositeParser)p).getParsers().get(MediaType.parse(mediaType));
					}
					if (null == p) {
						context.getHarvestStatus().logMessage("Failed to find application type " + mediaType + " in tika option: " + s, true);
						continue;
					}//TESTED
					
					// Get JSON objects and try to apply
					
					try {
						JsonElement jsonObj = new JsonParser().parse(jsonStr);
						for (Map.Entry<String, JsonElement> keyVal: jsonObj.getAsJsonObject().entrySet()) {
							if (keyVal.getValue().getAsJsonPrimitive().isBoolean()) { //boolean
								try {
									Method method = p.getClass().getMethod(keyVal.getKey(), Boolean.class);
									method.invoke(p, (Boolean)keyVal.getValue().getAsJsonPrimitive().getAsBoolean());
								}
								catch (Exception e) { 
									try {
										Method method = p.getClass().getMethod(keyVal.getKey(), Boolean.TYPE);
										method.invoke(p, keyVal.getValue().getAsJsonPrimitive().getAsBoolean());
									}
									catch (Exception e2) { 
										context.getHarvestStatus().logMessage("Failed to invoke " + keyVal.getKey() + " in tika option: " + s, true);
										continue;
									}//TESTED
								}								
							}//TESTED
							if (keyVal.getValue().getAsJsonPrimitive().isString()) { //string
								try {
									Method method = p.getClass().getMethod(keyVal.getKey(), String.class);
									method.invoke(p, keyVal.getValue().getAsJsonPrimitive().getAsString());
								}
								catch (Exception e) { 
									context.getHarvestStatus().logMessage("Failed to invoke " + keyVal.getKey() + " in tika option: " + s, true);
									continue;
								}
							}//TESTED (cut and paste)
							if (keyVal.getValue().getAsJsonPrimitive().isNumber()) { // number: int/long/double
								// Loads of options: Integer.class, Integer.TYPE, Long.class, Long.TYPE, Double.long, Double.TYPE
								boolean invoked = false;
								if (!invoked) { // Int.class
									try {
										Method method = p.getClass().getMethod(keyVal.getKey(), Integer.class);
										method.invoke(p, (Integer)keyVal.getValue().getAsJsonPrimitive().getAsInt());
										invoked = true;
									}
									catch (Exception e) {}
								}
								if (!invoked) { // Int.type
									try {
										Method method = p.getClass().getMethod(keyVal.getKey(), Integer.TYPE);
										method.invoke(p, keyVal.getValue().getAsJsonPrimitive().getAsInt());
										invoked = true;
									}
									catch (Exception e) {}
								}
								if (!invoked) { // Long.class
									try {
										Method method = p.getClass().getMethod(keyVal.getKey(), Long.class);
										method.invoke(p, (Long)keyVal.getValue().getAsJsonPrimitive().getAsLong());
										invoked = true;
									}
									catch (Exception e) {}
								}
								if (!invoked) { // Long.type
									try {
										Method method = p.getClass().getMethod(keyVal.getKey(), Long.TYPE);
										method.invoke(p, keyVal.getValue().getAsJsonPrimitive().getAsLong());
										invoked = true;
									}
									catch (Exception e) {}
								}
								if (!invoked) { // Double.class
									try {
										Method method = p.getClass().getMethod(keyVal.getKey(), Double.class);
										method.invoke(p, (Double)keyVal.getValue().getAsJsonPrimitive().getAsDouble());
										invoked = true;
									}
									catch (Exception e) {}
								}
								if (!invoked) { // Double.type
									try {
										Method method = p.getClass().getMethod(keyVal.getKey(), Double.TYPE);
										method.invoke(p, keyVal.getValue().getAsJsonPrimitive().getAsDouble());
										invoked = true;
									}
									catch (Exception e) {}
								}
							}//TOTEST (all the different options)
							
						}//(end loop over options)
					}
					catch (Exception e) {
						context.getHarvestStatus().logMessage("Failed to parse JSON in tika option: " + s, true);						
					}//TESTED
				}
				else {
					context.getHarvestStatus().logMessage("Failed to parse tika option: " + s, true);
				}//TESTED
				
			}//TESTED
		}//(end if has options)
		
		_tika = new Tika(TikaConfig.getDefaultConfig().getDetector(), autoDetectParser);
		
	}//TESTED (apart from unused number option configuration)
	
	// (See http://stackoverflow.com/questions/9051183/how-to-use-tikas-xwpfwordextractordecorator-class)
	 private static TransformerHandler getTransformerHandler(String method, StringWriter sw) 
	 {
		 try {
	        SAXTransformerFactory factory = (SAXTransformerFactory)SAXTransformerFactory.newInstance();
	        TransformerHandler handler = factory.newTransformerHandler();
	        handler.getTransformer().setOutputProperty(OutputKeys.METHOD, method);
	        handler.getTransformer().setOutputProperty(OutputKeys.INDENT, "yes");
	        handler.setResult(new StreamResult(sw));
	        return handler;
		 }
		 catch (Exception e) {
			 return null;
		 }
	 }//TESTED	
}
