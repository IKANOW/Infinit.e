package com.ikanow.infinit.e.harvest.enrichment.legacy;

import com.ikanow.infinit.e.data_model.InfiniteEnums.ExtractorDailyLimitExceededException;
import com.ikanow.infinit.e.data_model.InfiniteEnums.ExtractorDocumentLevelException;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;

public interface IEntityExtractor 
{
	String getName();
	
	/**
	 * Takes a feed with some of the information stored in it
	 * such as title, desc, etc, and needs to parse the full
	 * text and add entities, events, and other metadata.
	 * 
	 * 
	 * @param partialDoc The DocumentPojo before extraction with fulltext field to extract on
	 * @return The DocumentPojo after extraction with entities, events, and full metadata
	 */
	void extractEntities(DocumentPojo partialDoc) 
		throws ExtractorDailyLimitExceededException, ExtractorDocumentLevelException;
	
	/**
	 * Simliar to extractEntities except this case assumes that
	 * text extraction has not been done and therefore takes the
	 * url and extracts the full text and entities/events.
	 * 
	 * @param partialDoc The DocumentPojo before text extraction (empty fulltext field)
	 * @return The DocumentPojo after text extraction and entity/event extraction with fulltext, entities, events, etc
	 */
	void extractEntitiesAndText(DocumentPojo partialDoc)
		throws ExtractorDailyLimitExceededException, ExtractorDocumentLevelException;
	
	/**
	 * Attempts to lookup if this extractor has a given capability,
	 * if it does returns value, otherwise null
	 * 
	 * @param capability Extractor capability we are looking for
	 * @return Value of capability, or null if capability not found
	 */
	String getCapability(EntityExtractorEnum capability);
}
