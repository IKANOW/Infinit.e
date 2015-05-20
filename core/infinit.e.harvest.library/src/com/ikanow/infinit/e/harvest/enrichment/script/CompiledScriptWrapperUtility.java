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
package com.ikanow.infinit.e.harvest.enrichment.script;

import java.util.ArrayList;
import java.util.List;

import javax.script.ScriptContext;

import org.apache.log4j.Logger;

import com.ikanow.infinit.e.data_model.store.document.ChangeAwareDocumentWrapper;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.harvest.enrichment.custom.JavaScriptUtils;

/**
 * This class' purpose is to provide commonly used for initialization and wrapping across multiple harvester classes.
 * @author jfreydank
 *
 */
public class CompiledScriptWrapperUtility {
	private static final Logger logger = Logger.getLogger(CompiledScriptFactory.class);

	public static List<DocumentPojo> convertToWrappedDocumentPojos(List<DocumentPojo> toAdd,CompiledScriptFactory compiledScriptFactory) {
		List<DocumentPojo> docs = new ArrayList<DocumentPojo>();
		for (DocumentPojo delegate : toAdd) {
			DocumentPojo doc = convertToWrappedDocumentPojo(delegate, compiledScriptFactory);
			docs.add(doc);
		}
		return docs;
	}

	public static DocumentPojo convertToWrappedDocumentPojo(DocumentPojo delegate,CompiledScriptFactory compiledScriptFactory) {
		ChangeAwareDocumentWrapper doc = new ChangeAwareDocumentWrapper(delegate);
		ScriptCallbackNotifier attributeChangeListener = new ScriptCallbackNotifier(compiledScriptFactory,JavaScriptUtils.setDocumentAttributeScript,JavaScriptUtils.docAttributeName,JavaScriptUtils.docAttributeValue);
		doc.setAttributeChangeListener(attributeChangeListener);
		ScriptEngineContextAttributeNotifier dirtyChangeListener = new ScriptEngineContextAttributeNotifier(compiledScriptFactory,"_dirtyDoc",true);
		doc.setDirtyChangeListener(dirtyChangeListener);
		return doc;
	}
	/** 
	 * This function sets the documentPojo inside the engine if the reference of the document has changed. 
	 * @param compiledScriptFactory
	 * @param f
	 */
	public static void initializeDocumentPojoInEngine(CompiledScriptFactory compiledScriptFactory,DocumentPojo f){
		if(f!=null){
			if(f instanceof ChangeAwareDocumentWrapper){
				DocumentPojo existingDocument = (DocumentPojo)compiledScriptFactory.getScriptContext().getAttribute("_docPojo");
				// The original document is the delegate inside the ChangeAwareDocumentWrapper. 
				// We compare here if it has changed. If it has, the new document is put inside the engine.
				if(existingDocument!=((ChangeAwareDocumentWrapper)f).getDelegate()){
					compiledScriptFactory.getScriptContext().setAttribute("_docPojo",((ChangeAwareDocumentWrapper)f).getDelegate(),ScriptContext.ENGINE_SCOPE);					
				}
			}else{
				logger.error("Could not initialize Engine, DocumentPojo is not instance of ChangeAwareDocumentWrapper:"+f);
			}
		}
	}

}
