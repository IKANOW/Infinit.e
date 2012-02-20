package com.ikanow.infinit.e.workspace.model.presentation.layout
{
	import com.ikanow.infinit.e.shared.event.WorkspaceEvent;
	import com.ikanow.infinit.e.shared.model.presentation.base.PresentationModel;
	import com.ikanow.infinit.e.shared.model.vo.Widget;
	import com.ikanow.infinit.e.shared.util.CollectionUtil;
	import com.ikanow.infinit.e.shared.util.WidgetUtil;
	import flash.events.Event;
	import mx.collections.ArrayCollection;
	import mx.core.UIComponent;
	import mx.events.DragEvent;
	import mx.managers.DragManager;
	import mx.utils.ObjectUtil;
	
	/**
	 *  Workspace Layout Presentation Model
	 */
	public class WorkspaceLayoutModel extends PresentationModel
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Bindable]
		/**
		 * Data provider for layout tiles.
		 */
		public var layoutTiles:ArrayCollection;
		
		[Bindable]
		[Inject( "workspaceManager.selectedWidgets", bind = "true" )]
		public var selectedWidgets:ArrayCollection;
		
		[Inject]
		public var navigator:WorkspaceLayoutNavigator;
		
		//======================================
		// private properties 
		//======================================
		
		private var view:UIComponent;
		
		private var quadrants:Array = [ [ 0, 2 ], [ 1, 3 ] ];
		
		// 0 based index of quadrant being targeted for drop
		private var targetIndex:int = -1;
		
		// widget being dragged into workspace
		private var targetWidget:Widget;
		
		
		//======================================
		// public methods 
		//======================================
		
		public function handleDragDrop( event:DragEvent ):void
		{
			navigator.closeWidgetsDrawer();
			
			dispatcher.dispatchEvent( new WorkspaceEvent( WorkspaceEvent.ADD_WIDGET, targetWidget, targetIndex ) );
			
			killEnterFrame();
		}
		
		public function handleDragEnter( event:DragEvent ):void
		{
			var dragObj:Vector.<Object> = event.dragSource.dataForFormat( "itemsByIndex" ) as Vector.<Object>;
			targetWidget = dragObj[ 0 ] as Widget;
			
			// make sure what was dragged in is a Widget
			if ( !targetWidget )
				return;
			
			targetWidget.isBeingDragged = true;
			view = event.target as UIComponent;
			
			// copy set of currently selected widgets
			var widgetsArray:Array = [];
			
			for each ( var widget:Widget in selectedWidgets )
			{
				widgetsArray.push( widget.clone() );
			}
			
			layoutTiles = new ArrayCollection( widgetsArray );
			
			// add the widget if it doesn't exist
			if ( !CollectionUtil.doesCollectionContainItem( layoutTiles, targetWidget ) )
			{
				targetWidget.positionIndex = selectedWidgets.length;
				layoutTiles.addItem( targetWidget.clone() );
			}
			
			DragManager.acceptDragDrop( view );
			
			navigator.showView();
			
			// start monitoring drag activity
			view.addEventListener( Event.ENTER_FRAME, handleEnterFrame );
		}
		
		public function handleDragExit( event:DragEvent ):void
		{
			killEnterFrame();
		}
		
		//======================================
		// private methods 
		//======================================
		
		private function handleEnterFrame( event:Event ):void
		{
			// find target quadrant based on mouse position
			var colIndex:int = ( view.mouseX < view.width * .5 ) ? 0 : 1;
			var rowIndex:int = ( view.mouseY < view.height * .5 ) ? 0 : 1;
			
			if ( layoutTiles.length == 3 && colIndex == 0 )
				rowIndex = 0;
			
			targetIndex = ( layoutTiles.length > 2 ) ? quadrants[ colIndex ][ rowIndex ] : colIndex;
			
			if ( layoutTiles.length == 3 && targetIndex == 3 )
				targetIndex = 2;
			
			var widget:Widget = CollectionUtil.getItemById( layoutTiles, targetWidget._id ) as Widget;
			
			// if the widget is already in the right place, bail out
			if ( widget.positionIndex == targetIndex )
				return;
			
			// move widget
			WidgetUtil.moveWidget( layoutTiles, widget, targetIndex );
		}
		
		// clean up
		private function killEnterFrame():void
		{
			navigator.hideView();
			view.removeEventListener( Event.ENTER_FRAME, handleEnterFrame );
			targetIndex = -1;
			targetWidget.isBeingDragged = false;
			targetWidget = null;
			layoutTiles = null;
		}
	}
}

