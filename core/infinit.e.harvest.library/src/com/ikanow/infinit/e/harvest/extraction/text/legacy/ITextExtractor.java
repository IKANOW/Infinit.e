package com.ikanow.infinit.e.harvest.extraction.text.legacy;

import com.ikanow.infinit.e.data_model.InfiniteEnums.ExtractorDailyLimitExceededException;
import com.ikanow.infinit.e.data_model.InfiniteEnums.ExtractorDocumentLevelException;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;

public interface ITextExtractor 
{
	String getName();
	
	/**
	 * Takes a url and spits back the text of the
	 * site, usually cleans it up some too.
	 * 
	 * @param partialDoc The document whose fulltext we want to populate from its url
	 * @return The success/error status
	 */
	void extractText(DocumentPojo partialDoc)
		throws ExtractorDailyLimitExceededException, ExtractorDocumentLevelException;
}
