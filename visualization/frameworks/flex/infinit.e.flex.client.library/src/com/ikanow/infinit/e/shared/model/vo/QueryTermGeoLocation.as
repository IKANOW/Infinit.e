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
