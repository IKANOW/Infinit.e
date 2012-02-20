package com.ikanow.infinit.e.welcome.model.presentation
{
	import com.ikanow.infinit.e.shared.model.presentation.base.PresentationModel;
	
	/**
	 *  Welcome Presentation Model
	 */
	public class WelcomeModel extends PresentationModel
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Bindable]
		[Inject]
		public var navigator:WelcomeNavigator;
	}
}

