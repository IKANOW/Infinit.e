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
package com.ikanow.infinit.e.shared.model.vo.ui
{
	import com.ikanow.infinit.e.shared.util.QueryUtil;
	import flash.events.EventDispatcher;
	import mx.collections.ArrayCollection;
	
	[Bindable]
	public class QueryTermGroup extends EventDispatcher implements IQueryTerm, IQueryTermGroup
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var _id:String;
		
		public var level:int;
		
		public var children:ArrayCollection;
		
		public var logicOperator:String;
		
		
		//======================================
		// public methods 
		//======================================
		
		public function clone():QueryTermGroup
		{
			var clone:QueryTermGroup = new QueryTermGroup();
			
			clone._id = QueryUtil.getRandomNumber().toString();
			clone.children = new ArrayCollection();
			clone.children.addAll( children );
			clone.level = level;
			clone.logicOperator = logicOperator;
			
			return clone;
		}
	}
}
