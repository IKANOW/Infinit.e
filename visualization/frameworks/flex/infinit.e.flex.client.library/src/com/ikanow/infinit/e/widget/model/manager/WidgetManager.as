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
package com.ikanow.infinit.e.widget.model.manager
{
	import com.ikanow.infinit.e.shared.model.manager.base.InfiniteManager;
	import com.ikanow.infinit.e.shared.model.vo.Setup;
	import flash.events.Event;
	import mx.collections.ArrayCollection;
	
	/**
	 * Widget Manager
	 */
	public class WidgetManager extends InfiniteManager
	{
		
		//======================================
		// public properties 
		//======================================
		
		private var _widgets:ArrayCollection;
		
		[Bindable( event = "widgetsChange" )]
		/**
		 * Widget master collection
		 */
		public function get widgets():ArrayCollection
		{
			return _widgets;
		}
		
		/**
		 * @private
		 */
		public function set widgets( value:ArrayCollection ):void
		{
			_widgets = value;
		}
		
		[Bindable]
		/**
		 * Collection of widgets that the user has
		 * chosen to view in the workspace
		 */
		public var widgetSummaries:ArrayCollection;
		
		
		//======================================
		// public methods 
		//======================================
		
		[Inject( "setupManager.setup", bind = "true" )]
		/**
		 * Set the widget summaries from the UI setup openModules
		 */
		public function setWidgetSummariesFromSetup( value:Setup ):void
		{
			if ( value )
				widgetSummaries = value.openModules;
			else
				widgetSummaries = new ArrayCollection();
		}
		
		[Inject( "setupManager.widgets", bind = "true" )]
		/**
		 * Widgets Master Collection
		 * @param value
		 */
		public function setWidgets( value:ArrayCollection ):void
		{
			widgets = value;
			
			dispatchEvent( new Event( "widgetsChange" ) );
		}
	}
}
