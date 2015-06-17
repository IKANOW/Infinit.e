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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.bson.types.ObjectId;
import org.elasticsearch.common.joda.time.Interval;
import org.xml.sax.SAXException;

import com.ikanow.infinit.e.api.knowledge.QueryHandler;
import com.ikanow.infinit.e.data_model.Globals;
import com.ikanow.infinit.e.data_model.api.knowledge.AdvancedQueryPojo;
import com.ikanow.infinit.e.data_model.custom.ICustomInfiniteInternalEngine;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.config.source.SourcePojo;
import com.ikanow.infinit.e.data_model.store.custom.mapreduce.CustomMapReduceJobPojo;
import com.ikanow.infinit.e.data_model.store.social.person.PersonCommunityPojo;
import com.ikanow.infinit.e.data_model.store.social.person.PersonPojo;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo.ShareCommunityPojo;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFSDBFile;

public class InfiniteHadoopUtils {

	final public static long MS_IN_DAY = 86400000;
	final public static long SECONDS_60 = 60000;
	
	public static final String BUILT_IN_JOB_PATH = "file:///opt/infinite-home/lib/plugins/infinit.e.hadoop.prototyping_engine.jar"; 
	private static final String BUILT_IN_JOB_NAME = "infinit.e.hadoop.prototyping_engine.jar";

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
		DBObject dbo = null;
		try
		{
			dbo = (DBObject) com.mongodb.util.JSON.parse(query);
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
			catch (Exception e) // (malformed query gets you here)
			{
				if ( querySpec == QuerySpec.QUERY )
					throw new RuntimeException("Malformed query: " + query);
				else
					return null;
			}
		}
	}
	
	public static Date dateStringFromObject(Object o, boolean minNotMax) {
		if (null == o) {
			return null;
		}
		else if (o instanceof Long) {
			return new Date((Long)o);
		}
		else if (o instanceof Integer) {
			return new Date((long)(int)(Integer)o);
		}
		else if (o instanceof Date) {
			return (Date)o;
		}
		else if (o instanceof DBObject) {
			o = ((DBObject) o).get("$date");
		}
		if (o instanceof String) {
			AdvancedQueryPojo.QueryTermPojo.TimeTermPojo time = new AdvancedQueryPojo.QueryTermPojo.TimeTermPojo();
			if (minNotMax) {
				time.min = (String) o;
				Interval i = QueryHandler.parseMinMaxDates(time, 0L, new Date().getTime(), false);
				return i.getStart().toDate();
			}
			else {
				time.max = (String) o;
				Interval i = QueryHandler.parseMinMaxDates(time, 0L, new Date().getTime(), false);
				return i.getEnd().toDate();				
			}
		}
		else {
			return null;
		}
	}//TESTED: relative string. string, $date obj, number - parse failure + success
	
	public static BasicDBObject createDateRange(Date min, Date max, boolean timeNotOid) {
		BasicDBObject toFrom = new BasicDBObject();
		if (null != min) {
			if (timeNotOid) {
				toFrom.put(DbManager.gte_, min.getTime());
			}
			else {
				toFrom.put(DbManager.gte_, new ObjectId(min));
				
			}
		}
		if (null != max) {
			if (timeNotOid) {
				toFrom.put(DbManager.lte_, max.getTime());
			}
			else {
				toFrom.put(DbManager.lte_, new ObjectId(max));				
			}
		}
		return toFrom;
	}//TESTED (min/max, _id/time)
	
	/**
	 * Downloads jar file from web using URL call.  Typically
	 * the jar files we be kept in our /share store so we will
	 * be calling our own api.
	 * 
	 * @param jarURL
	 * @return
	 * @throws Exception 
	 */
	public static String downloadJarFile(String jarURL, List<ObjectId> communityIds, PropertiesManager prop_custom, ObjectId submitterId) throws Exception
	{		
		String shareStringOLD = "$infinite/share/get/";
		String shareStringNEW = "$infinite/social/share/get/";
		//jar is local use id to grab jar (skips authentication)
		String shareid = null;
		try {
			new ObjectId(jarURL);
			shareid = jarURL;
		}
		catch (Exception e) {} // that's fine it's just not a raw ObjectId

		if ( jarURL.startsWith(shareStringOLD) || jarURL.startsWith(shareStringNEW) || (null != shareid) )
		{
			if (null == shareid) {
				if ( jarURL.startsWith(shareStringOLD) )
				{
					shareid = jarURL.substring(shareStringOLD.length());
				}
				else
				{
					shareid = jarURL.substring(shareStringNEW.length());
				}
			}
			BasicDBObject query = new BasicDBObject(SharePojo._id_, new ObjectId(shareid));
			query.put(ShareCommunityPojo.shareQuery_id_, new BasicDBObject(MongoDbManager.in_, communityIds));

			SharePojo share = SharePojo.fromDb(DbManager.getSocial().getShare().findOne(query),SharePojo.class);
			
			if (null == share) {
				throw new RuntimeException("Can't find JAR file or share or custom table or source, or insufficient permissions: " + shareid);
			}
			
			// The JAR owner needs to be an admin:
			//TODO (INF-2118): At some point would like there to be a choice ... if not admin then must inherit the Infinit.e sandbox version
			// ... there seemed to be some issues with that however so for now will just allow all admin jars and no non-admin jars
			// (see other INF-2118 branch)
			if (prop_custom.getHarvestSecurity()) {
				if (!AuthUtils.isAdmin(share.getOwner().get_id())) {
					throw new RuntimeException("Permissions error: only administrators can upload custom JARs");
				}
			}//TESTED (by hand)
						
			String extension = ".cache";
			if ((null != share.getMediaType()) && (share.getMediaType().contains("java-archive"))) {
				extension = ".cache.jar";
			}
			else if ((null != share.getMediaType()) && (share.getMediaType().contains("gzip"))) {
				extension = ".cache.tgz";
			}
			else if ((null != share.getMediaType()) && (share.getMediaType().contains("zip"))) {
				extension = ".cache.zip";
			}
			
			//TODO (IN-461): handle reference shares (it's easy, see else clause below)			
			
			String tempFileName = assignNewJarLocation(prop_custom, shareid + extension);
			File tempFile = new File(tempFileName);
			
			// Compare dates (if it exists) to see if we need to update the cache) 
			
			if (!tempFile.exists() || (tempFile.lastModified() < share.getModified().getTime())) {
				if (null != share.getDocumentLocation()) {
					tempFileName = share.getDocumentLocation().getCollection(); // ie just return the pointer					
					if (!(new File(tempFileName).exists())) { // (this is really only when debugging)
						// Try looking in temp path
						tempFileName = tempFileName.substring(1 + tempFileName.lastIndexOf('/'));
						tempFileName = assignNewJarLocation(prop_custom, tempFileName);
					}
				}//TESTED
				else {
					OutputStream out = new BufferedOutputStream(new FileOutputStream(tempFileName));
					if ( share.getBinaryId() != null )
					{			
						GridFSDBFile file = DbManager.getSocial().getShareBinary().find(share.getBinaryId());						
						file.writeTo(out);				
					}
					else
					{
						out.write(share.getBinaryData());
					}
					out.flush();
					out.close();
				}
			}//TESTED			
			
			return tempFileName;
		}
		else // Not an infinit.e share - either a local file or served externally
		{
			if (jarURL.startsWith("$infinite")) { // Local web server
				if (prop_custom.getHarvestSecurity()) {
					if (!AuthUtils.isAdmin(submitterId)) {
						throw new RuntimeException("Permissions error: only administrators can run custom JARs served from a web server (users can run custom JARs when uploaded by an admin to the share store)");
					}
				}//TOTEST				
				jarURL = jarURL.replace("$infinite", "http://localhost:8080");
			}//TESTED (by hand) 
			else if (!jarURL.startsWith("http")) {
				// Can't access the file system, except for this one nominated file:
				if (!jarURL.equals(BUILT_IN_JOB_PATH)) {
					throw new RuntimeException("Can't find JAR file or insufficient permissions: " + jarURL);
				}
				jarURL = BUILT_IN_JOB_PATH.substring(7);
				if (!(new File(jarURL).exists())) { // (this is really only when debugging)
					// Try looking in temp path
					jarURL = assignNewJarLocation(prop_custom, BUILT_IN_JOB_NAME);
				}
				return jarURL;
			}//TESTED
			else { // Access a JAR from an external web server, can only do this if admin
				if (prop_custom.getHarvestSecurity()) {
					if (!AuthUtils.isAdmin(submitterId)) {
						throw new RuntimeException("Permissions error: only administrators can run custom JARs served from a web server (users can run custom JARs when uploaded by an admin to the share store)");
					}
				}//TOTEST				
			}//TESTED (by hand)
			
			String tempFileName = assignNewJarLocation(prop_custom, null);
			OutputStream out = new BufferedOutputStream(new FileOutputStream(tempFileName));
			
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
			out.close();
			return tempFileName;
		}//(end share - first clause - or served externally - second clause)
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
	public static String assignNewJarLocation(PropertiesManager prop_custom, String nameOverride) 
	{		
		String dirname = prop_custom.getHadoopConfigPath() + "/jars/";
		File dir = new File(dirname);
		if ( !dir.exists() )
			dir.mkdir();
		
		if (null == nameOverride) {
		
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
		else {
			return dirname + nameOverride;			
		}
	}
	/**
	 * Removes the config file that is not being used anymore.
	 * 
	 * @param file
	 */
	public static void removeTempFile(String file)
	{
		if (file != null) {
			File f = new File(file);
			if (f.getName().startsWith("temp")) {
				f.delete();				
			}
		}
	}
	
	/**
	 * Exception message generation
	 * 
	 */
	public static StringBuffer createExceptionMessage(Throwable e) {
		return createExceptionMessage(null, e);
	}
	public static StringBuffer createExceptionMessage(String prefix, Throwable e) {
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
	
	// Some Share utilities
	
	public static void authenticateShareList(CustomMapReduceJobPojo cmr, String[] path) {
		
		ArrayList<ObjectId> list = new ArrayList<ObjectId>(path.length);
		for (String s: path) {
			list.add(new ObjectId(s));
		}
		BasicDBObject query = new BasicDBObject(SharePojo._id_, new BasicDBObject(DbManager.in_, list));
		query.put(ShareCommunityPojo.shareQuery_id_, new BasicDBObject(DbManager.ne_, new BasicDBObject(DbManager.in_, list)));
		BasicDBObject fields = new BasicDBObject(ShareCommunityPojo.shareQuery_id_, 1);
		DBCursor dbc = DbManager.getSocial().getShare().find(query, fields);
		StringBuffer sb = new StringBuffer();
		for (Object o: dbc) {
			BasicDBObject dbo = (BasicDBObject) o;
			if (0 != sb.length()) {
				sb.append(", ");
				sb.append(dbo.toString());
			}
		}
		if (sb.length() > 0) {
			throw new RuntimeException("Share authentication error: " + sb.toString());
		}
	}//TODO (INF-2865): TOTEST (pass and fail)
	
	/////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////
	
	// Some HDFS utilities
	
	// Ensure the input directory is allowed, exceptions out if not allowed
	
	public static String authenticateInputDirectory(CustomMapReduceJobPojo cmr, String path) {
		if (path.startsWith("hdfs://")) {
			path = path.substring(7);
		}
		if (path.startsWith("hdfs:")) {
			path = path.substring(5);
		}
		if (path.startsWith("/user/tomcat/")) { // the home directory from which everything is run			
			path = path.substring(13);
		}
		if (path.startsWith("/")) {			
			path = path.substring(1);
		}
		String[] pathCheck = path.split("/", 3);
		if (pathCheck.length > 1) {
			for (ObjectId communityId: cmr.communityIds) {	
				if (pathCheck[1].contains(communityId.toString())) {				
					return path;
				}
			}
		}
		throw new RuntimeException("Access to this directory is not authenticated: the second directory level must contain a matching community ID - eg 'completed/50bcd6fffbf0fd0b27875a7c/', 'input/52b1be6145ce02c2c6fbab9e,50bcd6fffbf0fd0b27875a7c/' etc: " + path);
	}
	
	// Create an output directory
	
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
	
	// Cache a local file so can be used in a distributed cache
	
	public static Path cacheLocalFile(String localPath, String localName, Configuration config) throws IOException {
		FileSystem fs = FileSystem.get(config);
		Path toDir = new Path("cache");
		Path destFile = new Path("cache/" + localName);
		File fromFile = new File(localPath + "/" + localName);
		if (!fromFile.exists()) {
			throw new IOException("Source file does not exist: " + fromFile.toString());
		}
		boolean needToCopyFile = true;
		if (!fs.exists(toDir)) { // (ie relative to WD)
			fs.mkdirs(toDir);
		}
		else {
			// Now check if the file already exists
			if (fs.exists(destFile)) {
				FileStatus fsStat = fs.getFileStatus(destFile);
				if ((fsStat.getLen() == fromFile.length())
						&&
						(fromFile.lastModified() <= fsStat.getModificationTime()))
				{
					needToCopyFile = false;
				}
			}
		}
		if (needToCopyFile) {
			fs.copyFromLocalFile(false, true, new Path(localPath + "/" + localName), destFile); 
		}
		return new Path(fs.getFileStatus(destFile).getPath().toUri().getPath());
			// (apparently the path has to be in absolute format without even the hdfs:// at the front?!)
	}//TESTED
	
	// Handle a list of cache files of various types:
	// - shares
	// - files
	// (returns a list of cached JAR files for local mode running - you're on your own for other cached files though)
	
	@SuppressWarnings("rawtypes")
	public static List<URL> handleCacheList(Object cacheFileList, CustomMapReduceJobPojo job, Configuration config, PropertiesManager prop_custom) throws MalformedURLException, IOException, Exception {
		if (null == cacheFileList) {
			return null;
		}
		LinkedList<URL> localJarCache = null;
		
		Collection cacheFiles = null;
		if (cacheFileList instanceof String) { // comma separated list
			String[] cacheFilesArray = ((String) cacheFileList).split("\\s*,\\s*");
			cacheFiles = Arrays.asList(cacheFilesArray);
		}
		else {
			cacheFiles = (Collection) cacheFileList;
		}		
		for (Object cache: cacheFiles) {
			String cacheStr = (String) cache;
			ObjectId cacheId = null;
			try {
				cacheId = new ObjectId(cacheStr);
			}
			catch (Exception e) {} // fine
			
			FileSystem fs = null;
			
			if ((null != cacheId) || cacheStr.startsWith("http:") || cacheStr.startsWith("https:") || cacheStr.startsWith("$")) {
				if (null != cacheId) { // this might be a custom cache in which case just bypass all this, handled in the main list
					if (checkIfSourceOrCustomAndAuthenticate(null, cacheId, job)) {
						continue;
					}
				}//TESTED (by hand = skip and continue)
				
				// Use existing code to cache to local fs (and then onwards to HDFS!)
				URL localPathURL = new File(downloadJarFile(cacheStr, job.communityIds, prop_custom, job.submitterID)).toURI().toURL(); 
				String localPath = localPathURL.getPath();
				String pathMinusName = localPath.substring(0, localPath.lastIndexOf('/') + 1);
				String name = localPath.substring(localPath.lastIndexOf('/') + 1);
				Path distPath = cacheLocalFile(pathMinusName, name, config);
				if (name.endsWith(".jar")) {
					if (null == localJarCache) {
						localJarCache = new LinkedList<URL>();
					}
					localJarCache.add(localPathURL);
					DistributedCache.addFileToClassPath(distPath, config);
				}//TESTED
				else if (name.endsWith(".zip") || name.endsWith("gz")) {
					DistributedCache.addCacheArchive(distPath.toUri(), config);
				}//TESTED
				else {
					DistributedCache.addCacheFile(distPath.toUri(), config);
				}//TESTED
			}
			else { // this is the location of a file (it is almost certainly an input/output path)
				if (checkIfSourceOrCustomAndAuthenticate(cacheStr, null, job)) {
					continue;
				}//TESTED (by hand - seen it skip if not a jobid/sourcekey - currently not possible for it to be one anyway; c/p from checkIfSourceOrCustomAndAuthenticate call in previous call anyway^2)				
				
				String path = authenticateInputDirectory(job, cacheStr);
				if (null == fs) {
					fs = FileSystem.get(config);
				}
				Path distPath = new Path(fs.getFileStatus(new Path(path)).getPath().toUri().getPath());
				if (path.endsWith(".jar")) {
					DistributedCache.addFileToClassPath(distPath, config);
				}//TESTED
				else if (path.endsWith(".zip") || path.endsWith("gz")) {
					DistributedCache.addCacheArchive(distPath.toUri(), config);
				}//TESTED
				else {
					DistributedCache.addCacheFile(distPath.toUri(), config);
				}//TESTED
			}//TESTED
		}		
		return localJarCache;
	}//TOTEST (logically TESTED but needs local testing)
	
	//////////////////////////////////////////////////////////////////////////////////////////////

	private static boolean checkIfSourceOrCustomAndAuthenticate(String cacheObjStr, ObjectId cacheObjectId, CustomMapReduceJobPojo job) {
		// Custom: 
		BasicDBObject query = new BasicDBObject();
		BasicDBObject fields = new BasicDBObject();
		BasicDBObject fields1 = new BasicDBObject(CustomMapReduceJobPojo.communityIds_, 1);
		if (null != cacheObjectId) {
			query.put(CustomMapReduceJobPojo._id_, cacheObjectId);
		}
		else {			
			if (cacheObjStr.startsWith("custom:")) {
				cacheObjStr = cacheObjStr.substring(7);
			}
			query.put(CustomMapReduceJobPojo.jobtitle_, cacheObjStr);
		}
		CustomMapReduceJobPojo targetJob = CustomMapReduceJobPojo.fromDb(DbManager.getCustom().getLookup().findOne(query, fields1), CustomMapReduceJobPojo.class);
		if (null != targetJob) {
			BasicDBObject submitterQuery = new BasicDBObject("_id", job.submitterID);
			submitterQuery.put("communities._id", new BasicDBObject(DbManager.all_, targetJob.communityIds));
			
			// This object exists as a custom object ... check authentication
			if (null == DbManager.getSocial().getPerson().findOne(submitterQuery, fields)) {
				throw new RuntimeException("Custom authentication error: " + query);
			}			
			return true;
		}//TESTED (by hand - matches and doesn't match, single and multiple communities)
		
		// Source
		query = new BasicDBObject();
		if (null != cacheObjectId) {
			query.put(SourcePojo._id_, cacheObjectId);
		}
		else {
			if (cacheObjStr.startsWith("source:")) {
				cacheObjStr = cacheObjStr.substring(7);
			}
			query.put(SourcePojo.key_, cacheObjStr);
		}
		if (null != DbManager.getIngest().getSource().findOne(query, fields)) {
			
			// This object exists as a source object ... check authentication
			// (source auth is simpler - is this source in the new job's data groups?) 
			query.put(SourcePojo.communityIds_, new BasicDBObject(DbManager.in_, job.communityIds));
			
			if (null == DbManager.getIngest().getSource().findOne(query, fields)) {
				throw new RuntimeException("Source authentication error: " + query);
			}
			return true;
		}//TESTED (not working - not in community, working)
		return false;
	}//TESTED (by hand - see above clauses for details)	
	
	//////////////////////////////////////////////////////////////////////////////////////////////
	
	// Supports post-task activities for custom internal engine
	
	public static void handlePostTaskActivities(CustomMapReduceJobPojo cmr, boolean isError, StringBuffer postTaskActivityErrors) {
		if (null == cmr.jarURL) {
			// (it's a saved query so just ignore)
			return;
		}
		
		ClassLoader savedClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			// Get the latest version of this file, if necessary
			List<ObjectId> communityIds = InfiniteHadoopUtils.getUserCommunities(cmr.submitterID);
			InfiniteHadoopUtils.downloadJarFile(cmr.jarURL, communityIds, new PropertiesManager(), cmr.submitterID);
			
			URLClassLoader child = new URLClassLoader (new URL[] { new File(cmr.tempJarLocation).toURI().toURL() }, savedClassLoader);			
			Thread.currentThread().setContextClassLoader(child);
			
			Class<?> mapperClazz = null;
			try {
				mapperClazz = Class.forName (cmr.mapper, true, child);
			}
			catch (Exception e) { return; } 
			catch (Error e) { return; }

			if (ICustomInfiniteInternalEngine.class.isAssignableFrom(mapperClazz)) { // Special case: internal custom engine, so gets an additional integration hook
				ICustomInfiniteInternalEngine postActivities = (ICustomInfiniteInternalEngine) mapperClazz.newInstance();
				String jobName = new StringBuffer(cmr.jobidS).append("_").append(cmr.jobidN).toString();
				postActivities.postTaskActivities(cmr._id, jobName, cmr.communityIds, cmr.arguments, isError, postTaskActivityErrors.toString());
			}
		}
		catch (Exception e) {
			if (postTaskActivityErrors.length() > 0) {
				postTaskActivityErrors.append('\n');
			}
			Globals.populateStackTrace(postTaskActivityErrors, e);
		}
		catch (Error e) {
			if (postTaskActivityErrors.length() > 0) {
				postTaskActivityErrors.append('\n');
			}
			Globals.populateStackTrace(postTaskActivityErrors, e);
		}
		finally {
			Thread.currentThread().setContextClassLoader(savedClassLoader);				
		}		
	}//TESTED
	
}
