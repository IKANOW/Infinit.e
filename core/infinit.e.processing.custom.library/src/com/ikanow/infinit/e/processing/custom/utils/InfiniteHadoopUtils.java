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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.bson.types.ObjectId;
import org.xml.sax.SAXException;

import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.custom.mapreduce.CustomMapReduceJobPojo;
import com.ikanow.infinit.e.data_model.store.social.person.PersonCommunityPojo;
import com.ikanow.infinit.e.data_model.store.social.person.PersonPojo;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo.ShareCommunityPojo;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFSDBFile;

public class InfiniteHadoopUtils {

	final public static long MS_IN_DAY = 86400000;
	final public static long SECONDS_60 = 60000;
	
	/**
	 * Takes the query argument from a CustomMapReduceJobPojo
	 * and returns either the query or post processing part
	 * 
	 * @param query
	 * @param wantQuery
	 * @return
	 */
	public enum QuerySpec { QUERY, POSTPROC, INPUTFIELDS };
	
	public static String getQueryOrProcessing(String query, QuerySpec querySpec)
	{
		if ( query.equals("") || query.equals("null") || query == null )
			query = "{}";
		DBObject dbo = (DBObject) com.mongodb.util.JSON.parse(query);
		try
		{
			BasicDBList dbl = (BasicDBList)dbo;
			//is a list
			if ( querySpec == QuerySpec.QUERY )
			{
				return dbl.get(0).toString();
			}
			else if ( querySpec == QuerySpec.POSTPROC )
			{
				if ( dbl.size() > 1 ) {
					if (null == dbl.get(1)) // (only query and fields are specified) 
						return null;
					else
						return dbl.get(1).toString();
				}
				else
					return null;
			}
			else if ( querySpec == QuerySpec.INPUTFIELDS ) 
			{
				if ( dbl.size() > 2 ) 
					return dbl.get(2).toString();
				else
					return null;
			}
			else
				return null;
		}
		catch (Exception ex)
		{
			try
			{
				//is just a an object
				if ( querySpec == QuerySpec.QUERY )
					return dbo.toString();
				else if ( querySpec == QuerySpec.INPUTFIELDS )
					return ((BasicDBObject) dbo.get("$fields")).toString();
				else if ( querySpec == QuerySpec.POSTPROC )
					return ((BasicDBObject) dbo.get("$output")).toString();
				else 
					return null;
			}
			catch (Exception e) // (not sure how we can get to here)
			{
				if ( querySpec == QuerySpec.QUERY )
					return "{}";
				else
					return null;
			}
		}
	}
	
	/**
	 * Downloads jar file from web using URL call.  Typically
	 * the jar files we be kept in our /share store so we will
	 * be calling our own api.
	 * 
	 * @param jarURL
	 * @return
	 * @throws Exception 
	 */
	public static String downloadJarFile(String jarURL, List<ObjectId> communityIds, PropertiesManager prop_custom) throws Exception
	{		
		String shareStringOLD = "$infinite/share/get/";
		String shareStringNEW = "$infinite/social/share/get/";
		String tempFileName = assignNewJarLocation(prop_custom);
		OutputStream out = new BufferedOutputStream(new FileOutputStream(tempFileName));
		if ( jarURL.startsWith(shareStringOLD) || jarURL.startsWith(shareStringNEW))
		{
			//jar is local use id to grab jar (skips authentication)
			String shareid = null;
			if ( jarURL.startsWith(shareStringOLD) )
			{
				shareid = jarURL.substring(shareStringOLD.length());
			}
			else
			{
				shareid = jarURL.substring(shareStringNEW.length());
			}
			BasicDBObject query = new BasicDBObject(SharePojo._id_, new ObjectId(shareid));
			query.put(ShareCommunityPojo.shareQuery_id_, new BasicDBObject(MongoDbManager.in_, communityIds));

			SharePojo share = SharePojo.fromDb(DbManager.getSocial().getShare().findOne(query),SharePojo.class);
			if (null == share) {
				if (null != out) {
					out.close();
				}
				throw new RuntimeException("Can't find JAR file or insufficient permissions");
			}
			if ( share.getBinaryId() != null )
			{			
				GridFSDBFile file = DbManager.getSocial().getShareBinary().find(share.getBinaryId());						
				file.writeTo(out);				
			}
			else
			{
				out.write(share.getBinaryData());
			}
		}
		else
		{
			if (jarURL.startsWith("$infinite")) {
				jarURL = jarURL.replace("$infinite", "http://localhost:8080");
			}
			else if (jarURL.startsWith("file://")) {
				// Can't access the file system, except for this one nominated file:
				if (!jarURL.equals("file:///opt/infinite-home/lib/plugins/infinit.e.hadoop.prototyping_engine.jar")) {
					if (null != out) {
						out.close();
					}
					throw new RuntimeException("Can't find JAR file or insufficient permissions");
				}
			}
			
			//download jar from external site
			URL url = new URL(jarURL);
			
			URLConnection ucon = url.openConnection();
			InputStream in = ucon.getInputStream();
			byte[] buf = new byte[1024];
			int byteRead = 0;
			while ((byteRead = in.read(buf)) != -1 )
			{
				out.write(buf,0,byteRead);				
			}
			in.close();
		}
		out.close();
		return tempFileName;
	}


