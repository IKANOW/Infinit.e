package infinit.e.web.util;

import java.net.*;
import java.security.MessageDigest;
import java.util.Date;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.json.JSONArray;
import sun.misc.BASE64Encoder;

public class Utils 
{
	
	/**
	 * encrypt
	 * Encrypts a string value using SHA-256
	 * @param value
	 * @return
	 */
	public static String encrypt(String value) 
	{	
		try
		{
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(value.getBytes("UTF-8"));	
			return URLEncoder.encode((new BASE64Encoder()).encode(md.digest()), "UTF-8");
		} 
		catch (Exception e)
		{
			return null;
		}
	}
	
	
	/**
	 * getUserId
	 * @param json
	 * @return
	 */
	public static String getUserId(String json)
	{
		try
		{
			JSONObject jsonResponse = new JSONObject(json);
			JSONObject response = jsonResponse.getJSONObject("response");
			if (response.getString("success").equalsIgnoreCase("true"))
			{
				return jsonResponse.getJSONObject("data").getString("_id");
			}
		}
		catch (Exception e)
		{
			return null;
		}
		return null;
	}
	
	
	/**
	 * getUserDisplayName
	 * @param json
	 * @return
	 */
	public static String getUserDisplayName(String json)
	{
		try
		{
			JSONObject jsonResponse = new JSONObject(json);
			JSONObject response = jsonResponse.getJSONObject("response");
			if (response.getString("success").equalsIgnoreCase("true"))
			{
				return jsonResponse.getJSONObject("data").getString("displayName");
			}
		}
		catch (Exception e)
		{
			return null;
		}
		return null;
	}
	
	
	/**
	 * getUserDisplayName
	 * @param json
	 * @return
	 */
	public static String[] getUserInfo(String json)
	{
		String[] user = new  String[5];
		try
		{
			JSONObject jsonResponse = new JSONObject(json);
			JSONObject response = jsonResponse.getJSONObject("response");
			if (response.getString("success").equalsIgnoreCase("true"))
			{
				user[0] = jsonResponse.getJSONObject("data").getString("_id"); 
				user[1] = jsonResponse.getJSONObject("data").getString("email");
				user[2] = jsonResponse.getJSONObject("data").getString("firstName");
				user[3] = jsonResponse.getJSONObject("data").getString("lastName");
				user[4] = jsonResponse.getJSONObject("data").getString("displayName");
			}
		}
		catch (Exception e)
		{
			return null;
		}
		return user;
	}
	
	
	/**
	 * createNewSource
	 * 
	 * @return
	 */
	public static JSONObject createNewSource(String userId, String personString)
	{
		try
		{
			JSONObject person = new JSONObject(new JSONObject(personString).getString("data"));
			JSONObject share = new JSONObject();
			JSONObject source = new JSONObject();
			
			// 
			share.put("_id", new ObjectId());
			share.put("created", new Date());
			share.put("modified", new Date());
			share.put("type", "source");
			
			// Owner object
			JSONObject owner = new JSONObject();
			owner.put("_id", new ObjectId(userId));
			owner.put("email", person.getString("email"));
			owner.put("displayName", person.getString("displayName"));
			share.put("owner", owner);
			
			//
			source.put("_id", new ObjectId());
			source.put("ownerId", userId);
			source.put("title", "");
			source.put("description", "");
			source.put("url", "");
			source.put("extractType", "");
			source.put("mediaType", "");
			source.put("created", new Date());
			source.put("modified", new Date());
			source.put("harvestBadSource", false);
			source.put("isApproved", false);
			source.put("isPublic", true);
			
			// 
			share.put("share", source);
			
			// Community object
			JSONArray communities = new JSONArray();
			JSONObject community = new JSONObject();
			community.put("_id", person.get("_id"));
			community.put("name", person.getString("displayName") + "'s Personal Community");
			communities.put(community);
			share.put("communities", communities);
			
			return share;
		}
		catch (Exception e)
		{
			return null;
		}
	}
	
	
}
