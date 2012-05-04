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
	import com.ikanow.infinit.e.shared.model.vo.QueryStringRequest;
	import com.ikanow.infinit.e.shared.model.vo.ui.DialogControl;
	import flash.events.Event;
	import mx.collections.ArrayCollection;
	
	public class SetupEvent extends InfiniteEvent
	{
		
		//======================================
		// public static properties 
		//======================================
		
		public static const GET_SETUP:String = "getSetupEvent";
		
		public static const SAVE_SETUP:String = "saveSetupEvent";
		
		public static const UPDATE_SETUP:String = "updateSetupEvent";
		
		public static const GET_MODULES_ALL:String = "getModulesAllEvent";
		
		public static const GET_MODULES_USER:String = "getModulesUserEvent";
		
		public static const SET_MODULES_USER:String = "setModulesUserEvent";
		
		public static const SELECT_MODULE_FAVORITE:String = "setModuleFavoriteEvent";
		
		public static const RESET:String = "resetSetupEvent";
		
		public static const GET_WIDGET_OPTIONS:String = "getWidgetOptionsEvent";
		
		
		//======================================
		// public properties 
		//======================================
		
		public var urlParams:String;
		
		public var queryString:Object;
		
		public var userModules:String;
		
		public var moduleId:String;
		
		public var selected:Boolean;
		
		//======================================
		// constructor 
		//======================================
		
		public function SetupEvent( type:String, bubbles:Boolean = true, cancelable:Boolean = false, dialogControl:DialogControl = null, urlParams:String = null, queryString:Object = null, userModules:String = null, moduleId:String = null, selected:Boolean = false )
		{
			super( type, bubbles, cancelable, dialogControl );
			
			this.urlParams = urlParams;
			this.queryString = queryString;
			this.userModules = userModules;
			this.moduleId = moduleId;
			this.selected = selected;
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		override public function clone():Event
		{
			return new SetupEvent( type, bubbles, cancelable, dialogControl, urlParams, queryString, userModules, moduleId, selected );
		}
	}
}