	/**
	 * Gets a user's communities from his user id
	 * 
	 * @param submitterId
	 * @return
	 * @throws Exception 
	 */
	public static List<ObjectId> getUserCommunities(ObjectId submitterId) {
		// Set up the query
		PersonPojo personQuery = new PersonPojo();
		personQuery.set_id(submitterId);
		
		BasicDBObject dbo = (BasicDBObject) DbManager.getSocial().getPerson().findOne(personQuery.toDb());
		PersonPojo person = PersonPojo.fromDb(dbo, PersonPojo.class);
		
		if (null == person) {
			throw new RuntimeException("User no longer exists?");
		}
		if ((null == person.getCommunities()) || person.getCommunities().isEmpty()) {
			throw new RuntimeException("Corrupt user, no community access?");
		}
		ArrayList<ObjectId> retVal = new ArrayList<ObjectId>(person.getCommunities().size());
		for (PersonCommunityPojo personInfo: person.getCommunities()) {
			retVal.add(personInfo.get_id());
		}
		return retVal;
	}

	/**
	 * Returns a new xml file name following the format
	 * tempConfigXXXX.xml where XXXX is the next incrementing
	 * number in the directory.
	 * 
	 * @return a unique filename for the config file.
	 */
	public static String assignNewConfigLocation(PropertiesManager prop_custom) 
	{		
		String dirname = prop_custom.getHadoopConfigPath() + "/xmlFiles/";
		File dir = new File(dirname);
		if ( !dir.exists() )
			dir.mkdir();
		String prefix = "tempConfig";
		String suffix = ".xml";
		
		String lastFile = "tempConfig000000.xml";
		String[] filenames = dir.list();
		if ( filenames.length > 0 )
			 lastFile = filenames[filenames.length-1];
		String increment = lastFile.replaceFirst(prefix, "");
		increment = increment.replaceFirst(suffix, "");
		//add 1 to increment, and add leading 0's to keep in order
		String nextNumber = (Integer.parseInt(increment) + 1) + "";
		String zeros = "000000" + nextNumber;
		String newincrement = zeros.substring(zeros.length()-6);
				
		return dirname + prefix + newincrement + suffix;
	}
	
