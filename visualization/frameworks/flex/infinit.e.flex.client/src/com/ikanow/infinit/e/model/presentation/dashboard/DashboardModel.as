package com.ikanow.infinit.e.model.presentation.dashboard
{
	import com.ikanow.infinit.e.shared.model.presentation.base.PresentationModel;
	
	/**
	 *  Dashboard Presentation Model
	 */
	public class DashboardModel extends PresentationModel
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Bindable]
		[Inject]
		public var navigator:DashboardNavigator;
	}
}

