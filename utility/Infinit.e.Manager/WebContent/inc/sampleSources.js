//Sample source "templates" for the newsource creator

var SAMPLE_SOURCES = {

		"empty":
		{
			"description": "Empty source",
			"isPublic": true,
			"mediaType": "Report",
			"title": "Empty source template, eg for use with the Flow Builder UI",
			"processingPipeline": [
			                       ]		
		},
		"rss": 
		{
			"description": "Get one of more RSS feeds, all the listed documents are extracted",
			"isPublic": true,
			"mediaType": "News",
			"title": "Basic RSS Source Template",
			"processingPipeline": [
			                       {
			                    	   "display": "Get one of more RSS feeds, all the listed documents are extracted",
			                    	   "feed": {
			                    		   "extraUrls": [
			                    		                 {
			                    		                	 "url": "http://youraddress.com/then/e.g./news.rss"
			                    		                 }
			                    		                 ]
			                    	   }
			                       },
			                       {
			                    	   "display": "Extract the useful text from the HTML",
			                    	   "textEngine": {
			                    		   "engineName": "default"
			                    	   }
			                       },
			                       {
			                    	   "display": "Enrich the document metadata with entities (people, places) and associations generated using NLP",
			                    	   "featureEngine": {
			                    		   "engineName": "default"
			                    	   }
			                       }
			                       ]
		},

		"web": 
		{
			"description": "Extract each URL (re-extracting every 'updateCycle_secs') with the specified title and summary text",
			"isPublic": true,
			"title": "Basic Web Page Source Template",
			"processingPipeline": [
			                       {
			                    	   "display": "Extract each document (re-extracting every 'updateCycle_secs') with the specified title and summary text",
			                    	   "web": {
			                    		   "extraUrls": [
			                    		                 {
			                    		                	 "description": "Optional",
			                    		                	 "title": "Page Title",
			                    		                	 "url": "http://youraddress.com/then/e.g./title.html"
			                    		                 }
			                    		                 ],
			                    		                 "updateCycle_secs": 86400
			                    	   }
			                       },
			                       {
			                    	   "display": "Extract the useful text from the HTML",
			                    	   "textEngine": {
			                    		   "engineName": "default"
			                    	   }
			                       },
			                       {
			                    	   "display": "Enrich the document metadata with entities (people, places) and associations generated using NLP",
			                    	   "featureEngine": {
			                    		   "engineName": "default"
			                    	   }
			                       }
			                       ]
		},	

		"json_api_simple": 
		{
			"description": "Specify one or more JSON (or XML or ...) endpoints from which to extract objects, each endpoint/URL generates a single document",
			"isPublic": true,
			"mediaType": "Record",
			"processingPipeline": [
			                       {
			                    	   "display": "Specify one or more JSON (or XML or ...) endpoints from which to extract objects, each endpoint/URL generates a single document",
			                    	   "web": {
			                    		   "extraUrls": [
			                    		                 {
			                    		                	 "url": "http://youraddress.com/then/e.g./query?out=json",
			                    		                	 "title": "dummy: replaced below"
			                    		                 }
			                    		                 ]
			                    	   }
			                       },
			                       {
			                    	   "harvest": {
			                    		   "duplicateExistingUrls": true,
			                    		   "searchCycle_secs": 600
			                    	   },
			                    	   "display": "Only check the API every 10 minutes (can be set to whatever you'd like)"
			                       },
			                       {
			                    	   "display": "Convert the text into a JSON object in the document's metadata field: _doc.metadata.json[0]",
			                    	   "contentMetadata": [
			                    	                       {
			                    	                    	   "fieldName": "json",
			                    	                    	   "scriptlang": "javascript",
			                    	                    	   "script": "var json = eval('('+text+')'); json; "
			                    	                       }
			                    	                       ]
			                       },
			                       {
			                    	   "display": "Use the JSON fields to fill in useful document metadata, using js or substitutions, eg:",
			                    	   "docMetadata": {
			                    		   "title": "$metadata.json.TITLE_FIELD",
			                    		   "publishedDate": "$SCRIPT( return _doc.metadata.json[0].DATE_FIELD; )",
			                    		   "fullText": "$metadata.json.TEXT_FIELD1 $metadata.json.TEXT_FIELD2"
			                    	   }
			                       },
			                       {
			                    	   "display": "Pass a text object generated from the object into the NLP for further enrichment, if enough text is present.",
			                    	   "featureEngine": {
			                    		   "engineName": "default",
			                    		   "exitOnError": true
			                    	   }
			                       },
			                       {
			                    	   "display": "For JSON records you often won't have enough text to perform NLP-based enrichment, but you can create entities directly from the fields, eg:",
			                    	   "entities": [
			                    	                {
			                    	                	"useDocGeo": false,
			                    	                	"type": "Person",
			                    	                	"iterateOver": "metadata.json.people",
			                    	                	"disambiguated_name": "$personName",
			                    	                	"actual_name": "$personName",
			                    	                	"dimension": "Who"
			                    	                },
			                    	                {
			                    	                	"useDocGeo": false,
			                    	                	"type": "Location",
			                    	                	"disambiguated_name": "$metadata.json.placeName",
			                    	                	"actual_name": "$metadata.json.placeName",
			                    	                	"geotag": {
			                    	                		"ontology_type": "point",
			                    	                		"alternatives": [
			                    	                		                 {
			                    	                		                	 "strictMatch": true,
			                    	                		                	 "country": "$metadata.json.country",
			                    	                		                	 "geoType": "auto",
			                    	                		                	 "city": "$metadata.json.city"
			                    	                		                 }
			                    	                		                 ],
			                    	                		                 "lon": "$metadata.json.longitude",
			                    	                		                 "geoType": "manual",
			                    	                		                 "lat": "$metadata.json.latitude"
			                    	                	},
			                    	                	"dimension": "Where"
			                    	                }
			                    	                ]
			                       },
			                       {
			                    	   "display": "For JSON records you often won't have enough text to perform NLP-based enrichment, but you can create associations directly from the fields, eg:",
			                    	   "associations": [
			                    	                    {
			                    	                    	"verb": "visits",
			                    	                    	"assoc_type": "Event",
			                    	                    	"entity1": "Person",
			                    	                    	"verb_category": "location",
			                    	                    	"iterateOver": "entity1,entity2",
			                    	                    	"entity2": "Location"
			                    	                    },
			                    	                    {
			                    	                    	"verb": "resides_at",
			                    	                    	"entity1_index": "${metadata.json.author.personName}/person",
			                    	                    	"assoc_type": "Fact",
			                    	                    	"entity2_index": "${metadata.json.author.homeAddress}/location",
			                    	                    	"verb_category": "location"
			                    	                    },
			                    	                    {
			                    	                    	"verb": "said",
			                    	                    	"entity1": "${metadata.json.author.personName}/person",
			                    	                    	"verb_category": "quotation",
			                    	                    	"entity2": "${metadata.json.quotation}"
			                    	                    }
			                    	                    ]
			                       },
			                       {
			                    	   "display": "Improve ingest performance by not full-text-indexing the JSON object itself (the full text, entities etc still get indexed)",
			                    	   "searchIndex": {
			                    		   "metadataFieldList": "+"
			                    	   }
			                       }
			                       ],
			                       "title": "Basic Simple JSON API Source Template"
		},

		"json_api_links": 
		{
			"description": "Specify one or more JSON (or XML or ...) endpoints from which to extract objects, each endpoint/URL generates one or more links that can be harvested from the web",
			"isPublic": true,
			"mediaType": "Social",
			"processingPipeline": [
			                       {
			                    	   "display": "Specify one or more JSON (or XML or ...) endpoints from which to extract objects, each endpoint/URL generates one or more links that can be harvested from the web",
			                    	   "web": {
			                    		   "extraUrls": [
			                    		                 {
			                    		                	 "url": "http://youraddress.com/then/e.g./query?out=json",
			                    		                	 "title": "doesn't matter, this document is deleted by the splitter element"
			                    		                 }
			                    		                 ]
			                    	   }
			                       },
			                       {
			                    	   "display": "A global space to group all the complex parsing and processing logic, can be called from anywhere",
			                    	   "globals": {
			                    		   "scripts": [
			                    		               "function create_links( urls, input_array )\n{\n    for (var x in input_array) {\n        var input = input_array[x];\n        urls.push( { url: input.url, title: input.title, description: input.desc, publishedData: input.date });\n    }\n}"
			                    		               ],
			                    		               "scriptlang": "javascript"
			                    	   }
			                       },
			                       {
			                    	   "harvest": {
			                    		   "duplicateExistingUrls": true,
			                    		   "searchCycle_secs": 600
			                    	   },
			                    	   "display": "Only check the API every 10 minutes (can be set to whatever you'd like)"
			                       },
			                       {
			                    	   "display": "Convert API reply into the web documents to which they point",
			                    	   "links": {
			                    		   "scriptlang": "javascript",
			                    		   "numPages": 10,
			                    		   "extraMeta": [
			                    		                 {
			                    		                	 "fieldName": "json",
			                    		                	 "script": "var json = eval('('+text+')'); json; ",
			                    		                	 "scriptlang": "javascript"
			                    		                 }
			                    		                 ],
			                    		                 "scriptflags": "m",
			                    		                 "stopPaginatingOnDuplicate": false,
			                    		                 "numResultsPerPage": 1,
			                    		                 "script": "var urls = []; create_links( urls, _metadata.json[0].data ); urls;"
			                    	   }
			                       },
			                       {
			                    	   "display": "Extract the useful text from the HTML",
			                    	   "textEngine": {
			                    		   "engineName": "default"
			                    	   }
			                       },
			                       {
			                    	   "display": "Enrich the document metadata with entities (people, places) and associations generated using NLP",
			                    	   "featureEngine": {
			                    		   "engineName": "default"
			                    	   }
			                       }
			                       ],
			                       "title": "Basic Complex JSON API Source Template #1 (link following)"
		},

		"json_api_complex": 
		{
			"description": "Specify one or more JSON (or XML or ...) endpoints from which to extract objects, each endpoint/URL generates multiple documents",
			"isPublic": true,
			"mediaType": "Record",
			"processingPipeline": [
			                       {
			                    	   "display": "Specify one or more JSON (or XML or ...) endpoints from which to extract objects, each endpoint/URL generates multiple documents",
			                    	   "feed": {
			                    		   "extraUrls": [
			                    		                 {
			                    		                	 "url": "http://youraddress.com/query?out=json"
			                    		                 }
			                    		                 ]
			                    	   }
			                       },
			                       {
			                    	   "display": "A global space to group all the complex parsing and processing logic, can be called from anywhere",
			                    	   "globals": {
			                    		   "scripts": [
			                    		               "function create_links( urls, input_array )\n{\n    for (var x in input_array) {\n        var input = input_array[x];\n        urls.push( { url: input.url, title: input.title, description: input.desc, publishedData: input.date, fullText: input.text });\n    }\n}"
			                    		               ],
			                    		               "scriptlang": "javascript"
			                    	   }
			                       },
			                       {
			                    	   "harvest": {
			                    		   "duplicateExistingUrls": true,
			                    		   "searchCycle_secs": 600
			                    	   },
			                    	   "display": "Only check the API every 10 minutes (can be set to whatever you'd like)"
			                       },
			                       {
			                    	   "display": "Convert the text into a JSON object in the document's metadata field: _doc.metadata.json[0]",
			                    	   "contentMetadata": [
			                    	                       {
			                    	                    	   "store": true,
			                    	                    	   "fieldName": "json",
			                    	                    	   "index": false,
			                    	                    	   "script": "var json = eval('('+text+')'); json; ",
			                    	                    	   "scriptlang": "javascript"
			                    	                       }
			                    	                       ]
			                       },
			                       {
			                    	   "display": "Take the original documents, split them using their metadaata into new documents, and then delete the originals",
			                    	   "splitter": {
			                    		   "deleteExisting": true,
			                    		   "scriptflags": "m",
			                    		   "script": "var urls = []; create_links( urls, _metadata.json[0].data ); urls;",
			                    		   "scriptlang": "javascript"
			                    	   }
			                       },
			                       {
			                    	   "display": "Convert the text into a JSON object in the document's metadata field: _doc.metadata.json[0]",
			                    	   "contentMetadata": [
			                    	                       {
			                    	                    	   "store": true,
			                    	                    	   "fieldName": "json",
			                    	                    	   "index": false,
			                    	                    	   "script": "var json = eval('('+text+')'); json; ",
			                    	                    	   "scriptlang": "javascript"
			                    	                       }
			                    	                       ]
			                       },
			                       {
			                    	   "display": "Use the JSON fields to fill in useful document metadata, using js or substitutions, eg:",
			                    	   "docMetadata": {
			                    		   "title": "$metadata.json.TITLE_FIELD",
			                    		   "publishedDate": "$SCRIPT( return _doc.metadata.json[0].DATE_FIELD; )",
			                    		   "appendTagsToDocs": false,
			                    		   "fullText": "$metadata.json.TEXT_FIELD1 $metadata.json.TEXT_FIELD2"
			                    	   }
			                       },
			                       {
			                    	   "display": "Pass a text object generated from the object into the NLP for further enrichment, if enough text is present.",
			                    	   "featureEngine": {
			                    		   "exitOnError": true,
			                    		   "engineName": "default"
			                    	   }
			                       },
			                       {
			                    	   "display": "For JSON records you often won't have enough text to perform NLP-based enrichment, but you can create entities directly from the fields, eg:",
			                    	   "entities": [
			                    	                {
			                    	                	"useDocGeo": false,
			                    	                	"type": "Person",
			                    	                	"disambiguated_name": "$personName",
			                    	                	"actual_name": "$personName",
			                    	                	"iterateOver": "metadata.json.people",
			                    	                	"dimension": "Who"
			                    	                },
			                    	                {
			                    	                	"useDocGeo": false,
			                    	                	"type": "Location",
			                    	                	"disambiguated_name": "$metadata.json.placeName",
			                    	                	"actual_name": "$metadata.json.placeName",
			                    	                	"geotag": {
			                    	                		"ontology_type": "point",
			                    	                		"alternatives": [
			                    	                		                 {
			                    	                		                	 "strictMatch": true,
			                    	                		                	 "country": "$metadata.json.country",
			                    	                		                	 "geoType": "auto",
			                    	                		                	 "city": "$metadata.json.city"
			                    	                		                 }
			                    	                		                 ],
			                    	                		                 "lon": "$metadata.json.longitude",
			                    	                		                 "geoType": "manual",
			                    	                		                 "lat": "$metadata.json.latitude"
			                    	                	},
			                    	                	"dimension": "Where"
			                    	                }
			                    	                ]
			                       },
			                       {
			                    	   "display": "For JSON records you often won't have enough text to perform NLP-based enrichment, but you can create associations directly from the fields, eg:",
			                    	   "associations": [
			                    	                    {
			                    	                    	"verb": "visits",
			                    	                    	"assoc_type": "Event",
			                    	                    	"entity1": "Person",
			                    	                    	"verb_category": "location",
			                    	                    	"iterateOver": "entity1,entity2",
			                    	                    	"entity2": "Location"
			                    	                    },
			                    	                    {
			                    	                    	"verb": "resides_at",
			                    	                    	"entity1_index": "${metadata.json.author.personName}/person",
			                    	                    	"assoc_type": "Fact",
			                    	                    	"entity2_index": "${metadata.json.author.homeAddress}/location",
			                    	                    	"verb_category": "location"
			                    	                    },
			                    	                    {
			                    	                    	"verb": "said",
			                    	                    	"entity1": "${metadata.json.author.personName}/person",
			                    	                    	"verb_category": "quotation",
			                    	                    	"entity2": "${metadata.json.quotation}"
			                    	                    }
			                    	                    ]
			                       },
			                       {
			                    	   "display": "Improve ingest performance by not full-text-indexing the JSON object itself (the full text, entities etc still get indexed)",
			                    	   "searchIndex": {
			                    		   "indexOnIngest": true,
			                    		   "metadataFieldList": "+"
			                    	   }
			                       }
			                       ],
			                       "title": "Basic Complex JSON API Source Template #2 (document splitting)"
		},

		"local_file": 
		{
			"description": "For single node clusters: get files from the local file system (otherwise use 'remote file'/'amazon'). Type can be set to 'csv', 'json', 'xml', or 'office' - by default it will attempt to auto-detect",
			"extractType": "File",
			"isPublic": true,
			"mediaType": "Report",
			"title": "Basic Local File Source Template (any file type)",
			"processingPipeline": [
			                       {
			                    	   "display": "For single node clusters: the location of the directory on the local file system (otherwise use 'remote file'/'amazon'). Type can be set to 'csv', 'json', 'xml', or 'office' - by default it will attempt to auto-detect",
			                    	   "file": {
			                    		   "XmlRootLevelValues": [],
			                    		   "url": "file:///directory1/directory2/"
			                    	   }
			                       },
			                       {
			                    	   "display": "If a JSON/XML object you should do further extraction first, see JSON samples. For 'office' types, the text has been extracted already.",
			                    	   "featureEngine": {
			                    		   "engineName": "default"
			                    	   }
			                       },
			                       {
			                    	   "display": "Improve ingest performance by not full-text-indexing the JSON/XML/CSV object itself if an object (the full text, entities etc still get indexed)",
			                    	   "searchIndex": {
			                    		   "metadataFieldList": "+"
			                    	   }
			                       }
			                       ]
		},		

		"remote_file": 
		{
			"description": "Get files from the Windows/Samba file share. Type can be set to 'csv', 'json', 'xml', or 'office' - by default it will attempt to auto-detect",
			"isPublic": true,
			"mediaType": "Report",
			"title": "Basic Fileshare Source Template (any file type)",
			"processingPipeline": [
			                       {
			                    	   "display": "The location of the directory on the Windows/Samba file share. Type can be set to 'csv', 'json', 'xml', or 'office' - by default it will attempt to auto-detect",
			                    	   "file": {
			                    		   "XmlRootLevelValues": [],
			                    		   "domain": "DOMAIN",
			                    		   "password": "PASSWORD",
			                    		   "username": "USERNAME",
			                    		   "url": "smb://HOST:PORT/share/directory1/"
			                    	   }
			                       },
			                       {
			                    	   "display": "(Only check the filesystem hourly - you can set this to whatever time you want)",
			                    	   "harvest": {
			                    		   "searchCycle_secs": 3600
			                    	   }
			                       },
			                       {
			                    	   "display": "If a JSON/XML object you should do further extraction first, see JSON samples. For 'office' types, the text has been extracted already.",
			                    	   "featureEngine": {
			                    		   "engineName": "default"
			                    	   }
			                       },
			                       {
			                    	   "display": "Improve ingest performance by not full-text-indexing the JSON/XML/CSV object itself if an object (the full text, entities etc still get indexed)",
			                    	   "searchIndex": {
			                    		   "metadataFieldList": "+"
			                    	   }
			                       }
			                       ]
		},

		"remote_file_logs": 
		{
			"description": "Extract line-separated (eg csv) files from the Windows/Samba file share",
			"isPublic": true,
			"mediaType": "Report",
			"title": "Basic Fileshare Source Template (log file type)",
			"processingPipeline": [
			                       {
			                    	   "display": "The location of the directory containing line-separated (eg csv) files on the Windows/Samba file share",
			                    	   "file": {
			                    		   "XmlRootLevelValues": [],
			                    		   "type": "csv",
			                    		   "domain": "DOMAIN",
			                    		   "password": "PASSWORD",
			                    		   "username": "USERNAME",
			                    		   "url": "smb://HOST:PORT/share/directory1/"
			                    	   }
			                       },
			                       {
			                    	   "display": "(Only check the filesystem hourly - you can set this to whatever time you want)",
			                    	   "harvest": {
			                    		   "searchCycle_secs": 3600
			                    	   }
			                       },
			                       {
			                    	   "display": "Use the columns to fill in useful document metadata, using js or substitutions, eg:",
			                    	   "docMetadata": {
			                    		   "title": "$metadata.csv.TITLE_FIELD",
			                    		   "publishedDate": "$SCRIPT( return _doc.metadata.csv[0].DATE_FIELD; )",
			                    		   "fullText": "$metadata.csv.TEXT_FIELD1 $metadata.csv.TEXT_FIELD2"
			                    	   }
			                       },
			                       {
			                    	   "display": "For log records you often won't have enough text to perform NLP-based enrichment, but you can create entities directly from the columns, eg:",
			                    	   "entities": [
			                    	                {
			                    	                	"useDocGeo": false,
			                    	                	"type": "Person",
			                    	                	"disambiguated_name": "$metadata.csv.personName",
			                    	                	"actual_name": "$metadata.csv.personName",
			                    	                	"dimension": "Who"
			                    	                },
			                    	                {
			                    	                	"useDocGeo": false,
			                    	                	"type": "Location",
			                    	                	"disambiguated_name": "$metadata.csv.placeName",
			                    	                	"actual_name": "$metadata.csv.placeName",
			                    	                	"geotag": {
			                    	                		"ontology_type": "point",
			                    	                		"alternatives": [
			                    	                		                 {
			                    	                		                	 "strictMatch": true,
			                    	                		                	 "country": "$metadata.csv.country",
			                    	                		                	 "geoType": "auto",
			                    	                		                	 "city": "$metadata.csv.city"
			                    	                		                 }
			                    	                		                 ],
			                    	                		                 "lon": "$metadata.csv.longitude",
			                    	                		                 "geoType": "manual",
			                    	                		                 "lat": "$metadata.csv.latitude"
			                    	                	},
			                    	                	"dimension": "Where"
			                    	                }
			                    	                ]
			                       },
			                       {
			                    	   "display": "For log records you often won't have enough text to perform NLP-based enrichment, but you can create associations directly from the columns, eg:",
			                    	   "associations": [
			                    	                    {
			                    	                    	"verb": "visits",
			                    	                    	"assoc_type": "Event",
			                    	                    	"entity1": "Person",
			                    	                    	"verb_category": "location",
			                    	                    	"iterateOver": "entity1,entity2",
			                    	                    	"entity2": "Location"
			                    	                    },
			                    	                    {
			                    	                    	"verb": "resides_at",
			                    	                    	"entity1_index": "${metadata.csv.personName}/person",
			                    	                    	"assoc_type": "Fact",
			                    	                    	"entity2_index": "${metadata.csv.homeAddress}/location",
			                    	                    	"verb_category": "location"
			                    	                    },
			                    	                    {
			                    	                    	"verb": "said",
			                    	                    	"entity1": "${metadata.csv.personName}/person",
			                    	                    	"verb_category": "quotation",
			                    	                    	"entity2": "${metadata.csv.quotation}"
			                    	                    }
			                    	                    ]
			                       },
			                       {
			                    	   "display": "Improve ingest performance by not full-text-indexing the CSV object itself", 
			                    	   "searchIndex": {
			                    		   "metadataFieldList": "+"
			                    	   }
			                       }
			                       ]
		},

		"amazon_file": 
		{
			"description": "Get files from the Amazon S3 file share. Type can be set to 'csv', 'json', 'xml', or 'office' - by default it will attempt to auto-detect",
			"isPublic": true,
			"mediaType": "Report",
			"title": "Basic Amazon S3 Source Template (and file type)",
			"processingPipeline": [
			                       {
			                    	   "display": "The location of the bucket/directory on the Amazon S3 file share. Type can be set to 'csv', 'json', 'xml', or 'office' - by default it will attempt to auto-detect",
			                    	   "file": {
			                    		   "XmlRootLevelValues": [],
			                    		   "password": "AWS_SECRETKEY",
			                    		   "username": "AWS_ACCESSID",
			                    		   "url": "s3://BUCKET_NAME/FOLDERS/"
			                    	   }
			                       },
			                       {
			                    	   "display": "(Only check the filesystem hourly - you can set this to whatever time you want)",
			                    	   "harvest": {
			                    		   "searchCycle_secs": 3600
			                    	   }
			                       },
			                       {
			                    	   "display": "If a JSON/XML object you should do further extraction first, see JSON samples. For 'office' types, the text has been extracted already.",
			                    	   "featureEngine": {
			                    		   "engineName": "default"
			                    	   }
			                       },
			                       {
			                    	   "display": "Improve ingest performance by not full-text-indexing the JSON/XML object itself if a JSON object (the full text, entities etc still get indexed)",
			                    	   "searchIndex": {
			                    		   "metadataFieldList": "+"
			                    	   }
			                       }
			                       ]
		},		

		"infinite_share_upload": 
		{
			"description": "Ingest a zip or JSON file uploaded to Infinit.e as a 'share'. Type can be set to 'csv', 'json', 'xml', or 'office' - by default it will attempt to auto-detect",
			"extractType": "Ingest ZIP archives or JSON records from Infinit.e shares",
			"isPublic": true,
			"mediaType": "Report",
			"title": "Title",
			"processingPipeline": [
			                       {
			                    	   "display": "The location of a zip or JSON file uploaded to Infinit.e as a 'share'. Type can be set to 'csv', 'json', 'xml', or 'office' - by default it will attempt to auto-detect",
			                    	   "file": {
			                    		   "XmlRootLevelValues": [],
			                    		   "url": "inf://share/SHAREID/miscDescription/"
			                    	   }
			                       },
			                       {
			                    	   "display": "If a JSON/XML object you should do further extraction first, see JSON samples. For 'office' types, the text has been extracted already.",
			                    	   "featureEngine": {
			                    		   "engineName": "default"
			                    	   }
			                       },
			                       {
			                    	   "display": "Improve ingest performance by not full-text-indexing the JSON/XML object itself if a JSON object (the full text, entities etc still get indexed)",
			                    	   "searchIndex": {
			                    		   "metadataFieldList": "+"
			                    	   }
			                       }
			                       ]
		},		

		"infinite_custom_ingest": 
		{
			"description": "Point to an Infinit.e custom job and ingest the results as JSON.",
			"extractType": "File",
			"isPublic": true,
			"mediaType": "Report",
			"title": "Convert the output of Infinit.e custom analytics into documents",
			"processingPipeline": [
			                       {
			                    	   "display": "Point to an Infinit.e custom job and ingest the results as JSON.",
			                    	   "file": {
			                    		   "type": "json",
			                    		   "XmlRootLevelValues": [],
			                    		   "url": "inf://custom/JOBID_OR_JOBTITLE/miscDescription/"
			                    	   }
			                       },
			                       {
			                    	   "display": "Use the JSON fields to fill in useful document metadata, using js or substitutions, eg:",
			                    	   "docMetadata": {
			                    		   "title": "$metadata.json.TITLE_FIELD",
			                    		   "publishedDate": "$SCRIPT( return _doc.metadata.json[0].DATE_FIELD; )",
			                    		   "fullText": "$metadata.json.TEXT_FIELD1 $metadata.json.TEXT_FIELD2"
			                    	   }
			                       },
			                       {
			                    	   "display": "For JSON records you often won't have enough text to perform NLP-based enrichment, but you can create entities directly from the fields, eg:",
			                    	   "entities": [
			                    	                {
			                    	                	"useDocGeo": false,
			                    	                	"type": "Person",
			                    	                	"iterateOver": "metadata.json.people",
			                    	                	"disambiguated_name": "$personName",
			                    	                	"actual_name": "$personName",
			                    	                	"dimension": "Who"
			                    	                },
			                    	                {
			                    	                	"useDocGeo": false,
			                    	                	"type": "Location",
			                    	                	"disambiguated_name": "$metadata.json.placeName",
			                    	                	"actual_name": "$metadata.json.placeName",
			                    	                	"geotag": {
			                    	                		"ontology_type": "point",
			                    	                		"alternatives": [
			                    	                		                 {
			                    	                		                	 "strictMatch": true,
			                    	                		                	 "country": "$metadata.json.country",
			                    	                		                	 "geoType": "auto",
			                    	                		                	 "city": "$metadata.json.city"
			                    	                		                 }
			                    	                		                 ],
			                    	                		                 "lon": "$metadata.json.longitude",
			                    	                		                 "geoType": "manual",
			                    	                		                 "lat": "$metadata.json.latitude"
			                    	                	},
			                    	                	"dimension": "Where"
			                    	                }
			                    	                ]
			                       },
			                       {
			                    	   "display": "For JSON records you often won't have enough text to perform NLP-based enrichment, but you can create associations directly from the fields, eg:",
			                    	   "associations": [
			                    	                    {
			                    	                    	"verb": "visits",
			                    	                    	"assoc_type": "Event",
			                    	                    	"entity1": "Person",
			                    	                    	"verb_category": "location",
			                    	                    	"iterateOver": "entity1,entity2",
			                    	                    	"entity2": "Location"
			                    	                    },
			                    	                    {
			                    	                    	"verb": "resides_at",
			                    	                    	"entity1_index": "${metadata.json.author.personName}/person",
			                    	                    	"assoc_type": "Fact",
			                    	                    	"entity2_index": "${metadata.json.author.homeAddress}/location",
			                    	                    	"verb_category": "location"
			                    	                    },
			                    	                    {
			                    	                    	"verb": "said",
			                    	                    	"entity1": "${metadata.json.author.personName}/person",
			                    	                    	"verb_category": "quotation",
			                    	                    	"entity2": "${metadata.json.quotation}"
			                    	                    }
			                    	                    ]
			                       },
			                       {
			                    	   "display": "Improve ingest performance by not full-text-indexing the JSON/XML object itself if a JSON object (the full text, entities etc still get indexed)",
			                    	   "searchIndex": {
			                    		   "metadataFieldList": "+"
			                    	   }
			                       }
			                       ]
		},		

		"database": 
		{
			"description": "Access a SQL database - the first time through 'query' is called, subsequently 'deltaQuery' is called.",
			"extractType": "Database",
			"isPublic": true,
			"mediaType": "Record",
			"title": "Basic SQL Database Source Template",
			"processingPipeline": [
			                       {
			                    	   "display": "The parameters necessary to access a SQL database - the first time through 'query' is called, subsequently 'deltaQuery' is called.",
			                    	   "database": {
			                    		   "authentication": {
			                                   "password": "PASSWORD",
			                                   "username": "USERNAME"
			                               },			                    		   
			                    		   "databaseName": "DATABASE",
			                    		   "databaseType": "mysql",
			                    		   "deleteQuery": "",
			                    		   "deltaQuery": "select * from TABLE where TIME_FIELD >= (select adddate(curdate(),-7))",
			                    		   "hostname": "DB_HOST",
			                    		   "port": "3306",
			                    		   "primaryKey": "KEY_FIELD",
			                    		   "publishedDate": "TIME_FIELD",
			                    		   "query": "select * from TABLE",
			                    		   "snippet": "DESC_FIELD",
			                    		   "title": "TITLE_FIELD",
			                    		   "url": "jdbc:mysql://DB_HOST:3306/DATABASE"
			                    	   }
			                       },
			                       {
			                    	   "display": "(Only check the DB every 5 minutes - you can set this to whatever time you want)",
			                    	   "harvest": {
			                    		   "searchCycle_secs": 300
			                    	   }
			                       },
			                       {
			                    	   "display": "Use the SQL fields to fill in useful document metadata, using js or substitutions, eg:",
			                    	   "docMetadata": {
			                    		   "title": "$metadata.TITLE_FIELD",
			                    		   "publishedDate": "$SCRIPT( return _doc.metadata.DATE_FIELD[0]; )",
			                    		   "fullText": "$metadata.TEXT_FIELD1 $metadata.TEXT_FIELD2"
			                    	   }
			                       },
			                       {
			                    	   "display": "For SQL records you often won't have enough text to perform NLP-based enrichment, but you can create entities directly from the SQL columns, eg:",
			                    	   "entities": [
			                    	                {
			                    	                	"useDocGeo": false,
			                    	                	"type": "Person",
			                    	                	"disambiguated_name": "$metadata.personName",
			                    	                	"actual_name": "$metadata.personName",
			                    	                	"dimension": "Who"
			                    	                },
			                    	                {
			                    	                	"useDocGeo": false,
			                    	                	"type": "Location",
			                    	                	"disambiguated_name": "$metadata.placeName",
			                    	                	"actual_name": "$metadata.placeName",
			                    	                	"geotag": {
			                    	                		"ontology_type": "point",
			                    	                		"alternatives": [
			                    	                		                 {
			                    	                		                	 "strictMatch": true,
			                    	                		                	 "country": "$metadata.country",
			                    	                		                	 "geoType": "auto",
			                    	                		                	 "city": "$metadata.city"
			                    	                		                 }
			                    	                		                 ],
			                    	                		                 "lon": "$metadata.longitude",
			                    	                		                 "geoType": "manual",
			                    	                		                 "lat": "$metadata.latitude"
			                    	                	},
			                    	                	"dimension": "Where"
			                    	                }
			                    	                ]
			                       },
			                       {
			                    	   "display": "For SQL records you often won't have enough text to perform NLP-based enrichment, but you can create associations directly from the SQL columns, eg:",
			                    	   "associations": [
			                    	                    {
			                    	                    	"verb": "visits",
			                    	                    	"assoc_type": "Event",
			                    	                    	"entity1": "Person",
			                    	                    	"verb_category": "location",
			                    	                    	"iterateOver": "entity1,entity2",
			                    	                    	"entity2": "Location"
			                    	                    },
			                    	                    {
			                    	                    	"verb": "resides_at",
			                    	                    	"entity1_index": "${metadata.personName}/person",
			                    	                    	"assoc_type": "Fact",
			                    	                    	"entity2_index": "${metadata.homeAddress}/location",
			                    	                    	"verb_category": "location"
			                    	                    },
			                    	                    {
			                    	                    	"verb": "said",
			                    	                    	"entity1": "${metadata.personName}/person",
			                    	                    	"verb_category": "quotation",
			                    	                    	"entity2": "${metadata.quotation}"
			                    	                    }
			                    	                    ]
			                       },
			                       {
			                    	   "display": "Improve ingest performance by not full-text-indexing the DB fields themselves (the full text, entities etc still get indexed)",
			                    	   "searchIndex": {
			                    		   "metadataFieldList": "+"
			                    	   }
			                       }
			                       ]
		},
		
		"logstash": {
			"description": "Use logstash to pull records into the logstash-specific index (does not generate Infinit.e documents like other harvest types).",
			"extractType": "Logstash",
			"isPublic": true,
			"mediaType": "Record",
			"title": "Logstash template source",			
			"processingPipeline": [
			                       {
			                    	   "display": "Just contains a string in which to put the logstash configuration (minus the output, which is appended by Infinit.e)",
			                    	   "logstash": {
			                    		   "config": "input {\n}\n\nfilter {\n}\n\n",
			                    		   "streaming": true,
			                    		   "distributed": false,
			                    		   "testDebugOutput": false,
			                    		   "testInactivityTimeout_secs": 10
			                    	   }
			                       }
			                       ]
		},
		
		"data_bucket": 
{
    "description": "This object generates a V2 analytics thread / data bucket if the V2 migration plugin is installed.\r\n Note created/modified/_id/display_name/tags/owner_id/access_rights are taken from the parent source.",
    "extractType": "V2DataBucket",
    "isPublic": true,
    "mediaType": "Record",
    "processingPipeline": [
        {
            "data_bucket": {
                "data_schema": {
                    "search_index_schema": {},
                    "storage_schema": {}
                },
                "harvest_configs": [
                    {
                        "config": {}
                    }
                ]
            }
        }
    ],
    "title": "Template V2 analytics data bucket",
    "templateProcessingFlow": {
        "root": true,
        "label": "Bucket",
        "children": [
            {
                "root": false,
                "label": "Bucket Metadata",
                "element": {
                    "enabled": true,
                    "short_name": "Bucket Metadata",
                    "summary": "",
                    "row": 0,
                    "col": 0,
                    "sizeX": 1,
                    "sizeY": 1,
                    "expandable": false,
                    "configurable": true,
                    "deletable": true,
                    "form_model": {"poll_frequency": "10 min"},
                    "template": {
                        "display_name": "Bucket Metadata",
                        "key": "data_bucket",
                        "categories": [
                            "Metadata"
                        ],
                        "filters": [
                            "Bucket"
                        ],
                        "expandable": false,
                        "form_info": "General bucket parameters",
                        "schema": [
                            {
                                "key": "full_name",
                                "type": "horizontalInput",
                                "templateOptions": {
                                    "label": "Bucket Path",
                                    "placeholder": "The virtual bucket path, eg /path/to/bucket",
                                    "required": true
                                }
                            },
                            {
                                "key": "poll_frequency",
                                "type": "horizontalInput",
                                "templateOptions": {
                                    "label": "Poll Frequency",
                                    "placeholder": "Human readable frequency (eg '10min', '1 day') for how often this harvester is polled",
                                    "required": false
                                }
                            },
                            {
                                "template": "<hr/>"
                            },
                            {
                                "key": "show_test_settings",
                                "type": "checkbox",
                                "templateOptions": {
                                    "label": "Show Test Settings"
                                }
                            },
                            {
                                "key": "requested_num_objects",
                                "hideExpression": "!model.show_test_settings",
                                "type": "horizontalInput",
                                "defaultValue": "100",
                                "templateOptions": {
                                    "pattern": "[0-9]+",
                                    "label": "Requested Data Objects",
                                    "placeholder": "The desired number of data objects to be returned by the test",
                                    "required": true
                                }
                            },
                            {
                                "key": "max_startup_time_secs",
                                "hideExpression": "!model.show_test_settings",
                                "type": "horizontalInput",
                                "defaultValue": "60",
                                "templateOptions": {
                                    "pattern": "[0-9]+",
                                    "label": "Max Startup Time (s)",
                                    "placeholder": "The maximum number of seconds to wait for the test to startup",
                                    "required": true
                                }
                            },
                            {
                                "key": "max_run_time_secs",
                                "hideExpression": "!model.show_test_settings",
                                "type": "horizontalInput",
                                "defaultValue": "120",
                                "templateOptions": {
                                    "pattern": "[0-9]+",
                                    "label": "Max Run Time (s)",
                                    "placeholder": "The maximum number of seconds to wait for the test to run, after it has started up",
                                    "required": true
                                }
                            },
                            {
                                "key": "max_storage_time_secs",
                                "hideExpression": "!model.show_test_settings",
                                "type": "horizontalInput",
                                "defaultValue": "86400",
                                "templateOptions": {
                                    "pattern": "[0-9]+",
                                    "label": "Max Test Storage Time (s)",
                                    "placeholder": "The maximum time (secs) to keep the test result data",
                                    "required": false
                                }
                            },
                            {
                                "key": "overwrite_existing_data",
                                "hideExpression": "!model.show_test_settings",
                                "type": "horizontalCheckbox",
                                "defaultValue": true,
                                "templateOptions": {
                                    "label": "Overwrite existing data",
                                    "required": false
                                }
                            }
                        ],
                        "building_function": {
                            "_fn": "function(errs, template, curr_obj, all_templates, root_obj, hierarchy, rows, cols) { \n  curr_obj.full_name = template.element.form_model.full_name;\n    \tvar pf = template.element.form_model.poll_frequency; \n  curr_obj.poll_frequency = (pf || '').length > 0 ? pf : undefined; \t\tif (template.element.form_model.requested_num_objects) { curr_obj.test_params = {};\n\t\tvar doc = curr_obj.test_params;\n\t\tdoc.requested_num_objects = parseInt(template.element.form_model.requested_num_objects);\n\t\tdoc.max_startup_time_secs = parseInt(template.element.form_model.max_startup_time_secs);\n\t\tdoc.max_run_time_secs = parseInt(template.element.form_model.max_run_time_secs);\n\t\tif ((template.element.form_model.max_storage_time_secs || '').length > 0) doc.max_storage_time_secs = parseInt(template.element.form_model.max_storage_time_secs);\n\t\tdoc.overwrite_existing_data = template.element.form_model.overwrite_existing_data; }\n\n}"
                        }
                    }
                },
                "children": []
            },
            {
                "root": false,
                "label": "Data Schema Container",
                "element": {
                    "enabled": true,
                    "short_name": "Data Schema Container",
                    "summary": "",
                    "row": 0,
                    "col": 1,
                    "sizeX": 1,
                    "sizeY": 1,
                    "expandable": true,
                    "configurable": true,
                    "deletable": true,
                    "form_model": {},
                    "template": {
                        "display_name": "Data Schema Container",
                        "key": "data_schema",
                        "categories": [
                            "Metadata"
                        ],
                        "filters": [
                            "Bucket"
                        ],
                        "child_filters": [
                            "data_service_schema"
                        ],
                        "expandable": true,
                        "building_function": {
                            "_fn": "function(errs, template, curr_obj, all_templates, root_obj, hierarchy, rows, cols) { curr_obj.data_schema = {}; return curr_obj.data_schema; }"
                        },
                        "form_info": "<p>This is a container for the data schema for the different attributes of the stored data</p>\n<p>It has no attributes of its own - instead expand it using the <a class=\"glyphicon glyphicon-fullscreen\"></a> icon\n and then add the desired attributes from those available.\n</p>\n"
                    }
                },
                "children": [
                    {
                        "root": false,
                        "label": "Raw JSON Search Index Schema",
                        "element": {
                            "enabled": true,
                            "short_name": "Raw JSON Search Index Schema",
                            "summary": "",
                            "row": 0,
                            "col": 0,
                            "sizeX": 1,
                            "sizeY": 1,
                            "expandable": false,
                            "configurable": true,
                            "deletable": true,
                            "form_model": {},
                            "template": {
                                "display_name": "Raw JSON Search Index Schema",
                                "key": "data_service_schema",
                                "categories": [
                                    "Schema"
                                ],
                                "filters": [
                                    "Bucket/**"
                                ],
                                "expandable": false,
                                "form_info": "For advanced users: create an empty search index schema JSON object that can be Raw JSON to provide the desired functionality\n",
                                "schema": [
                                    {
                                        "key": "schema",
                                        "type": "code_input",
                                        "defaultValue": "{\n}",
                                        "templateOptions": {
                                            "label": "Search Index Schema JSON",
                                            "codemirror": {
                                                "lineNumbers": true,
                                                "smartIndent": true,
                                                "mode": "javascript"
                                            }
                                        }
                                    }
                                ],
                                "building_function": {
                                    "_fn": "function(errs, template, curr_obj, all_templates, root_obj, hierarchy, rows, cols) { curr_obj.search_index_schema = JSON.parse(template.element.form_model.schema || '{}'); }"
                                }
                            }
                        },
                        "children": []
                    },
                    {
                        "root": false,
                        "label": "Raw JSON Storage Schema",
                        "element": {
                            "enabled": true,
                            "short_name": "Raw JSON Storage Schema",
                            "summary": "",
                            "row": 0,
                            "col": 1,
                            "sizeX": 1,
                            "sizeY": 1,
                            "expandable": true,
                            "configurable": true,
                            "deletable": true,
                            "form_model": {},
                            "template": {
                                "display_name": "Raw JSON Storage Schema",
                                "key": "data_service_schema",
                                "categories": [
                                    "Schema"
                                ],
                                "filters": [
                                    "Bucket/**"
                                ],
                                "expandable": true,
                                "form_info": "For advanced users: create an empty storage schema JSON object that can be Raw JSON to provide the desired functionality\n",
                                "schema": [
                                    {
                                        "key": "schema",
                                        "type": "code_input",
                                        "defaultValue": "{\n}",
                                        "templateOptions": {
                                            "label": "Storage Schema JSON",
                                            "codemirror": {
                                                "lineNumbers": true,
                                                "smartIndent": true,
                                                "mode": "javascript"
                                            }
                                        }
                                    }
                                ],
                                "building_function": {
                                    "_fn": "function(errs, template, curr_obj, all_templates, root_obj, hierarchy, rows, cols) { curr_obj.storage_schema = JSON.parse(template.element.form_model.schema || '{}'); return curr_obj.storage_schema; }"
                                }
                            }
                        },
                        "children": []
                    },
                    {
                        "root": false,
                        "label": "Raw JSON Temporal Schema",
                        "element": {
                            "enabled": false,
                            "short_name": "Raw JSON Temporal Schema",
                            "summary": "",
                            "row": 0,
                            "col": 2,
                            "sizeX": 1,
                            "sizeY": 1,
                            "expandable": false,
                            "configurable": true,
                            "deletable": true,
                            "form_model": {},
                            "template": {
                                "display_name": "Raw JSON Temporal Schema",
                                "key": "data_service_schema",
                                "categories": [
                                    "Schema"
                                ],
                                "filters": [
                                    "Bucket/**"
                                ],
                                "expandable": false,
                                "form_info": "For advanced users: create an empty temporal schema JSON object that can be Raw JSON to provide the desired functionality\n",
                                "schema": [
                                    {
                                        "key": "schema",
                                        "type": "code_input",
                                        "defaultValue": "{\n}",
                                        "templateOptions": {
                                            "label": "Temporal Schema JSON",
                                            "codemirror": {
                                                "lineNumbers": true,
                                                "smartIndent": true,
                                                "mode": "javascript"
                                            }
                                        }
                                    }
                                ],
                                "building_function": {
                                    "_fn": "function(errs, template, curr_obj, all_templates, root_obj, hierarchy, rows, cols) { curr_obj.temporal_schema = JSON.parse(template.element.form_model.schema || '{}'); }"
                                }
                            }
                        },
                        "children": []
                    },
                    {
                        "root": false,
                        "label": "Raw JSON Document Schema",
                        "element": {
                            "enabled": false,
                            "short_name": "Raw JSON Document Schema",
                            "summary": "",
                            "row": 0,
                            "col": 3,
                            "sizeX": 1,
                            "sizeY": 1,
                            "expandable": true,
                            "configurable": true,
                            "deletable": true,
                            "form_model": {},
                            "template": {
                                "display_name": "Raw JSON Document Schema",
                                "key": "data_service_schema",
                                "categories": [
                                    "Schema"
                                ],
                                "filters": [
                                    "Bucket/**"
                                ],
                                "expandable": true,
                                "child_filters": [
                                    "enrichment_meta"
                                ],
                                "form_info": "For advanced users: create an empty document schema JSON object that can be Raw JSON to provide the desired functionality\n",
                                "schema": [
                                    {
                                        "key": "schema",
                                        "type": "code_input",
                                        "defaultValue": "{\n}",
                                        "templateOptions": {
                                            "label": "Document Schema JSON",
                                            "codemirror": {
                                                "lineNumbers": true,
                                                "smartIndent": true,
                                                "mode": "javascript"
                                            }
                                        }
                                    }
                                ],
                                "building_function": {
                                    "_fn": "function(errs, template, curr_obj, all_templates, root_obj, hierarchy, rows, cols) { curr_obj.document_schema = JSON.parse(template.element.form_model.schema || '{}'); curr_obj.document_schema.custom_deduplication_configs = []; return curr_obj.document_schema.custom_deduplication_configs; }"
                                }
                            }
                        },
                        "children": []
                    },
                    {
                        "root": false,
                        "label": "Raw JSON Columnar Schema",
                        "element": {
                            "enabled": false,
                            "short_name": "Raw JSON Columnar Schema",
                            "summary": "",
                            "row": 1,
                            "col": 0,
                            "sizeX": 1,
                            "sizeY": 1,
                            "expandable": false,
                            "configurable": true,
                            "deletable": true,
                            "form_model": {},
                            "template": {
                                "display_name": "Raw JSON Columnar Schema",
                                "key": "data_service_schema",
                                "categories": [
                                    "Schema"
                                ],
                                "filters": [
                                    "Bucket/**"
                                ],
                                "expandable": false,
                                "form_info": "For advanced users: create an empty columnar schema JSON object that can be Raw JSON to provide the desired functionality\n",
                                "schema": [
                                    {
                                        "key": "schema",
                                        "type": "code_input",
                                        "defaultValue": "{\n}",
                                        "templateOptions": {
                                            "label": "Columnar Schema JSON",
                                            "codemirror": {
                                                "lineNumbers": true,
                                                "smartIndent": true,
                                                "mode": "javascript"
                                            }
                                        }
                                    }
                                ],
                                "building_function": {
                                    "_fn": "function(errs, template, curr_obj, all_templates, root_obj, hierarchy, rows, cols) { curr_obj.columnar_schema = JSON.parse(template.element.form_model.schema || '{}'); }"
                                }
                            }
                        },
                        "children": []
                    }
                ]
            },
            {
                "root": false,
                "label": "Generic Harvester",
                "element": {
                    "enabled": true,
                    "short_name": "Generic Harvester",
                    "summary": "",
                    "row": 1,
                    "col": 0,
                    "sizeX": 1,
                    "sizeY": 1,
                    "expandable": false,
                    "configurable": true,
                    "deletable": true,
                    "form_model": {},
                    "template": {
                        "display_name": "Generic Harvester",
                        "key": "harvest_engine",
                        "categories": [
                            "Harvester"
                        ],
                        "filters": [
                            "Bucket"
                        ],
                        "expandable": false,
                        "form_info": "For advanced users: create an empty Harvester Config that users can build with the form; and configure by writing raw JSON",
                        "schema": [
                            {
                                "key": "harvest_technology_name_or_id",
                                "type": "horizontalInput",
                                "templateOptions": {
                                    "label": "Technology Library Name",
                                    "placeholder": "The path to the technology, eg /app/aleph2/library/harvester.jar",
                                    "required": true
                                }
                            },
                            {
                                "template": "<hr/>"
                            },
                            {
                                "key": "show_advanced",
                                "type": "checkbox",
                                "templateOptions": {
                                    "label": "Show Advanced Options"
                                }
                            },
                            {
                                "key": "module_name_or_id",
                                "type": "horizontalInput",
                                "hideExpression": "!model.show_advanced",
                                "templateOptions": {
                                    "label": "Technology Module Name",
                                    "placeholder": "For harvesters with pluggable modules, the path to the module, eg /app/aleph2/library/harvester_module.jar",
                                    "required": false
                                }
                            },
                            {
                                "key": "entry_point",
                                "type": "horizontalInput",
                                "hideExpression": "!model.show_advanced",
                                "templateOptions": {
                                    "label": "Entry Point Override",
                                    "placeholder": "For harvest modules with multiple entry points, specifies the JVM class to execute, eg com.ikanow.aleph2.harvest.module.Module1EntryPoint",
                                    "required": false
                                }
                            },
                            {
                                "key": "library_names_or_ids",
                                "type": "multiInput",
                                "hideExpression": "!model.show_advanced",
                                "templateOptions": {
                                    "label": "Additional Library Modules",
                                    "inputOptions": {
                                        "type": "input",
                                        "templateOptions": {
                                            "label": "Technology Module Name",
                                            "placeholder": "For harvesters that need additional library modules, the path to the module, eg /app/aleph2/library/harvester_module.jar",
                                            "required": false
                                        }
                                    }
                                }
                            },
                            {
                                "key": "node_list_rules",
                                "type": "horizontalInput",
                                "hideExpression": "!model.show_advanced",
                                "templateOptions": {
                                    "label": "Node rules",
                                    "placeholder": "Comma-separated list of rules determining on which nodes this harvester will run",
                                    "required": false
                                }
                            },
                            {
                                "key": "multi_node_enabled",
                                "type": "horizontalCheckbox",
                                "defaultValue": false,
                                "hideExpression": "!model.show_advanced",
                                "templateOptions": {
                                    "label": "Harvester multi-node enabled",
                                    "required": false
                                }
                            },
                            {
                                "key": "lock_to_nodes",
                                "type": "horizontalCheckbox",
                                "defaultValue": true,
                                "hideExpression": "!model.show_advanced",
                                "templateOptions": {
                                    "label": "Lock harvester to the same node/set of nodes (recommend leave as true)",
                                    "required": false
                                }
                            },
                            {
                                "template": "<hr/>"
                            },
                            {
                                "key": "config",
                                "type": "code_input",
                                "defaultValue": "{\n}",
                                "templateOptions": {
                                    "label": "Harvest Configuration JSON",
                                    "codemirror": {
                                        "lineNumbers": true,
                                        "smartIndent": true,
                                        "mode": "javascript"
                                    }
                                }
                            }
                        ],
                        "building_function": {
                            "_fn": "function(errs, template, curr_obj, all_templates, root_obj, hierarchy, rows, cols) { \n   curr_obj.harvest_technology_name_or_id = template.element.form_model.harvest_technology_name_or_id; \n  if (template.element.form_model.node_list_rules && (template.element.form_model.node_list_rules.length > 0)) {\n\t  curr_obj.node_list_rules = (template.element.form_model.node_list_rules || '').replace(/\\s*,\\s*/g, \",\").split(\",\");\n}\n\t  curr_obj.multi_node_enabled = template.element.form_model.multi_node_enabled;\n  curr_obj.lock_to_nodes = template.element.form_model.lock_to_nodes;\n  curr_obj.harvest_configs = [\n    {\n      module_name_or_id: template.element.form_model.module_name_or_id, \n      library_names_or_ids: template.element.form_model.library_names_or_ids,\n      entry_point: template.element.form_model.entry_point, \n      config: JSON.parse(template.element.form_model.config || '{}') \n    }]; \n}"
                        }
                    }
                },
                "children": []
            },
            {
                "root": false,
                "label": "Batch Enrichment Pipeline",
                "element": {
                    "enabled": false,
                    "short_name": "Batch Enrichment Pipeline",
                    "summary": "",
                    "row": 2,
                    "col": 0,
                    "sizeX": 1,
                    "sizeY": 1,
                    "expandable": true,
                    "configurable": true,
                    "deletable": true,
                    "form_model": {},
                    "template": {
                        "display_name": "Batch Enrichment Pipeline",
                        "key": "enrichment_engine",
                        "categories": [
                            "Enrichment"
                        ],
                        "filters": [
                            "Bucket"
                        ],
                        "child_filters": [
                            "batch_enrichment_meta",
                            "enrichment_meta"
                        ],
                        "expandable": true,
                        "building_function": {
                            "_fn": "function(errs, template, curr_obj, all_templates, root_obj, hierarchy, rows, cols) { curr_obj.master_enrichment_type = 'batch'; curr_obj.batch_enrichment_configs = []; return curr_obj.batch_enrichment_configs; }"
                        },
                        "form_info": "<p>This is a container for a batch enrichment pipeline that will transform incoming objects before writing them into the bucket output.</p>\n<p>It has no attributes of its own - instead expand it using the <a class=\"glyphicon glyphicon-fullscreen\"></a> icon\n and then add the desired attributes from those available.\n</p>\n"
                    }
                },
                "children": []
            },
            {
                "root": false,
                "label": "Streaming Enrichment Topology",
                "element": {
                    "enabled": false,
                    "short_name": "Streaming Enrichment Topology",
                    "summary": "",
                    "row": 2,
                    "col": 1,
                    "sizeX": 1,
                    "sizeY": 1,
                    "expandable": true,
                    "configurable": true,
                    "deletable": true,
                    "form_model": {},
                    "template": {
                        "display_name": "Streaming Enrichment Topology",
                        "key": "enrichment_engine",
                        "categories": [
                            "Enrichment"
                        ],
                        "filters": [
                            "Bucket"
                        ],
                        "child_filters": [
                            "stream_enrichment_meta",
                            "enrichment_meta"
                        ],
                        "expandable": true,
                        "building_function": {
                            "_fn": "function(errs, template, curr_obj, all_templates, root_obj, hierarchy, rows, cols) { curr_obj.master_enrichment_type = 'streaming'; curr_obj.streaming_enrichment_topology = {}; return curr_obj.streaming_enrichment_topology; }"
                        },
                        "form_info": "<p>This is a container for a streaming enrichment engine that will transform incoming objects before writing them into the bucket output.</p>\n<p>It has no attributes of its own - instead expand it using the <a class=\"glyphicon glyphicon-fullscreen\"></a> icon\n and then add the desired attributes from those available.\n</p>\n"
                    }
                },
                "children": []
            }
        ]
    }
}
,

		"analytics_bucket": 		
{
    "description": "This object generates a V2 analytics thread / data bucket if the V2 migration plugin is installed.\r\n Note created/modified/_id/display_name/tags/owner_id/access_rights are taken from the parent source.",
    "extractType": "V2DataBucket",
    "isPublic": true,
    "mediaType": "Record",
    "processingPipeline": [
        {
            "data_bucket": {
                "data_schema": {
                    "search_index_schema": {},
                    "storage_schema": {}
                },
                "analytic_thread": {
                    "jobs": [],
                    "trigger_config": {
                        "auto_calculate": false,
                        "schedule": "10 min"
                    }
                }
            }
        }
    ],
    "title": "Template V2 analytics data bucket",
    "templateProcessingFlow": {
        "root": true,
        "label": "Bucket",
        "children": [
            {
                "root": false,
                "label": "Bucket Metadata",
                "element": {
                    "enabled": true,
                    "short_name": "Bucket Metadata",
                    "summary": "",
                    "row": 0,
                    "col": 0,
                    "sizeX": 1,
                    "sizeY": 1,
                    "expandable": false,
                    "configurable": true,
                    "deletable": true,
                    "form_model": {},
                    "template": {
                        "display_name": "Bucket Metadata",
                        "key": "data_bucket",
                        "categories": [
                            "Metadata"
                        ],
                        "filters": [
                            "Bucket"
                        ],
                        "expandable": false,
                        "form_info": "General bucket parameters",
                        "schema": [
                            {
                                "key": "full_name",
                                "type": "horizontalInput",
                                "templateOptions": {
                                    "label": "Bucket Path",
                                    "placeholder": "The virtual bucket path, eg /path/to/bucket",
                                    "required": true
                                }
                            },
                            {
                                "key": "poll_frequency",
                                "type": "horizontalInput",
                                "templateOptions": {
                                    "label": "Poll Frequency",
                                    "placeholder": "Human readable frequency (eg '10min', '1 day') for how often this harvester is polled",
                                    "required": false
                                }
                            },
                            {
                                "template": "<hr/>"
                            },
                            {
                                "key": "show_test_settings",
                                "type": "checkbox",
                                "templateOptions": {
                                    "label": "Show Test Settings"
                                }
                            },
                            {
                                "key": "requested_num_objects",
                                "hideExpression": "!model.show_test_settings",
                                "type": "horizontalInput",
                                "defaultValue": "100",
                                "templateOptions": {
                                    "pattern": "[0-9]+",
                                    "label": "Requested Data Objects",
                                    "placeholder": "The desired number of data objects to be returned by the test",
                                    "required": true
                                }
                            },
                            {
                                "key": "max_startup_time_secs",
                                "hideExpression": "!model.show_test_settings",
                                "type": "horizontalInput",
                                "defaultValue": "60",
                                "templateOptions": {
                                    "pattern": "[0-9]+",
                                    "label": "Max Startup Time (s)",
                                    "placeholder": "The maximum number of seconds to wait for the test to startup",
                                    "required": true
                                }
                            },
                            {
                                "key": "max_run_time_secs",
                                "hideExpression": "!model.show_test_settings",
                                "type": "horizontalInput",
                                "defaultValue": "120",
                                "templateOptions": {
                                    "pattern": "[0-9]+",
                                    "label": "Max Run Time (s)",
                                    "placeholder": "The maximum number of seconds to wait for the test to run, after it has started up",
                                    "required": true
                                }
                            },
                            {
                                "key": "max_storage_time_secs",
                                "hideExpression": "!model.show_test_settings",
                                "type": "horizontalInput",
                                "defaultValue": "86400",
                                "templateOptions": {
                                    "pattern": "[0-9]+",
                                    "label": "Max Test Storage Time (s)",
                                    "placeholder": "The maximum time (secs) to keep the test result data",
                                    "required": false
                                }
                            },
                            {
                                "key": "overwrite_existing_data",
                                "hideExpression": "!model.show_test_settings",
                                "type": "horizontalCheckbox",
                                "defaultValue": true,
                                "templateOptions": {
                                    "label": "Overwrite existing data",
                                    "required": false
                                }
                            }
                        ],
                        "building_function": {
                            "_fn": "function(errs, template, curr_obj, all_templates, root_obj, hierarchy, rows, cols) { \n  curr_obj.full_name = template.element.form_model.full_name;\n    \tvar pf = template.element.form_model.poll_frequency; \n  curr_obj.poll_frequency = (pf || '').length > 0 ? pf : undefined; \t\tif (template.element.form_model.requested_num_objects) { curr_obj.test_params = {};\n\t\tvar doc = curr_obj.test_params;\n\t\tdoc.requested_num_objects = parseInt(template.element.form_model.requested_num_objects);\n\t\tdoc.max_startup_time_secs = parseInt(template.element.form_model.max_startup_time_secs);\n\t\tdoc.max_run_time_secs = parseInt(template.element.form_model.max_run_time_secs);\n\t\tif ((template.element.form_model.max_storage_time_secs || '').length > 0) doc.max_storage_time_secs = parseInt(template.element.form_model.max_storage_time_secs);\n\t\tdoc.overwrite_existing_data = template.element.form_model.overwrite_existing_data; }\n\n}"
                        }
                    }
                },
                "children": []
            },
            {
                "root": false,
                "label": "Data Schema Container",
                "element": {
                    "enabled": true,
                    "short_name": "Data Schema Container",
                    "summary": "",
                    "row": 0,
                    "col": 1,
                    "sizeX": 1,
                    "sizeY": 1,
                    "expandable": true,
                    "configurable": true,
                    "deletable": true,
                    "form_model": {},
                    "template": {
                        "display_name": "Data Schema Container",
                        "key": "data_schema",
                        "categories": [
                            "Metadata"
                        ],
                        "filters": [
                            "Bucket"
                        ],
                        "child_filters": [
                            "data_service_schema"
                        ],
                        "expandable": true,
                        "building_function": {
                            "_fn": "function(errs, template, curr_obj, all_templates, root_obj, hierarchy, rows, cols) { curr_obj.data_schema = {}; return curr_obj.data_schema; }"
                        },
                        "form_info": "<p>This is a container for the data schema for the different attributes of the stored data</p>\n<p>It has no attributes of its own - instead expand it using the <a class=\"glyphicon glyphicon-fullscreen\"></a> icon\n and then add the desired attributes from those available.\n</p>\n"
                    }
                },
                "children": [
                    {
                        "root": false,
                        "label": "Raw JSON Search Index Schema",
                        "element": {
                            "enabled": true,
                            "short_name": "Raw JSON Search Index Schema",
                            "summary": "",
                            "row": 0,
                            "col": 0,
                            "sizeX": 1,
                            "sizeY": 1,
                            "expandable": false,
                            "configurable": true,
                            "deletable": true,
                            "form_model": {},
                            "template": {
                                "display_name": "Raw JSON Search Index Schema",
                                "key": "data_service_schema",
                                "categories": [
                                    "Schema"
                                ],
                                "filters": [
                                    "Bucket/**"
                                ],
                                "expandable": false,
                                "form_info": "For advanced users: create an empty search index schema JSON object that can be Raw JSON to provide the desired functionality\n",
                                "schema": [
                                    {
                                        "key": "schema",
                                        "type": "code_input",
                                        "defaultValue": "{\n}",
                                        "templateOptions": {
                                            "label": "Search Index Schema JSON",
                                            "codemirror": {
                                                "lineNumbers": true,
                                                "smartIndent": true,
                                                "mode": "javascript"
                                            }
                                        }
                                    }
                                ],
                                "building_function": {
                                    "_fn": "function(errs, template, curr_obj, all_templates, root_obj, hierarchy, rows, cols) { curr_obj.search_index_schema = JSON.parse(template.element.form_model.schema || '{}'); }"
                                }
                            }
                        },
                        "children": []
                    },
                    {
                        "root": false,
                        "label": "Raw JSON Storage Schema",
                        "element": {
                            "enabled": true,
                            "short_name": "Raw JSON Storage Schema",
                            "summary": "",
                            "row": 0,
                            "col": 1,
                            "sizeX": 1,
                            "sizeY": 1,
                            "expandable": true,
                            "configurable": true,
                            "deletable": true,
                            "form_model": {},
                            "template": {
                                "display_name": "Raw JSON Storage Schema",
                                "key": "data_service_schema",
                                "categories": [
                                    "Schema"
                                ],
                                "filters": [
                                    "Bucket/**"
                                ],
                                "expandable": true,
                                "form_info": "For advanced users: create an empty storage schema JSON object that can be Raw JSON to provide the desired functionality\n",
                                "schema": [
                                    {
                                        "key": "schema",
                                        "type": "code_input",
                                        "defaultValue": "{\n}",
                                        "templateOptions": {
                                            "label": "Storage Schema JSON",
                                            "codemirror": {
                                                "lineNumbers": true,
                                                "smartIndent": true,
                                                "mode": "javascript"
                                            }
                                        }
                                    }
                                ],
                                "building_function": {
                                    "_fn": "function(errs, template, curr_obj, all_templates, root_obj, hierarchy, rows, cols) { curr_obj.storage_schema = JSON.parse(template.element.form_model.schema || '{}'); return curr_obj.storage_schema; }"
                                }
                            }
                        },
                        "children": []
                    },
                    {
                        "root": false,
                        "label": "Raw JSON Temporal Schema",
                        "element": {
                            "enabled": false,
                            "short_name": "Raw JSON Temporal Schema",
                            "summary": "",
                            "row": 0,
                            "col": 2,
                            "sizeX": 1,
                            "sizeY": 1,
                            "expandable": false,
                            "configurable": true,
                            "deletable": true,
                            "form_model": {},
                            "template": {
                                "display_name": "Raw JSON Temporal Schema",
                                "key": "data_service_schema",
                                "categories": [
                                    "Schema"
                                ],
                                "filters": [
                                    "Bucket/**"
                                ],
                                "expandable": false,
                                "form_info": "For advanced users: create an empty temporal schema JSON object that can be Raw JSON to provide the desired functionality\n",
                                "schema": [
                                    {
                                        "key": "schema",
                                        "type": "code_input",
                                        "defaultValue": "{\n}",
                                        "templateOptions": {
                                            "label": "Temporal Schema JSON",
                                            "codemirror": {
                                                "lineNumbers": true,
                                                "smartIndent": true,
                                                "mode": "javascript"
                                            }
                                        }
                                    }
                                ],
                                "building_function": {
                                    "_fn": "function(errs, template, curr_obj, all_templates, root_obj, hierarchy, rows, cols) { curr_obj.temporal_schema = JSON.parse(template.element.form_model.schema || '{}'); }"
                                }
                            }
                        },
                        "children": []
                    },
                    {
                        "root": false,
                        "label": "Raw JSON Document Schema",
                        "element": {
                            "enabled": false,
                            "short_name": "Raw JSON Document Schema",
                            "summary": "",
                            "row": 0,
                            "col": 3,
                            "sizeX": 1,
                            "sizeY": 1,
                            "expandable": true,
                            "configurable": true,
                            "deletable": true,
                            "form_model": {},
                            "template": {
                                "display_name": "Raw JSON Document Schema",
                                "key": "data_service_schema",
                                "categories": [
                                    "Schema"
                                ],
                                "filters": [
                                    "Bucket/**"
                                ],
                                "expandable": true,
                                "child_filters": [
                                    "enrichment_meta"
                                ],
                                "form_info": "For advanced users: create an empty document schema JSON object that can be Raw JSON to provide the desired functionality\n",
                                "schema": [
                                    {
                                        "key": "schema",
                                        "type": "code_input",
                                        "defaultValue": "{\n}",
                                        "templateOptions": {
                                            "label": "Document Schema JSON",
                                            "codemirror": {
                                                "lineNumbers": true,
                                                "smartIndent": true,
                                                "mode": "javascript"
                                            }
                                        }
                                    }
                                ],
                                "building_function": {
                                    "_fn": "function(errs, template, curr_obj, all_templates, root_obj, hierarchy, rows, cols) { curr_obj.document_schema = JSON.parse(template.element.form_model.schema || '{}'); curr_obj.document_schema.custom_deduplication_configs = []; return curr_obj.document_schema.custom_deduplication_configs; }"
                                }
                            }
                        },
                        "children": []
                    },
                    {
                        "root": false,
                        "label": "Raw JSON Columnar Schema",
                        "element": {
                            "enabled": false,
                            "short_name": "Raw JSON Columnar Schema",
                            "summary": "",
                            "row": 1,
                            "col": 0,
                            "sizeX": 1,
                            "sizeY": 1,
                            "expandable": false,
                            "configurable": true,
                            "deletable": true,
                            "form_model": {},
                            "template": {
                                "display_name": "Raw JSON Columnar Schema",
                                "key": "data_service_schema",
                                "categories": [
                                    "Schema"
                                ],
                                "filters": [
                                    "Bucket/**"
                                ],
                                "expandable": false,
                                "form_info": "For advanced users: create an empty columnar schema JSON object that can be Raw JSON to provide the desired functionality\n",
                                "schema": [
                                    {
                                        "key": "schema",
                                        "type": "code_input",
                                        "defaultValue": "{\n}",
                                        "templateOptions": {
                                            "label": "Columnar Schema JSON",
                                            "codemirror": {
                                                "lineNumbers": true,
                                                "smartIndent": true,
                                                "mode": "javascript"
                                            }
                                        }
                                    }
                                ],
                                "building_function": {
                                    "_fn": "function(errs, template, curr_obj, all_templates, root_obj, hierarchy, rows, cols) { curr_obj.columnar_schema = JSON.parse(template.element.form_model.schema || '{}'); }"
                                }
                            }
                        },
                        "children": []
                    }
                ]
            },
            {
                "root": false,
                "label": "Analytic Thread Container",
                "element": {
                    "enabled": true,
                    "short_name": "Analytic Thread Container",
                    "summary": "",
                    "row": 1,
                    "col": 0,
                    "sizeX": 1,
                    "sizeY": 1,
                    "expandable": true,
                    "configurable": true,
                    "deletable": true,
                    "form_model": {},
                    "template": {
                        "display_name": "Analytic Thread Container",
                        "key": "analytic_thread",
                        "categories": [
                            "Analytics"
                        ],
                        "filters": [
                            "Bucket"
                        ],
                        "child_filters": [
                            "analytic_job",
                            "analytic_trigger"
                        ],
                        "expandable": true,
                        "building_function": {
                            "_fn": "function(errs, template, curr_obj, all_templates, root_obj, hierarchy, rows, cols) { curr_obj.analytic_thread = { jobs: [] }; return curr_obj.analytic_thread; }"
                        },
                        "form_info": "<p>This is a container for the analytic jobs that fill the bucket with data</p>\n<p>It has no attributes of its own - instead expand it using the <a class=\"glyphicon glyphicon-fullscreen\"></a> icon\n and then add the desired attributes from those available.\n</p>\n"
                    }
                },
                "children": [
                    {
                        "root": false,
                        "label": "Raw JSON Analytic Trigger",
                        "element": {
                            "enabled": true,
                            "short_name": "Raw JSON Analytic Trigger",
                            "summary": "",
                            "row": 0,
                            "col": 0,
                            "sizeX": 1,
                            "sizeY": 1,
                            "expandable": false,
                            "configurable": true,
                            "deletable": true,
                            "form_model": {
                                "trigger": "{\n  \"auto_calculate\": false\n}",
                                "schedule": "10 min"
                            },
                            "template": {
                                "display_name": "Raw JSON Analytic Trigger",
                                "key": "analytic_trigger",
                                "categories": [
                                    "Scheduling"
                                ],
                                "filters": [
                                    "Bucket/analytic_thread"
                                ],
                                "expandable": false,
                                "form_info": "For advanced users: create an empty trigger JSON object that can be Raw JSON to provide the desired functionality\n",
                                "schema": [
                                    {
                                        "key": "schedule",
                                        "type": "horizontalInput",
                                        "templateOptions": {
                                            "label": "Trigger Check Schedule",
                                            "placeholder": "Human readable frequency (eg '10min', '1 day') for how often this harvester is polled",
                                            "required": false
                                        }
                                    },
                                    {
                                        "template": "<hr/>"
                                    },
                                    {
                                        "key": "trigger",
                                        "type": "code_input",
                                        "defaultValue": "{\n}",
                                        "templateOptions": {
                                            "label": "Analytic Thread Trigger JSON",
                                            "codemirror": {
                                                "lineNumbers": true,
                                                "smartIndent": true,
                                                "mode": "javascript"
                                            }
                                        }
                                    }
                                ],
                                "building_function": {
                                    "_fn": "function(errs, template, curr_obj, all_templates, root_obj, hierarchy, rows, cols) { curr_obj.trigger_config = JSON.parse(template.element.form_model.trigger || '{}'); var pf = template.element.form_model.schedule; curr_obj.trigger_config.schedule = (pf || '').length > 0 ? pf : undefined; }"
                                }
                            }
                        },
                        "children": []
                    },
                    {
                        "root": false,
                        "label": "Generic Analytic Job",
                        "element": {
                            "enabled": false,
                            "short_name": "Generic Analytic Job",
                            "summary": "",
                            "row": 0,
                            "col": 1,
                            "sizeX": 1,
                            "sizeY": 1,
                            "expandable": true,
                            "configurable": true,
                            "deletable": true,
                            "form_model": {},
                            "template": {
                                "display_name": "Generic Analytic Job",
                                "key": "analytic_job",
                                "categories": [
                                    "Generic Processing"
                                ],
                                "filters": [
                                    "Bucket/**"
                                ],
                                "child_filters": [
                                    "analytic_input",
                                    "analytic_output"
                                ],
                                "expandable": true,
                                "form_info": "A streaming or batch analytic job that performs processing on input or stored data. ",
                                "schema": [
                                    {
                                        "key": "_short_name",
                                        "type": "horizontalInput",
                                        "templateOptions": {
                                            "label": "Unique Job Name",
                                            "pattern": "[a-zA-Z0-9_]+",
                                            "placeholder": "A Short Name For This Element (Alphanumeric/_ only, no spaces - used for dependencies etc)",
                                            "required": true
                                        }
                                    },
                                    {
                                        "key": "analytic_type",
                                        "type": "horizontalSelect",
                                        "templateOptions": {
                                            "required": true,
                                            "label": "Analytic Type",
                                            "options": [
                                                {
                                                    "name": "Batch",
                                                    "value": "batch"
                                                },
                                                {
                                                    "name": "Streaming",
                                                    "value": "streaming"
                                                }
                                            ]
                                        }
                                    },
                                    {
                                        "key": "dependencies",
                                        "type": "horizontalInput",
                                        "hideExpression": "model.analytic_type !== \"batch\"",
                                        "templateOptions": {
                                            "label": "Dependencies",
                                            "placeholder": "A comma-separated list of dependencies on other analytic jobs",
                                            "required": false
                                        }
                                    },
                                    {
                                        "template": "<hr/>"
                                    },
                                    {
                                        "key": "analytic_technology_name_or_id",
                                        "type": "horizontalInput",
                                        "templateOptions": {
                                            "label": "Technology Library Name",
                                            "placeholder": "The path to the technology, eg /app/aleph2/library/analytic_technology.jar",
                                            "required": true
                                        }
                                    },
                                    {
                                        "template": "<hr/>"
                                    },
                                    {
                                        "key": "show_advanced",
                                        "type": "checkbox",
                                        "templateOptions": {
                                            "label": "Show Advanced Options"
                                        }
                                    },
                                    {
                                        "key": "module_name_or_id",
                                        "type": "horizontalInput",
                                        "hideExpression": "!model.show_advanced",
                                        "templateOptions": {
                                            "label": "Technology Module Name",
                                            "placeholder": "For analytic technologies with pluggable modules, the path to the module, eg /app/aleph2/library/harvester_module.jar",
                                            "required": false
                                        }
                                    },
                                    {
                                        "key": "entry_point",
                                        "type": "horizontalInput",
                                        "hideExpression": "!model.show_advanced",
                                        "templateOptions": {
                                            "label": "Entry Point Override",
                                            "placeholder": "For analytics modules with multiple entry points, specifies the JVM class to execute, eg com.ikanow.aleph2.harvest.module.Module1EntryPoint",
                                            "required": false
                                        }
                                    },
                                    {
                                        "key": "library_names_or_ids",
                                        "type": "multiInput",
                                        "hideExpression": "!model.show_advanced",
                                        "templateOptions": {
                                            "label": "Additional Library Modules",
                                            "inputOptions": {
                                                "type": "input",
                                                "templateOptions": {
                                                    "label": "Technology Module Name",
                                                    "placeholder": "For analytics that need additional library modules, the path to the module, eg /app/aleph2/library/analytic_module_lib.jar",
                                                    "required": false
                                                }
                                            }
                                        }
                                    },
                                    {
                                        "key": "node_list_rules",
                                        "type": "horizontalInput",
                                        "hideExpression": "!model.show_advanced",
                                        "templateOptions": {
                                            "label": "Node rules",
                                            "placeholder": "Comma-separated list of rules determining on which nodes this analytic job will run",
                                            "required": false
                                        }
                                    },
                                    {
                                        "key": "lock_to_nodes",
                                        "type": "horizontalCheckbox",
                                        "defaultValue": false,
                                        "hideExpression": "!model.show_advanced",
                                        "templateOptions": {
                                            "label": "Lock analytic job to the same node/set of nodes (recommend leave as false)",
                                            "required": false
                                        }
                                    },
                                    {
                                        "key": "external_emit_paths",
                                        "type": "horizontalInput",
                                        "hideExpression": "!model.show_advanced",
                                        "templateOptions": {
                                            "label": "Allowed external output paths",
                                            "placeholder": "Comma-separated list of paths/globs to which the bucket is allowed to 'externalEmit'",
                                            "required": false
                                        }
                                    },
                                    {
                                        "template": "<hr/>"
                                    },
                                    {
                                        "key": "config",
                                        "type": "code_input",
                                        "defaultValue": "{\n}",
                                        "templateOptions": {
                                            "label": "Analytic Job Configuration JSON",
                                            "codemirror": {
                                                "lineNumbers": true,
                                                "smartIndent": true,
                                                "mode": "javascript"
                                            }
                                        }
                                    }
                                ],
                                "building_function": {
                                    "_fn": "function(errs, template, curr_obj, all_templates, root_obj, hierarchy, rows, cols) { \n  root_obj.external_emit_paths = template.element.form_model.external_emit_paths;\n var new_obj = { inputs: [] };\n  new_obj.name = template.element.short_name; new_obj.lock_to_nodes = template.element.form_model.lock_to_nodes; new_obj.analytic_type = template.element.form_model.analytic_type; \n  if (template.element.form_model.dependencies && (template.element.form_model.dependencies.length > 0)) {\n  \tnew_obj.dependencies = (template.element.form_model.dependencies || '').replace(\"\\\\s*,\\\\s*\", \",\").split(\",\");\n  }\n  new_obj.analytic_technology_name_or_id = template.element.form_model.analytic_technology_name_or_id; \n  new_obj.module_name_or_id = template.element.form_model.module_name_or_id; \n  new_obj.entry_point = template.element.form_model.entry_point; \n  new_obj.library_names_or_ids = template.element.form_model.library_names_or_ids; \n  if (template.element.form_model.node_list_rules && (template.element.form_model.node_list_rules.length > 0)) {\n\t  new_obj.node_list_rules = (template.element.form_model.node_list_rules || '').replace(\"\\\\s*,\\\\s*\", \",\").split(\",\");\n  }\n  new_obj.config = JSON.parse(template.element.form_model.config || '{}'); \n  curr_obj.jobs.push(new_obj);\n  return new_obj;\n}"
                                }
                            }
                        },
                        "children": [
                            {
                                "root": false,
                                "label": "Basic Output",
                                "element": {
                                    "enabled": true,
                                    "short_name": "Basic Output",
                                    "summary": "",
                                    "row": 1,
                                    "col": 0,
                                    "sizeX": 1,
                                    "sizeY": 1,
                                    "expandable": false,
                                    "configurable": true,
                                    "deletable": true,
                                    "form_model": {
                                        "is_transient": "false",
                                        "preserve_existing_data": "false"
                                    },
                                    "template": {
                                        "display_name": "Basic Output",
                                        "categories": [
                                            "Output"
                                        ],
                                        "key": "analytic_output",
                                        "filters": [
                                            "Bucket/**"
                                        ],
                                        "expandable": false,
                                        "schema": [
                                            {
                                                "key": "is_transient",
                                                "type": "horizontalCheckbox",
                                                "defaultValue": "false",
                                                "templateOptions": {
                                                    "label": "Is Transient?",
                                                    "required": false
                                                }
                                            },
                                            {
                                                "key": "preserve_existing_data",
                                                "type": "horizontalCheckbox",
                                                "defaultValue": "false",
                                                "templateOptions": {
                                                    "label": "Preserve existing data?",
                                                    "required": false
                                                }
                                            }
                                        ],
                                        "form_info": "Input element for adding bucket data",
                                        "building_function": {
                                            "_fn": "function (errs, template, curr_obj, all_templates, root_obj, hierarchy, rows, cols) {               \n        var obj = {};\n        obj.is_transient = template.element.form_model.is_transient || false;\n        obj.preserve_existing_data = template.element.form_model.preserve_existing_data || false;        \n        curr_obj.output = obj;\n      }"
                                        }
                                    }
                                },
                                "children": []
                            },
                            {
                                "root": false,
                                "label": "Bucket Input",
                                "element": {
                                    "enabled": false,
                                    "short_name": "Bucket Input",
                                    "summary": "",
                                    "row": 0,
                                    "col": 0,
                                    "sizeX": 1,
                                    "sizeY": 1,
                                    "expandable": false,
                                    "configurable": true,
                                    "deletable": true,
                                    "form_model": {
                                        "data_service": "search_index_service",
                                        "resource_name_or_id": "/some/other/bucket",
                                        "time_max": "2 days",
                                        "time_min": "4 days"
                                    },
                                    "template": {
                                        "display_name": "Bucket Input",
                                        "categories": [
                                            "Input"
                                        ],
                                        "key": "analytic_input",
                                        "filters": [
                                            "Bucket/**"
                                        ],
                                        "expandable": false,
                                        "schema": [
                                            {
                                                "key": "data_service",
                                                "type": "horizontalInput",
                                                "defaultValue": "search_index_service",
                                                "templateOptions": {
                                                    "label": "Data Service",
                                                    "placeholder": "Name of the data service you want to read from (search_index_service, storage_service, document_service.V1DocumentService)",
                                                    "required": true
                                                }
                                            },
                                            {
                                                "key": "resource_name_or_id",
                                                "type": "horizontalInput",
                                                "defaultValue": "/some/other/bucket",
                                                "templateOptions": {
                                                    "label": "Resource Name",
                                                    "placeholder": "Path or ID of the data service you want e.g. bucket full name",
                                                    "required": true
                                                }
                                            },
                                            {
                                                "key": "time_max",
                                                "type": "horizontalInput",
                                                "defaultValue": "2 days",
                                                "templateOptions": {
                                                    "label": "Time Max",
                                                    "placeholder": "Max days to check (?)",
                                                    "required": true
                                                }
                                            },
                                            {
                                                "key": "time_min",
                                                "type": "horizontalInput",
                                                "defaultValue": "4 days",
                                                "templateOptions": {
                                                    "label": "Time Min",
                                                    "placeholder": "Min days to check (?)",
                                                    "required": true
                                                }
                                            }
                                        ],
                                        "form_info": "Input element for adding bucket data",
                                        "building_function": {
                                            "_fn": "function (errs, template, curr_obj, all_templates, root_obj, hierarchy, rows, cols) {               \n        var obj = {};\n        obj.data_service = template.element.form_model.data_service;\n        obj.enabled = true;\n        obj.resource_name_or_id = template.element.form_model.resource_name_or_id;\n        var config_obj = {};\n        config_obj.time_max = template.element.form_model.time_max;\n        config_obj.time_min = template.element.form_model.time_min;\n        obj.config = config_obj;\n        curr_obj.inputs.push(obj);\n      }"
                                        }
                                    }
                                },
                                "children": []
                            },
                            {
                                "root": false,
                                "label": "Raw JSON Analytic Input",
                                "element": {
                                    "enabled": false,
                                    "short_name": "Raw JSON Analytic Input",
                                    "summary": "",
                                    "row": 0,
                                    "col": 1,
                                    "sizeX": 1,
                                    "sizeY": 1,
                                    "expandable": false,
                                    "configurable": true,
                                    "deletable": true,
                                    "form_model": {},
                                    "template": {
                                        "display_name": "Raw JSON Analytic Input",
                                        "key": "analytic_input",
                                        "categories": [
                                            "Input"
                                        ],
                                        "filters": [
                                            "Bucket/**"
                                        ],
                                        "expandable": false,
                                        "form_info": "Empty JSON object used to configure an input for this analytic job\n",
                                        "schema": [
                                            {
                                                "key": "input",
                                                "type": "code_input",
                                                "defaultValue": "{\n}",
                                                "templateOptions": {
                                                    "label": "Analytic Input JSON",
                                                    "codemirror": {
                                                        "lineNumbers": true,
                                                        "smartIndent": true,
                                                        "mode": "javascript"
                                                    }
                                                }
                                            }
                                        ],
                                        "building_function": {
                                            "_fn": "function(errs, template, curr_obj, all_templates, root_obj, hierarchy, rows, cols) { curr_obj.inputs.push(JSON.parse(template.element.form_model.input || '{}')); }"
                                        }
                                    }
                                },
                                "children": []
                            }
                        ]
                    }
                ]
            }
        ]
    }
}
}
