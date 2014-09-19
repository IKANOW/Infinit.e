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

import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;

import com.ikanow.infinit.e.data_model.InfiniteEnums;
import com.ikanow.infinit.e.data_model.InfiniteEnums.ExtractorDocumentLevelException;
import com.ikanow.infinit.e.data_model.interfaces.harvest.EntityExtractorEnum;
import com.ikanow.infinit.e.data_model.interfaces.harvest.ITextExtractor;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.data_model.utils.IkanowSecurityManager;
import com.ikanow.infinit.e.harvest.extraction.text.legacy.TextExtractorTika;
import com.ikanow.infinit.e.harvest.utils.PropertiesManager;
import com.ikanow.infinit.e.harvest.utils.ProxyManager;

import de.l3s.boilerpipe.extractors.ArticleExtractor;

public class TextExtractorBoilerpipe implements ITextExtractor
{
	protected PropertiesManager _props = null;
	protected String _defaultUserAgent = null;
	
	protected Tika _tika = null;
	
	protected IkanowSecurityManager _secManager = null;
	
	@Override
	public String getName() { return "boilerplate"; }
		
	@Override
	public void extractText(DocumentPojo partialDoc) throws ExtractorDocumentLevelException 
	{
		if (null == _secManager) {
			_secManager = new IkanowSecurityManager();
		}
		if ( partialDoc.getUrl() != null )
		{
			try
			{
				boolean userAgentSet = false;
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
								userAgentSet = true;
							}
							if (null != partialDoc.getTempSource().getRssConfig().getHttpFields()) {
								for (Map.Entry<String, String> httpFieldPair: partialDoc.getTempSource().getRssConfig().getHttpFields().entrySet()) {
									urlConnect.setRequestProperty(httpFieldPair.getKey(), httpFieldPair.getValue());														
								}
							}//TESTED
						}// TESTED
						
						if (!userAgentSet) {
							if (null == _props) {
								_props = new PropertiesManager();
								_defaultUserAgent = _props.getHarvestUserAgent();
							}
							if (null != _defaultUserAgent) {
								urlConnect.setRequestProperty("User-Agent", _defaultUserAgent);
							}
						}//TOTEST
						
						InputStream urlStream = null;
						if (null != _secManager) { // ie turn on...
							_secManager.setSecureFlag(true);
						}//TESTED
						
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
						}//TESTED
						
						String contentType = urlConnect.getContentType();
						
						if ((null != contentType) && contentType.contains("html")) { // HTML
							Scanner s = new Scanner(urlStream, "UTF-8");
							s.useDelimiter("\\A");
							text = s.next();
							s.close();
							partialDoc.setFullText(text);																			
						}
						else { // not HTML, send to tika instead
							if (null == _tika) {
								_tika = new Tika();
							}
							Metadata metadata = new Metadata();
							text = _tika.parseToString(urlStream, metadata);
							partialDoc.setFullText(text);					
							TextExtractorTika.addMetadata(partialDoc, metadata);
							return; // (don't send to boilerpipe in this case - eventually if set to output as HTML then can I guess?)
						}//TESTED
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
				throw new InfiniteEnums.ExtractorDocumentLevelException(ex.getMessage());
			}
		}
	}

	@Override
	public String getCapability(EntityExtractorEnum capability) {
		if (capability == EntityExtractorEnum.URLTextExtraction_local)
			return "true";
		
		return null;
	}

}
