package com.ikanow.infinit.e.community.model.presentation
{
	import com.ikanow.infinit.e.shared.event.CommunityEvent;
	import com.ikanow.infinit.e.shared.model.presentation.base.PresentationModel;
	import com.ikanow.infinit.e.shared.model.vo.ui.DialogControl;
	
	/**
	 *  Community Presentation Model
	 */
	public class CommunitiesModel extends PresentationModel
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Bindable]
		[Inject]
		public var navigator:CommunitiesNavigator;
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Select all communities
		 */
		public function selectAllCommunities():void
		{
			var communityEvent:CommunityEvent = new CommunityEvent( CommunityEvent.SELECT_ALL_COMMUNITIES );
			communityEvent.dialogControl = DialogControl.create( false );
			dispatcher.dispatchEvent( communityEvent );
		}
		
		/**
		 * Select no communities
		 */
		public function selectNoCommunities():void
		{
			var communityEvent:CommunityEvent = new CommunityEvent( CommunityEvent.SELECT_NO_COMMUNITIES );
			communityEvent.dialogControl = DialogControl.create( false );
			dispatcher.dispatchEvent( communityEvent );
		}
	}
}

