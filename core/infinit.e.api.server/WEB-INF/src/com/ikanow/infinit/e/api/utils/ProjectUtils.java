package com.ikanow.infinit.e.api.utils;

import com.ikanow.infinit.e.api.social.sharing.ShareHandler;
import com.ikanow.infinit.e.data_model.api.BaseApiPojo;
import com.ikanow.infinit.e.data_model.api.ResponsePojo;
import com.ikanow.infinit.e.data_model.api.knowledge.AdvancedQueryPojo;
import com.ikanow.infinit.e.data_model.api.knowledge.AdvancedQueryPojo.QueryInputPojo;
import com.ikanow.infinit.e.data_model.store.social.sharing.SharePojo;
import com.ikanow.infinit.e.data_model.store.utils.ProjectPojo;

public class ProjectUtils {
	public static final String query_param = "project_id";
	private static InMemoryCache<ProjectPojo> cache = new InMemoryCache<ProjectPojo>(5000); //5s

	public static ProjectPojo authenticate(String project_id, String user_id) throws Exception
	{
		//check cache first
		ProjectPojo project = cache.getEntry(getCacheKey(project_id, user_id));
		if ( project != null )
			return project;
		//verify user can get share, when we grab the communities it will do the other authentication
		//we just assume the project permissions are set up correctly (we don't handle that part currently)
		ResponsePojo response = new ShareHandler().getShare(user_id, project_id, true);
		SharePojo share = (SharePojo)response.getData();
		if ( share != null )
		{
			String json = share.getShare();
			if ( json != null )
			{
				project = BaseApiPojo.getDefaultBuilder().create().fromJson(json, ProjectPojo.class);
				if ( project != null )
					return cache.addEntry(getCacheKey(project_id, user_id), project);
			}
		}
		
		//if it falls through, something was wrong, (could not find/get share (doesn't exist/auth), share was not a projectpojo?
		//TODO throw a specific exception
		throw new Exception("Could not authenticate user against project_id");
	}
	
	public static String filterQuery(String project_id, AdvancedQueryPojo query_pojo, String user_id) throws Exception {
		ProjectPojo project = authenticate(project_id, user_id);		
		
		//change the query pojo to use the projects source key
		/*StringBuilder communityIdStr = new StringBuilder();
		List<ObjectId> data_ids = new ArrayList<ObjectId>();
		for ( String id : project.getDataGroups().getIds() )
		{
			data_ids.add(new ObjectId(id));
			communityIdStr.append(id).append(",");
		}
		if ( communityIdStr.length() > 0 )
			communityIdStr.deleteCharAt(communityIdStr.length()-1); //remove last comma
			*/
		
		//query_pojo.communityIds = data_ids; //NOTE: these aren't used in the query logic, so I think we are okay not bothering
		query_pojo.input = new QueryInputPojo();
		query_pojo.input.srcInclude = true;
		query_pojo.input.sources = project.getSources().getKeys();
		
		//return the community ids of the project
		return getCommunityIdStr(project); 
	}
	
	public static String getCommunityIdStr(ProjectPojo project)
	{
		//TODO if I just append the projects datagroup will that
		//allow all the usergroups under it to be searched?
		StringBuilder communityIdStr = new StringBuilder(project.getProjectDataGroupId());
		for ( String id : project.getDataGroups().getIds() )
		{
			communityIdStr.append(",").append(id);
		}
		
		
		
		/*
		for ( String id : project.getUserGroups().getIds() )
		{
			communityIdStr.append(id).append(",");
		}
		if ( communityIdStr.length() > 0 )
			communityIdStr.deleteCharAt(communityIdStr.length()-1); //remove last comma
		*/
		return communityIdStr.toString();
	}
	
	public static String getCacheKey(String project_id, String user_id)
	{
		return project_id + "_" + user_id;
	}

	public static InMemoryCache<ProjectPojo>.CacheStats getCacheStats() {		
		return cache.getCacheStats();
	}
}
