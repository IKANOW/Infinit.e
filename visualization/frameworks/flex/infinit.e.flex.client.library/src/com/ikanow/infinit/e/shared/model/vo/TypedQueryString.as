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
	import com.ikanow.infinit.e.shared.model.constant.types.QueryStringTypes;
	
	import flash.events.EventDispatcher;
	
	[Bindable]
	public class TypedQueryString extends EventDispatcher
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var queryDate:Date = new Date();
		
		public var type:String = QueryStringTypes.QUERY;
		
		public var queryString:QueryString;
		
		public var time:Number = queryDate.time;
		
		/*public function get time():Number
		{
			return queryDate.time;
		}*/
	}
}
