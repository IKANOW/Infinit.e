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
	import flash.events.Event;
	import flash.events.EventDispatcher;
	
	[Bindable]
	public class LatLong extends EventDispatcher
	{
		
		//======================================
		// public properties 
		//======================================
		
		private var _lat:Number;
		
		[Bindable( "typeChanged" )]
		public function get lat():Number
		{
			return _lat;
		}
		
		public function set lat( pLat:Number ):void
		{
			if ( ( pLat > 90 ) || ( pLat < -90 ) )
				_lat = 0;
			else
				_lat = pLat;
			dispatchEvent( new Event( "typeChanged" ) );
		}
		
		private var _lng:Number;
		
		[Bindable( "typeChanged" )]
		public function get lng():Number
		{
			return _lng;
		}
		
		public function set lng( pLong:Number ):void
		{
			if ( ( pLong > 180 ) || ( pLong < -180 ) )
				_lng = 0;
			else
				_lng = pLong;
			dispatchEvent( new Event( "typeChanged" ) );
		}
		
		//======================================
		// constructor 
		//======================================
		
		public function LatLong( lat:Number = 0, lng:Number = 0 )
		{
			this.lat = lat;
			this.lng = lng;
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		override public function toString():String
		{
			return lat + ", " + lng;
		}
		
		public function equals(value:LatLong):Boolean{
			return (value.lat==this.lat && value.lng==this.lng);
		}
	}
}
