package com.ikanow.infinit.e.harvest.extraction.text.boilerpipe;

import java.net.URL;

import org.apache.log4j.Logger;

import com.ikanow.infinit.e.data_model.InfiniteEnums;
import com.ikanow.infinit.e.data_model.InfiniteEnums.ExtractorDocumentLevelException;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.harvest.extraction.text.legacy.ITextExtractor;

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
