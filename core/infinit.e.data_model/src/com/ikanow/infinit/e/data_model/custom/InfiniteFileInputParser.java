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
package com.ikanow.infinit.e.data_model.custom;

import java.io.IOException;
import java.io.InputStream;

import org.bson.BSONObject;

import com.ikanow.infinit.e.data_model.store.config.source.SourceFileConfigPojo;

public interface InfiniteFileInputParser {

	// Initialize the parser (returns self)
	InfiniteFileInputParser initialize(InputStream inStream, SourceFileConfigPojo fileConfig) throws IOException;
	
	// Returns null when done
	BSONObject getNextRecord() throws IOException;
	
	// Closes all the streams
	void close() throws IOException;
	
	// Returns the canonical extension for this type (including the ".")
	String getCanonicalExtension();
}
