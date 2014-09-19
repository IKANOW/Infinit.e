package com.ikanow.infinit.e.harvest.extraction.text.legacy;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.Map;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.CompositeParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.txt.TXTParser;
import org.xml.sax.ContentHandler;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.ikanow.infinit.e.data_model.InfiniteEnums.ExtractorDailyLimitExceededException;
import com.ikanow.infinit.e.data_model.InfiniteEnums.ExtractorDocumentLevelException;
import com.ikanow.infinit.e.data_model.interfaces.harvest.EntityExtractorEnum;
import com.ikanow.infinit.e.data_model.interfaces.harvest.ITextExtractor;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.data_model.store.document.GeoPojo;
import com.ikanow.infinit.e.data_model.utils.IkanowSecurityManager;
import com.ikanow.infinit.e.harvest.utils.ProxyManager;

public class TextExtractorTika implements ITextExtractor {

	@Override
	public String getName() {
		return "Tika";
	}
	private Tika _tika = null;
	ContentHandler _tikaOutputFormat = null;
	StringWriter _tikaXmlFormatWriter;
	ParseContext _tikaOutputParseContext = null;
	
	protected IkanowSecurityManager _secManager = null;
	
	@Override
	public void extractText(DocumentPojo partialDoc)
			throws ExtractorDailyLimitExceededException,
			ExtractorDocumentLevelException
	{
		if (null == _secManager) {
			_secManager = new IkanowSecurityManager();
		}
		
		if (null == _tika) {
			initializeTika(partialDoc.getTempSource());
		}
		
		try {
			
			// Disable the string length limit
			_tika.setMaxStringLength(-1);
			//input = new FileInputStream(new File(resourceLocation));
			// Create a metadata object to contain the metadata
			Metadata metadata = new Metadata();
			// Parse the file and get the text of the file
			InputStream urlStream = null;
			
			if (null == partialDoc.getFullText()) {
				URL url = new URL(partialDoc.getUrl());
				String proxyOverride = null;
				if ((null != partialDoc.getTempSource()) && 
						(null != partialDoc.getTempSource().getRssConfig())) 
				{
					proxyOverride = partialDoc.getTempSource().getRssConfig().getProxyOverride();
				}						
				URLConnection urlConnect = url.openConnection(ProxyManager.getProxy(url, proxyOverride));
				if (null != partialDoc.getTempSource() && (null != partialDoc.getTempSource().getRssConfig())) {
					if (null != partialDoc.getTempSource().getRssConfig().getUserAgent()) {
						urlConnect.setRequestProperty("User-Agent", partialDoc.getTempSource().getRssConfig().getUserAgent());
					}// TESTED
					if (null != partialDoc.getTempSource().getRssConfig().getHttpFields()) {
						for (Map.Entry<String, String> httpFieldPair: partialDoc.getTempSource().getRssConfig().getHttpFields().entrySet()) {
							urlConnect.setRequestProperty(httpFieldPair.getKey(), httpFieldPair.getValue());														
						}
					}//TESTED
				}
				
			if (null != _secManager) { // ie turn on...
				_secManager.setSecureFlag(true);
			}//TESTED (c/p TextExtractorBoilerpipe)
			
			try {
				urlStream = urlConnect.getInputStream();
			}
			catch (Exception e) { // Try one more time, this time exception out all the way
				urlStream = urlConnect.getInputStream();					 
			}
			finally {
				if (null != _secManager) { // ie turn back off again...
					_secManager.setSecureFlag(false);
				}								
			}//TESTED (c/p TextExtractorBoilerpipe)		
			}
			else { // already have text, run this through tika
				urlStream = new ByteArrayInputStream(partialDoc.getFullText().getBytes());
			}
			
			
			
			String fullText = _tika.parseToString(urlStream, metadata);
			partialDoc.setFullText(fullText);				

			if ((null != fullText) && (null == partialDoc.getDescription())) {
			
				int descCap = 500;
				if (descCap > fullText.length())
				{
					descCap = fullText.length();
				}
				partialDoc.setDescription(fullText.substring(0,descCap));
			}
			addMetadata(partialDoc, metadata);
		} 
		catch (TikaException e) {
			throw new ExtractorDocumentLevelException(e.getMessage());
		}
		catch (Exception e) {
			throw new ExtractorDocumentLevelException(e.getMessage());
		}
	}
	//TESTED (called from SAH, from UAH, and directly from HC) - pdf only but beyond that it's a tika problem not an Infinit.e one

	public static void addMetadata(DocumentPojo partialDoc, Metadata metadata) {
		// If the metadata contains a more plausible date then use that
		try {
			String title = metadata.get(Metadata.TITLE);
			if (null != title) {
				partialDoc.setTitle(title);
			}
		}
		catch (Exception e) { // Fine just carry on						
		}
		try { 
			Date date = metadata.getDate(Metadata.CREATION_DATE); // MS Word
			if (null != date) { 
				partialDoc.setPublishedDate(date);
			}
			else {
				date = metadata.getDate(Metadata.DATE); // Dublin
				if (null != date) {
					partialDoc.setPublishedDate(date);
				}
				else {
					date = metadata.getDate(Metadata.ORIGINAL_DATE);
					if (null != date) {
						partialDoc.setPublishedDate(date);
					}
				}
			}
		}
		catch (Exception e) { // Fine just carry on						
		}
		// If the metadata contains a geotag then apply that:
		try {
			String lat = metadata.get(Metadata.LATITUDE);
			String lon = metadata.get(Metadata.LONGITUDE);
			if ((null != lat) && (null != lon)) {
				GeoPojo gt = new GeoPojo();
				gt.lat = Double.parseDouble(lat);
				gt.lon = Double.parseDouble(lon);
				partialDoc.setDocGeo(gt);
			}
		}
		catch (Exception e) { // Fine just carry on						
		}
		
		// Save the entire metadata:
		partialDoc.addToMetadata("_FILE_METADATA_", metadata);		
	}
	
	@Override
	public String getCapability(EntityExtractorEnum capability) {
		if (capability == EntityExtractorEnum.URLTextExtraction_local)
			return "false";
		
		return null;
	}
	/////////////////////////////////////////////////////////////////////////////////////
	
	// Get tika options:
	// Bonus option output:xhtml|text
	// Bonus option bypass:<media type>
	// Example option: "application/pdf:{setEnableAutoSpace:false}", ie format is mediaType:JSON
	// where JSON is key/value pairs for the function name and the arg (only String, bool, int/long/double types are possible)
	
	private void initializeTika(SourcePojo source)
	{
		AutoDetectParser autoDetectParser = new AutoDetectParser();
		
		if ((null != source) && (null != source.getExtractorOptions())) {
			for (Map.Entry<String, String> kv: source.getExtractorOptions().entrySet()) {
				String mediaType = kv.getKey();
				String jsonStr = kv.getValue();

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
				else if (mediaType.matches("bypass[0-9]*")) {
					Map<MediaType, Parser> parsers = autoDetectParser.getParsers();
					parsers.put(MediaType.parse(jsonStr), new TXTParser());
					autoDetectParser.setParsers(parsers);
					continue;
				}
				// Try to get media type parser:
				mediaType = mediaType.replaceFirst("([?][0-9]*$", "");
				
				Parser p = autoDetectParser.getParsers().get(MediaType.parse(mediaType));
				while (p instanceof CompositeParser) {
					p = ((CompositeParser)p).getParsers().get(MediaType.parse(mediaType));
				}
				if (null == p) {
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
							
						}//(end loop over options)
					}					
				}
				catch (Exception e) {
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
