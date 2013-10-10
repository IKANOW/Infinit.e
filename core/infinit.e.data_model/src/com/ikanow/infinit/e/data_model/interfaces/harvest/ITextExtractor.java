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

	/**
	 * Attempts to lookup if this extractor has a given capability,
	 * if it does returns value, otherwise null
	 * 
	 * @param capability Extractor capability we are looking for
	 * @return Value of capability, or null if capability not found
	 */
	String getCapability(EntityExtractorEnum capability);
}
