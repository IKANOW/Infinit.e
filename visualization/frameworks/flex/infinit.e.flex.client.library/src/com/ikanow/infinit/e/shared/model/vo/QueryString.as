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
	import mx.collections.ArrayCollection;
	
	[Bindable]
	public class QueryString extends EventDispatcher
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var input:QueryInput;
		
		public var logic:String;
		
		public var output:QueryOutput;
		
		public var qtOptions:Object;
		
		[ArrayCollectionElementType( "com.ikanow.infinit.e.shared.model.vo.QueryTerm" )]
		public var qt:ArrayCollection;
		
		[ArrayCollectionElementType( "String" )]
		public var communityIds:ArrayCollection;
		
		public var score:QueryScoreOptions;
		
		
		//======================================
		// public methods 
		//======================================
		
		public function clone():QueryString
		{
			var clone:QueryString = new QueryString();
			
			clone.input = input;
			clone.logic = logic.toString();
			clone.output = new QueryOutput();
			clone.output.aggregation = output.aggregation.clone();
			clone.output.docs = output.docs.clone();
			clone.output.filter = output.filter.clone();
			clone.output.format = output.format.toString();
			clone.qtOptions = qtOptions;
			clone.communityIds = communityIds;
			clone.score = score.clone();
			
			clone.qt = new ArrayCollection();
			
			for each ( var queryTerm:QueryTerm in qt )
			{
				clone.qt.addItem( queryTerm.clone() );
			}
			
			return clone;
		}
	}
}
