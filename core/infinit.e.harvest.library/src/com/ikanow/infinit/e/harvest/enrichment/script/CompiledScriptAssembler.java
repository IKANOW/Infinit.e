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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.log4j.Logger;

import com.ikanow.infinit.e.data_model.store.config.source.SimpleTextCleanserPojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePipelinePojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePipelinePojo.DocumentJoinSpecPojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePipelinePojo.ManualTextExtractionSpecPojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePipelinePojo.MetadataSpecPojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePipelinePojo.StorageSettingsPojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourceRssConfigPojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourceSearchFeedConfigPojo;
import com.ikanow.infinit.e.data_model.store.config.source.StructuredAnalysisConfigPojo;
import com.ikanow.infinit.e.data_model.store.config.source.StructuredAnalysisConfigPojo.AssociationSpecPojo;
import com.ikanow.infinit.e.data_model.store.config.source.StructuredAnalysisConfigPojo.EntitySpecPojo;
import com.ikanow.infinit.e.data_model.store.config.source.UnstructuredAnalysisConfigPojo;
import com.ikanow.infinit.e.data_model.store.config.source.UnstructuredAnalysisConfigPojo.metaField;
import com.ikanow.infinit.e.data_model.utils.IkanowSecurityManager;
import com.ikanow.infinit.e.harvest.enrichment.custom.JavaScriptUtils;

/**
 * This class responsibility is to compile all scripts in the source/pipeline. 
 * @author jfreydank
 * 
 */
public class CompiledScriptAssembler {

	private static final Logger logger = Logger.getLogger(CompiledScriptAssembler.class);

	
	protected CompiledScriptFactory factory = null;


	private IkanowSecurityManager securityManager;
	
	protected CompiledScriptAssembler(CompiledScriptFactory compiledScriptFactory, IkanowSecurityManager securityManager){
		this.factory = compiledScriptFactory;
		this.securityManager = securityManager;
	}

	public void compileSourceScripts(SourcePojo source) {
		if (source != null) {
			compileGlobalScripts(source);
			compileStructuredAnalysisScripts(source.getStructuredAnalysisConfig());
			compileUnstructuredAnalysisScripts(source.getUnstructuredAnalysisConfig());
			compileSourceRssConfig(source.getRssConfig());
			compileProcessingPipelineScripts(source.getProcessingPipeline());
		} // if
	}
	

	protected void addAndCompileScript(String script,int key,boolean addCacheCheckScript){	
		factory.addAndCompileScript(script, key, addCacheCheckScript);
	}

