function convertToPipeline(source) {

	try {
	    // 0. Object initialization: In order of application
	
	    // 0.1. extractors of various types
	    var _extractor = {};
	    var _feedOrWeb = null; 
	    	
	    // 0.2. globals
	    var _globalVars = null;
	    var _lookups = null;
	    var _harvestControl = null;
	
	    // 0.3. pre-auto extractor
	    var _links = null; 
	    var _docMeta = null;
	    var _contentMetaPre = null;
	    var _contentMetaPost = null;
	    var _manualText = null;
	    
	    // 0.4. Text and entity extraction
	    var _autoText = null;
	    var _autoEntities = null;
	    
	    // 0.5. Manual entities and associations 
	    var _entities = null;
	    var _assocs = null;
	
	    // 0.6. post-processing
	    var _postProc = null;
	    var _searchIndexes = null;
	
	    // 1] Extractor logic
	
	    if (source.file) {
	        _extractor.file = source.file;
	        _extractor.file.url = source.url;
	    } 
	    else if (source.database) {
	        _extractor.sql = source.database;
	        _extractor.sql.url = source.url;
	    }
	    else {
	        if (source.rss) {
	            _feedOrWeb = source.rss;
	            var isWeb = false;
	            var urls = [];
	            if (source.url) {
	                urls.push({
	                    url: source.url
	                });
	            }//TESTED
	            if (_feedOrWeb.extraUrls) {
	            	for (var x in _feedOrWeb.extraUrls) {
	            		urls.push(_feedOrWeb.extraUrls[x]);
	            	}
	            }//TESTED
	            if (!isWeb) {
	                for (var x in urls) {
	                    if (urls[x].title) {
	                        isWeb = true;
	                        break;
	                    }
	                }
	            }//TESTED
	            _feedOrWeb.extraUrls = urls;
	            if (isWeb) {
	                _extractor.web = _feedOrWeb;
	            } 
	            else {
	                _extractor.feed = _feedOrWeb;
	            }//TESTED
	            
	            if (_feedOrWeb.searchConfig) {
	                _links = _feedOrWeb.searchConfig;
	            }//TESTED
	        } 
	        else {
	            _extractor.feed = {};
	            _extractor.feed.extraUrls = [];
	            _extractor.feed.extraUrls.push({
	                url: source.url
	            });
	        }//TESTED
	    }
	
	    // 2] Globals
	
	    // 2.1] Copy SAH into globals
	    
	    if (source.structuredAnalysis) {
	        if (source.structuredAnalysis.script) {
	            _globalVars = {};
	            _globalVars.scripts = [source.structuredAnalysis.script];
	        }//TESTED
	        if (source.structuredAnalysis.scriptFiles) {
	            if (!_globalVars) {
	                _globalVars = {};
	            }
	            _globalVars.imports = source.structuredAnalysis.scriptFiles;
	        }//TESTED
	        if (source.structuredAnalysis.caches) {
	            _lookups = source.structuredAnalysis.caches;
	        }//TESTED
	    }
	    
	    // 2.2] Copy UAH into globals, like above except need to concat with SAH values if set
	    
	    if (source.unstructuredAnalysis) {
	        if (source.unstructuredAnalysis.script) {
	            if (!_globalVars) {
	                _globalVars = {};
	            }
	            if (!_globalVars.scripts) {
	                _globalVars.scripts = [source.unstructuredAnalysis.script];
	            }
	            else {
	                _globalVars.scripts = [ _globalVars.scripts[0] + "\n" + source.unstructuredAnalysis.script ];
	            }
	        }//TESTED
	        if (source.unstructuredAnalysis.scriptFiles) {
	            if (!_globalVars) {
	                _globalVars = {};
	            }
	            if (!_globalVars.imports) {
	                _globalVars.imports = source.unstructuredAnalysis.scriptFiles;
	            }
	            else {
	            	for (var x in source.unstructuredAnalysis.scriptFiles) {
	            		_globalVars.imports.push(source.unstructuredAnalysis.scriptFiles[x]);
	            	}
	            }
	        }//TESTED
	        if (source.unstructuredAnalysis.caches) {
	            if (null == _lookups) {
	                _lookups = source.unstructuredAnalysis.caches;
	            }
	            else {
	            	for (var x in source.unstructuredAnalysis.caches) {
	            		_lookups[x] = source.unstructuredAnalysis.caches[x]
	            	}
	            }
	        }//TESTED
	    }
	
	    // 2.3] rss.searchConfig globals gets moved into main globals
	    
	    if (null != _links) {
	    	if (null != _links.globals) {
	            if (!_globalVars) {
	                _globalVars = {};
	            }
	    		if (!_globalVars.scripts) {
	    			_globalVars.scripts = [ _links.globals ];
	    		}
	    		else {
	                _globalVars.scripts = [ _globalVars.scripts[0] + "\n" + _links.globals ];	    			
	    		}
	    	}//TESTED (both cases)
	    }
	    
	    // 2.4] Harvest control settings
	    
	    var harvestFields = [ "searchCycle_secs", "maxDocs", "distributionFactor", "duplicateExistingUrls" ];
	    for (var srcField in harvestFields) {
	        if (source[harvestFields[srcField]]) {	        	
	            if (null == _harvestControl) {
	                _harvestControl = {};
	            }
	            if ("maxDocs" == harvestFields[srcField]) {
	                _harvestControl.maxDocs_global = source[harvestFields[srcField]];
	            }//TESTED
	            if ("searchCycle_secs" == harvestFields[srcField]) {
            		_harvestControl[harvestFields[srcField]] = Math.abs(source[harvestFields[srcField]]);
	            }//TESTED
	            else {
	                _harvestControl[harvestFields[srcField]] = source[harvestFields[srcField]];
	            }//TESTED
	        }
	    }
	    
	    // 3] Pre auto extraction
	
	    // 3.1] Metadata fields with context "First"
	    
	    if (source.unstructuredAnalysis) {
	        if (source.unstructuredAnalysis.meta) {
	            for (var y in source.unstructuredAnalysis.meta) {
	                var meta = source.unstructuredAnalysis.meta[y];
	                if ("First" == meta.context) {
	                    if (null == _contentMetaPre) {
	                        _contentMetaPre = [];
	                    }
	                    var contentMeta = {};
	                    contentMeta.fieldName = meta.fieldName;
	                    contentMeta.script = meta.script;
	                    contentMeta.scriptlang = meta.scriptlang;
	                    contentMeta.flags = meta.flags;
	                    contentMeta.replace = meta.replace;
	                    _contentMetaPre.push(contentMeta);
	                }
	                else if (("Header" == meta.context) || ("Footer" == meta.context)) {
	                	return "Header and Footer objects are not supported in the new pipeline, you will need to start the conversion process by hand.";
	                }
	            }
	        }
	    }//TESTED
	    
	    // 3.2] Manual text transformation
	    
	    if (source.unstructuredAnalysis && source.unstructuredAnalysis.simpleTextCleanser) {
	        for (var xx in source.unstructuredAnalysis.simpleTextCleanser) {
	            var cleanser = source.unstructuredAnalysis.simpleTextCleanser[xx];
	            if (null == _manualText) {
	                _manualText = [];
	            }
	            var manualEl = {};
	            manualEl.fieldName = cleanser.field;
	            manualEl.script = cleanser.script;
	            manualEl.scriptlang = cleanser.scriptlang;
	            manualEl.flags = cleanser.flags;
	            manualEl.replacement = cleanser.replacement;
	            _manualText.push(manualEl);
	        }
	    }//TESTED
	    
	    // 3.3] Metadata fields with other contexts
	    
	    if (source.unstructuredAnalysis) {
	        if (source.unstructuredAnalysis.meta) {
	            for (var yy in source.unstructuredAnalysis.meta) {
	                var meta2 = source.unstructuredAnalysis.meta[yy];
	                if ("First" != meta2.context) {
	                    if (null == _contentMetaPost) {
	                    	_contentMetaPost = [];
	                    }
	                    var contentMeta2 = {};
	                    contentMeta2.fieldName = meta2.fieldName;
	                    contentMeta2.script = meta2.script;
	                    contentMeta2.scriptlang = meta2.scriptlang;
	                    contentMeta2.flags = meta2.flags;
	                    contentMeta2.replace = meta2.replace;
	                    _contentMetaPost.push(contentMeta2);
	                }
	            }
	        }
	    }//TESTED
	    
	    // 3.4] ("Automated") Text Extraction
	    
	    if (null == source.useTextExtractor) {
	        source.useTextExtractor = "default";
	    }//TESTED
	    if (("None" != source.useTextExtractor) && ("none" != source.useTextExtractor)) {
	        _autoText = {};
	        _autoText.engineName = source.useTextExtractor;
	        _autoText.engineConfig = source.extractorOptions;
	    }//TESTED
	    
	    // 3.5] Document fields 
	
	    if (source.structuredAnalysis) {
	    	var docMetaFields = [ "title", "description", "fullText", "publishedDate", "displayUrl", "docGeo" ];
	        for (var field in docMetaFields) {
	            if (source.structuredAnalysis[docMetaFields[field]]) {
	            	
	                if (null == _docMeta) {
	                    _docMeta = {};
	                }
	                if ("docGeo" == docMetaFields[field]) {
	                    _docMeta.geotag = source.structuredAnalysis.docGeo;
	                }//TESTED
	                else {
	                	_docMeta[docMetaFields[field]] = source.structuredAnalysis[docMetaFields[field]];
	                }//TESTED
	            }
	        }
	    }//TESTED
	    
	    // 4] Automatic feature extraction
	    
	    if (null == source.useExtractor) {
	        source.useExtractor = "default";
	    }//TESTED
	    if (("None" != source.useExtractor) && ("none" != source.useExtractor)) {
	        _autoEntities = {};
	        _autoEntities.engineName = source.useExtractor;
	        _autoEntities.engineConfig = source.extractorOptions;
	    }//TESTED
	    
	    // 5] Entities and associations
	    
	    var nestedEntities = false; // (do all the de-nesting at the end because it will modify the source object)
	    if (source.structuredAnalysis && source.structuredAnalysis.entities) {
	        _entities = source.structuredAnalysis.entities;
	        
	        // Check for entity nested ... 1 level we'll fix, 2 levels we'll bail 
	        for (var x in _entities) {
	        	var ent = _entities[x];
	        	if (ent.entities) {
	        		nestedEntities = true;
	        		for (var y in ent.entities) {
	    	        	var ent2 = ent.entities[y];
	        			if (ent2.entities) {
	        				return "Too many levels of nesting in entities (>1). You will need to perform some manual conversion first";
	        			}
	        		}
	        	}
	        }//TESTED (1 level of nesting, 2+ levels)
	        
	    }//TESTED
	    
	    var nestedAssocs = true; // (do all the de-nesting at the end because it will modify the source object)
	    if (source.structuredAnalysis && source.structuredAnalysis.associations) {
	        _assocs = source.structuredAnalysis.associations;

	        // Check for entity nested ... 1 level we'll fix, 2 levels we'll bail 
	        for (var x in _assocs) {
	        	var assoc = _assocs[x];
	        	if (assoc.associations) {
	        		nestedAssocs = true;
	        		for (var y in assoc.associations) {
	    	        	var assoc2 = assoc.associations[y];
	        			if (assoc2.associations) {
	        				return "Too many levels of nesting in associations (>1). You will need to perform some manual conversion first";
	        			}
	        		}
	        	}
	        }//TESTED (1 level of nesting, 2+ levels)
	    
	    }//TESTED
	    
	    // 6] Post processing
	    
	    if (source.structuredAnalysis && source.structuredAnalysis.onUpdateScript) {
	        if (null == _postProc)  {
	            _postProc = {};
	        }
	        _postProc.onUpdateScript = source.structuredAnalysis.onUpdateScript;
	    }//TESTED
	    if (source.structuredAnalysis && source.structuredAnalysis.metadataFields) {
	        if (null == _postProc)  {
	            _postProc = {};
	        }
	        _postProc.metadataFields = source.structuredAnalysis.metadataFields;
	    }
	    if (source.structuredAnalysis && source.structuredAnalysis.rejectDocCriteria) {
	        if (null == _postProc)  {
	            _postProc = {};
	        }
	        _postProc.rejectDocCriteria = source.structuredAnalysis.rejectDocCriteria;
	    }//TESTED	    
	    
	    if (source.searchIndexFilter) {
	        _searchIndexes = source.searchIndexFilter;
	    }//TESTED
	    
	    // 7] Nothing can stop us generating the pipeline, so perform transforms that "corrupt" the original object
	    
	    // 7.1] Entities and associations
	    
	    if (nestedEntities) {
	        for (var x in _entities) {
	        	var ent = _entities[x];
	        	if (ent.entities) {
	        		for (var y in ent.entities) {
	    	        	var ent2 = ent.entities[y];
	    	        	if (ent.iterateOver) {
	    	        		if (ent2.iterateOver) {
	    	        			ent2.iterateOver = ent.iterateOver + "." + ent2.iterateOver;
	    	        		}
		    	        	else {
		    	        		ent2.iterateOver = ent.iterateOver;	    	        		
		    	        	}
	    	        	}
	    	        	_entities.push(ent2);
	        		}
		        	_entities.splice(x, 1);
	        	}//(end if nested)
	        }//TESTED
	    }
	    
	    if (nestedAssocs) {
	        for (var x in _assocs) {
	        	var assoc = _assocs[x];
	        	if (assoc.associations) {
	        		nestedAssocs = true;
	        		for (var y in assoc.associations) {
	    	        	var assoc2 = assoc.associations[y];
	    	        	if (assoc.iterateOver) {
	    	        		if (assoc2.iterateOver) {
	    	        			assoc2.iterateOver = assoc.iterateOver + "." + assoc2.iterateOver;
	    	        		}
		    	        	else {
		    	        		assoc2.iterateOver = assoc.iterateOver;	    	        		
		    	        	}
	    	        	}
	    	        	_assocs.push(assoc2);
	        		}
	        		_assocs.splice(x, 1);
	        	}//(end if nested)
	        }//TESTED
	    }
	    
	    // 8] Now generate pipeline
	    
	    var pxPipeline = [];
	    
	    pxPipeline.push(_extractor);
	    if (null != _feedOrWeb) {
	    	delete _feedOrWeb.searchConfig;
	    }//TESTED
	    if (null != _globalVars) {
	        pxPipeline.push({ globals: _globalVars});
	    }//TESTED
	    if (null != _lookups) {
	        pxPipeline.push({ lookupTables: _lookups});    	
	    }//TESTED
	    if (null != _harvestControl) {
	    	pxPipeline.push({ harvest: _harvestControl });
	    }//TESTED
	    if (null != _links) {
	    	pxPipeline.push({ links: _links });    	
	    }//TESTED
	    if (null != _docMeta) {
	    	pxPipeline.push({ docMetadata: _docMeta });    	
	    }//TESTED
	    if (null != _contentMetaPre) {
	    	pxPipeline.push({ contentMetadata: _contentMetaPre });    	
	    }//TESTED
	    if (null != _manualText) {
	    	pxPipeline.push({ text: _manualText });    	    	
	    }//TESTED
	    if (null != _contentMetaPost) {
	    	pxPipeline.push({ contentMetadata: _contentMetaPost });    	
	    }//TESTED
	    if (null != _autoText) {
	    	pxPipeline.push({ textEngine: _autoText });    	    	
	    }//TESTED
	    if (null != _autoEntities) {
	    	pxPipeline.push({ featureEngine: _autoEntities });    	    	
	    }//TESTED
	    if (null != _entities) {
	    	pxPipeline.push({ entities: _entities });    	    	
	    }//TESTED
	    if (null != _assocs) {
	    	pxPipeline.push({ associations: _assocs });    	    	    	
	    }//TESTED
	    if (null != _postProc) {
	    	pxPipeline.push({ storageSettings: _postProc });    	    	    	    	
	    }//TESTED
	    if (null != _searchIndexes) {
	    	pxPipeline.push({ searchIndex: _searchIndexes });    	    	    	    	
	    }//TESTED
	    source.processingPipeline = pxPipeline;
	    
	    // 8] remove unwanted source fields
	
	    delete source.unstructuredAnalysis;
	    delete source.structuredAnalysis;
	    delete source.file;
	    delete source.database;
	    delete source.rss;
	    delete source.authentication;
	    delete source.url;
	    delete source.useExtractor;
	    delete source.useTextExtractor;
	    delete source.extractorOptions;
	    delete source.maxDocs;
	    delete source.throttleDocs;
	    delete source.duplicateExistingUrls;
	    delete source.appendTagsToDocs;
	    delete source.searchIndexFilter; 
	    delete source.extractType;
	    //(note leave source.searchCycle_secs - that is used for suspend/active settings)
	    //(TESTED)
	    
	    return null; // (null==success)
	}
	catch (error) {
		return error.message;
	}
}