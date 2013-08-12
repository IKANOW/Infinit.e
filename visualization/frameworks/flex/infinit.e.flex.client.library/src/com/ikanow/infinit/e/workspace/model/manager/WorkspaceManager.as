/*******************************************************************************
 * Copyright 2012, The Infinit.e Open Source Project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.ikanow.infinit.e.workspace.model.manager
{
	import com.ikanow.infinit.e.shared.event.NavigationEvent;
	import com.ikanow.infinit.e.shared.event.SetupEvent;
	import com.ikanow.infinit.e.shared.model.constant.Constants;
	import com.ikanow.infinit.e.shared.model.constant.NavigationConstants;
	import com.ikanow.infinit.e.shared.model.constant.WidgetConstants;
	import com.ikanow.infinit.e.shared.model.manager.base.InfiniteManager;
	import com.ikanow.infinit.e.shared.model.vo.Widget;
	import com.ikanow.infinit.e.shared.model.vo.ui.DialogControl;
	import com.ikanow.infinit.e.shared.util.CollectionUtil;
	import com.ikanow.infinit.e.shared.util.WidgetUtil;
	import flash.sampler.NewObjectSample;
	import mx.collections.ArrayCollection;
	import mx.collections.ListCollectionView;
	import mx.collections.Sort;
	import mx.collections.SortField;
	import mx.controls.AdvancedDataGrid;
	import mx.events.CollectionEvent;
	import mx.resources.ResourceManager;
	
	public class WorkspaceManager extends InfiniteManager
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Bindable]
		[Inject( "setupManager.widgets", bind = "true" )]
		public var widgets:ArrayCollection;
		
		[Bindable]
		[Inject( "setupManager.selectedWidgets", bind = "true" )]
		public var selectedWidgets:ArrayCollection;
		
		[Bindable]
		public var selectedWidgetsSorted:ListCollectionView;
		
		[Bindable]
		public var maximizedWidget:Widget;
		
		[Bindable]
		public var maximized:Boolean;
		
		[Bindable]
		public var workspaceIsFull:Boolean;
		
		
		//======================================
		// public methods 
		//======================================
		
		public function addWidget( targetWidget:Widget, index:int ):void
		{
			var widget:Widget = CollectionUtil.getItemById( widgets, targetWidget._id ) as Widget;
			
			if ( !workspaceIsFull || selectedWidgets.contains( widget ) )
			{
				// set flag
				widget.selected = true;
				widget.favorite = true;
				
				// add the widget if it doesn't exist
				if ( !selectedWidgets.contains( widget ) )
				{
					widget.positionIndex = selectedWidgets.length;
					selectedWidgets.addItem( widget );
				}
				
				// use the next available index if the widget was
				// added with the '+' button instead of drag/drop
				index = ( index == WidgetConstants.USE_NEXT_AVAILABLE_INDEX ) ? widget.positionIndex : index;
				
				// move widget
				if ( selectedWidgets.length > 1 )
					WidgetUtil.moveWidget( selectedWidgets, widget, index );
				
				// minimize the widgets
				minimizeWidgets();
				
				// update the selected widgets count
				updateSelectedWidgetsCount();
			}
			else
			{
				trace( "WORKSPACE IS FULL. CANNOT ADD." );
			}
			
			// create the selected widgets sorted list collection view
			createSelectedWidgetsSorted();
			
			// save the workspace
			saveWorkspace();
		}
		
		[PostConstruct]
		public function init():void
		{
			selectedWidgets.addEventListener( CollectionEvent.COLLECTION_CHANGE, onSelectedWidgetsChanged );
			
			updateSelectedWidgetsCount();
		}
		
		public function maximizeWidget( targetWidget:Widget ):void
		{
			for each ( var widget:Widget in selectedWidgets )
			{
				widget.maximized = ( widget._id == targetWidget._id );
			}
			
			maximizedWidget = targetWidget;
			maximized = true;
			
			// create the selected widgets sorted list collection view
			createSelectedWidgetsSorted();
			
			var navigationEvent:NavigationEvent = new NavigationEvent( NavigationEvent.NAVIGATE_BY_ID );
			navigationEvent.navigationItemId = NavigationConstants.WORKSPACE_CONTENT_MAXIMIZED_ID;
			dispatcher.dispatchEvent( navigationEvent );
		}
		
		public function minimizeWidgets():void
		{
			var navigationEvent:NavigationEvent = new NavigationEvent( NavigationEvent.NAVIGATE_BY_ID );
			navigationEvent.navigationItemId = NavigationConstants.WORKSPACE_CONTENT_TILES_ID;
			dispatcher.dispatchEvent( navigationEvent );
			
			for each ( var widget:Widget in selectedWidgets )
			{
				widget.maximized = false;
			}
			
			maximizedWidget = null;
			maximized = false;
			selectedWidgetsSorted = null;
		}
		
		public function removeWidget( targetWidget:Widget, saveTheWorkspace:Boolean = true ):void
		{
			var widget:Widget = CollectionUtil.getItemById( widgets, targetWidget._id ) as Widget;
			var index:int = selectedWidgets.getItemIndex( widget );
			var position:int = widget.positionIndex;
			
			if ( index > -1 )
			{
				selectedWidgets.removeItemAt( index );
				widget.selected = false;
				WidgetUtil.clenupWidgetPositions( selectedWidgets, position );
			}
			
			// minimize the widgets
			minimizeWidgets();
			
			// create the selected widgets sorted list collection view
			createSelectedWidgetsSorted();
			
			// save the workspace
			if ( saveTheWorkspace )
				saveWorkspace();
		}
		
		/**
		 * Reset
		 * Used to reset on logout
		 */
		public function reset():void
		{
			minimizeWidgets();
			widgets = new ArrayCollection();
			selectedWidgets = new ArrayCollection();
			workspaceIsFull = false;
			selectedWidgetsSorted = null;
		}
		
		//======================================
		// protected methods 
		//======================================
		
		protected function createSelectedWidgetsSorted():void
		{
			// sort the widgets by the position index
			selectedWidgetsSorted = new ListCollectionView( selectedWidgets );
			var sortOrderSortField:SortField = new SortField();
			sortOrderSortField.name = Constants.POSITION_INDEX_PROPERTY;
			sortOrderSortField.numeric = true;
			selectedWidgetsSorted.sort = new Sort();
			selectedWidgetsSorted.sort.fields = [ sortOrderSortField ];
			selectedWidgetsSorted.refresh();
		}
		
		protected function onSelectedWidgetsChanged( event:CollectionEvent ):void
		{
			workspaceIsFull = selectedWidgets.length == 4;
			
			updateSelectedWidgetsCount();
		}
		
		protected function saveWorkspace():void
		{
			// save the ui setup
			var setupEvent:SetupEvent = new SetupEvent( SetupEvent.SAVE_SETUP );
			setupEvent.dialogControl = DialogControl.create( false, ResourceManager.getInstance().getString( 'infinite', 'setupService.saveSetup' ) );
			dispatcher.dispatchEvent( setupEvent );
			
			// update the widgets count
			updateSelectedWidgetsCount();
		}
		
		protected function updateSelectedWidgetsCount():void
		{
			for each ( var widget:Widget in selectedWidgets )
			{
				widget.parentCollectionCount = selectedWidgets.length;
			}
		}
	}
}