	private void compileGlobalScripts(SourcePojo source) {
		List<SourcePipelinePojo> processingPipeline = source.getProcessingPipeline();
		StringBuffer globalScript = new StringBuffer();
		if (processingPipeline != null) {
			for (SourcePipelinePojo pxPojo : processingPipeline) {
				if (pxPojo.globals != null) {
					globalScript.append(createGlobalScript(pxPojo.globals.imports, pxPojo.globals.scripts));
					globalScript.append("\n");
				} // if globalScripts
				else if (pxPojo.splitter != null && pxPojo.splitter.getGlobals()!=null) {					
					globalScript.append(pxPojo.splitter.getGlobals());
					globalScript.append("\n");					
				}
				else if (pxPojo.links != null && pxPojo.links.getGlobals()!=null) {				
					globalScript.append(pxPojo.links.getGlobals());
					globalScript.append("\n");					
				}
			} // for
		} // if pipeline

		// append rssSearchConfig global scripts
		if(source.getRssConfig()!=null && source.getRssConfig().getSearchConfig()!=null && source.getRssConfig().getSearchConfig().getGlobals()!=null && "javascript".equalsIgnoreCase(source.getRssConfig().getSearchConfig().getScriptlang())){
			globalScript.append(source.getRssConfig().getSearchConfig().getGlobals());
			globalScript.append("\n");					
		}

		
		// attach 'global' script block from structured analysis 
		StructuredAnalysisConfigPojo structuredAnalysis = source.getStructuredAnalysisConfig();
		if(structuredAnalysis!=null){
			List<String> scriptList = null;
			List<String> scriptFileList = null;
			try {
				// Script code embedded in source
				if(structuredAnalysis.getScript()!=null){
					scriptList = Arrays.asList(structuredAnalysis.getScript());
				}
			}
			catch (Exception e) {}
			try {
				// scriptFiles - can contain String[] of script files to import into the engine
				if(structuredAnalysis.getScriptFiles()!=null){
					scriptFileList = Arrays.asList(structuredAnalysis.getScriptFiles());
				}
			}
			catch (Exception e) {}
			globalScript.append(createGlobalScript(scriptFileList, scriptList));
		} // if structuredAnalysis
		
		// attach 'global' script block from unstructured analysis 
		UnstructuredAnalysisConfigPojo unstructuredAnalysis = source.getUnstructuredAnalysisConfig();
		if(unstructuredAnalysis!=null){
			List<String> scriptList = null;
			List<String> scriptFileList = null;
			try {
				// Script code embedded in source
				if(structuredAnalysis.getScript()!=null){
					scriptList = Arrays.asList(unstructuredAnalysis.getScript());
				}
			}
			catch (Exception e) {}
			try {
				// scriptFiles - can contain String[] of script files to import into the engine
				if(structuredAnalysis.getScriptFiles()!=null){
					scriptFileList = Arrays.asList(unstructuredAnalysis.getScriptFiles());
				}
			}
			catch (Exception e) {}
			globalScript.append(createGlobalScript(scriptFileList, scriptList));
		} // if structuredAnalysis

		globalScript.append(JavaScriptUtils.generateParsingScript());
		globalScript.append(JavaScriptUtils.getOm2jsDeclarationScript());		
		factory.addAndCompileScript(globalScript.toString(), CompiledScriptFactory.GLOBAL,false);
		
	}// compileGlobalScripts

	private String createGlobalScript(List<String> imports, List<String> scripts) {
		StringBuffer sb = new StringBuffer();
		// Pass scripts into the engine

		// Retrieve script files in s.scriptFiles
		if (imports != null) {
			for (String file : imports) {
				if (null != file) {
					try {
						String fileContent = JavaScriptUtils.getJavaScriptFile(file, securityManager);
						sb.append(fileContent);
						sb.append("\n\n");
					} catch (Exception e) {
						logger.error("ScriptException (imports): " + e.getMessage());
					}
				}
			}
		}// (end load imports)

		// Eval script passed in s.script
		if (null != scripts) {
			for (String script : scripts) {
				if (null != script) {
					sb.append(script);
					sb.append("\n");
				}
			}
		}// (end load scripts)
		return sb.toString();
	}//

	private void compileStructuredAnalysisScripts(StructuredAnalysisConfigPojo structuredAnalysisConfigPojo) {
		if (structuredAnalysisConfigPojo != null) {			
			compileObjectsScripts(structuredAnalysisConfigPojo);
			compileEntitiesScripts(structuredAnalysisConfigPojo.getEntities());
			compileAssociationScripts(structuredAnalysisConfigPojo.getAssociations());			
			compileObjectsScripts(structuredAnalysisConfigPojo.getDocumentGeo());
		} // if
	}
	private void compileEntitiesScripts( List<EntitySpecPojo> entities) {
		if (entities != null) {
			for (EntitySpecPojo entS : entities) {
				compileObjectsScripts(entS);
				compileObjectsScripts(entS.getGeotag());
				// recursive traversing and compiling of child entities
				compileEntitiesScripts(entS.getEntities());
			}
		} // if
	}
	private void compileAssociationScripts(List<AssociationSpecPojo> associations) {

		if (associations != null) {
			for (AssociationSpecPojo assocS : associations) {
				compileObjectsScripts(assocS);
				compileObjectsScripts(assocS.getGeotag());
				// recursive traversing and compiling of child entities
				compileAssociationScripts(assocS.getAssociations());
			}
		} // if
	}
	
