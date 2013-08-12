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
import java.util.LinkedList;
import java.util.List;

import au.com.bytecode.opencsv.CSVParser;

import com.google.gson.JsonObject;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;

public class CsvToMetadataParser {

	private int _debugMaxDocs = Integer.MAX_VALUE;
	
	CsvToMetadataParser(int debugMaxDocs) {
		_debugMaxDocs = debugMaxDocs;		
	}
	
	public List<DocumentPojo> parseDocument(BufferedReader lineReader, SourcePojo source) throws IOException {
		String line;
		List<DocumentPojo> partials = new LinkedList<DocumentPojo>();
		int docs = 0;
		
		CSVParser parser = null;
		Object[] indexToField = null;
		if ((null != source.getFileConfig()) && (null != source.getFileConfig().XmlRootLevelValues)
				&& (!source.getFileConfig().XmlRootLevelValues.isEmpty()))
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
			}
			if (null == parser) {
				parser = new CSVParser();
			}
			indexToField = source.getFileConfig().XmlRootLevelValues.toArray();
		}//TESTED
		
		while ((line = lineReader.readLine()) != null) {
			// Ignore header lines:
			if ((null != source.getFileConfig()) && (null != source.getFileConfig().XmlIgnoreValues)) {
				boolean bMatched = false;
				for (String ignore: source.getFileConfig().XmlIgnoreValues) {
					if (line.startsWith(ignore)) {
						bMatched = true;
					}
				}
				if (bMatched) continue;
			}//TESTED
			
			DocumentPojo newDoc = new DocumentPojo();
			String primaryKey = null;
			if (null != parser) {
				JsonObject json = new JsonObject();
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
				if ((null != primaryKey) && (null != source.getFileConfig().XmlSourceName)) {
					newDoc.setUrl(source.getFileConfig().XmlSourceName + primaryKey);
				}//TESTED
				newDoc.addToMetadata("csv", JsonToMetadataParser.convertJsonObjectToLinkedHashMap(json));
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
