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
package com.ikanow.infinit.e.data_model.store.config.source;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bson.BSONObject;
import org.bson.types.ObjectId;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.ikanow.infinit.e.data_model.Globals;
import com.ikanow.infinit.e.data_model.store.BasePojoDbMap;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbManager;
import com.ikanow.infinit.e.data_model.store.MongoDbUtil;
import com.ikanow.infinit.e.data_model.store.social.person.PersonCommunityPojo;
import com.ikanow.infinit.e.data_model.store.social.person.PersonPojo;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

public class SourcePojoSubstitutionDbMap implements BasePojoDbMap<SourcePojo> {

	protected ObjectId _callingUserId;
	protected SourcePojoSubstitutionDeserializer _errorHandler1;
	protected SourcePojoDeserializer _errorHandler2;
	
	public SourcePojoSubstitutionDbMap(ObjectId callingUserId) {
		_callingUserId = callingUserId;
	}
	
	public SourcePojoSubstitutionDbMap() {
		// No calling id known at this point, so we'll do it in 2 steps:
		// 1) grab the ownerId from the JSON
		// 2) then apply the per-string deser
	}

	@Override
	public GsonBuilder extendBuilder(GsonBuilder gp) {
		if (null != _callingUserId) {
			return gp.registerTypeAdapter(String.class, (_errorHandler1 = new SourcePojoSubstitutionDeserializer(_callingUserId)));
		}
		else {
			return gp.registerTypeAdapter(SourcePojo.class, (_errorHandler2 = new SourcePojoDeserializer()));
			//return gp.registerTypeAdapter(String.class, );			
		}
	}
	public List<String> getErrorMessages() {
		if (null != _errorHandler1) {
			return _errorHandler1.getErrMessages();
		}
		else if (null != _errorHandler2) {
			return _errorHandler2.getErrMessages();			
		}
		else {
			return null;
		}
	}

	protected static class SourcePojoDeserializer implements JsonDeserializer<SourcePojo>	
	{
		protected SourcePojoSubstitutionDeserializer _errorHandler1;

		public List<String> getErrMessages() {
			if (null != _errorHandler1) {
				return _errorHandler1.getErrMessages();
			}
			else {
				return null;
			}
		}		
		
