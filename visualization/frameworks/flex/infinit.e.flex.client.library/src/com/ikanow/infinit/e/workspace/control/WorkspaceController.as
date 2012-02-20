package com.ikanow.infinit.e.workspace.control
{
	import com.ikanow.infinit.e.shared.event.WorkspaceEvent;
	import com.ikanow.infinit.e.shared.model.vo.Widget;
	import com.ikanow.infinit.e.workspace.model.manager.WorkspaceManager;
	
	public class WorkspaceController
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Inject]
		public var workspaceManager:WorkspaceManager;
		
		
		//======================================
		// public methods 
		//======================================
		
		[EventHandler( "WorkspaceEvent.ADD_WIDGET", properties = "widget, index" )]
		public function addWidget( widget:Widget, index:int ):void
		{
			// add to collection at specified index
			workspaceManager.addWidget( widget, index );
		}
		
		[EventHandler( "WorkspaceEvent.MAXIMIZE_WIDGET", properties = "widget" )]
		public function maximizeWidget( widget:Widget ):void
		{
			// maximize a widget
			workspaceManager.maximizeWidget( widget );
		}
		
		[EventHandler( "WorkspaceEvent.MINIMIZE_WIDGETS" )]
		public function minimizeWidgets():void
		{
			// minimize a widget
			workspaceManager.minimizeWidgets();
		}
		
		[EventHandler( "WorkspaceEvent.REMOVE_WIDGET", properties = "widget" )]
		public function removeWidget( widget:Widget ):void
		{
			// remove from collection
			workspaceManager.removeWidget( widget );
		}
		
		[EventHandler( event = "WorkspaceEvent.RESET" )]
		/**
		 * Reset Workspaces
		 * @param event
		 */
		public function resetWorkspaces( event:WorkspaceEvent ):void
		{
			workspaceManager.reset();
		}
	}
}
