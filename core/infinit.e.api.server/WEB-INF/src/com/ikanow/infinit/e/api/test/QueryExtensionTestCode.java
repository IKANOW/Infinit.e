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
package com.ikanow.infinit.e.api.test;

import java.util.Date;
import java.util.List;

import org.bson.types.ObjectId;

import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.api.knowledge.AdvancedQueryPojo;
import com.ikanow.infinit.e.data_model.interfaces.query.IQueryExtension;
import com.ikanow.infinit.e.data_model.store.document.DocumentPojo;
import com.mongodb.BasicDBObject;

public class QueryExtensionTestCode implements IQueryExtension {

	String[] _savedCommIdStrs;
	AdvancedQueryPojo _query; 
	
	@Override
	public void preQueryActivities(ObjectId queryId, AdvancedQueryPojo query, String[] communityIdStrs) {
		
		System.out.println("**QUERY ID=" + queryId);
		System.out.println("**QUERY=" + query.toApi());
		
		_query = query;
		_savedCommIdStrs = communityIdStrs;
	}

	@Override
	public void postQueryActivities(ObjectId queryId, List<BasicDBObject> docs, ResponsePojo response) {
		
		System.out.println("**QUERY ID=" + queryId);
		BasicDBObject topDoc = null;
		if ((null != docs) && !docs.isEmpty()) {
			topDoc = docs.iterator().next();
			System.out.println("**TOP DOC=" + topDoc);
		}
		DocumentPojo newDoc = new DocumentPojo();
		newDoc.setTitle("test1 title: " + queryId);
		newDoc.setUrl("http://www.bbc.com");
		newDoc.setDescription("test1 desc");
		newDoc.setCreated(new Date());
		newDoc.setModified(new Date());
		newDoc.setPublishedDate(new Date());
		newDoc.setId(queryId);
		newDoc.setMediaType("Social");
		newDoc.addToMetadata("query", _query);
		newDoc.setSourceKey("test1");
		newDoc.setCommunityId(new ObjectId(_savedCommIdStrs[0]));
		if (null != topDoc) {
			newDoc.setScore(topDoc.getDouble(DocumentPojo.score_, 100.0));
			newDoc.setAggregateSignif(topDoc.getDouble(DocumentPojo.aggregateSignif_, 100.0));
		}
		else {
			newDoc.setScore(100.0);
			newDoc.setAggregateSignif(100.0);
		}
		if (null != docs) {
			docs.add(0, (BasicDBObject) newDoc.toDb());
		}
		response.getStats().found++;
	}

}
