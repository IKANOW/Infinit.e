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
package com.ikanow.infinit.e.api.authentication;

import java.util.Date;
import org.apache.log4j.Logger;

import com.ikanow.infinit.e.data_model.store.DbManager;
import com.mongodb.BasicDBObject;
import com.mongodb.WriteResult;

public class CookieManager 
{
	// Initialize the Logger
	private static final Logger logger = Logger.getLogger(CookieManager.class);
	
	public String cleanCookies(long milliInactive)
	{
		int removedCount = 0;
		//calculate time of last activity to remove things
		long inactiveBefore = new Date().getTime() - milliInactive;
		//Remove every cookie that has been inactive longer than milliInactive
		try {
			BasicDBObject query = new BasicDBObject();
			query.put("apiKey", new BasicDBObject(DbManager.exists_, false)); // (don't ever delete API keys)
			query.put("lastActivity", new BasicDBObject("$lt", new Date(inactiveBefore)));
				// (pojo method doesn't support this type of query)
			
			WriteResult wr = DbManager.getSocial().getCookies().remove(query);
			removedCount = wr.getN();
		} catch (Exception e) {
			// If an exception occurs log the error
			logger.error("Exception Message: " + e.getMessage(), e);
		}
		return "Removed " + removedCount + " inactive cookies";		
	}
}
