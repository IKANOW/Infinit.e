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
		
		"data_bucket": {
			"description": "This object generates a V2 data bucket if the V2 migration plugin is installed.\n Note created/modified/_id/display_name/tags/owner_id/access_rights are taken from the parent source.",
			"extractType": "V2DataBucket",
			"isPublic": true,
			"mediaType": "Record",
			"title": "Template V2 data bucket",
			"processingPipeline": [
			                       {
			                    	   "display": "Currently this is the only supported element in the pipeline (TODO: enable other elements to be added, and aggregate them into a single bucket)owner_id",
			                    	   "data_bucket": {
			                    		   "full_name": "/bucket/path/here",
			                    		   "multi_node_enabled": false,
			                    		   "node_list_rules": [],
			                    		   "aliases": [],
			                    		   "test_params": {
			                    			   "max_run_time_secs": 60
			                    		   },
			                    		   "harvest_technology_name_or_id": "/app/aleph2/library/import/harvest/tech/XXX",
			                    		   "harvest_configs": 
			                    			   [
                		                       {
                		                    	   "name": "harvester_1",
                		                    	   "enabled": false,
                		                    	   "library_ids_or_names": [],
                		                    	   "config": {
                		                    		   "key1": "value1"
                		                    	   }
                		                       }
                		                       ],
			                    		   "master_enrichment_type": "streaming",
			                    		   "streaming_enrichment_topology":
		                    			  {
	         		                    	   "name": "streaming_topology_1",
	         		                    	   "dependencies": [],
	         		                    	   "enabled": false,
	         		                    	   "library_ids_or_names": [],
	         		                    	   "config": {
	         		                    		   "key1": "value1"
	         		                    	   } 
		                    			  },
			                    		   "batch_enrichment_configs": 
			                    			  [
                		                       {
                		                    	   "name": "batch_module_1",
                		                    	   "dependencies": [],
                		                    	   "enabled": false,
                		                    	   "library_ids_or_names": [],
                		                    	   "config": {
                		                    		   "key1": "value1"
                		                    	   }                		                    	   
                		                       }
                		                    ],
			                    		   "data_schema": {
			                    			   "storage_schema": {
			                    				   "enabled": true,
			                    				   "json_grouping_time_period": "week"
			                    			   },
			                    			   "search_index_schema": {
			                    				   "enabled": true,
			                    				   "technology_override_schema": {}
			                    			   },
			                    			   "columnar_schema": {
			                    				   "enabled": true,
			                    				   "field_include_list": [],
			                    				   "field_exclude_list": [],
			                    				   "field_include_pattern_list": [],
			                    				   "field_exclude_pattern_list": [],
			                    				   "field_type_include_list": [],
			                    				   "field_type_exclude_list": [],
			                    				   "technology_override_schema": {}
			                    			   },
			                    			   "temporal_schema": {
			                    				   "enabled": true,
			                    				   "grouping_time_period": "day",
			                    				   "technology_override_schema": {}
			                    			   },
			                    		   },
			                    		   "poll_frequency": "daily",
			                    		   "misc_properties": {}
			                    	   }
			                       }
			                       ]
		}
}
