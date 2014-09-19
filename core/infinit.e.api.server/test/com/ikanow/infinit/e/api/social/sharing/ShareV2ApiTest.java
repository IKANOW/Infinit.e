package com.ikanow.infinit.e.api.social.sharing;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Cookie;
import org.restlet.data.Method;
import org.restlet.data.Reference;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.util.Series;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ikanow.infinit.e.data_model.api.ApiManager;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.api.social.sharing.SharePojoApiMap;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo.DocumentLocationPojo;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo.ShareCommunityPojo;

public class ShareV2ApiTest extends Object
{
	private static String test_api_key = "123qweasd";
	private ShareV2Interface shareV2Interface = new ShareV2Interface();
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception 
	{
		
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGet() throws IOException 
	{
		//2 things to test, get s specific share and search shares
		
		//first inject a share
		SharePojo share = createJSONShare();
		
		//assert the share we get has the same id as the one we created
		assertTrue(getShare(share.get_id().toString()).get_id().equals(share.get_id()));
		
		//assert our search brings up the same share
		List<SharePojo> shares = searchShares(share.getType());
		boolean found = false;
		for ( SharePojo sp : shares )
		{
			if ( sp.get_id().equals(share.get_id()))
			{
				found = true;
				break;
			}
		}
		deleteShare(share.get_id().toString());		
		
		assertTrue(found);
		
		
	}

	@Test
	public void testPostRepresentation() throws IOException 
	{
		//test post json
		SharePojo share_json = createJSONShare();
		assertTrue( share_json != null );
		
		//test post ref
		SharePojo share_ref = createRefShare();
		assertTrue( share_ref != null );
		
		//test post bin
		SharePojo share_bin = createBinShare();
		assertTrue( share_bin != null );
		
		//cleanup
		deleteShare(share_json.get_id().toString());
		deleteShare(share_ref.get_id().toString());
		deleteShare(share_bin.get_id().toString());
	}

	@Test
	public void testPutRepresentation() throws IOException 
	{
		SharePojo share_json = createJSONShare();
		
		//test update metadata
		SharePojo updated_pojo = new SharePojo();
		updated_pojo.set_id(share_json.get_id());
		updated_pojo.setTitle("new title");
		SharePojo returned_pojo = updateShare(updated_pojo);		
		assertTrue(returned_pojo.getTitle().equals(updated_pojo.getTitle()));		
		
		//test update comms
		updated_pojo = new SharePojo();
		updated_pojo.set_id(share_json.get_id());
		List<ShareCommunityPojo> communities = new ArrayList<SharePojo.ShareCommunityPojo>();
		ShareCommunityPojo scp = new ShareCommunityPojo();
		scp.set_id(new ObjectId("53ff3851e4b0db2c2b27c82e")); //test api community
		communities.add(scp);
		updated_pojo.setCommunities(communities);
		returned_pojo = updateShare(updated_pojo);
		assertTrue(returned_pojo.getCommunities().get(0).get_id().equals(new ObjectId("53ff3851e4b0db2c2b27c82e")));
		
		//test update json/ref/bin
		updated_pojo = new SharePojo();
		updated_pojo.set_id(share_json.get_id());
		updated_pojo.setShare("{\"test\":\"update\"}");
		returned_pojo = updateShare(updated_pojo);
		assertTrue(updated_pojo.getShare().length() != share_json.getShare().length());
		deleteShare(returned_pojo.get_id().toString());
	}
	
	@Test
	public void testDeleteRepresentation() throws IOException 
	{
		SharePojo share = createJSONShare();
		assertTrue(getShare(share.get_id().toString()).get_id().equals(share.get_id()));
		deleteShare(share.get_id().toString());
		SharePojo non_exist = getShare(share.get_id().toString());
		assertNull(non_exist);
	}
	
	private SharePojo updateShare(SharePojo updated_share) throws IOException 
	{
		Request request = getRequestWithCookie(Method.POST, "");		
		shareV2Interface.setRequest(request);	
		shareV2Interface.setResponse(new Response(request));
		shareV2Interface.doInit();
		
		
		String share_string = updated_share.toDb().toString();
		
		Representation entity = new StringRepresentation(share_string);
		Representation rep = shareV2Interface.put(entity);
		ResponsePojo rp = ResponsePojo.fromApi( rep.getText(), null, ResponsePojo.class);
		SharePojo share = ApiManager.mapFromApi((JsonElement)rp.getData(), SharePojo.class, new SharePojoApiMap(null)); 
		return share;
	}	
	
	private SharePojo getShare(String share_id) throws IOException
	{		
		Request request = getRequestWithCookie(Method.GET, "");		
		Map<String,Object> attributes = request.getAttributes();
		attributes.put("id", share_id);
		request.setAttributes(attributes);		
		shareV2Interface.setRequest(request);	
		shareV2Interface.setResponse(new Response(request));
		shareV2Interface.doInit();
		Representation rep = shareV2Interface.get();
		ResponsePojo rp = ResponsePojo.fromApi( rep.getText(), null, ResponsePojo.class);
		SharePojo share = ApiManager.mapFromApi((JsonObject)rp.getData(), SharePojo.class, new SharePojoApiMap(null)); 
		return share;
	}
	
	private List<SharePojo> searchShares(String type) throws IOException
	{
		Reference resource_ref = new Reference();
		resource_ref.addQueryParameter("type", type);				
		Request request = getRequestWithCookie(Method.GET, "");
		request.setResourceRef(resource_ref);		
		shareV2Interface.setRequest(request);	
		shareV2Interface.setResponse(new Response(request));
		shareV2Interface.doInit();
		Representation rep = shareV2Interface.get();
		ResponsePojo rp = ResponsePojo.fromApi( rep.getText(), null, ResponsePojo.class);
		List<SharePojo> shares = null;
		try
		{
			//might be a list
			shares = ApiManager.mapListFromApi((JsonObject)rp.getData(), SharePojo.listType(), new SharePojoApiMap(null));
		}
		catch (Exception ex)
		{
			//might be a single item
			shares = new ArrayList<SharePojo>();
			SharePojo share = ApiManager.mapFromApi((JsonObject)rp.getData(), SharePojo.class, new SharePojoApiMap(null));
			shares.add(share);
		}
		return shares;
	}

	private SharePojo createJSONShare() throws IOException
	{
		Request request = getRequestWithCookie(Method.POST, "");		
		shareV2Interface.setRequest(request);	
		shareV2Interface.setResponse(new Response(request));
		shareV2Interface.doInit();
		
		Representation entity = new StringRepresentation("{	\"title\":\"test\",\"description\":\"test desc\",\"type\":\"test1\",\"communities\":[{\"_id\":\"4c927585d591d31d7b37097a\"}],\"share\":\"{}\"}");
		Representation rep = shareV2Interface.post(entity);
		ResponsePojo rp = ResponsePojo.fromApi( rep.getText(), null, ResponsePojo.class);
		SharePojo share = ApiManager.mapFromApi((JsonElement)rp.getData(), SharePojo.class, new SharePojoApiMap(null)); 
		return share;
	}
	
	private SharePojo createRefShare() throws IOException 
	{
		Request request = getRequestWithCookie(Method.POST, "");		
		shareV2Interface.setRequest(request);	
		shareV2Interface.setResponse(new Response(request));
		shareV2Interface.doInit();
		
		SharePojo share_ref = new SharePojo();
		share_ref.setTitle("test_ref");
		share_ref.setDescription("123");
		share_ref.setType("test1");
		List<ShareCommunityPojo> communities = new ArrayList<SharePojo.ShareCommunityPojo>();
		ShareCommunityPojo scp = new ShareCommunityPojo();
		scp.set_id(new ObjectId("4c927585d591d31d7b37097a")); //this is inf system i think
		communities.add(scp);
		share_ref.setCommunities(communities);
		DocumentLocationPojo dlp = new DocumentLocationPojo();
		dlp.setDatabase("doc_metadata");
		dlp.setCollection("metadata");
		dlp.set_id(new ObjectId("53fe2c83e4b060b5b2a6e779")); //this needs to be a valid doc
		share_ref.setDocumentLocation(dlp);
		String share_string = share_ref.toDb().toString();
		
		Representation entity = new StringRepresentation(share_string);
		Representation rep = shareV2Interface.post(entity);
		ResponsePojo rp = ResponsePojo.fromApi( rep.getText(), null, ResponsePojo.class);
		SharePojo share = ApiManager.mapFromApi((JsonElement)rp.getData(), SharePojo.class, new SharePojoApiMap(null)); 
		return share;
	}

	private SharePojo createBinShare() throws IOException 
	{
		Request request = getRequestWithCookie(Method.POST, "");		
		shareV2Interface.setRequest(request);	
		shareV2Interface.setResponse(new Response(request));
		shareV2Interface.doInit();
		
		SharePojo share_bin = new SharePojo();
		share_bin.setTitle("test_bin");
		share_bin.setDescription("123");
		share_bin.setType("test1");
		List<ShareCommunityPojo> communities = new ArrayList<SharePojo.ShareCommunityPojo>();
		ShareCommunityPojo scp = new ShareCommunityPojo();
		scp.set_id(new ObjectId("4c927585d591d31d7b37097a")); //this is inf system i think
		communities.add(scp);
		share_bin.setCommunities(communities);
		String binary_data = "secret binary data";
		share_bin.setBinaryData(binary_data.getBytes());
		String share_string = share_bin.toDb().toString();
		
		Representation entity = new StringRepresentation(share_string);
		Representation rep = shareV2Interface.post(entity);
		ResponsePojo rp = ResponsePojo.fromApi( rep.getText(), null, ResponsePojo.class);
		SharePojo share = ApiManager.mapFromApi((JsonElement)rp.getData(), SharePojo.class, new SharePojoApiMap(null)); 
		return share;
	}
	
	private void deleteShare(String share_id)
	{
		Request request = getRequestWithCookie(Method.DELETE, "");		
		shareV2Interface.setRequest(request);	
		shareV2Interface.setResponse(new Response(request));
		shareV2Interface.doInit();
		Representation entity = new StringRepresentation("{\"_id\":\""+share_id+"\"}");
		shareV2Interface.delete(entity);
	}
	
	private Request getRequestWithCookie(Method method, String resourceUri)
	{
		Request request = new Request(method, resourceUri);
		request.setMethod(method);
		Series<Cookie> cookies = request.getCookies();
		cookies.add(new Cookie("infinitecookie","api:"+test_api_key));
		request.setCookies(cookies);
		return request;
	}
	
}
