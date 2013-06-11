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
package com.ikanow.infinit.e.harvest.extraction.text.boilerpipe;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.Scanner;

import org.apache.log4j.Logger;

import com.ikanow.infinit.e.data_model.InfiniteEnums;
import com.ikanow.infinit.e.data_model.InfiniteEnums.ExtractorDocumentLevelException;
import com.ikanow.infinit.e.data_model.interfaces.harvest.ITextExtractor;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.harvest.utils.ProxyManager;

import de.l3s.boilerpipe.extractors.ArticleExtractor;

public class TextExtractorBoilerpipe implements ITextExtractor
{
	private static final Logger logger = Logger.getLogger(TextExtractorBoilerpipe.class);

	@Override
	public String getName() { return "boilerplate"; }
		
	@Override
	public void extractText(DocumentPojo partialDoc) throws ExtractorDocumentLevelException 
	{
		if ( partialDoc.getUrl() != null )
		{
			try
			{
				String text = null;
				try {					
					if ((null == partialDoc.getFullText()) || (0 == partialDoc.getFullText().length()))
					{
						URL url = new URL(partialDoc.getUrl());
						String proxyOverride = null;
						if ((null != partialDoc.getTempSource()) && 
								(null != partialDoc.getTempSource().getRssConfig())) 
						{
							proxyOverride = partialDoc.getTempSource().getRssConfig().getProxyOverride();
						}						
						URLConnection urlConnect = url.openConnection(ProxyManager.getProxy(url, proxyOverride));
						if ((null != partialDoc.getTempSource()) && 
								(null != partialDoc.getTempSource().getRssConfig()))
									
						{
							if (null != partialDoc.getTempSource().getRssConfig().getUserAgent()) {
								urlConnect.setRequestProperty("User-Agent", partialDoc.getTempSource().getRssConfig().getUserAgent());
							}
							if (null != partialDoc.getTempSource().getRssConfig().getHttpFields()) {
								for (Map.Entry<String, String> httpFieldPair: partialDoc.getTempSource().getRssConfig().getHttpFields().entrySet()) {
									urlConnect.setRequestProperty(httpFieldPair.getKey(), httpFieldPair.getValue());														
								}
							}//TESTED
						}// TESTED
						
						InputStream urlStream = null;
						try {
							urlStream = urlConnect.getInputStream();
						}
						catch (Exception e) { // Try one more time, this time exception out all the way
							urlStream = urlConnect.getInputStream();					 
						}
						text = new Scanner(urlStream, "UTF-8").useDelimiter("\\A").next();
						partialDoc.setFullText(text);
					}
					if (partialDoc.getFullText().length() < 2097152) { //2MB max
						text = ArticleExtractor.INSTANCE.getText(partialDoc.getFullText());	
					}
					else {
						throw new RuntimeException("Document is too large for boilerpipe.");						
					}
				}
				catch (Error e) { // probably memory related
					throw new RuntimeException("Document is too large for boilerpipe.");
				}
				if (null == text){
					text = "";
				}
				if (text.length() < 32) { // Try and elongate full text if necessary
					StringBuilder sb = new StringBuilder(partialDoc.getTitle()).append(": ").append(partialDoc.getDescription()).append(". \n").append(text);
					partialDoc.setFullText(sb.toString());
				}
				else {
					partialDoc.setFullText(text);				
				}
			}
			catch (Exception ex)
			{
				logger.error("Boilerpipe extract error=" + ex.getMessage() );
				throw new InfiniteEnums.ExtractorDocumentLevelException(ex.getMessage());
			}			
		}
	}

}
