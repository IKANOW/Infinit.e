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
	import flash.events.EventDispatcher;
	
	[Bindable]
	public class QueryTermEvent extends EventDispatcher
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var entity1:QueryTerm;
		
		public var entity2:QueryTerm;
		
		public var verb:String;
		
		
		//======================================
		// public methods 
		//======================================
		
		public function clone():QueryTermEvent
		{
			var clone:QueryTermEvent = new QueryTermEvent();
			
			if ( entity1 )
				clone.entity1 = entity1.clone();
			
			if ( entity2 )
				clone.entity2 = entity2.clone();
			
			clone.verb = verb;
			
			return clone;
		}
	}
}
