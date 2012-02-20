package com.ikanow.infinit.e.community.model.presentation.request
{
	import com.ikanow.infinit.e.shared.event.CommunityEvent;
	import com.ikanow.infinit.e.shared.model.presentation.base.PresentationModel;
	import com.ikanow.infinit.e.shared.model.vo.Community;
	import com.ikanow.infinit.e.shared.model.vo.CommunityApproval;
	import com.ikanow.infinit.e.shared.model.vo.ui.DialogControl;
	import flash.events.Event;
	import mx.resources.ResourceManager;
	
	/**
	 *  Community Request Presentation Model
	 */
	public class CommunityRequestModel extends PresentationModel
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Bindable]
		[Inject]
		public var navigator:CommunityRequestNavigator;
		
		[Bindable]
		[Inject( "communityManager.selectedCommunity", bind = "true" )]
		/**
		 * The selected community
		 */
		public var selectedCommunity:Community;
		
		[Bindable]
		/**
		 * Community approval for join
		 */
		public var communityApproval:CommunityApproval;
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Join a community
		 * @param communityId
		 */
		public function joinCommunity():void
		{
			navigator.showRequestingJoin();
			
			var communityEvent:CommunityEvent = new CommunityEvent( CommunityEvent.JOIN_COMMUNITY );
			communityEvent.community = selectedCommunity;
			communityEvent.communityID = selectedCommunity._id;
			trace( "join community id - " + selectedCommunity._id );
			communityEvent.dialogControl = DialogControl.create( false, ResourceManager.getInstance().getString( 'infinite', 'communityService.joinCommunity', [ selectedCommunity.name ] ) );
			dispatcher.dispatchEvent( communityEvent );
		}
		
		/**
		 * Leave a community
		 * @param communityId
		 */
		public function leaveCommunity():void
		{
			var communityEvent:CommunityEvent = new CommunityEvent( CommunityEvent.LEAVE_COMMUNITY );
			communityEvent.community = selectedCommunity;
			//communityEvent.communityID = selectedCommunity._id;
			communityEvent.communityID = "4da5eb037bd2e8e23f229e99";
			communityEvent.dialogControl = DialogControl.create( false, ResourceManager.getInstance().getString( 'infinite', 'communityService.leaveCommunity', [ selectedCommunity.name ] ) );
			dispatcher.dispatchEvent( communityEvent );
		}
		
		/**
		 * Join Community Response
		 * @param value
		 */
		[Inject( "communityManager.communityApproval", bind = "true" )]
		public function setCommunityApproval( value:CommunityApproval ):void
		{
			if ( value )
			{
				communityApproval = value;
				
				if ( communityApproval.approved )
					navigator.showRequestJoinApproved();
				else
					navigator.showRequestJoinPending();
			}
		}
	}
}

