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

// CUSTOM LOGIC TO MAP BETWEEN THE INTERNAL REPRESENTATION OF OBJECTS AND THE API VERSION SHOULD RESIDE IN OBJECTTRANSLATORUTIL AND QUERYUTIL
// (SEE setAggregationOptions/setScoringOptions)

package com.ikanow.infinit.e.shared.model.vo
{
	import com.ikanow.infinit.e.shared.model.constant.QueryConstants;
	import flash.events.EventDispatcher;
	
	[Bindable]
	public class QueryOutputRequest extends EventDispatcher
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var aggregation:Object;
		
		public var docs:QueryOutputDocumentOptions;
		
		public var filter:QueryOutputFilterOptions;
		
		public var format:String;
		
		//======================================
		// constructor 
		//======================================
		
		public function QueryOutputRequest()
		{
			this.format = QueryConstants.OUTPUT_FORMAT;
		}
	}
}
