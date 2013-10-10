/*******************************************************************************
 * Copyright 2012 The Infinit.e Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.ikanow.infinit.e.data_model.interfaces.harvest;

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
	 * (NOTE: partialDoc is set to null to indicate a source has finished, client implementations SHOULD support this 
	 *  eg by returning or statelessly throwing an exception if batching is not supported/enabled)
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
