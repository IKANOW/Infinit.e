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
package com.ikanow.infinit.e.processing.custom.utils;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Writable;
import org.apache.mahout.clustering.Cluster;
import org.apache.mahout.common.Pair;
import org.apache.mahout.common.iterator.sequencefile.PathFilters;
import org.apache.mahout.common.iterator.sequencefile.PathType;
import org.apache.mahout.common.iterator.sequencefile.SequenceFileDirIterable;
import org.apache.mahout.math.NamedVector;
import org.apache.mahout.math.Vector;
import org.bson.types.ObjectId;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.ikanow.infinit.e.data_model.store.MongoDbUtil;
import com.ikanow.infinit.e.data_model.store.custom.mapreduce.CustomMapReduceJobPojo;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.hadoop.io.BSONWritable;

public class HadoopUtils {

	public static void deleteHadoopDir(CustomMapReduceJobPojo cmr) throws SAXException, IOException, ParserConfigurationException {
		PropertiesManager props = new PropertiesManager();
		Configuration conf = getConfiguration(props);				
		Path pathDir = HadoopUtils.getPathForJob(cmr, conf, false);
		FileSystem fs = FileSystem.get(conf);
		if (fs.exists(pathDir)) {
			fs.delete(pathDir, true);
		}
	}
	
	public static BasicDBList getBsonFromSequenceFile(CustomMapReduceJobPojo cmr, int nLimit, String fields) throws SAXException, IOException, ParserConfigurationException {

		BasicDBList dbl = new BasicDBList();
		
		PropertiesManager props = new PropertiesManager();
		Configuration conf = getConfiguration(props);		
		
		Path pathDir = HadoopUtils.getPathForJob(cmr, conf, false);
		
		@SuppressWarnings({ "unchecked", "rawtypes" })
		SequenceFileDirIterable<? extends Writable, ? extends Writable> seqFileDir = 
			new SequenceFileDirIterable(pathDir, PathType.LIST, PathFilters.logsCRCFilter(), conf);


		// Very basic, only allow top level, 1 level of nesting, and field removal
		HashSet<String> fieldLookup = null;
		if (null != fields) {
			fieldLookup = new HashSet<String>();
			String[] fieldArray = fields.split(","); 
			for (String field: fieldArray) {
				String[] fieldDecomp = field.split(":");
				fieldLookup.add(fieldDecomp[0]);
			}
		}//TOTEST
		
		int nRecords = 0;
		for (Pair<? extends Writable, ? extends Writable> record: seqFileDir) {
			BasicDBObject element = new BasicDBObject();
			
			// KEY
			
			Writable key = record.getFirst();
			if (key instanceof org.apache.hadoop.io.Text) {
				org.apache.hadoop.io.Text writable = (org.apache.hadoop.io.Text)key;
				element.put("key", writable.toString());																
			}
			else if (key instanceof org.apache.hadoop.io.DoubleWritable) {
				org.apache.hadoop.io.DoubleWritable writable = (org.apache.hadoop.io.DoubleWritable)key;
				element.put("key", Double.toString(writable.get()));								
			}
			else if (key instanceof org.apache.hadoop.io.IntWritable) {
				org.apache.hadoop.io.IntWritable writable = (org.apache.hadoop.io.IntWritable)key;
				element.put("key", Integer.toString(writable.get()));				
			}
			else if (key instanceof org.apache.hadoop.io.LongWritable) {
				org.apache.hadoop.io.LongWritable writable = (org.apache.hadoop.io.LongWritable)key;
				element.put("key", Long.toString(writable.get()));
			}
			else if (key instanceof BSONWritable) {
				element.put("key", MongoDbUtil.convert((BSONWritable)key));
			}
			
			// VALUE

			Writable value = record.getSecond();
			if (value instanceof org.apache.hadoop.io.Text) {
				org.apache.hadoop.io.Text writable = (org.apache.hadoop.io.Text)value;
				element.put("value", writable.toString());																
			}
			else if (value instanceof org.apache.hadoop.io.DoubleWritable) {
				org.apache.hadoop.io.DoubleWritable writable = (org.apache.hadoop.io.DoubleWritable)value;
				element.put("value", Double.toString(writable.get()));								
			}
			else if (value instanceof org.apache.hadoop.io.IntWritable) {
				org.apache.hadoop.io.IntWritable writable = (org.apache.hadoop.io.IntWritable)value;
				element.put("value", Integer.toString(writable.get()));				
			}
			else if (value instanceof org.apache.hadoop.io.LongWritable) {
				org.apache.hadoop.io.LongWritable writable = (org.apache.hadoop.io.LongWritable)value;
				element.put("value", Long.toString(writable.get()));
			}
			else if (value instanceof BSONWritable) {
				element.put("value", MongoDbUtil.convert((BSONWritable)value));
			}
			else if (value instanceof org.apache.mahout.math.VectorWritable) {
				Vector vec = ((org.apache.mahout.math.VectorWritable)value).get();
				BasicDBList dbl2 = listFromMahoutVector(vec, "value", element);
				element.put("value", dbl2);					
			}
			else if (value instanceof org.apache.mahout.clustering.classify.WeightedVectorWritable) {
				org.apache.mahout.clustering.classify.WeightedVectorWritable vecW = (org.apache.mahout.clustering.classify.WeightedVectorWritable)value;
				element.put("valueWeight", vecW.getWeight());
				BasicDBList dbl2 = listFromMahoutVector(vecW.getVector(), "value", element);
				element.put("value", dbl2);					
			}
			else if (value instanceof org.apache.mahout.clustering.iterator.ClusterWritable) {
				Cluster cluster = ((org.apache.mahout.clustering.iterator.ClusterWritable)value).getValue();
				BasicDBObject clusterVal = new BasicDBObject();
				clusterVal.put("center", listFromMahoutVector(cluster.getCenter(), "center", clusterVal));
				clusterVal.put("radius", listFromMahoutVector(cluster.getRadius(), "radius", clusterVal));
				element.put("value", clusterVal);					
			}
			else {
				element.put("unknownValue", value.getClass().toString());
			}
			
			// Check the fields settings:
			// Only handle a few...
			if (null != fieldLookup) {
				for (String fieldToRemove: fieldLookup) {
					if (fieldToRemove.startsWith("value.")) {
						fieldToRemove = fieldToRemove.substring(6);
						BasicDBObject nested = (BasicDBObject) element.get("value.");
						if (null != nested) {
							nested.remove(fieldToRemove);
						}
					}
					else {
						element.remove(fieldToRemove);
					}
				}//TOTEST
			}
			
			dbl.add(element);
			nRecords++;
			if ((nLimit > 0) && (nRecords >= nLimit)) {
				break;
			}
		}
		
		return dbl;
	}//TOTEST
	
