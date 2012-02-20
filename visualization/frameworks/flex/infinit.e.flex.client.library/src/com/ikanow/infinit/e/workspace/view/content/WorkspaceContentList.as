package com.ikanow.infinit.e.workspace.view.content
{
	import com.ikanow.infinit.e.shared.view.component.common.InfDragImageList;
	
	/**
	 *  Dispatched when a maximize button is clicked
	 */
	[Event( name = "maximizeWidget", type = "mx.events.ItemClickEvent" )]
	/**
	 *  Dispatched when a minimize button is clicked
	 */
	[Event( name = "minimizeWidgets", type = "mx.events.ItemClickEvent" )]
	public class WorkspaceContentList extends InfDragImageList
	{
		
		//======================================
		// constructor 
		//======================================
		
		public function WorkspaceContentList()
		{
			super();
		}
	}
}
