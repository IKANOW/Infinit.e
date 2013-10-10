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
package com.ikanow.infinit.e.shared.model.vo
{
	import com.ikanow.infinit.e.shared.model.constant.Constants;
	import flash.events.EventDispatcher;
	
	[Bindable]
	public class QueryTermGeoLocation extends EventDispatcher
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var _centerll:String;
		
		public function get centerll():String
		{
			return _centerll;
		}
		
		public function set centerll( value:String ):void
		{
			_centerll = value;
			
			latLongArray = value.split( Constants.STRING_ARRAY_DELIMITER );
			centerLatLong = new LatLong( latLongArray[ 0 ], latLongArray[ 1 ] );
		}
		
		public var dist:int;
		
		public var minll:String;
		
		public var maxll:String;
		
		[Transient]
		public var latLongArray:Array;
		
		[Transient]
		public var centerLatLong:LatLong = new LatLong();
		
		
		//======================================
		// public methods 
		//======================================
		
		public function clone():QueryTermGeoLocation
		{
			var clone:QueryTermGeoLocation = new QueryTermGeoLocation();
			
			clone.centerll = centerll;
			clone.dist = dist;
			clone.minll = minll;
			clone.maxll = maxll;
			
			return clone;
		}
	}
}
