package com.ikanow.infinit.e.data_model.store.utils;

import java.util.List;

import com.google.gson.reflect.TypeToken;
import com.ikanow.infinit.e.data_model.store.BaseDbPojo;

public class ProjectPojo extends BaseDbPojo 
{
	// Standard static function for readability
	@SuppressWarnings("unchecked")
	static public TypeToken<List<ProjectPojo>> listType() { return new TypeToken<List<ProjectPojo>>(){}; }
	
	private String projectDataGroupId;
	private String projectOwnerId;
	//private String projectUserGroupId;
	//private ProjectGroups userGroups;
	private ProjectGroups dataGroups;
	private ProjectSources sources;
	
	public String getProjectOwnerId() {
		return projectOwnerId;
	}

	public void setProjectOwnerId(String projectOwnerId) {
		this.projectOwnerId = projectOwnerId;
	}	
	
	//TODO delete all this out
	/*public String getProjectUserGroupId() {
		return projectUserGroupId;
	}

	public void setProjectUserGroupId(String projectUserGroupId) {
		this.projectUserGroupId = projectUserGroupId;
	}

	public ProjectGroups getUserGroups() {
		return userGroups;
	}

	public void setUserGroups(ProjectGroups userGroups) {
		this.userGroups = userGroups;
	}*/

	public ProjectGroups getDataGroups() {
		return dataGroups;
	}

	public void setDataGroups(ProjectGroups dataGroups) {
		this.dataGroups = dataGroups;
	}

	public ProjectSources getSources() {
		return sources;
	}

	public void setSources(ProjectSources sources) {
		this.sources = sources;
	}

	public String getProjectDataGroupId() {
		return projectDataGroupId;
	}

	public void setProjectDataGroupId(String projectDataGroupId) {
		this.projectDataGroupId = projectDataGroupId;
	}

	public class ProjectGroups
	{
		private List<String> ids;

		public List<String> getIds() {
			return ids;
		}

		public void setIds(List<String> ids) {
			this.ids = ids;
		}
	}
	
	public class ProjectSources
	{
		private List<String> keys;
		private List<String> datagroups;
		
		public List<String> getKeys() {
			return keys;
		}
		public void setKeys(List<String> keys) {
			this.keys = keys;
		}
		public List<String> getDatagroups() {
			return datagroups;
		}
		public void setDatagroups(List<String> datagroups) {
			this.datagroups = datagroups;
		}
	}
}
