/*******************************************************************************
 * Copyright 2012 The Infinit.e Open Source Project sponsored by IKANOW.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.ikanow.infinit.e.application.handlers.actions;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.elasticsearch.action.admin.cluster.state.ClusterStateResponse;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.restlet.Request;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Options;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ServerResource;

import com.ikanow.infinit.e.application.data_model.DashboardPojo;
import com.ikanow.infinit.e.application.data_model.DashboardProxyResultPojo;
import com.ikanow.infinit.e.application.data_model.DashboardProxySearchResultPojo;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.api.ResponsePojo.ResponseObject;
import com.ikanow.infinit.e.data_model.driver.InfiniteDriver;
import com.ikanow.infinit.e.data_model.index.ElasticSearchManager;
import com.ikanow.infinit.e.data_model.store.social.person.PersonCommunityPojo;
import com.ikanow.infinit.e.data_model.store.social.person.PersonPojo;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo;
import com.ikanow.infinit.e.data_model.utils.PropertiesManager;

public class RecordInterface extends ServerResource {
	static String _esHostUrl = null;
	static PropertiesManager _props = null;
	
	// Per-call transaction state
	String _cookie;
	String _ipAddress;
	String _postData;
	boolean _deleteMode = false;
	
	final static String CONTROL_REF = "infinit.e.records/proxy/"; // (in debug mode, wont' start with infinit.e.records)
	final static String DUMMY_INDEX = "doc_dummy"; // (guaranteed to exist)
	
	String _proxyUrl;
	String _indexOrAdminCommand;
	String _indexCommand;
	
	String _urlParams;	
	Map<String, String> _queryOptions;
	
	InfiniteDriver _driver;
	//___________________________________________________________________________________
	
	// Constructor/main processing 
	
	@Override
	public void doInit() 
	{		
		if (null == _esHostUrl) {
			_props = new PropertiesManager();
			_esHostUrl = _props.getElasticUrl();
			if (_esHostUrl.endsWith(":9300")) { // deployed
				_esHostUrl = _esHostUrl.replace(":9300", ":9200");
			}
			if (_esHostUrl.endsWith("3")) { // debug, ugh
				_esHostUrl = _esHostUrl.substring(0, _esHostUrl.length()-1) + "2";
			}				
		}		
		
		 Request request = this.getRequest();
		
		// Some basic housekeeping
		 _cookie = request.getCookies().getFirstValue("infinitecookie",true);		 
		 _ipAddress =  request.getClientInfo().getAddress();

		 // If we're in here, then we're in a query call, we don't support any others...
		 Map<String,Object> attributes = request.getAttributes();	
		 _proxyUrl = getRequest().getOriginalRef().toUri().getPath();
		 _indexOrAdminCommand = (String) attributes.get("proxyterms");
		 int length = CONTROL_REF.length() + _indexOrAdminCommand.length() + 1; // +1 for the trailing /
		 if (!_proxyUrl.startsWith("/infinit.e.records/")) { //deployment vs dev...
			 length -= 18;
		 }
		 if (_proxyUrl.length() > length) {
			 _indexCommand = _proxyUrl.substring(1 + length);
		 }
		 //TESTED (see URLs below)
		 
		 //DEBUG
		 //System.out.println(_proxyUrl + " ... " + _indexOrAdminCommand + " THEN " + _indexCommand);
		
		 _queryOptions = this.getQuery().getValuesMap();
		 
	}//TESTED
	
	//___________________________________________________________________________________
	
	/**
	 * Handles an OPTIONS request (automatically)
	 */
	
	@Options
	public Representation options(Representation entity)
	{
		return entity;
	}
	
	//___________________________________________________________________________________
	
	/**
	 * Handles a POST
	 */

	@Post
	public Representation post(Representation entity) 
	{
		if (Method.POST == getRequest().getMethod()) 
		{
			try {
				_postData = entity.getText();
			} catch (Exception e) {} // do nothing, carry on as far as possible
		}		 
		
		return get();
	}//TESTED
	
	//___________________________________________________________________________________
	
	/**
	 * Handles a PUT
	 */

	@Put
	public Representation put(Representation entity) 
	{
		if (Method.PUT == getRequest().getMethod()) 
		{
			try {
				_postData = entity.getText();
			} catch (Exception e) {} // do nothing, carry on as far as possible
		}		 
		
		return get();
	}//TESTED
	
	//___________________________________________________________________________________
	
	/**
	 * Handles a DELETE
	 */

	@Delete
	public Representation delete(Representation entity) 
	{
		if (Method.DELETE == getRequest().getMethod()) 
		{
			_deleteMode = true;
			
			// Additional access control logic
			// Only allowed to do something on the following URLs:
			// - kibana-int/*
			if (!_indexOrAdminCommand.equals("kibana-int")) {
				return returnError("Record", "Dashboard deletion error - " + _proxyUrl);
			}
			
			try {
				_postData = entity.getText();
			} catch (Exception e) {} // do nothing, carry on as far as possible
		}		 
		
		return get();
	}//TESTED
	
	//___________________________________________________________________________________
	
	// TODO: (INF-2533): Handle the following cases
	// Here are the different commands that we proxy:
	// 1] GET  _nodes - used to check the versions (PROXY: need to remove everything but the versions for security)
	// 2] GET  {TENATIVE_INDEXLIST}/_aliases - used to get the date-generated list of indexes that actually exist
	//                                    (can return {})
	// 3] POST kibana-int/dashboard/_search - used to get available dashboards (PROXY: move this to using shares based on communities, also have kibana-int-live and kibana-int-stashed)
	//                                   (returns { hits: { total:, hits: [{_id==title, _source:JSON_share}] } }, not sure how much of that is used?)
	// 4] GET  kibana-int/dashboard/<_id>?time - used to get the dashboard (returns the same format as root.hits above) (PROXY: use communities/shares as per [2])
	// 5] {CONFIRMED_INDEXLIST}/_search - used to actually perform the various queries 	
	
	/**
	 * Handles a GET
	 */
	@Get
	public Representation get() 
	{
		// Authentication:
		HashSet<String> communityIds = null;
		
		_driver = new InfiniteDriver("http://localhost:8080/api/");
		//DEBUG
		//_driver = new InfiniteDriver("http://localhost:8184/");
		//_driver = new InfiniteDriver("http://URL/api/", "APIKEY");
		
		_driver.useExistingCookie(_cookie);
		ResponseObject response = new ResponseObject();
		PersonPojo me = _driver.getPerson(null, response);
		if (!response.isSuccess() || (null == me)) {
			return returnError("Cookie Lookup", "Cookie session expired or never existed, please login first or use valid key or user/pass");			
		}
		
		// OK some basic access controls:
		// 1] if it's an admin command then must be nodes:
		if (null == _indexCommand) { // 1]
			if (!_indexOrAdminCommand.equals("_nodes")) {
				return returnError("Record", "Admin command error - " + _proxyUrl);				
			}
		}//TESTED (1)
		else { // Indexes specified, need to check vs person's communities (2-5)
			
			// Mode: stashed/live (/neither)
			String mode = this._queryOptions.get("mode");
			if (null != mode) {
				if (!mode.equalsIgnoreCase("live") && !mode.equalsIgnoreCase("stashed")) { // only allowed values
					mode = null;
				}
			}
			
			// Do we have a community override set?
			String commIdStrList = this._queryOptions.get("cids");
			HashSet<String> commIdOverrideSet = null;
			if (null != commIdStrList) {
				String[] commIdStrArray = commIdStrList.split("\\s*,\\s*");
				if ((commIdStrArray.length > 1) || !commIdStrArray[0].isEmpty()) {
					commIdOverrideSet = new HashSet<String>(Arrays.asList(commIdStrArray));
					if (commIdOverrideSet.isEmpty()) { // not specified - all communities
						commIdOverrideSet = null;
					}
				}
			}
			
			communityIds = new HashSet<String>();
			for (PersonCommunityPojo myComm: me.getCommunities()) {
				String commIdStr =  myComm.get_id().toString();
				if (null != commIdOverrideSet) {
					if (commIdOverrideSet.contains(commIdStr)) { // community override but included
						communityIds.add(commIdStr);						
					}
				}
				else { // no override, all communities
					communityIds.add(commIdStr);
				}
			}//TESTED (see handleDashboardSharing, test 5)
			
			// Create a regex of user's communities
			StringBuffer indexBuffer = new StringBuffer();
			for (String communityId: communityIds) {
				if (0 != indexBuffer.length()) {
					indexBuffer.append("|");
				}
				indexBuffer.append(communityId.toString());
			}//TOTEST (works functionally, just needs a unit test at some point)
			if (0 == indexBuffer.length()) {
				indexBuffer.append("__NO__MATCH__");
			}
			Pattern commRegex = Pattern.compile(indexBuffer.toString());
			
			indexBuffer.setLength(0);

			// Derive whether Kibana is in live mode (recs_t_COMMUNITY_DATE) vs stashed mode (recs_COMMUNITY)
			// based on the format of the incoming indexes
			// TODO (INF-2533): should force Kibana to send ellipses instead of huge long lists of indexes and then support here			
			
			if (_indexOrAdminCommand.equals("NO_TIME_FILTER_OR_INDEX_PATTERN_NOT_MATCHED")) {
				//(handy alias - this value is generated in some cases by Kibana when _all is what they "mean")
				_indexOrAdminCommand = "_all";
			}
			
			if (_indexOrAdminCommand.equals("kibana-int") && (null != mode)) { // 3] and 4]
				// Special case, we're going to use Infinit.e shares
				return handleDashboardSharing(_indexCommand, mode, communityIds);
			}			
			else if (!_indexOrAdminCommand.equals("_all")) { // manually specified indexes, check vs allowed list 
				boolean inclusive = false; // if all the indexes are of type "-" then need to add _all...
				String[] indexList = _indexOrAdminCommand.split("\\s*,\\s*");
				for (String index: indexList) {
					if (index.startsWith("logstash-") || index.startsWith("ls-")) { // 2]
						// will treat these as a proxy for "everything I can see" - also means must be in live mode
						if (_indexCommand.equals("_aliases")) { // 2]
							if (0 != indexBuffer.length()) {
								indexBuffer.append(',');
							}
							// (replace with "recs_t_*_" to avoid exploding the command line, will then tidy up community permissions on the other side)
							if (index.startsWith("ls-")) {
								inclusive = true;
								indexBuffer.append(index.replace("ls-", "recs_t_*_"));
							}//TESTED (11)
							else {
								inclusive = true;
								indexBuffer.append(index.replace("logstash-", "recs_t_*_"));
							}//TESTED (11)
						}
						else { // don't think this will happen, in theory need to explode into communities
							return returnError("Record", "Command error - Logstash alias used in conjunction with search? - " + _proxyUrl);							
						}//TOTEST
						
					}
					else if (index.startsWith("doc")) {
						//TODO (INF-2533): Can't currently allow access to the doc views because of child communities security model ugh
						return returnError("Record", "Command error - doc views not currently supported: " + index);							
					}
					else if (index.equals("kibana-int")) { // (legacy kibana-int, leave this here...)
						inclusive = true;
						if (0 != indexBuffer.length()) {
							indexBuffer.append(',');
						}
						indexBuffer.append(index);
					}//TESTED (2,8,9)
					else if (index.startsWith("-") || index.startsWith("+")) {
						// Not supported:
						return returnError("Record", "Command error - malformed index: " + index);							
					}//TOTEST
					else { // These are probably indexes returned from the alias, so just double check the security // 5]
						if (commRegex.matcher(index).find()) {
							inclusive = true;
							if (0 != indexBuffer.length()) {
								indexBuffer.append(',');
							}
							indexBuffer.append(index);
						}
					}//TESTED (5)
				}
				if (!inclusive) {
					indexBuffer.append("recs_dummy"); // (will return nothing)
				}//TESTED
			}
			if (_indexOrAdminCommand.equals("_all")) { // Basically must be in stashed mode
				if (communityIds.size() > 0) {
					String[] indexes = new String[communityIds.size()];
	
					int i = 0;
					for (String communityId: communityIds) {
						indexes[i] = "recs_" + communityId;
						i++;
					}
					
					// Convert this generic list into a list of indexes that actually exists 
					// (ie duplicate the _alias call that is made in non-timestamp cases)
					// (https://github.com/elasticsearch/elasticsearch/blob/master/src/main/java/org/elasticsearch/rest/action/admin/indices/alias/get/RestGetIndicesAliasesAction.java)
					
					ElasticSearchManager indexMgr = ElasticSearchManager.getIndex(DUMMY_INDEX);
					ClusterStateResponse retVal = indexMgr.getRawClient().admin().cluster().prepareState()
							.setIndices(indexes)
							.setRoutingTable(false).setNodes(false).setListenerThreaded(false).get();
	
					indexBuffer.setLength(0);
					for (IndexMetaData indexMetadata: retVal.getState().getMetaData()) {
						if (0 != indexBuffer.length()) {
							indexBuffer.append(',');
						}
						indexBuffer.append(indexMetadata.index());
					}
				}
				if (0 == indexBuffer.length()) {
					indexBuffer.append("recs_dummy"); // (will return nothing)				
				}//TOTEST
				
			}//TESTED (3, 4)
			
			_indexOrAdminCommand = indexBuffer.toString();
			
			// Currently: only commands allowed are _mapping and _search
			// (contains not startsWith because can include types after the index)

			StringBuffer newIndexList = new StringBuffer();
			String[] commandOrParamList = _indexCommand.split("/");
			for (String commandOrParam: commandOrParamList) {
				if (commandOrParam.startsWith("_")) {
					if (!commandOrParam.equals("_mapping") && !commandOrParam.equals("_search") && !commandOrParam.equals("_aliases")) {
						return returnError("Record", "Index command error - " + _proxyUrl);							
					}//TESTED (inclusive and exclusive, 4-7)
				}
				if (0 != newIndexList.length()) {
					newIndexList.append('/');
				}
				try {
					newIndexList.append(URLEncoder.encode(commandOrParam, "UTF-8"));
				} catch (UnsupportedEncodingException e) {
					newIndexList.append(commandOrParam);
				}
			}//TESTED (11)
			_indexCommand = newIndexList.toString();
	
		}//(end if index/command format)

		// Proxy the request to elasticsearch
		
		StringBuffer errorString = new StringBuffer("Query error");
		String data = null;
		MediaType mediaType = MediaType.APPLICATION_JSON; 
		
		StringBuffer urlStr = new StringBuffer("http://").append(_esHostUrl).append('/').append(_indexOrAdminCommand);
		if (null != _indexCommand) {
			urlStr.append('/').append(_indexCommand);
		}
		if ((null != _queryOptions) && !_queryOptions.isEmpty()) {
			boolean firstCommand = true;
			for (Map.Entry<String, String> entry: _queryOptions.entrySet()) {
				String key = entry.getKey();
				// (ignore infinit.e-specific)
				if (!key.equals("infinite_api_key") && !key.equals("cids") && !key.equals("mode"))
				{
					if (firstCommand) {
						urlStr.append('?');
					}
					else {
						urlStr.append('&');					
		
					}
					urlStr.append(key);
					if (null != entry.getValue()) {
						urlStr.append('=').append(entry.getValue());
						firstCommand = false;
					}
				}
			}
		}//TESTED (2,9,11)
		
		URL url;
		HttpURLConnection conn = null;
		try {
			url = new URL(urlStr.toString());
			//DEBUG
			//System.out.println(getRequest().getMethod().toString() + " " + urlStr + " ?? " + url.toString());
			
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod(getRequest().getMethod().toString());
			if (null != _postData) {
				//DEBUG
				//System.out.println("POST/PUT/etc " + _postData.length());
				
				conn.setDoInput(true);
				conn.setDoOutput(true);				
				OutputStream os = conn.getOutputStream();
				BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
				writer.write(_postData);
				writer.flush();
				writer.close();
				os.close();
			}
			Scanner s = null; 
			try {
				s = new Scanner(conn.getInputStream(), "UTF-8");
				data = s.useDelimiter("\\A").next();
			}
			catch (IOException fe) {
				s = new Scanner(conn.getErrorStream(), "UTF-8"); 
				data = s.useDelimiter("\\A").next();
			}
			finally {
				if (null != s) {
					s.close();
				}
			}
		}//TESTED (all)
		catch (Exception e) {
			//DEBUG
			//e.printStackTrace();
			return returnError("Record", "Index proxy error - " + _proxyUrl + " : " + e.toString());
		}		
		
		// Tidy up data in some cases
		//TODO (INF-2533): for _nodes remove everything except versioning information: data.nodes.version
		
		if ((null != _indexCommand) && _indexCommand.equals("_aliases")) {
			//need to remove un-authorized indexes from _alias
			Pattern replacerRegex = Pattern.compile("recs_(?:t_)?([0-9a-zA-Z]+)_[0-9.]+");
			Matcher m = replacerRegex.matcher(data);
			StringBuffer newData = new StringBuffer();
			boolean found = false;
			while (m.find()) {
				found = true;
				if (!communityIds.contains(m.group(1))) {
					m.appendReplacement(newData, "recs_forbidden_2000-01-01");
				}
				else { // add the string itself
					m.appendReplacement(newData, m.group());
				}//TESTED
			}//TESTED
			if (found) {
				m.appendTail(newData);
				data = newData.toString();
			}//TESTED
		}//TESTED (11)		
		
		// One last check to ensure data has value
		if (data == null ) {
			return returnError("Record", errorString.toString());
		}
		return new StringRepresentation(data, mediaType);
	}		
	
	protected StringRepresentation returnError(String queryType, String errorString) {
		ResponsePojo rp = returnErrorPojo(queryType, errorString);
		return new StringRepresentation(rp.toApi(), MediaType.APPLICATION_JSON);
	}
	protected ResponsePojo returnErrorPojo(String queryType, String errorString) {
		ResponsePojo rp = new ResponsePojo();
		rp.setResponse(new ResponseObject(queryType, false, errorString));
		return rp;
	}
	
	//TEST urls:
	// 1] GET http://localhost:8184/knowledge/record/control/_nodes
	// 2] GET http://localhost:8184/knowledge/record/control/kibana-int/dashboard/test2?1395675612810
	// 3] GET http://localhost:4092/_all/_mapping
	// 4] POST http://dev.ikanow.com/api/knowledge/record/control/_all/_search
	// 5] POST http://dev.ikanow.com/api/knowledge/record/control/records_4c927585d591d31d7b37097a_2011.05.19/_search (with IP search shows 2 elements)
	// 6] POST http://dev.ikanow.com/api/knowledge/record/control/records_4c927585d591d31d7b37097a_2011.05.18/_search (same search shows 0 elements)
	// 7] POST http://dev.ikanow.com/api/knowledge/record/control/records_community_2011.05.18/_search (reverts back to _all, ie shows 2 with IP search)
	// 8] PUT http://dev.ikanow.com/api/knowledge/record/control/kibana-int/dashboard/NewSave
	// 9] GET http://dev.ikanow.com/api/knowledge/record/control/kibana-int/dashboard/NewSave?1395697773208
	// 10] DELETE http://dev.ikanow.com/api/knowledge/record/control/kibana-int/dashboard/NewSave
	// 11] GET http://dev.ikanow.com/api/knowledge/record/control/logstash-2010.04.19,ls-2011.05.18,ls-2011.05.19,logstash-2010.04.22/_aliases?ignore_missing=true

	///////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////
	
	// Handle dashboard-specific operations:
	
	// 3] POST kibana-int/dashboard/_search - used to get available dashboards (PROXY: move this to using shares based on communities, also have kibana-int-live and kibana-int-stashed)
	//                                   (returns { hits: { total:, hits: [{_id==title, _source:{user:, group, title, dashboard}}] } }, see below
	// 4a] GET  kibana-int/dashboard/<_id>?time - used to get the dashboard (after insert/4b only)
	//									 (returns { _index: "kibana-int", _type: "dashboard", "found": true, _version: "1", 
	//												"_source": { user: guest, group: guest, title: title, dashboard: jsonstr }}
	// 4b] PUT	kibana-int/dashboard/<_id>?time - used to update dashboard elements, returns {_index, _type, _id:"title", _version:1, created:true}
	// 4c] DELETE kibana-int/dashboard/<_id>?time - used to delete dashboard elements - returns eg {"found":true,"_index":"kibana-int","_type":"dashboard","_id":"alextest","_version":2}
	
	private static Pattern extractFilter = Pattern.compile("title:([^*}]*)");
	
	private Representation handleDashboardSharing(String command, String mode, HashSet<String> communityIds) {		
		ResponseObject responseObj = new ResponseObject();

		StringBuffer communityIdStrList = new StringBuffer();
		for (String s: communityIds) {
			if (communityIdStrList.length() > 0) {
				communityIdStrList.append(',');
			}
			communityIdStrList.append(s);
		}//TESTED (5)

		// Parse out from the REST:
		String commands[] = command.split("/");
		command = commands[commands.length - 1];
		
		// Annoyingly regardless of get/post we need to get the current list (because you can't search by title)
		List<SharePojo> results = _driver.searchShares("community", communityIdStrList.toString(), "kibana-int-" + mode, responseObj);
		if (null == results) {
			results = new ArrayList<SharePojo>(0);
		}
		if (_deleteMode) { // This one is very straightforward ... find the share and delete it...
			DashboardProxyResultPojo deleteResult = new DashboardProxyResultPojo();
			deleteResult._id = command;
			for (SharePojo result: results) {
				if (command.equals(result.getTitle())) {
					_driver.removeShare(result.get_id().toString(), responseObj);
					deleteResult.found = responseObj.isSuccess();
					return new StringRepresentation(deleteResult.toApi(), MediaType.APPLICATION_JSON);
				}
			}//TESTED (7)
			deleteResult.found = false;
			return new StringRepresentation(deleteResult.toApi(), MediaType.APPLICATION_JSON);
		}//TESTED (7)
		else if (command.equalsIgnoreCase("_search")) { // Search
			
			//Filter: (really need to start indexing)
			
			String titleFilter = null;
			if (null != _postData) { // pull out filter
				Matcher m = extractFilter.matcher(_postData);
				if (m.find()) {
					titleFilter = m.group(1).toLowerCase();
					if (titleFilter.isEmpty()) {
						titleFilter = null;
					}
				}
			}//TESTED (3)
			
			// Perform search
			
			DashboardProxySearchResultPojo reply = new DashboardProxySearchResultPojo();
			reply.hits = new DashboardProxySearchResultPojo.Hits();
			reply.hits.hits = new LinkedList<DashboardProxySearchResultPojo.Hits.HitElement>();
			int hits = 0;
			for (SharePojo result: results) {
				if ((null == titleFilter) || ((null != result.getTitle()) && (result.getTitle().toLowerCase().contains(titleFilter)))) {
					DashboardProxySearchResultPojo.Hits.HitElement hitEl = new DashboardProxySearchResultPojo.Hits.HitElement();
					hitEl._id = result.getTitle();
					hitEl._source.title = result.getTitle();
					hitEl._source.dashboard= result.getShare();
					reply.hits.hits.add(hitEl);
					hits++;
				}
			}//TESTED (2, 3)
			reply.hits.total = hits;
			return new StringRepresentation(reply.toApi(), MediaType.APPLICATION_JSON);			
		}//TESTED (2, 3)
		else if (null == _postData) { // GET
			DashboardProxyResultPojo getResult = new DashboardProxyResultPojo();
			getResult._id = command;
			for (SharePojo result: results) {
				if (command.equals(result.getTitle())) {
					getResult.found = true;
					getResult._source = new DashboardPojo("guest", "guest");
					getResult._source.title = command;
					getResult._source.dashboard = result.getShare();
					return new StringRepresentation(getResult.toApi(), MediaType.APPLICATION_JSON);
				}
			}		
			getResult.found = false;
			return new StringRepresentation(getResult.toApi(), MediaType.APPLICATION_JSON);
		}//TESTED (6)
		else { // We're overwriting the existing share with a new JSON file...
			DashboardProxyResultPojo putResult = new DashboardProxyResultPojo();
			putResult._id = command;

			// Extract dashboard from JSON:
			DashboardPojo dash = DashboardPojo.fromApi(_postData, DashboardPojo.class);
			
			SharePojo shareToUpdate = null;
			for (SharePojo result: results) {
				if (command.equals(result.getTitle())) {
					shareToUpdate = result;
					break;
				}
			}//TESTED (4, 5)
			if (null == shareToUpdate) { //create a new share...
				putResult.found = false;
				SharePojo addedShare = _driver.addShareJSON(command, "Added by infinit.e.records.server", "kibana-int-" + mode, dash.dashboard, responseObj);
				if (null != addedShare) {
					for (String commIdStr: communityIds) {
						_driver.addShareToCommunity(addedShare.get_id().toString(), "Added by infinit.e.records.server", commIdStr, responseObj);
					}					
				}
			}//TESTED (4, 5)
			else { // update an existing share
				putResult.found = true;
				// Update communities:
				for (SharePojo.ShareCommunityPojo shareShare: shareToUpdate.getCommunities()) {
					String shareShareIdStr = shareShare.get_id().toString();
					if (communityIds.contains(shareShareIdStr)) {
						communityIds.remove(shareShareIdStr);
					}
					else {
						_driver.removeShareFromCommunity(shareToUpdate.get_id().toString(), shareShareIdStr, responseObj);
					}
				}//TESTED (5)
				for (String commIdStr: communityIds) {
					_driver.addShareToCommunity(shareToUpdate.get_id().toString(), "Added by infinit.e.records.server", commIdStr, responseObj);
				}//TESTED (5)
				// Update content:
				_driver.updateShareJSON(shareToUpdate.get_id().toString(), shareToUpdate.getTitle(), shareToUpdate.getDescription(), 
											shareToUpdate.getType(), dash.dashboard, responseObj);
			}//TESTED (4,5)
			return new StringRepresentation(putResult.toApi(), MediaType.APPLICATION_JSON);
		}
	}//TESTED (See below)
	
	//TEST URLs for handleDashboardSharing():
	// 1] POST http://localhost:8185/proxy/kibana-int/dashboard/_search (just check still goes via old tested path)
	// {"query":{"query_string":{"query":"title:*"}},"size":20}
	//
	// 2] POST http://localhost:8185/proxy/kibana-int/dashboard/_search?mode=stashed (also &mode=live)
	// {"query":{"query_string":{"query":"title:*"}},"size":20}
	//
	// 3] POST http://localhost:8185/proxy/kibana-int/dashboard/_search?mode=stashed (first POST returns values, second doesn't)
	// {"query":{"query_string":{"query":"title:may*"}},"size":20}
	// {"query":{"query_string":{"query":"title:xxx*"}},"size":20}
	//
	// 4] PUT  http://localhost:8185/proxy/kibana-int/dashboard/May%2014%20Demo?mode=stashed (first time inserts, second time updates)
	// {"user":"guest","group":"guest","title":"May 14 Demo","dashboard":"{\"title\":\"May 14 Demo\",\"services\":{\"query\":{\"list\":{\"0\":{\"query\":\"*\",\"alias\":\"\",\"color\":\"#7EB26D\",\"id\":0,\"pin\":false,\"type\":\"lucene\",\"enable\":true}},\"ids\":[0]},\"filter\":{\"list\":{\"0\":{\"from\":\"2013-05-10T22:49:46.088Z\",\"to\":\"2013-05-20T22:49:46.088Z\",\"type\":\"time\",\"field\":\"@timestamp\",\"mandate\":\"must\",\"active\":true,\"alias\":\"\",\"id\":0}},\"ids\":[0]}},\"rows\":[{\"title\":\"Graph\",\"height\":\"350px\",\"editable\":true,\"collapse\":false,\"collapsable\":true,\"panels\":[{\"span\":12,\"editable\":true,\"group\":[\"default\"],\"type\":\"histogram\",\"mode\":\"count\",\"time_field\":\"@timestamp\",\"value_field\":null,\"auto_int\":true,\"resolution\":100,\"interval\":\"3h\",\"fill\":3,\"linewidth\":3,\"timezone\":\"browser\",\"spyable\":true,\"zoomlinks\":true,\"bars\":true,\"stack\":true,\"points\":false,\"lines\":false,\"legend\":true,\"x-axis\":true,\"y-axis\":true,\"percentage\":false,\"interactive\":true,\"queries\":{\"mode\":\"all\",\"ids\":[0]},\"title\":\"Events over time\",\"intervals\":[\"auto\",\"1s\",\"1m\",\"5m\",\"10m\",\"30m\",\"1h\",\"3h\",\"12h\",\"1d\",\"1w\",\"1M\",\"1y\"],\"options\":true,\"tooltip\":{\"value_type\":\"cumulative\",\"query_as_alias\":true},\"scale\":1,\"y_format\":\"none\",\"grid\":{\"max\":null,\"min\":0},\"annotate\":{\"enable\":false,\"query\":\"*\",\"size\":20,\"field\":\"_type\",\"sort\":[\"_score\",\"desc\"]},\"pointradius\":5,\"show_query\":true,\"legend_counts\":true,\"zerofill\":true,\"derivative\":false}],\"notice\":false},{\"title\":\"Events\",\"height\":\"350px\",\"editable\":true,\"collapse\":false,\"collapsable\":true,\"panels\":[{\"title\":\"All events\",\"error\":false,\"span\":12,\"editable\":true,\"group\":[\"default\"],\"type\":\"table\",\"size\":100,\"pages\":5,\"offset\":0,\"sort\":[\"@timestamp\",\"desc\"],\"style\":{\"font-size\":\"9pt\"},\"overflow\":\"min-height\",\"fields\":[],\"localTime\":true,\"timeField\":\"@timestamp\",\"highlight\":[],\"sortable\":true,\"header\":true,\"paging\":true,\"spyable\":true,\"queries\":{\"mode\":\"all\",\"ids\":[0]},\"field_list\":true,\"status\":\"Stable\",\"trimFactor\":300,\"normTimes\":true,\"all_fields\":false}],\"notice\":false}],\"editable\":true,\"failover\":false,\"index\":{\"interval\":\"day\",\"pattern\":\"[logstash-]YYYY.MM.DD\",\"default\":\"NO_TIME_FILTER_OR_INDEX_PATTERN_NOT_MATCHED\",\"warm_fields\":true},\"style\":\"dark\",\"panel_hints\":true,\"pulldowns\":[{\"type\":\"query\",\"collapse\":false,\"notice\":false,\"query\":\"*\",\"pinned\":true,\"history\":[],\"remember\":10,\"enable\":true},{\"type\":\"filtering\",\"collapse\":true,\"notice\":true,\"enable\":true}],\"nav\":[{\"type\":\"timepicker\",\"collapse\":false,\"notice\":false,\"status\":\"Stable\",\"time_options\":[\"5m\",\"15m\",\"1h\",\"6h\",\"12h\",\"24h\",\"2d\",\"7d\",\"30d\"],\"refresh_intervals\":[\"5s\",\"10s\",\"30s\",\"1m\",\"5m\",\"15m\",\"30m\",\"1h\",\"2h\",\"1d\"],\"timefield\":\"@timestamp\",\"now\":false,\"filter_id\":0,\"enable\":true}],\"loader\":{\"save_gist\":false,\"save_elasticsearch\":true,\"save_local\":true,\"save_default\":true,\"save_temp\":true,\"save_temp_ttl_enable\":true,\"save_temp_ttl\":\"30d\",\"load_gist\":true,\"load_elasticsearch\":true,\"load_elasticsearch_size\":20,\"load_local\":true,\"hide\":false},\"refresh\":false}"}
	//
	// 5] PUT  http://localhost:8185/proxy/kibana-int/dashboard/May%2014%20Demo?mode=stashed&cids=4c927585d591d31d7b37097a (change communities - also 506dc16dfbf042893dd6b8f2,5249dd5506d6f37e87ded59f)
	// {"user":"guest","group":"guest","title":"May 14 Demo","dashboard":"{\"title\":\"May 14 Demo\",\"services\":{\"query\":{\"list\":{\"0\":{\"query\":\"*\",\"alias\":\"\",\"color\":\"#7EB26D\",\"id\":0,\"pin\":false,\"type\":\"lucene\",\"enable\":true}},\"ids\":[0]},\"filter\":{\"list\":{\"0\":{\"from\":\"2013-05-10T22:49:46.088Z\",\"to\":\"2013-05-20T22:49:46.088Z\",\"type\":\"time\",\"field\":\"@timestamp\",\"mandate\":\"must\",\"active\":true,\"alias\":\"\",\"id\":0}},\"ids\":[0]}},\"rows\":[{\"title\":\"Graph\",\"height\":\"350px\",\"editable\":true,\"collapse\":false,\"collapsable\":true,\"panels\":[{\"span\":12,\"editable\":true,\"group\":[\"default\"],\"type\":\"histogram\",\"mode\":\"count\",\"time_field\":\"@timestamp\",\"value_field\":null,\"auto_int\":true,\"resolution\":100,\"interval\":\"3h\",\"fill\":3,\"linewidth\":3,\"timezone\":\"browser\",\"spyable\":true,\"zoomlinks\":true,\"bars\":true,\"stack\":true,\"points\":false,\"lines\":false,\"legend\":true,\"x-axis\":true,\"y-axis\":true,\"percentage\":false,\"interactive\":true,\"queries\":{\"mode\":\"all\",\"ids\":[0]},\"title\":\"Events over time\",\"intervals\":[\"auto\",\"1s\",\"1m\",\"5m\",\"10m\",\"30m\",\"1h\",\"3h\",\"12h\",\"1d\",\"1w\",\"1M\",\"1y\"],\"options\":true,\"tooltip\":{\"value_type\":\"cumulative\",\"query_as_alias\":true},\"scale\":1,\"y_format\":\"none\",\"grid\":{\"max\":null,\"min\":0},\"annotate\":{\"enable\":false,\"query\":\"*\",\"size\":20,\"field\":\"_type\",\"sort\":[\"_score\",\"desc\"]},\"pointradius\":5,\"show_query\":true,\"legend_counts\":true,\"zerofill\":true,\"derivative\":false}],\"notice\":false},{\"title\":\"Events\",\"height\":\"350px\",\"editable\":true,\"collapse\":false,\"collapsable\":true,\"panels\":[{\"title\":\"All events\",\"error\":false,\"span\":12,\"editable\":true,\"group\":[\"default\"],\"type\":\"table\",\"size\":100,\"pages\":5,\"offset\":0,\"sort\":[\"@timestamp\",\"desc\"],\"style\":{\"font-size\":\"9pt\"},\"overflow\":\"min-height\",\"fields\":[],\"localTime\":true,\"timeField\":\"@timestamp\",\"highlight\":[],\"sortable\":true,\"header\":true,\"paging\":true,\"spyable\":true,\"queries\":{\"mode\":\"all\",\"ids\":[0]},\"field_list\":true,\"status\":\"Stable\",\"trimFactor\":300,\"normTimes\":true,\"all_fields\":false}],\"notice\":false}],\"editable\":true,\"failover\":false,\"index\":{\"interval\":\"day\",\"pattern\":\"[logstash-]YYYY.MM.DD\",\"default\":\"NO_TIME_FILTER_OR_INDEX_PATTERN_NOT_MATCHED\",\"warm_fields\":true},\"style\":\"dark\",\"panel_hints\":true,\"pulldowns\":[{\"type\":\"query\",\"collapse\":false,\"notice\":false,\"query\":\"*\",\"pinned\":true,\"history\":[],\"remember\":10,\"enable\":true},{\"type\":\"filtering\",\"collapse\":true,\"notice\":true,\"enable\":true}],\"nav\":[{\"type\":\"timepicker\",\"collapse\":false,\"notice\":false,\"status\":\"Stable\",\"time_options\":[\"5m\",\"15m\",\"1h\",\"6h\",\"12h\",\"24h\",\"2d\",\"7d\",\"30d\"],\"refresh_intervals\":[\"5s\",\"10s\",\"30s\",\"1m\",\"5m\",\"15m\",\"30m\",\"1h\",\"2h\",\"1d\"],\"timefield\":\"@timestamp\",\"now\":false,\"filter_id\":0,\"enable\":true}],\"loader\":{\"save_gist\":false,\"save_elasticsearch\":true,\"save_local\":true,\"save_default\":true,\"save_temp\":true,\"save_temp_ttl_enable\":true,\"save_temp_ttl\":\"30d\",\"load_gist\":true,\"load_elasticsearch\":true,\"load_elasticsearch_size\":20,\"load_local\":true,\"hide\":false},\"refresh\":false}"}
	//
	// 6] GET http://localhost:8185/proxy/kibana-int/dashboard/May%2014%20Demo?mode=stashed (retrieves the uploaded version)
	//
	// 7] DELETE http://localhost:8185/proxy/kibana-int/dashboard/May%2014%20Demo?mode=stashed
	// (run 5] after this to check add records with communities)
}
