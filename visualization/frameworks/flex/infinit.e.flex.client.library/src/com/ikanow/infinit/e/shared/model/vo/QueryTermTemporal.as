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
