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
	
	public interface INavigationItem
	{
		function get navigatorId():String;
		function set navigatorId( value:String ):void;
		
		function get id():String;
		function set id( value:String ):void;
		
		function get type():String;
		function set type( value:String ):void;
		
		function get state():String;
		function set state( value:String ):void;
		
		function get label():String;
		function set label( value:String ):void;
		
		function get description():String;
		function set description( value:String ):void;
		
		function get toolTip():String;
		function set toolTip( value:String ):void;
		
		function get styleName():String;
		function set styleName( value:String ):void;
		
		function get itemCount():int;
		function set itemCount( value:int ):void;
		
		function get icon():Class;
		function set icon( value:Class ):void;
		
		function get altIcon():Class;
		function set altIcon( value:Class ):void;
		
		function get selected():Boolean;
		function set selected( value:Boolean ):void;
		
		function get enabled():Boolean;
		function set enabled( value:Boolean ):void;
	}
}
