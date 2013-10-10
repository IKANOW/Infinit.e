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
package com.ikanow.infinit.e.shared.event
{
	import com.ikanow.infinit.e.shared.event.base.InfiniteEvent;
	import com.ikanow.infinit.e.shared.model.vo.ui.DialogControl;
	import com.ikanow.infinit.e.widget.library.widget.IWidget;
	import flash.events.Event;
	
	public class WidgetEvent extends InfiniteEvent
	{
		
		//======================================
		// public static properties 
		//======================================
		
		public static const WIDGET_LOADED:String = "widgetLoaded";
		
		public static const WIDGET_UNLOADED:String = "widgetUnloaded";
		
		public static const SORT_WIDGETS:String = "sortWidgetsEvent";
		
		public static const EXPORT_PDF:String = "exportPDFWidgetsEvent";
		
		public static const CLEAR_WIDGET_FILTERS:String = "clearWidgetFiltersEvent";
		
		public static const RESET:String = "resetWidgetsEvent";
		
		
		//======================================
		// public properties 
		//======================================
		
		public var widget:IWidget;
		
		public var widgetUrl:String;
		
		//======================================
		// constructor 
		//======================================
		
		public function WidgetEvent( type:String, widget:IWidget = null, widgetUrl:String = null, dialogControl:DialogControl = null )
		{
			super( type, true, false, dialogControl );
			
			this.widget = widget;
			this.widgetUrl = widgetUrl;
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		override public function clone():Event
		{
			return new WidgetEvent( type, widget, widgetUrl, dialogControl );
		}
	}
}
