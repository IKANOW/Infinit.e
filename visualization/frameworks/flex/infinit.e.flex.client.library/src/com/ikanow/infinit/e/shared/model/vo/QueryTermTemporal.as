package com.ikanow.infinit.e.shared.model.vo
{
	import com.ikanow.infinit.e.shared.model.constant.Constants;
	import flash.events.EventDispatcher;
	import mx.formatters.DateFormatter;
	
	[Bindable]
	public class QueryTermTemporal extends EventDispatcher
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var min:Date;
		
		public var max:Date;
		
		protected var _startDateString:String;
		
		public function get startDateString():String
		{
			if ( min )
			{
				var formatter:DateFormatter = new DateFormatter();
				formatter.formatString = Constants.DEFAULT_DATE_TIME_FORMAT;
				return formatter.format( min );
			}
			
			return "";
		}
		
		public function set startDateString( value:String ):void
		{
			_startDateString = value;
		}
		
		protected var _endDateString:String;
		
		public function get endDateString():String
		{
			if ( max )
			{
				var formatter:DateFormatter = new DateFormatter();
				formatter.formatString = Constants.DEFAULT_DATE_TIME_FORMAT;
				return formatter.format( max );
			}
			
			return "";
		}
		
		public function set endDateString( value:String ):void
		{
			_endDateString = value;
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		public function clone():QueryTermTemporal
		{
			var clone:QueryTermTemporal = new QueryTermTemporal();
			
			clone.min = min;
			clone.max = max;
			
			return clone;
		}
	}
}
