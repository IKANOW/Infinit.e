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

import java.net.URLEncoder;
import java.util.Date;
import java.util.Random;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;

import com.ikanow.infinit.e.api.utils.PropertiesManager;
import com.ikanow.infinit.e.data_model.utils.SendMail;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.api.ResponsePojo.ResponseObject;
import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.social.authentication.AuthenticationPojo;
import com.ikanow.infinit.e.data_model.InfiniteEnums;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class LoginHandler 
{
	private static final Logger logger = Logger.getLogger(LoginHandler.class);

	
	/**
	 * resetPassword
	 * @param username
	 * @return
	 */
	public ResponsePojo resetPassword(String username, boolean bLoggedIn) 
	{
		ResponsePojo rp = new ResponsePojo();
		try
		{
			//lookup username
			BasicDBObject query = new BasicDBObject("username",username);
			DBObject dbo = DbManager.getSocial().getAuthentication().findOne(query);
			if (null == dbo) {
				rp.setResponse(new ResponseObject("Reset Password",true,"Email has been sent containing link to reset password."));
				return rp; // (lies but won't leak out usernames)
			}
			AuthenticationPojo ap = AuthenticationPojo.fromDb(dbo,AuthenticationPojo.class);			
			
			Date now = new Date();
			if (bLoggedIn) 
			{				
				//change pword
				String newpassword = createNewRandomPassword();
				//Take new password and encrypt it
				ap.setPassword(PasswordEncryption.encrypt(newpassword));
				ap.setModified(now);
				DbManager.getSocial().getAuthentication().save(ap.toDb());
				
				//email new password
				// Subject Line
				String subject = "Request to reset password";
	
				// Message Body
				String body = "<p>Your new password is: " + newpassword + "</p>"; 
	
				// Send
				new SendMail(new PropertiesManager().getAdminEmailAddress(), ap.getUsername(), subject, body).send("text/html");	
	
				// (Remove new password from end of this message once mailing works, Currently attached just so can use)
				rp.setResponse(new ResponseObject("Reset Password",true,"Password reset successfully, new password has been emailed to user."));
			}//TESTED
			else 
			{ // Two stage process ... first "forgotten password" just sends email containing link to click on
				
				// To avoid people just hitting this button 1000 times, ensure only sent once per 5 minutes
				if ((now.getTime() - ap.getModified().getTime()) < 300000L) { // ie 300s ie 5mins
					rp.setResponse(new ResponseObject("Reset Password",true,"Password reset request ignored, try later."));
					return rp; 
				}//TESTED
				
				// Update auth to ensure this isn't abused
				ap.setModified(now);
				DbManager.getSocial().getAuthentication().save(ap.toDb());
				
				//email new password
				// Subject Line
				String subject = "Request to reset password";
	
				PropertiesManager props = new PropertiesManager();
				
				// Message Body
				StringBuffer newLink = new StringBuffer(props.getUrlRoot()).append("auth/forgotpassword").
															append("?username=").append(URLEncoder.encode(username, "UTF-8")).
															append("&password=").append(URLEncoder.encode(ap.getPassword(), "UTF-8"));
				String body = "<p>Click on this link to reset password: " + newLink.toString() + "</p>"; 
	
				// Send
				new SendMail(props.getAdminEmailAddress(), ap.getUsername(), subject, body).send("text/html");	
	
				// (Remove new password from end of this message once mailing works, Currently attached just so can use)
				rp.setResponse(new ResponseObject("Reset Password",true,"Email has been sent containing link to reset password."));
			}//TESTED
		}
		catch (Exception e)
		{
			// If an exception occurs log the error
			logger.error("Exception Message: " + e.getMessage(), e);
			rp.setResponse(new ResponseObject("Reset Password",false,"Error while reseting password"));
		}
		return rp;
	}
	
	
	/**
	 * createNewRandomPassword
	 * @return
	 */
	private String createNewRandomPassword()
	{
		String newpassword = "";
		Random random = new Random();
		//Picks a random letter between [a-z]
		for ( int i = 0; i < 10; i++)
			newpassword += "" +((char)(random.nextInt(26) + 97));		
		return newpassword;
	}
	
	
	/**
	 * deactivateAccount
	 * @param username
	 * @return
	 */
	public ResponsePojo deactivateAccount(String username)
	{
		ResponsePojo rp = new  ResponsePojo();
		try
		{
			//Get user
			DBObject dbo = DbManager.getSocial().getAuthentication().findOne(new BasicDBObject("username",username));
			AuthenticationPojo ap = AuthenticationPojo.fromDb(dbo,AuthenticationPojo.class);			
			//change status to deactivate
			ap.setAccountStatus(InfiniteEnums.AccountStatus.DISABLED);
			DbManager.getSocial().getAuthentication().update(dbo, ap.toDb());
			//remove any cookie this user has
			removeCookies(ap.getProfileId().toString());			
			rp.setResponse(new ResponseObject("Deactivate Account",true,"Account deactivated successfully"));
		}
		catch (Exception e)
		{
			// If an exception occurs log the error
			logger.error("Exception Message: " + e.getMessage(), e);
			rp.setResponse(new ResponseObject("Deactivate Account",false,"Account deactivated unsuccessfully"));
		}
		return rp;
	}

	
	/**
	 * keepAlive
	 * @param cookieLookup
	 * @return
	 */
	public ResponsePojo keepAlive(String cookieLookup) 
	{
		return keepAlive(cookieLookup, null);
	}
	public ResponsePojo keepAlive(String cookieLookup, Boolean bActiveAdmin) 
	{
		ResponsePojo rp = new ResponsePojo();
		if ( cookieLookup == null )
		{
			//user has been logged out/has no cookie return error
			rp.setResponse(new ResponseObject("Keep Alive",false,"User has been inactive too long, \nor is not logged in, \nor is logged in elsewhere"));
		}
		else if (null == bActiveAdmin)
		{
			//cookie was found successfully, time was updated during cookieLookup()
			rp.setResponse(new ResponseObject("Keep Alive",true,"Cookie kept alive, 15min left."));
		}
		else if (bActiveAdmin) {
			//cookie was found successfully, time was updated during cookieLookup()
			rp.setResponse(new ResponseObject("Keep Alive",true,"Active Admin: Cookie kept alive, 15min left."));			
		}
		else if (!bActiveAdmin) {
			//cookie was found successfully, time was updated during cookieLookup()
			rp.setResponse(new ResponseObject("Keep Alive",true,"Inactive Admin: Cookie kept alive, 15min left."));			
		}
		return rp;
	}
	

	/**
	 * Removes all cookies for a userid, used when logging out of infinite
	 * 
	 * @param cookieLookup userid to remove all cookies for
	 * @return Response saying if logout was successful
	 */
	public ResponsePojo removeCookies(String cookieLookup) 
	{
		ResponsePojo rp = new ResponsePojo();
		if ( null != cookieLookup )
		{
			//remove all this userids cookies			
			try
			{
				BasicDBObject dbQuery = new BasicDBObject();
				dbQuery.put("profileId", new ObjectId(cookieLookup));
				dbQuery.put("apiKey", new BasicDBObject(DbManager.exists_, false));
					// (because of this 'exists' clause, can't use the ORM)
				DbManager.getSocial().getCookies().remove(dbQuery);
			}
			catch (Exception e )
			{
				logger.error("Line: [" + e.getStackTrace()[2].getLineNumber() + "] " + e.getMessage());
				e.printStackTrace();			
			}
		}		
		rp.setResponse(new ResponseObject("Logout",true,"User logged out successfully"));
		return rp;
	}
}
