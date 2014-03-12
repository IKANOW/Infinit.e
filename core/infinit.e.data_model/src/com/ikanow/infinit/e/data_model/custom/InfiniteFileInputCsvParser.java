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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

import org.bson.BSONObject;

import au.com.bytecode.opencsv.CSVParser;

import com.ikanow.infinit.e.data_model.store.config.source.SourceFileConfigPojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.mongodb.BasicDBObject;

//(taken from com.ikanow.infinit.e.harvest.extraction.document.file.CsvToMetadataParser)

public class InfiniteFileInputCsvParser implements InfiniteFileInputParser {

	private BufferedReader lineReader = null;
	private CSVParser parser = null;
	private Object[] indexToField = null;
	private char _quoteChar = '"';
	boolean foundHeaderLine = false;
	SourceFileConfigPojo fileConfig = null;
	
	
	@Override
	public InfiniteFileInputParser initialize(InputStream inStream,
			SourceFileConfigPojo fileConfig_) throws IOException {

		fileConfig = fileConfig_;
		lineReader = new BufferedReader(new InputStreamReader(inStream, "UTF-8"));
		
		// *Automated* parser, else will just grab the line and let subsequent pipeline elements extract the fields
		if ((null != fileConfig) && ( 
				 ((null != fileConfig.XmlIgnoreValues) && (!fileConfig.XmlIgnoreValues.isEmpty())) ||
				 ((null != fileConfig.XmlRootLevelValues) && (!fileConfig.XmlRootLevelValues.isEmpty()))
			))
		{
			if (null != fileConfig.XmlAttributePrefix) {
				String chars = fileConfig.XmlAttributePrefix;
				if (1 == chars.length()) {
					parser = new CSVParser(chars.charAt(0));
				}
				else if (2 == chars.length()) {
					parser = new CSVParser(chars.charAt(0), chars.charAt(1));
				}
				else if (chars.length() > 2) {
					parser = new CSVParser(chars.charAt(0), chars.charAt(1), chars.charAt(2));
				}
				if (chars.length() > 1) {
					_quoteChar = chars.charAt(1);
				}
			}
			if (null == parser) {
				parser = new CSVParser();
			}
			if ((null != fileConfig.XmlRootLevelValues) && (fileConfig.XmlRootLevelValues.size() > 0)) {
				indexToField = fileConfig.XmlRootLevelValues.toArray();
			}
		}//TESTED
		foundHeaderLine = (indexToField != null);
				
		return this;
	}

	@Override
	public BSONObject getNextRecord() throws IOException {
		
		String line = null;
		while ((line = lineReader.readLine()) != null) { // (while because may need to skip over records before returning
			// Ignore header lines:
			if ((null != fileConfig) && (null != fileConfig.XmlIgnoreValues)) {
				boolean bMatched = false;
				boolean firstIgnoreField = true; // (first ignore field in list can generate the headers)
				for (String ignore: fileConfig.XmlIgnoreValues) {			
					boolean lineMatches = false;
					if (ignore.charAt(0) == _quoteChar) {
						if (line.charAt(0) == _quoteChar) {
							lineMatches = line.startsWith(ignore);
						}//TESTED (["a","b","c"] and XmlIgnoreFields: [ "\"a" ])							
						else {
							lineMatches = line.startsWith(ignore.substring(1));
						}//TESTED ([a,b,c] vs XmlIgnoreFields: [ "a" ] and [ "\"a" ])							
					}
					else {
						lineMatches = line.startsWith(ignore);
					}//TESTED
					
					if (lineMatches) {
						if (!foundHeaderLine && firstIgnoreField && (null != parser)) {
							if (ignore.charAt(0) != _quoteChar) { // if using quotes then don't pull the char
								line = line.substring(ignore.length());
							}//TESTED (["a","b","c"] and [a,b,c] vs XmlIgnoreFields: [ "a" ] and [ "\"a" ])							
							String[] fields = parser.parseLine(line);
							// Now override the manual fields:
							indexToField = Arrays.asList(fields).toArray();
							
							if ((indexToField.length > 1) || (0 != ((String)indexToField[0]).length())) {
								foundHeaderLine = true;
							}//TESTED
						}//TESTED
						bMatched = true;
					}
					firstIgnoreField = false;
				}
				if (bMatched) continue;
			}//TESTED
			
			BasicDBObject currObj = new BasicDBObject();
			String primaryKey = null;
			if (null != parser) {
				BasicDBObject json = new BasicDBObject();
				try {
					String[] records = parser.parseLine(line);
					for (int i = 0; i < records.length; ++i) {
						String record = records[i];
						if ((record.length() > 0) && (i < indexToField.length)) {
							String fieldName = (String) indexToField[i];
							if ((null != fieldName) && (fieldName.length() > 0)) {
								json.put(fieldName, record);
								if (fieldName.equals(fileConfig.XmlPrimaryKey)) {
									primaryKey = record;
								}
							}
						}
					}
					if ((null != primaryKey) && (null != fileConfig.XmlSourceName)) {
						currObj.put(DocumentPojo.url_, fileConfig.XmlSourceName + primaryKey);
					}//TESTED
					currObj.put(DocumentPojo.metadata_, new BasicDBObject("csv", Arrays.asList(json)));
				}
				catch (Exception e) {} // can just skip over the line and carry on
				
			}//TESTED
			
			currObj.put(DocumentPojo.fullText_, line);
			if (line.length() > 128) {
				currObj.put(DocumentPojo.description_, line.substring(0, 128));
			}
			else {
				currObj.put(DocumentPojo.description_, line);
			}
			return currObj;
		}
		return null;
	}

	@Override
	public void close() {
		try {
			if (null != lineReader) lineReader.close();
		}
		catch (Exception e) {} // just carry on

	}

	@Override
	public String getCanonicalExtension() {
		return ".csv";
	}

}