	/**
	 * Returns a new jar file name following the format
	 * tempJarXXXX.jar where XXXX is the next incrementing
	 * number in the directory.
	 * 
	 * @return a unique filename for the jar file.
	 */
	public static String assignNewJarLocation(PropertiesManager prop_custom) 
	{		
		String dirname = prop_custom.getHadoopConfigPath() + "/jars/";
		File dir = new File(dirname);
		if ( !dir.exists() )
			dir.mkdir();
		String prefix = "tempJar";
		String suffix = ".jar";
		
		String lastFile = "tempJar000000.jar";
		String[] filenames = dir.list();
		if ( filenames.length > 0 )
			 lastFile = filenames[filenames.length-1];
		String increment = lastFile.replaceFirst(prefix, "");
		increment = increment.replaceFirst(suffix, "");
		//add 1 to increment, and add leading 0's to keep in order
		String nextNumber = (Integer.parseInt(increment) + 1) + "";
		String zeros = "000000" + nextNumber;
		String newincrement = zeros.substring(zeros.length()-6);
				
		return dirname + prefix + newincrement + suffix;
	}
	/**
	 * Removes the config file that is not being used anymore.
	 * 
	 * @param file
	 */
	public static void removeTempFile(String file)
	{
		if ( file != null )
		{
			File f = new File(file);
			f.delete();
		}
	}
	
	/**
	 * Exception message generation
	 * 
	 */
	public static StringBuffer createExceptionMessage(Exception e) {
		return createExceptionMessage(null, e);
	}
	public static StringBuffer createExceptionMessage(String prefix, Exception e) {
		StackTraceElement[] st = e.getStackTrace();
		StringBuffer errMessage = new StringBuffer();
		if (null != prefix) {
			errMessage.append(prefix).append(':');
		}
		errMessage.append((e.getMessage()==null?"NullPointerException":e.getMessage())).append(':');
		if (st.length > 0) {
			errMessage.append(st[0].getClassName()).append('.').append(st[0].getMethodName()).append(':').append(st[0].getLineNumber());
		}						
		return errMessage;
	}
	
	/**
	 * Calls the XML Parser to grab the job client address and opens a connection to
	 * the server.  The parameters must be in the hadoopconfig/mapred-site.xml file
	 * under the property "mapred.job.tracker"
	 * 
	 * @return Connection to the job client
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 */
	public static InetSocketAddress getJobClientConnection(PropertiesManager prop_custom) throws SAXException, IOException, ParserConfigurationException
	{
		String jobclientAddress = HadoopUtils.getXMLProperty(prop_custom.getHadoopConfigPath() + "/hadoop/mapred-site.xml", "mapred.job.tracker");
		String[] parts = jobclientAddress.split(":");
		String hostname = parts[0];
		int port = Integer.parseInt(parts[1]);		
		return new InetSocketAddress(hostname, port);
	}
	
	/////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////
	
	// Some HDFS utilities
	
	public static Path ensureOutputDirectory(CustomMapReduceJobPojo cmr, PropertiesManager prop_custom) throws IOException, SAXException, ParserConfigurationException {
		Configuration config = HadoopUtils.getConfiguration(prop_custom);
		Path path = HadoopUtils.getPathForJob(cmr, config, true);
		
		FileSystem fs = FileSystem.get(config);
		if (fs.exists(path)) { // delete it
			fs.delete(path, true); // (might be dir => recursive)
		}
		// (don't create the dir, this all gets sorted out by the reducer)
		return path;
	}
	
	public static void bringTempOutputToFront(CustomMapReduceJobPojo cmr, PropertiesManager prop_custom) throws IOException, SAXException, ParserConfigurationException {
		// Get the names:
		Configuration config = HadoopUtils.getConfiguration(prop_custom);
		FileSystem fs = FileSystem.get(config);
		Path pathTmp = HadoopUtils.getPathForJob(cmr, config, true);
		Path pathFinal = HadoopUtils.getPathForJob(cmr, config, false);
		
		// OK don't do anything if pathTmp doesn't exist...
		if (fs.exists(pathTmp)) {
			// If the final path exists, delete it
			
			if (!fs.exists(pathFinal)) { // create it, which guarantees the parent path also exists
				//(otherwise the rename fails sigh)
				fs.mkdirs(pathFinal);
			}
			fs.delete(pathFinal, true);
			fs.rename(pathTmp, pathFinal);
		}
	}
}
