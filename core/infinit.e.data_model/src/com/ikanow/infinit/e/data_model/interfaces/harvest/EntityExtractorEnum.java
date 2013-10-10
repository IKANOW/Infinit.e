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

public enum EntityExtractorEnum {
	
	Name, // (Unused, use getName)
	URLTextExtraction, // (Unused)
	URLTextExtraction_local, // (Currently Unused But On Roadmap) Indicate if can use full text instead of of URL if it exists
	Quality, // (Unused)
	AssociationExtraction, // (Unused)
	SentimentExtraction, // (Unused)
	GeotagExtraction, // (Unused)
	RemoteExtraction, // (Unused)
	OptimalRemoteConcurrentSessions, // (Unused)
	OptionsSpec, // (Currently Unused But On Roadmap) This should return a JSON object with the available configuration keys (as the fields) with corresponding descriptions (as the values)
	MaxInputBytes // (IN USE) If you set this, you should truncate the text to the value specified
}
