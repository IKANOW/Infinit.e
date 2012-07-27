package com.ikanow.infinit.e.harvest.extraction.text.legacy;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;

import com.ikanow.infinit.e.data_model.InfiniteEnums.ExtractorDailyLimitExceededException;
import com.ikanow.infinit.e.data_model.InfiniteEnums.ExtractorDocumentLevelException;
import com.ikanow.infinit.e.data_model.interfaces.harvest.ITextExtractor;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.data_model.store.document.GeoPojo;

public class TextExtractorTika implements ITextExtractor {

	@Override
	public String getName() {
		return "Tika";
	}

	@Override
	public void extractText(DocumentPojo partialDoc)
			throws ExtractorDailyLimitExceededException,
			ExtractorDocumentLevelException {

		if (null != partialDoc.getFullText()) {
			return; // something has already processed this for me
		}
		
		try {
			Tika tika = new Tika();
			
			// Disable the string length limit
			tika.setMaxStringLength(-1);
			//input = new FileInputStream(new File(resourceLocation));
			// Create a metadata object to contain the metadata
			Metadata metadata = new Metadata();
			// Parse the file and get the text of the file
			
			URL url = new URL(partialDoc.getUrl());
			URLConnection urlConnect = url.openConnection();
			if (null != partialDoc.getTempSource()) {
				if ((null != partialDoc.getTempSource().getRssConfig()) && (null != partialDoc.getTempSource().getRssConfig().getUserAgent())) {
					urlConnect.setRequestProperty("User-Agent", partialDoc.getTempSource().getRssConfig().getUserAgent());
				}// TESTED
			}
			
			InputStream urlStream = null;
			try {
				urlStream = urlConnect.getInputStream();
			}
			catch (Exception e) { // Try one more time, this time exception out all the way
				urlStream = urlConnect.getInputStream();					 
			}
			String fullText = tika.parseToString(urlStream, metadata);
			partialDoc.setFullText(fullText);				

			if ((null != fullText) && (null == partialDoc.getDescription())) {
			
				int descCap = 500;
				if (descCap > fullText.length())
				{
					descCap = fullText.length();
				}
				partialDoc.setDescription(fullText.substring(0,descCap));
			}
			
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
		catch (TikaException e) {
			throw new ExtractorDocumentLevelException(e.getMessage());
		}
		catch (Exception e) {
			throw new ExtractorDocumentLevelException(e.getMessage());
		}
	}
	//TESTED (called from SAH, from UAH, and directly from HC) - pdf only but beyond that it's a tika problem not an Infinit.e one
}
