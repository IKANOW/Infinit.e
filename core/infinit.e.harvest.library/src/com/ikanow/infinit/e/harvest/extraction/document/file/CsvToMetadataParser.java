/*******************************************************************************
 * Copyright 2012, The Infinit.e Open Source Project.
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
package com.ikanow.infinit.e.harvest.extraction.document.file;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import au.com.bytecode.opencsv.CSVParser;

import com.google.gson.JsonObject;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.ikanow.infinit.e.harvest.extraction.document.file.JsonToMetadataParser.ObjectLength;

public class CsvToMetadataParser {

	private int _debugMaxDocs = Integer.MAX_VALUE;
	
	CsvToMetadataParser(int debugMaxDocs) {
		_debugMaxDocs = debugMaxDocs;		
	}
	
	private char _quoteChar = '"';
	private String _sourceName = null;
	
	// Track approximate memory usage
	private ObjectLength _memUsage = new ObjectLength();			
	public long getMemUsage() {
		return _memUsage.memory*12; // 6x for overhead, 2x for string->byte
	}
	
	public List<DocumentPojo> parseDocument(BufferedReader lineReader, SourcePojo source) throws IOException {
		String line;
		List<DocumentPojo> partials = new LinkedList<DocumentPojo>();
		int docs = 0;
		_memUsage.memory = 0;
		_sourceName = source.getFileConfig().XmlSourceName;
		if (null == _sourceName) {
			_sourceName = "";
		}
		
		CSVParser parser = null;
		Object[] indexToField = null;
		// *Automated* parser, else will just grab the line and let subsequent pipeline elements extract the fields
		if ((null != source.getFileConfig()) && ( 
				 ((null != source.getFileConfig().XmlIgnoreValues) && (!source.getFileConfig().XmlIgnoreValues.isEmpty())) ||
				 ((null != source.getFileConfig().XmlRootLevelValues) && (!source.getFileConfig().XmlRootLevelValues.isEmpty()))
			))
		{
			if (null != source.getFileConfig().XmlAttributePrefix) {
				String chars = source.getFileConfig().XmlAttributePrefix;
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
			if ((null != source.getFileConfig().XmlRootLevelValues) && (source.getFileConfig().XmlRootLevelValues.size() > 0)) {
				indexToField = source.getFileConfig().XmlRootLevelValues.toArray();
			}
		}//TESTED
		
		boolean foundHeaderLine = (indexToField != null);
		while ((line = lineReader.readLine()) != null) {
			// Ignore header lines:
			if ((null != source.getFileConfig()) && (null != source.getFileConfig().XmlIgnoreValues)) {
				boolean bMatched = false;
				boolean firstIgnoreField = true; // (first ignore field in list can generate the headers)
				for (String ignore: source.getFileConfig().XmlIgnoreValues) {			
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
			
			DocumentPojo newDoc = new DocumentPojo();
			String primaryKey = null;
			if (null != parser) {
				JsonObject json = new JsonObject();
				try {
					String[] records = parser.parseLine(line);
					for (int i = 0; i < records.length; ++i) {
						String record = records[i];
						if ((record.length() > 0) && (i < indexToField.length)) {
							String fieldName = (String) indexToField[i];
							if ((null != fieldName) && (fieldName.length() > 0)) {
								json.addProperty(fieldName, record);
								if (fieldName.equals(source.getFileConfig().XmlPrimaryKey)) {
									primaryKey = record;
								}
							}
						}
					}
					if ((null != primaryKey) && (null != _sourceName)) {
						newDoc.setUrl(_sourceName + primaryKey);
					}//TESTED
					newDoc.addToMetadata("csv", JsonToMetadataParser.convertJsonObjectToLinkedHashMap(json, _memUsage));					
				}
				catch (Exception e) {} // can just skip over the line and carry on
				
			}//TESTED
			
			newDoc.setFullText(line);
			if (line.length() > 128) {
				newDoc.setDescription(line.substring(0, 128));
			}
			else {
				newDoc.setDescription(line);
			}
			partials.add(newDoc);
			docs++;
			if (docs >= _debugMaxDocs) { // debug mode only, otherwise commit to all docs in this file
				break;
			}
		}
		return partials;
	}
}
