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
	import com.ikanow.infinit.e.shared.model.constant.types.QueryTermTypes;
	
	public class QueryObject
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var keywordString:String = null;
		
		public var image:String = null;
		
		public var queryType:String = QueryTermTypes.ENTITY; //entity,event,geo,time
		
		public var queryString:String = null; //like entity:Obama type:Person
		
		public var boolString:String = null; //boolean if needed
		
		public var saveString:String = null;
		
		public var qObj:Object = new Object();
		
		//======================================
		// constructor 
		//======================================
		
		public function QueryObject()
		{
		
		}
	}
}
