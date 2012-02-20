package com.ikanow.infinit.e.data_model.api.social.community;

import com.ikanow.infinit.e.data_model.api.BaseApiPojo;

public class CommunityApprovalPojo extends BaseApiPojo
{
	public boolean approved = false;
	
	public CommunityApprovalPojo()
	{
		
	}
	
	public CommunityApprovalPojo(boolean _approved)
	{
		approved = _approved;
	}
}
