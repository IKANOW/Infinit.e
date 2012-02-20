package com.ikanow.infinit.e.model.presentation.dashboard.workspaces.body
{
	import com.ikanow.infinit.e.shared.model.presentation.base.PresentationModel;
	
	/**
	 *  Workspaces Body Presentation Model
	 */
	public class WorkspacesBodyModel extends PresentationModel
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Bindable]
		[Inject]
		public var navigator:WorkspacesBodyNavigator;
	}
}

