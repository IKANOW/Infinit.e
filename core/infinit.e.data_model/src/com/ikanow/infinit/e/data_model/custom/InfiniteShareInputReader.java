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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import net.sf.jazzlib.GridFSZipFile;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.log4j.Logger;
import org.bson.BSONObject;
import org.bson.types.ObjectId;

import com.ikanow.infinit.e.data_model.api.ApiManager;
import com.ikanow.infinit.e.data_model.custom.InfiniteShareInputFormat.InfiniteShareInputSplit;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbUtil;
import com.ikanow.infinit.e.data_model.store.config.source.SourceFileConfigPojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.utility.GridFSRandomAccessFile;
import com.mongodb.BasicDBObject;

public class InfiniteShareInputReader extends RecordReader<Object, BSONObject> {

	private static Logger _logger = Logger.getLogger(InfiniteShareInputReader.class);
	
	protected SourceFileConfigPojo _fileConfig;
	
	protected BasicDBObject _fieldsToDelete = null;
	
	protected InfiniteShareInputSplit _fileSplit;
	protected InputStream _inStream = null;
	protected Configuration _config;
	
	protected InfiniteFileInputParser _parser;
	
	protected BSONObject _record = null;
	
	protected Date _splitDate;
	protected int _oneUp = 0;
	protected int _debugLimit = Integer.MAX_VALUE;
	
	protected int _currFile = 0;
	protected int _numFiles = 1;
	
	public InfiniteShareInputReader() {
	}
	
	@Override
	public void close() throws IOException {
		if (null != _parser) {
			_parser.close();
		}
		else if (null != _inStream) {
			_inStream.close();
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
		_fileSplit = (InfiniteShareInputSplit) inputSplit;
		_numFiles = 1;

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
			try {
				GridFSRandomAccessFile file = new GridFSRandomAccessFile(MongoDbManager.getSocial().getShareBinary(), _fileSplit.get_fileId());					
				GridFSZipFile zipView = new GridFSZipFile("dummy", file);
				_inStream = zipView.getInputStream(zipView.getEntry(_fileSplit.get_title()));
			}
			catch (FileNotFoundException e) { // probably: this is a spare mapper, and the original mapper has deleted this file using renameAfterParse
				_currFile++;
				if (_currFile < _numFiles) {
					_inStream = null;
					return nextKeyValue();		// (just a lazy way of getting to the next file)		
				}
				else {
					return false; // all done
				}
			}
			
			_splitDate = new Date(_fileSplit.get_time());
			
			// Step 2: get parser
			
			if (null == _fileConfig.type) {
				String filename = _fileSplit.get_title();
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
		String urlPath = "inf://share/" + _fileSplit.get_shareId() + "/" + _fileSplit.get_title();
		
		_record.put(DocumentPojo.sourceUrl_, urlPath);
		_record.put(DocumentPojo.title_, _fileSplit.get_title());
		if (!_record.containsField(DocumentPojo.url_)) {
			_record.put(DocumentPojo.url_, urlPath + "/" + _oneUp + _parser.getCanonicalExtension());
		}
		_record.put(DocumentPojo.modified_, _splitDate);
		_record.put(DocumentPojo.created_, new Date());
		_record.put(DocumentPojo._id_, new ObjectId()); // (makes file "records" look more like other things in the DB)
		
		if (null != _fieldsToDelete) {
			for (String field: _fieldsToDelete.keySet()) {
				MongoDbUtil.removeProperty((BasicDBObject) _record, field);
			}
		}//TOTEST
		
		return true;
	}//TESTED
}
