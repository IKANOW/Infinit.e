package com.ikanow.infinit.e.shared.model.vo
{
	import flash.events.EventDispatcher;
	
	[Bindable]
	public class GeoProximityRequest extends EventDispatcher
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var ll:String = "";
		
		public var decay:String = "";
		
		//======================================
		// constructor 
		//======================================
		
		public function GeoProximityRequest( value:GeoProximity )
		{
			this.decay = value.decay;
			this.ll = value.ll;
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		public function clone():GeoProximityRequest
		{
			var geoProx:GeoProximity = new GeoProximity();
			geoProx.ll = ll;
			geoProx.decay = decay;
			
			var clone:GeoProximityRequest = new GeoProximityRequest( geoProx );
			
			return clone;
		}
	}
}