		@Override
		public SourcePojo deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			JsonElement ownerIdJson = json.getAsJsonObject().get(SourcePojo.ownerId_);
			if (null != ownerIdJson) {
				String ownerIdStr = null;
				ObjectId ownerId = null;
				try {
					ownerIdStr = ownerIdJson.getAsString();
					ownerId = new ObjectId(ownerIdStr);
				}
				catch (Exception e) { // this is fine, just means it's an $oid
					try {
						ownerIdStr = ownerIdJson.getAsJsonObject().get("$oid").getAsString();
						ownerId = new ObjectId(ownerIdStr);
					}
					catch (Exception ee) {} // just carry on - this isn't a source sub specific error, and it will break elsewhere
				}
				if (null != ownerId) {
					return SourcePojo.getDefaultBuilder().
							registerTypeAdapter(String.class, (_errorHandler1 = new SourcePojoSubstitutionDeserializer(ownerId))).
								create().fromJson(json, SourcePojo.class);
				}
			}
			return SourcePojo.getDefaultBuilder().create().fromJson(json, SourcePojo.class);
		}		
	}//TESTED
	
	public static class SourcePojoSubstitutionDeserializer implements JsonDeserializer<String> 
	{
		protected ObjectId _callingUserId;
		protected static Pattern SUBVARIABLE = Pattern.compile("#IKANOW\\{([^.}]+)[.]([^}]+)\\}", Pattern.CASE_INSENSITIVE);
		// Options inside the #IKANOW{}:
		// <source_type>.var where source_type is any JSON field meeting criteria source_type && owner by _callingUserId
		// <sourceid>.var
		
		// cache
		protected HashMap<String, BasicDBObject> _savedCredentials = null;
		protected List<ObjectId> _userCommunities = null;
		
		public SourcePojoSubstitutionDeserializer(ObjectId callingUserId) {
			_callingUserId = callingUserId;			
		}

		@Override
		public String deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
						
			String val = json.getAsString();
			Matcher m = SUBVARIABLE.matcher(val);
			StringBuffer sb = null;
			while (m.find()) {
				if (null == sb) {
					sb = new StringBuffer();
				}
				String replaceStr = m.group(); // (will be overwritten if the sub succeeds)
				String sourceInfo = m.group(1); // source type (must be owner) or source id
				String fieldPath = m.group(2);

				BasicDBObject credentials = null;
				String authCredential = null;
				if (null == _savedCredentials) {
					_savedCredentials = new HashMap<String, BasicDBObject>();
				}
				else {
					credentials = _savedCredentials.get(sourceInfo);
				}//TESTED
				if (null == credentials) {
					credentials = populateCredentials(sourceInfo);
					if (null == credentials) { // permissison error, already logged issue 
						continue;
					}
				}//TESTED
				authCredential = MongoDbUtil.getProperty(credentials, fieldPath);

				if (null != authCredential) {
					replaceStr = authCredential;
				}
				else {
					addErrMessage("Couldn't find credential: " + fieldPath);
				}
				m.appendReplacement(sb, replaceStr);
			}
			if (null != sb) {
				m.appendTail(sb);
				val = sb.toString();
			}
			return val;
		}
		
		///////////////////////
		
		// UTILS
		
		protected BasicDBObject populateCredentials(String sourceInfo) {
			BasicDBObject retVal = null;
			try {
				ObjectId sourceId = new ObjectId(sourceInfo);
				try {
					// If here then grab source if I'm allowed
					// a) get my communities:
					if (null == _userCommunities) {
						BasicDBObject personQuery = new BasicDBObject("_id", _callingUserId);
						PersonPojo person = PersonPojo.fromDb(MongoDbManager.getSocial().getPerson().findOne(personQuery), PersonPojo.class);
						if (null == person) {
							addErrMessage("Didn't find user: " + _callingUserId);
							return null;
						}
						_userCommunities = new ArrayList<ObjectId>(person.getCommunities().size());
						for (PersonCommunityPojo personComm: person.getCommunities()) {
							_userCommunities.add(personComm.get_id());
						}
					}//TESTED (by hand)
					
					// b) try getting the source
					BasicDBObject shareQuery = new BasicDBObject(SharePojo._id_, sourceId);
					shareQuery.put(SharePojo.ShareCommunityPojo.shareQuery_id_, new BasicDBObject(DbManager.in_, _userCommunities));
					SharePojo share = SharePojo.fromDb(MongoDbManager.getSocial().getShare().findOne(shareQuery), SharePojo.class);					
					if (null == share) {
						addErrMessage("Couldn't find share, or don't have read access: " + sourceInfo);
						return null;
					}
					_savedCredentials.put(sourceInfo, (retVal = (BasicDBObject) com.mongodb.util.JSON.parse(share.getShare())));
				}
				catch (Exception e) {
					StringBuffer sbErr = new StringBuffer();
					Globals.populateStackTrace(sbErr, e);
					addErrMessage("Unknown error: " + sbErr.toString());
					return null; // (do nothing)
				}
			}//TESTED (including cache, by hand)
			catch (Exception e) {
				// Source info is a share type
				BasicDBObject shareQuery = new BasicDBObject(SharePojo.type_, sourceInfo);
				shareQuery.put(SharePojo.ShareOwnerPojo.shareQuery_id_, _callingUserId);
				
				List<SharePojo> credentialShares = SharePojo.listFromDb(DbManager.getSocial().getShare().find(shareQuery), SharePojo.listType());
				if ((null == credentialShares) || credentialShares.isEmpty()) {
					addErrMessage("Couldn't find shares of this type, or don't have read access: " + sourceInfo);
					return null;					
				}//TESTED (by hand)
				retVal = new BasicDBObject();
				for (SharePojo share: credentialShares) {
					try {
						Object x = com.mongodb.util.JSON.parse(share.getShare());
						if (x instanceof BasicDBList) { // It's a list add all the k/vs from all the array els
							BasicDBList xl = (BasicDBList)x;
							for (Object o: xl) {
								retVal.putAll((BSONObject)o);								
							}
						}
						else { // (normal case, it's an object just add all k/vs)
							retVal.putAll((BSONObject)x);
						}
					}
					catch (Exception ee) {
						StringBuffer sbErr = new StringBuffer();
						Globals.populateStackTrace(sbErr, e);
						addErrMessage("Failed to parse: " + share.get_id() + " : " + share.getTitle() + ": " + sbErr.toString());						
					}
				}//TESTED (by hand)
				if (retVal.isEmpty()) {
					addErrMessage("Failed to add any fields from: " + sourceInfo);
					return null;
				}
				_savedCredentials.put(sourceInfo, retVal);
			}//TESTED
			
			return retVal;
		}//TESTED
		
		////////////////////////////
		
		// Error Handling
		
		protected List<String> _errMessages;
		
		public void addErrMessage(String errMessage) {
			if (null == _errMessages) {
				_errMessages = new LinkedList<String>();
			}
			_errMessages.add(errMessage);
		}
		public List<String> getErrMessages() {
			return _errMessages;
		}
		
	}	
}
