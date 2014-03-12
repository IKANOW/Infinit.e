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
import java.util.List;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.CombineFileInputFormat;
import org.bson.BSONObject;

import com.ikanow.infinit.e.data_model.api.ApiManager;
import com.ikanow.infinit.e.data_model.store.config.source.SourceFileConfigPojo;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;

public class InfiniteFileInputFormat extends CombineFileInputFormat<Object, BSONObject> {

    @Override
    protected boolean isSplitable(JobContext context, Path filename) {
            return false;
    }

	@Override
	public List<InputSplit> getSplits(JobContext job) throws IOException {
		List<InputSplit> splits = super.getSplits(job);
		
		return splits;
	}

	
	@Override
	public RecordReader<Object, BSONObject> createRecordReader(InputSplit inputSplit, TaskAttemptContext context) throws IOException 
	{
		InfiniteFileInputReader reader = new InfiniteFileInputReader();
		try {
			reader.initialize(inputSplit, context);
		} 
		catch (InterruptedException e) {
			throw new IOException(e);
		}
		return reader;
	}		

	// Implement the regex filter
	
	static public void setInfiniteInputPathFilter(Job job, Configuration config) {
		
		String sourceStr = config.get("mongo.input.query");
		SourcePojo source = ApiManager.mapFromApi(sourceStr, SourcePojo.class, null);
		SourceFileConfigPojo fileConfig = source.getFileConfig();
		if ((null != fileConfig) && 
				((null != fileConfig.pathInclude) || (null != fileConfig.pathExclude)))
		{
			Pattern includeRegex = null;
			Pattern excludeRegex = null;
			if (null != source.getFileConfig().pathInclude) {
				includeRegex = Pattern.compile(source.getFileConfig().pathInclude, Pattern.CASE_INSENSITIVE);
			}
			if (null != source.getFileConfig().pathExclude) {
				excludeRegex = Pattern.compile(source.getFileConfig().pathExclude, Pattern.CASE_INSENSITIVE);
			}
			InfiniteFilePathFilter.initialize(includeRegex, excludeRegex);
			setInputPathFilter(job, InfiniteFilePathFilter.class);
		}			
	}//TESTED
	
	static public class InfiniteFilePathFilter implements PathFilter {
		private static Pattern _includeRegex;
		private static Pattern _excludeRegex;
		public static void initialize(Pattern includeRegex, Pattern excludeRegex) {
			_includeRegex = includeRegex;
			_excludeRegex = excludeRegex;
		}
	    @Override
		public boolean accept(Path path) {
	    	String pathName = path.toString();
			if (null != _includeRegex) {
				if (!_includeRegex.matcher(pathName).matches()) {
					return false;
				}
			}
			if (null != _excludeRegex) {
				if (_excludeRegex.matcher(pathName).matches()) {
					return false;
				}							
			}//TESTED
	    	return true;
	    }		
	}//TESTED
	
}