	private void compileSourceRssConfig(SourceRssConfigPojo rssConfig) {
		if(rssConfig!=null){
			compileSourceSearchFeedConfig(rssConfig.getSearchConfig());
		}
	}

	private void compileProcessingPipelineScripts(List<SourcePipelinePojo> processingPipeline) {
		if (processingPipeline != null) {
			for (SourcePipelinePojo pxPojo : processingPipeline) {
				compileObjectsScripts(pxPojo);
				// special treatment for criteria.
				addCriteraScript(pxPojo.criteria);

				// TODO check if criteria and other fields always have script
				// 0] Common fields:				
				// 1] Pipeline elements:

				// 1.1] Starting points:
				compileObjectsScripts(pxPojo.database);
				compileObjectsScripts(pxPojo.file);
				compileObjectsScripts(pxPojo.feed);
				compileObjectsScripts(pxPojo.web);
				compileObjectsScripts(pxPojo.nosql);
				compileObjectsScripts(pxPojo.logstash);
				compileObjectsScripts(pxPojo.federatedQuery);

				// 1.2] Global operations
				// treat globals separately, first

				// 1.3] Secondary document extraction
				// separate compiled script for splitter,FeedHarvester_searchEngineSubsystem.generateFeedFromSearch
				compileSourceSearchFeedConfig(pxPojo.splitter);
				// public SourceSearchFeedConfigPojo splitter;
				// separate compiled scripts for links,HarvestControllerPipeline.applyGlobalsToDocumentSplitting
				compileSourceSearchFeedConfig(pxPojo.links);
				// TODO where used?
				// separate compiled scripts for joins,HarvestControllerPipeline.applyGlobalsToDocumentSplitting
				compileJoinScript(pxPojo.joins);

				// 1.4] Text and Linked-Document extraction
				
				// separate compiled script for each,see UAH.cleanseField
				// public List<ManualTextExtractionSpecPojo> text;
				compileManualTextExtractionScripts(pxPojo.text);

				// 1.5] Document-level field (including metadata extraction)
				if(pxPojo.docMetadata!=null){
					compileObjectsScripts(pxPojo.docMetadata);
					compileObjectsScripts(pxPojo.docMetadata.geotag);
				}
				// separate compiled script for each,see _uah.processMetadataChain			
				compileContentMetadataScripts(pxPojo.contentMetadata);				

				// 1.6] Entities and Associations				
				compileEntitiesScripts(pxPojo.entities);
				compileAssociationScripts(pxPojo.associations);

				// 1.7] Finishing steps
				compileStorageSettings(pxPojo.storageSettings);

			}// for pxPojo
		} // if
	} // compileProcessingPipelineFunctions

	/*
	 * Criteria is treated differently because it in the code $Script is added. 
	 * Therefore  if it is not wrapped in $SCRIPT it will be wrapped here.
	 */	
	private void addCriteraScript(String criteria) {
		if(criteria!=null){
			if (!criteria.toLowerCase().startsWith("$script") )
			{
				factory.addAndCompileScript("$SCRIPT("+criteria+")",true);
			}
		}
	}

	/**
	 * This function checks all getters that return a string and (potentially) compiles the function values.
	 * If the returned value  with $script or $func it compiles the script.
	 * @param pojo
	 */
	private void compileObjectsScripts(Object pojo) {
		if (pojo != null) {
			// loop through public getter (properties)
			Map<String, Object> properties;
			try {
				properties = PropertyUtils.describe(pojo);
				for (Map.Entry<String, Object> entry : properties.entrySet()) {
					//String property = entry.getKey();
					if (entry.getValue() != null && entry.getValue() instanceof String) {
						String script = (String) entry.getValue();
						checkAndCompileScript(script);
					}
				}
				// deal with public attributes
				Field[] srcFields = pojo.getClass().getDeclaredFields();
				Field field = null;
				for (int i = 0; i < srcFields.length; i++) {
					field = srcFields[i];
					int mod = field.getModifiers();
					if (!Modifier.isStatic(mod) && Modifier.isPublic(mod) && String.class.equals(field.getType())) {
						String attrName = field.getName();
						if (!properties.containsKey(attrName)) {
							try {
								String script = (String) field.get(pojo);
								checkAndCompileScript(script);
							} catch (IllegalArgumentException e) {
								logger.error(e);
							} catch (IllegalAccessException e) {
								// TODO Auto-generated catch block
								logger.error(e);
							}
						} // if
					} // if modifier
				}// for fields

			} catch (Exception e) {
				logger.error("compileObjectsFunctions caught exception",e);
			}
		}
	}

