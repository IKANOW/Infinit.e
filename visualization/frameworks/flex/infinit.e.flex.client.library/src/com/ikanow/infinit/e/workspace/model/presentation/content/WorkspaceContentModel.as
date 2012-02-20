package com.ikanow.infinit.e.workspace.model.presentation.content
{
	import com.ikanow.infinit.e.shared.event.WorkspaceEvent;
	import com.ikanow.infinit.e.shared.model.presentation.base.PresentationModel;
	import com.ikanow.infinit.e.shared.model.vo.Widget;
	import mx.collections.ArrayCollection;
	
	/**
	 *  Workspace Content Presentation Model
	 */
	public class WorkspaceContentModel extends PresentationModel
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Bindable]
		[Inject]
		public var navigator:WorkspaceContentNavigator;
		
		[Bindable]
		[Inject( "workspaceManager.selectedWidgets", bind = "true" )]
		public var selectedWidgets:ArrayCollection;
		
		
		//======================================
		// public methods 
		//======================================
		
		public function maximizeWidget( widget:Widget ):void
		{
			dispatcher.dispatchEvent( new WorkspaceEvent( WorkspaceEvent.MAXIMIZE_WIDGET, widget ) );
		}
		
		public function minimizeWidgets():void
		{
			dispatcher.dispatchEvent( new WorkspaceEvent( WorkspaceEvent.MINIMIZE_WIDGETS ) );
		}
	}
}

