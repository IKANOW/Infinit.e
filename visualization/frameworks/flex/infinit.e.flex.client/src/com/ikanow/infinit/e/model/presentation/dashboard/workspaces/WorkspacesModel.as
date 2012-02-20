package com.ikanow.infinit.e.model.presentation.dashboard.workspaces
{
	import com.ikanow.infinit.e.shared.model.presentation.base.PresentationModel;
	
	/**
	 *  Workspaces Presentation Model
	 */
	public class WorkspacesModel extends PresentationModel
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Bindable]
		[Inject]
		public var navigator:WorkspacesNavigator;
	}
}

