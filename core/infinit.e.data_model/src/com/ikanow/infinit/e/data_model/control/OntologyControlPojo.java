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
package com.ikanow.infinit.e.data_model.control;

import java.util.List;

import com.google.gson.reflect.TypeToken;
import com.ikanow.infinit.e.data_model.api.BaseApiPojo;
import com.ikanow.infinit.e.data_model.store.document.AssociationPojo;
import com.ikanow.infinit.e.data_model.store.feature.association.AssociationFeaturePojo;

public class OntologyControlPojo extends BaseApiPojo {

	// Standard static function for readability
	@SuppressWarnings("unchecked")
	static public TypeToken<List<OntologyControlPojo>> listType() { return new TypeToken<List<OntologyControlPojo>>(){}; }

	public static class TypeInfo {
		// One of these 3 must be filled in:
		String type;
		AssociationFeaturePojo assocType; // (types go in entity1 and entity2; otherwise only verb_category is read)
		String category; // (optional if type/assocType present)
		
		// Entity replacements
		String iconUri;
		String dimension;		
		List<String> replacingTypes; // (optional - these can be "aliased" if present)
		
		// Assoc replacements
		String assocStyle;
		List<AssociationFeaturePojo> replacingAssocs; // (optional, only applicable if assocType specified - these can be aliased, matches entity1 and entity2)
	}
}
