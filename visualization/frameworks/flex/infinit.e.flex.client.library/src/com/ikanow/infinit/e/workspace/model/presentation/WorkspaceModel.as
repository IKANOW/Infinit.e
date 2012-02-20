package com.ikanow.infinit.e.workspace.model.presentation
{
	import com.ikanow.infinit.e.shared.model.presentation.base.PresentationModel;
	import com.ikanow.infinit.e.shared.model.vo.Widget;
	import mx.collections.ArrayCollection;
	import mx.events.DragEvent;
	
	/**
	 *  Workspace Presentation Model
	 */
	public class WorkspaceModel extends PresentationModel
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Bindable]
		[Inject]
		public var navigator:WorkspaceNavigator;
		
		[Bindable]
		[Inject( "workspaceManager.selectedWidgets", bind = "true" )]
		public var selectedWidgets:ArrayCollection;
		
		[Bindable]
		[Inject( "workspaceManager.workspaceIsFull", bind = "true" )]
		public var workspaceIsFull:Boolean;
		
		
		//======================================
		// public methods 
		//======================================
		
		public function handleDragEnter( event:DragEvent ):void
		{
			var dragObj:Vector.<Object> = event.dragSource.dataForFormat( "itemsByIndex" ) as Vector.<Object>;
			var widget:Widget = dragObj[ 0 ] as Widget;
			
			// make sure either there is capacity in the workspace
			// or an existing widget is being moved
			if ( !workspaceIsFull || selectedWidgets.contains( widget ) )
			{
				navigator.showLayoutView();
				navigator.closeWidgetDrawer();
			}
		}
	}
}

