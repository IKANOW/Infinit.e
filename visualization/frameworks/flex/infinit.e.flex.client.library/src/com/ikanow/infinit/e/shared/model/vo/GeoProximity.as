package com.ikanow.infinit.e.shared.model.vo
{
	import com.ikanow.infinit.e.shared.model.constant.Constants;
	import com.ikanow.infinit.e.shared.model.constant.settings.QueryAdvancedSettingsConstants;
	import flash.events.EventDispatcher;
	
	[Bindable]
	public class GeoProximity extends EventDispatcher
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var lat:String;
		
		public var lon:String;
		
		public var decay:String;
		
		private var _ll:String = Constants.BLANK;
		
		public function get ll():String
		{
			return lat + Constants.COMMA + lon;
		}
		
		public function set ll( value:String ):void
		{
			if ( value && value != Constants.BLANK )
			{
				if ( Constants.PARENTHESIS_LEFT == value.charAt( 0 ) )
					value = value.substr( 1, value.length - 2 );
				
				var latLons:Array = value.split( Constants.COMMA, 2 );
				
				if ( latLons.length == 2 )
				{
					lat = latLons[ 0 ];
					lon = latLons[ 1 ];
				}
			}
			else
			{
				value = Constants.BLANK;
				lat = Constants.BLANK;
				lon = Constants.BLANK;
			}
			
			_ll = value;
		}
		
		//======================================
		// constructor 
		//======================================
		
		public function GeoProximity()
		{
			super();
			reset();
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		public function apply( options:GeoProximity ):void
		{
			lat = options.lat;
			lon = options.lon;
			decay = options.decay;
		}
		
		public function clone():GeoProximity
		{
			var clone:GeoProximity = new GeoProximity();
			
			clone.lat = lat;
			clone.lon = lon;
			clone.decay = decay;
			
			return clone;
		}
		
		public function reset():void
		{
			lat = Constants.BLANK;
			lon = Constants.BLANK;
			decay = QueryAdvancedSettingsConstants.SCORING_GEO_PROX_DECAY;
		}
	}
}
