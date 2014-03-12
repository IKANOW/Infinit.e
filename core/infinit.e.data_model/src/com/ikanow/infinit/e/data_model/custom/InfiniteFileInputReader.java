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
import java.util.Date;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.mapreduce.lib.input.CombineFileSplit;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.log4j.Logger;
import org.bson.BSONObject;
import org.bson.types.ObjectId;

import com.ikanow.infinit.e.data_model.api.ApiManager;
import com.ikanow.infinit.e.data_model.store.MongoDbUtil;
import com.ikanow.infinit.e.data_model.store.config.source.SourceFileConfigPojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.mongodb.BasicDBObject;

public class InfiniteFileInputReader extends RecordReader<Object, BSONObject> {

	private static Logger _logger = Logger.getLogger(InfiniteFileInputReader.class);
	
	protected SourceFileConfigPojo _fileConfig;
	
	protected BasicDBObject _fieldsToDelete = null;
	
	protected CombineFileSplit _fileSplit;
	protected InputStream _inStream = null;
	protected FileSystem _fs;
	protected Configuration _config;
	
	protected InfiniteFileInputParser _parser;
	
	protected BSONObject _record = null;
	
	protected Date _splitDate;
	protected int _oneUp = 0;
	protected int _debugLimit = Integer.MAX_VALUE;
	
	protected int _currFile = 0;
	protected int _numFiles = 1;
	
	public InfiniteFileInputReader() {
	}
	
	@Override
	public void close() throws IOException {
		if (null != _parser) {
			_parser.close();
		}
		else if (null != _inStream) {
			_inStream.close();
		}
		if (null != _fs) {
			_fs.close();
		}
	}//TESTED

	@Override
	public Object getCurrentKey() throws IOException, InterruptedException {
		return new ObjectId(_splitDate, _oneUp);
	}

	@Override
	public BSONObject getCurrentValue() throws IOException,
			InterruptedException {
		
		return _record;
	}

	@Override
	public float getProgress() throws IOException, InterruptedException {
		return (float)_currFile/(float)_numFiles;
	}

	@Override
	public void initialize(InputSplit inputSplit, TaskAttemptContext context)
			throws IOException, InterruptedException {
		_config = context.getConfiguration();
		_fileSplit = (CombineFileSplit) inputSplit;
		_numFiles = _fileSplit.getNumPaths();

		String jobName = _config.get("mapred.job.name", "unknown");
		_logger.info(jobName + ": new split, contains " + _numFiles + " files, total size: " + _fileSplit.getLength());		
		
		String sourceStr = _config.get("mongo.input.query");
		SourcePojo source = ApiManager.mapFromApi(sourceStr, SourcePojo.class, null);
		_fileConfig = source.getFileConfig();
		
		String fields = _config.get("mongo.input.fields", "");
		if (fields.length() > 2) {
			try {
				_fieldsToDelete = (BasicDBObject) com.mongodb.util.JSON.parse(fields);
			}
			catch (Exception e) {
				throw new IOException("Invalid fields specification: " + fields);
			} 
		}
		
		_debugLimit = _config.getInt("mongo.input.limit", Integer.MAX_VALUE);
		if (_debugLimit <= 0) { // (just not set)
			_debugLimit = Integer.MAX_VALUE;
		}
	}//TESTED

	@Override
	public boolean nextKeyValue() throws IOException, InterruptedException {
		_oneUp++;
		if (_oneUp > _debugLimit) {
			return false;
		}
		if (null == _inStream) {
			
			// Step 1: get input stream
			_fs = FileSystem.get(_config);
			_inStream = _fs.open(_fileSplit.getPath(_currFile));
			
			_splitDate = new Date(_fs.getFileStatus(_fileSplit.getPath(_currFile)).getModificationTime());
			
			// Step 2: get parser
			
			if (null == _fileConfig.type) {
				String filename = _fileSplit.getPath(_currFile).toString();
				if (filename.endsWith(".xml")) {
					_parser = new InfiniteFileInputXmlParser().initialize(_inStream, _fileConfig);					
				}
				else if (filename.endsWith(".json")) {
					_parser = new InfiniteFileInputJsonParser().initialize(_inStream, _fileConfig);					
				}
				else if (filename.endsWith("sv")) {
					_parser = new InfiniteFileInputCsvParser().initialize(_inStream, _fileConfig);					
				}
				else {
					throw new RuntimeException("Currently only support XML, JSON, *sv");
				}
			}
			else if (_fileConfig.type.equalsIgnoreCase("xml")) {
				_parser = new InfiniteFileInputXmlParser().initialize(_inStream, _fileConfig);
			}
			else if (_fileConfig.type.equalsIgnoreCase("json")) {
				_parser = new InfiniteFileInputJsonParser().initialize(_inStream, _fileConfig);				
			}
			else if (_fileConfig.type.endsWith("sv")) {
				_parser = new InfiniteFileInputCsvParser().initialize(_inStream, _fileConfig);				
			}
			else {
				throw new RuntimeException("Currently only support XML, JSON, *sv");
			}
		}//TESTED
		_record = _parser.getNextRecord();
		if (null == _record) { // Finished this file - are there any others?
			_currFile++;
			if (_currFile < _numFiles) {
				_parser.close(); // (closes everything down)
				_inStream = null;
				return nextKeyValue();				
			}
			else {
				return false; // all done
			}
		}//TESTED
		
		// Tidy up a few things:
		_record.put(DocumentPojo.sourceUrl_, _fileSplit.getPath(_currFile).toUri().toString());
		_record.put(DocumentPojo.title_, _fileSplit.getPath(_currFile).getName());
		if (!_record.containsField(DocumentPojo.url_)) {
			_record.put(DocumentPojo.url_, _fileSplit.getPath(_currFile).toUri() + "/" + _oneUp + _parser.getCanonicalExtension());
		}
		_record.put(DocumentPojo.modified_, _splitDate);
		_record.put(DocumentPojo.created_, new Date());
		
		if (null != _fieldsToDelete) {
			for (String field: _fieldsToDelete.keySet()) {
				MongoDbUtil.removeProperty((BasicDBObject) _record, field);
			}
		}//TOTEST
		
		return true;
	}//TESTED

}