	private void checkAndCompileScript(String script){
		if ((script != null)  && JavaScriptUtils.containsScript(script)){
				factory.addAndCompileScript(script,true);								
		}		
	}
	
	// see HarvestControllerPipeline.splitDocuments
	private void compileSourceSearchFeedConfig(SourceSearchFeedConfigPojo splitter) {
		if(splitter!=null && "javascript".equalsIgnoreCase(splitter.getScriptlang())){
		// separate compiled script for splitter,FeedHarvester_searchEngineSubsystem.generateFeedFromSearch
			String splitScript = splitter.getScript(); 			
			factory.addAndCompileScript(splitScript,true);
			compileMetaFieldScripts(splitter.getExtraMeta());
		} // if splitter
	}
	
	
	private void compileJoinScript(List<DocumentJoinSpecPojo> joins) {
		if (joins != null) {
			for (DocumentJoinSpecPojo join : joins) {
				compileObjectsScripts(join);
			}
		} // if
	}

	private void compileManualTextExtractionScripts(List<ManualTextExtractionSpecPojo> text) {
		if(text!=null){
		for (ManualTextExtractionSpecPojo manualText : text) {	
			if(manualText!=null && "javascript".equalsIgnoreCase(manualText.scriptlang)){			
				String mScript = manualText.script;
				factory.addAndCompileScript(mScript,true);
			} // if meta		
		} // for
		}
	}
	// see separate compiled script for each,see _uah.processMetadataChain
	private void compileContentMetadataScripts(List<MetadataSpecPojo> contentMetadata) {
		if(contentMetadata!=null){
			for (MetadataSpecPojo meta : contentMetadata) {	
				if(meta!=null && "javascript".equalsIgnoreCase(meta.scriptlang)){			
					String mScript = meta.script; 
					factory.addAndCompileScript(mScript,true);			
				} // if meta		
			} // for	
		}
	}
	
	//see UnstructuredAnalysisHarvester.intializeScriptEngine
	private void compileUnstructuredAnalysisScripts(UnstructuredAnalysisConfigPojo uap) {
		if (uap != null) {
			compileObjectsScripts(uap);
        	compileSimpleTextCleanserScripts(uap.getSimpleTextCleanser());
        	compileMetaFieldScripts(uap.getMeta());
		} // if
	}
	
	private void compileSimpleTextCleanserScripts(List<SimpleTextCleanserPojo> list) {
		if(list!=null){
			for (SimpleTextCleanserPojo meta : list) {	
				if(meta!=null && "javascript".equalsIgnoreCase(meta.getScriptlang())){			
					String mScript = meta.getScript(); 
					factory.addAndCompileScript(mScript,true);				
				} // if meta
			} // for	
		}
	}

	private void compileMetaFieldScripts(List<metaField> metaList) {
		if(metaList!=null){
			for (metaField meta : metaList) {	
				if(meta!=null && "javascript".equalsIgnoreCase(meta.scriptlang)){			
					String mScript = meta.script; 
					factory.addAndCompileScript(mScript,true);				
				} // if meta		
			} // for	
		}
	}

	private void compileStorageSettings(StorageSettingsPojo storageSettings) {
		if(storageSettings!=null){
			factory.addAndCompileScript(storageSettings.onUpdateScript,true);
			factory.addAndCompileScript(storageSettings.rejectDocCriteria,true);
		}
	}

}
