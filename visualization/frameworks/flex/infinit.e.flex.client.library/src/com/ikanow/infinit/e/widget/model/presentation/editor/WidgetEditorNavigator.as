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
package com.ikanow.infinit.e.widget.model.presentation.editor
{
	import com.ikanow.infinit.e.shared.model.constant.NavigationConstants;
	import com.ikanow.infinit.e.shared.model.constant.types.NavigationItemTypes;
	import com.ikanow.infinit.e.shared.model.presentation.base.Navigator;
	
	/**
	 * Widget Editor Navigator
	 */
	public class WidgetEditorNavigator extends Navigator
	{
		
		//======================================
		// public static properties 
		//======================================
		
		public static const WIDGET_LIST_ID:String = NavigationConstants.WIDGET_LIST_ID;
		
		
		//======================================
		// public properties 
		//======================================
		
		/**
		 * Presentation Model
		 */
		[Bindable]
		[Inject]
		public var model:WidgetEditorModel;
		
		//======================================
		// constructor 
		//======================================
		
		/**
		 * Constructor
		 */
		public function WidgetEditorNavigator()
		{
			navigatorId = NavigationConstants.WIDGET_EDITOR_ID;
			parentNavigatorId = NavigationConstants.WORKSPACES_BODY_WIDGETS_ID;
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Show List View
		 */
		public function showListView():void
		{
			navigateById( WIDGET_LIST_ID );
		}
	}
}
