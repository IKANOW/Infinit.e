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
	import com.ikanow.infinit.e.shared.model.constant.settings.QueryAdvancedSettingsConstants;
	import flash.events.EventDispatcher;
	
	[Bindable]
	public class QueryOutputDocumentOptions extends EventDispatcher
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var enable:Boolean;
		
		public var ents:Boolean;
		
		public var events:Boolean;
		
		public var eventsTimeline:Boolean;
		
		public var facts:Boolean;
		
		public var geo:Boolean;
		
		public var numReturn:int;
		
		public var skip:int;
		
		public var summaries:Boolean;
		
		public var metadata:Boolean;
		
		//======================================
		// constructor 
		//======================================
		
		public function QueryOutputDocumentOptions()
		{
			super();
			reset();
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		public function apply( options:QueryOutputDocumentOptions ):void
		{
			enable = options.enable;
			ents = options.ents;
			events = options.events;
			eventsTimeline = options.eventsTimeline;
			facts = options.facts;
			geo = options.geo;
			numReturn = options.numReturn;
			skip = options.skip;
			summaries = options.summaries;
			metadata = options.metadata;
		}
		
		public function clone():QueryOutputDocumentOptions
		{
			var clone:QueryOutputDocumentOptions = new QueryOutputDocumentOptions();
			
			clone.enable = enable;
			clone.ents = ents;
			clone.events = events;
			clone.eventsTimeline = eventsTimeline;
			clone.facts = facts;
			clone.geo = geo;
			clone.numReturn = numReturn;
			clone.skip = skip;
			clone.summaries = summaries;
			clone.metadata = metadata;
			
			return clone;
		}
		
		public function reset():void
		{
			enable = QueryAdvancedSettingsConstants.OUTPUT_DOC_ENABLE;
			ents = QueryAdvancedSettingsConstants.OUTPUT_DOC_ENTS;
			events = QueryAdvancedSettingsConstants.OUTPUT_DOC_EVENTS;
			eventsTimeline = QueryAdvancedSettingsConstants.OUTPUT_DOC_EVENTS_TIMELINE;
			facts = QueryAdvancedSettingsConstants.OUTPUT_DOC_FACTS;
			geo = QueryAdvancedSettingsConstants.OUTPUT_DOC_GEO;
			numReturn = QueryAdvancedSettingsConstants.OUTPUT_DOC_NUM_RETURN;
			skip = QueryAdvancedSettingsConstants.OUTPUT_DOC_SKIP;
			summaries = QueryAdvancedSettingsConstants.OUTPUT_DOC_SUMMARIES;
			metadata = QueryAdvancedSettingsConstants.OUTPUT_DOC_METADATA;
		}
	}
}
