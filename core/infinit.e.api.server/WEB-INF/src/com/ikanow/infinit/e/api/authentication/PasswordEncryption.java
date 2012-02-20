/**
 * 
 */
package com.ikanow.infinit.e.api.authentication;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.log4j.Logger;
import org.elasticsearch.common.Hex;

import com.ikanow.infinit.e.data_model.store.DbManager;
import com.ikanow.infinit.e.data_model.store.social.authentication.AuthenticationPojo;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import sun.misc.BASE64Encoder;

/**
 * Class used to encrypt and decrypt passwords held in the environment
 * @author cmorgan
 *
 */
public class PasswordEncryption {
	/**
	 * Private variables
	 */
	//private BCrypt encryptor = null;
	//private String salt = BCrypt.gensalt();
	private static final Logger logger = Logger.getLogger(PasswordEncryption.class);
	/**
	 *  Constructor for password encryption class
	 */
	public PasswordEncryption() {
		//encryptor = new BCrypt();
	}
	/**
	 *  Encrypt the password
	 * @throws NoSuchAlgorithmException 
	 * @throws UnsupportedEncodingException 
	 */
	public static String encrypt(String password) throws NoSuchAlgorithmException, UnsupportedEncodingException 
	{	
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		md.update(password.getBytes("UTF-8"));		
		return (new BASE64Encoder()).encode(md.digest());	
	}
	/**
	 *  Check the password
	 * @throws UnsupportedEncodingException 
	 * @throws NoSuchAlgorithmException 
	 */
	public static boolean checkPassword(String plainPassword, String encryptedPassword) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		return encryptedPassword.equals(encrypt(plainPassword));
		//return encryptor.checkpw(plainPassword, encryptedPassword);
	}	
	
	/**
	 * Checks if a user is in authentication DB
	 * and returns their userid if successful.
	 * Returns null otherwise.
	 * 
	 * @param username
	 * @param userEncryptPword
	 * @return
	 */
	public static AuthenticationPojo validateUser(String username, String userEncryptPword)
	{
		return validateUser(username, userEncryptPword, true);
	}
	public static AuthenticationPojo validateUser(String username, String userPword, boolean bPasswdEncrypted)
	{
		try
		{
			//Get user auth on username
			BasicDBObject query = new BasicDBObject();
			query.put("username", username);
			DBObject dbo = DbManager.getSocial().getAuthentication().findOne(query);
			if (dbo != null )
			{			
				//	check if pwords match
				AuthenticationPojo ap = AuthenticationPojo.fromDb(dbo, AuthenticationPojo.class);
				if ( ap.getPassword().equals(userPword))
					return ap;
				else if (!bPasswdEncrypted) {
					if ( ap.getPassword().equals(encrypt(userPword)))
						return ap;					
				}
			}
		}
		catch (Exception e)
		{
			// If an exception occurs log the error
			logger.error("Messaging Exception Message: " + e.getMessage(), e);
		}
		
		return null;
	}
	
	public static String md5checksum(String toHash)
	{
		try
		{
			MessageDigest m = MessageDigest.getInstance("MD5");
			m.reset();
			m.update(toHash.getBytes(Charset.forName("UTF8")));
			byte[] digest = m.digest();
			return new String(Hex.encodeHex(digest));			
		}
		catch (Exception ex)
		{
			return toHash;
		}		
	}
}
