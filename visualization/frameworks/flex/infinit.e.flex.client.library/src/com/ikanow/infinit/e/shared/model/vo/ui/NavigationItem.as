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
package com.ikanow.infinit.e.shared.model.vo.ui
{
	import flash.events.EventDispatcher;
	
	/**
	 * Navigation Item
	 * @private
	 */
	[Bindable]
	public class NavigationItem extends EventDispatcher implements INavigationItem
	{
		
		//======================================
		// public properties 
		//======================================
		
		private var _navigatorId:String = "";
		
		public function get navigatorId():String
		{
			return _navigatorId;
		}
		
		/**
		 * Navigator ID
		 * id of the parent Navigator
		 * This property is used to determine which Navigator
		 * receives the navigation change.
		 * @param value
		 */
		public function set navigatorId( value:String ):void
		{
			_navigatorId = value;
		}
		
		private var _id:String = "";
		
		public function get id():String
		{
			return _id;
		}
		
		/**
		 * ID
		 * id of the navigation item
		 */
		public function set id( value:String ):void
		{
			_id = value;
		}
		
		private var _type:String = "";
		
		public function get type():String
		{
			return _type;
		}
		
		/**
		 * Type of the navigation item
		 * module, view, link, dialog, action, state
		 * @param value
		 */
		public function set type( value:String ):void
		{
			_type = value;
		}
		
		private var _state:String = "";
		
		public function get state():String
		{
			return _state;
		}
		
		/**
		 * State - the associated view state
		 * @param value
		 */
		public function set state( value:String ):void
		{
			_state = value;
		}
		
		private var _label:String = "";
		
		public function get label():String
		{
			return _label;
		}
		
		/**
		 * Label - text to display
		 * @param value
		 */
		public function set label( value:String ):void
		{
			_label = value;
		}
		
		private var _description:String = "";
		
		public function get description():String
		{
			return _description;
		}
		
		/**
		 * Description - additional text to display
		 * @param value
		 */
		public function set description( value:String ):void
		{
			_description = value;
		}
		
		private var _toolTip:String = "";
		
		public function get toolTip():String
		{
			return _toolTip;
		}
		
		/**
		 * Tool Tip - text to display in the tool tip
		 * @param value
		 */
		public function set toolTip( value:String ):void
		{
			_toolTip = value;
		}
		
		private var _styleName:String = "";
		
		public function get styleName():String
		{
			return _styleName;
		}
		
		/**
		 * Style Name - styles for individual buttons
		 * @param value
		 */
		public function set styleName( value:String ):void
		{
			_styleName = value;
		}
		
		private var _itemCount:int = 0;
		
		public function get itemCount():int
		{
			return _itemCount;
		}
		
		/**
		 * Item Count - number of associated items
		 * @param value
		 */
		public function set itemCount( value:int ):void
		{
			_itemCount = value;
		}
		
		private var _icon:Class = null;
		
		public function get icon():Class
		{
			return _icon;
		}
		
		/**
		 * Icon - image to display as icon
		 * @param value
		 */
		public function set icon( value:Class ):void
		{
			_icon = value;
		}
		
		private var _altIcon:Class = null;
		
		public function get altIcon():Class
		{
			return _altIcon;
		}
		
		/**
		 * altIcon - alternate image to display as icon
		 * @param value
		 */
		public function set altIcon( value:Class ):void
		{
			_altIcon = value;
		}
		
		private var _selected:Boolean = false;
		
		public function get selected():Boolean
		{
			return _selected;
		}
		
		/**
		 * Navigation Item is Selected
		 * @param value
		 */
		public function set selected( value:Boolean ):void
		{
			_selected = value;
		}
		
		private var _enabled:Boolean = true;
		
		public function get enabled():Boolean
		{
			return _enabled;
		}
		
		/**
		 * Navigation Item is enabled
		 * @param value
		 */
		public function set enabled( value:Boolean ):void
		{
			_enabled = value;
		}
	}
}

