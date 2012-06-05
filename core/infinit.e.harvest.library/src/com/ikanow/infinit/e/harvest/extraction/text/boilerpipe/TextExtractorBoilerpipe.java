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

import java.net.URL;

import org.apache.log4j.Logger;

import com.ikanow.infinit.e.data_model.InfiniteEnums;
import com.ikanow.infinit.e.data_model.InfiniteEnums.ExtractorDocumentLevelException;
import com.ikanow.infinit.e.data_model.interfaces.harvest.ITextExtractor;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;

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
				URL url = new URL(partialDoc.getUrl());
				partialDoc.setFullText(ArticleExtractor.INSTANCE.getText(url));				
			}
			catch (Exception ex)
			{
				logger.error("Boilerpipe extract error=" + ex.getMessage());
				throw new InfiniteEnums.ExtractorDocumentLevelException();
			}			
		}
	}

}
