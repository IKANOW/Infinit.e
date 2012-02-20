package com.ikanow.infinit.e.community.view.list
{
	import com.ikanow.infinit.e.shared.view.component.common.InfItemClickList;
	
	/**
	 *  Dispatched when a join community button is clicked
	 */
	[Event( name = "joinCommunity", type = "mx.events.ItemClickEvent" )]
	/**
	 *  Dispatched when a leave community button is clicked
	 */
	[Event( name = "leaveCommunity", type = "mx.events.ItemClickEvent" )]
	public class CommunityListComponent extends InfItemClickList
	{
		
		//======================================
		// constructor 
		//======================================
		
		public function CommunityListComponent()
		{
			super();
			
			this.focusEnabled = false;
		}
	}
}
