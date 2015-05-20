/*******************************************************************************
 * Copyright 2015, The Infinit.e Open Source Project.
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
package com.ikanow.infinit.e.data_model.document;

import java.util.LinkedHashMap;

import com.ikanow.infinit.e.data_model.store.document.ChangeAwareDocumentWrapper;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.data_model.utils.ModifiedChangeListener;

public class ChangeAwareDocumentWrapperTest {
	DocumentPojo doc = null;

	public void testNotification(){
		ModifiedChangeListener changeListener = new ModifiedChangeListener();
		ChangeAwareDocumentWrapper caDoc = new ChangeAwareDocumentWrapper(doc);		
		caDoc.setAttributeChangeListener(changeListener);
		caDoc.setDisplayUrl("http://news.google.com");
	}
	
	protected void setup(){
		doc = new ChangeAwareDocumentWrapper(doc);
		doc.setTitle("DocumentPojo_Title");
		doc.setUrl("http://www.google.com");
		doc.setDescription("DocumentPojo_Description");
		LinkedHashMap<String, Object[]> metadata =  new LinkedHashMap<String, Object[]>();
		metadata.put("lastname", new Object[]{"lastname_val"});
		metadata.put("programlist", new Object[]{"programlist_0"});
		// _doc.metadata.json[0].correlations_array
		metadata.put("correlations_array", new Object[]{"c_0","c_1","c_2"});
		doc.setMetadata(metadata);		
	}

	public static void main(String[] args) {
		ChangeAwareDocumentWrapperTest test =  new ChangeAwareDocumentWrapperTest();
		test.setup();
		test.testNotification();
	}
}
