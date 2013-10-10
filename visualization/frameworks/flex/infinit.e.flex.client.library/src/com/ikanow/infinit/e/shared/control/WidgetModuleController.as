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
package com.ikanow.infinit.e.shared.control
{
	import com.ikanow.infinit.e.shared.control.base.InfiniteController;
	import com.ikanow.infinit.e.shared.event.WidgetEvent;
	import com.ikanow.infinit.e.shared.model.manager.SetupManager;
	import com.ikanow.infinit.e.shared.model.manager.WidgetModuleManager;
	import com.ikanow.infinit.e.shared.model.vo.Widget;
	import com.ikanow.infinit.e.widget.library.widget.IWidget;
	import com.ikanow.infinit.e.widget.model.presentation.list.WidgetListModel;
	
	public class WidgetModuleController extends InfiniteController
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Inject]
		public var widgetModuleManager:WidgetModuleManager;
		
		[Inject]
		public var setupManager:SetupManager;
		
		
		//======================================
		// public methods 
		//======================================
		
		[EventHandler( event = "WidgetEvent.CLEAR_WIDGET_FILTERS" )]
		/**
		 * Clear the widget filters
		 * @param event
		 */
		public function clearWidgetFilters( event:WidgetEvent ):void
		{
			widgetModuleManager.clearWidgetFilters();
		}
		
		[EventHandler( event = "WidgetEvent.EXPORT_PDF" )]
		/**
		 * Export Widgets to PDF
		 * @param event
		 */
		public function exportPDF( event:WidgetEvent ):void
		{
			widgetModuleManager.exportPDF();
		}
		
		[EventHandler( "WidgetEvent.WIDGET_LOADED", properties = "widget,widgetUrl" )]
		public function handleWidgetLoaded( widget:IWidget, widgetUrl:String ):void
		{
			widgetModuleManager.loadWidget( widget, widgetUrl );
		}
		
		[EventHandler( "WorkspaceEvent.REPLACE_WIDGET", properties = "widget" )]
		public function handleWidgetReplaced( widget:Widget ):void
		{
			widgetModuleManager.unloadWidget( widget );
		}
		
		[EventHandler( "WorkspaceEvent.REMOVE_WIDGET", properties = "widget" )]
		public function handleWidgetUnloaded( widget:Widget ):void
		{
			widgetModuleManager.unloadWidget( widget );
		}
		
		[EventHandler( event = "WidgetEvent.RESET" )]
		/**
		 * Reset Widget Modules
		 * @param event
		 */
		public function resetWidgetModules( event:WidgetEvent ):void
		{
			widgetModuleManager.reset();
		}
		
		[EventHandler( "WidgetEvent.SORT_WIDGETS" )]
		public function sortWidgets():void
		{
			setupManager.sortWidgets();
		}
	}
}
