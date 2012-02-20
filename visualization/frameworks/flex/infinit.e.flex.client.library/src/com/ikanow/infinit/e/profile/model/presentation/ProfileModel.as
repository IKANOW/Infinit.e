package com.ikanow.infinit.e.profile.model.presentation
{
	import com.ikanow.infinit.e.shared.model.presentation.base.PresentationModel;
	import com.ikanow.infinit.e.shared.model.vo.User;
	
	/**
	 *  Profile Presentation Model
	 */
	public class ProfileModel extends PresentationModel
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Bindable]
		/**
		 * The current user
		 */
		public var currentUser:User;
		
		[Bindable]
		[Inject]
		public var navigator:ProfileNavigator;
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Person Get response from server
		 * @param value
		 */
		[Inject( "profileManager.currentUser", bind = "true" )]
		public function setCurrentUser( value:User ):void
		{
			currentUser = value;
		}
	}
}