	private static BasicDBList listFromMahoutVector(Vector vec, String prefix, BasicDBObject element) {
		if (vec instanceof NamedVector) {
			element.put(prefix + "Name", ((NamedVector)vec).getName());
		}
		BasicDBList dbl2 = new BasicDBList();
		if (vec.isDense()) {
			int nSize = vec.size();
			dbl2.ensureCapacity(nSize);
			for (int i = 0; i < nSize; ++i) {
				dbl2.add(vec.getQuick(i));						
			}
		}
		else { // sparse, write as a set in the format [{int:double}]
			Iterator<org.apache.mahout.math.Vector.Element> elIt = vec.iterateNonZero();
			while (elIt.hasNext()) {
				BasicDBObject el2 = new BasicDBObject();
				org.apache.mahout.math.Vector.Element el = elIt.next();
				el2.put("k", el.index());
				el2.put("v", el.get());
				dbl2.add(el2);
			}
		}
		return dbl2;
	}
	
	/**
	 * Returns an HDFS path for the custom task
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 * @throws SAXException 
	 * 
	 */
	public static Path getPathForJob(CustomMapReduceJobPojo cmr, Configuration config, boolean bTemp) throws SAXException, IOException, ParserConfigurationException {
		// Get the name:
		StringBuffer sb = null;
		if (bTemp) {		
			sb = new StringBuffer("in_progress/"); // (will move this after it's complete)
		}
		else {
			sb = new StringBuffer("completed/"); // (final location)			
		}
		for (ObjectId commId: cmr.communityIds) {			
			sb.append(commId.toString()).append('_');
		}
		sb.append('/');
		sb.append(cmr.jobtitle).append('/');
		String pathName = sb.toString();
		
		return new Path(pathName);		
	}//TOTEST
	
	public static Configuration getConfiguration(PropertiesManager prop_custom) throws SAXException, IOException, ParserConfigurationException
	{
		Configuration conf = new Configuration();
		if (prop_custom.getHadoopLocalMode()) {
			conf.set("fs.default.name", "local");							
		}
		else {
			String fsUrl = getXMLProperty(prop_custom.getHadoopConfigPath() + "/hadoop/core-site.xml", "fs.default.name");
			conf.set("fs.default.name", fsUrl);				
		}		
		return conf;
	}//TOTEST
	
	/**
	 * Parses a given xml file and returns the requested value of propertyName.
	 * The XML is expected to be in a format: <configuration><property><name>some.prop.name</name><value>some.value</value></property></configuration>
	 * 
	 * @param xmlFileLocation
	 * @param propertyName
	 * @return
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	public static String getXMLProperty(String xmlFileLocation, String propertyName) throws SAXException, IOException, ParserConfigurationException
	{
		File configFile = new File(xmlFileLocation);
		
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(configFile);        
        doc.getDocumentElement().normalize();
        
        NodeList listOfProps = doc.getElementsByTagName("property");
        
        for ( int i = 0; i < listOfProps.getLength(); i++ )
        {
        	Node prop = listOfProps.item(i);
        	if ( prop.getNodeType() == Node.ELEMENT_NODE)
        	{
	        	Element propElement = (Element)prop;	        	
	        	NodeList name = propElement.getElementsByTagName("name").item(0).getChildNodes();
	        	Node nameValue = (Node) name.item(0);
	        	String nameString = nameValue.getNodeValue().trim();
	        	
	        	//found the correct property
	        	if ( nameString.equals(propertyName) )
	        	{
	        		//return the value
	        		NodeList value = propElement.getElementsByTagName("value").item(0).getChildNodes();
		        	Node valueValue = (Node) value.item(0);
		        	String valueString = valueValue.getNodeValue().trim();		        	
		        	return valueString;		        	
	        	}
        	}
        }
        return null;
	}//TESTED
	
}
