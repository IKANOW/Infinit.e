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
	public class QueryOutputAggregationOptions extends EventDispatcher
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var entsNumReturn:int;
		
		public var eventsNumReturn:int;
		
		public var factsNumReturn:int;
		
		public var geoNumReturn:int;
		
		public var sourceMetadata:int;
		
		public var sources:int;
		
		public var timesInterval:String;
		
		[Transient]
		public var aggregateEntities:Boolean;
		
		[Transient]
		public var aggregateEvents:Boolean;
		
		[Transient]
		public var aggregateFacts:Boolean;
		
		[Transient]
		public var aggregateGeotags:Boolean;
		
		[Transient]
		public var aggregateSourceMetadata:Boolean;
		
		[Transient]
		public var aggregateSources:Boolean;
		
		[Transient]
		public var aggregateTimes:Boolean;
		
		//======================================
		// constructor 
		//======================================
		
		public function QueryOutputAggregationOptions()
		{
			super();
			reset();
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		public function apply( options:QueryOutputAggregationOptions ):void
		{
			entsNumReturn = options.entsNumReturn;
			eventsNumReturn = options.eventsNumReturn;
			factsNumReturn = options.factsNumReturn;
			geoNumReturn = options.geoNumReturn;
			sourceMetadata = options.sourceMetadata;
			sources = options.sources;
			timesInterval = options.timesInterval;
			
			aggregateEntities = options.aggregateEntities;
			aggregateEvents = options.aggregateEvents;
			aggregateFacts = options.aggregateFacts;
			aggregateGeotags = options.aggregateGeotags;
			aggregateSourceMetadata = options.aggregateSourceMetadata;
			aggregateSources = options.aggregateSources;
			aggregateTimes = options.aggregateTimes;
		}
		
		public function clone():QueryOutputAggregationOptions
		{
			var clone:QueryOutputAggregationOptions = new QueryOutputAggregationOptions();
			
			clone.entsNumReturn = entsNumReturn;
			clone.eventsNumReturn = eventsNumReturn;
			clone.factsNumReturn = factsNumReturn;
			clone.geoNumReturn = geoNumReturn;
			clone.sourceMetadata = sourceMetadata;
			clone.sources = sources;
			clone.timesInterval = timesInterval;
			
			clone.aggregateEntities = aggregateEntities;
			clone.aggregateEvents = aggregateEvents;
			clone.aggregateFacts = aggregateFacts;
			clone.aggregateGeotags = aggregateGeotags;
			clone.aggregateSourceMetadata = aggregateSourceMetadata;
			clone.aggregateSources = aggregateSources;
			clone.aggregateTimes = aggregateTimes;
			
			return clone;
		}
		
		public function reset():void
		{
			entsNumReturn = QueryAdvancedSettingsConstants.OUTPUT_AGG_ENTS_NUM_RETURN;
			eventsNumReturn = QueryAdvancedSettingsConstants.OUTPUT_AGG_EVENTS_NUM_RETURN;
			factsNumReturn = QueryAdvancedSettingsConstants.OUTPUT_AGG_FACTS_NUM_RETURN;
			geoNumReturn = QueryAdvancedSettingsConstants.OUTPUT_AGG_GEO_NUM_RETURN;
			sourceMetadata = QueryAdvancedSettingsConstants.OUTPUT_AGG_SOURCE_METADATA;
			sources = QueryAdvancedSettingsConstants.OUTPUT_AGG_SOURCES;
			timesInterval = QueryAdvancedSettingsConstants.OUTPUT_AGG_TIMES_INTERVAL;
			
			aggregateEntities = QueryAdvancedSettingsConstants.OUTPUT_AGG_AGGREGATE_ENTITIES;
			aggregateEvents = QueryAdvancedSettingsConstants.OUTPUT_AGG_AGGREGATE_EVENTS;
			aggregateFacts = QueryAdvancedSettingsConstants.OUTPUT_AGG_AGGREGATE_FACTS;
			aggregateGeotags = QueryAdvancedSettingsConstants.OUTPUT_AGG_AGGREGATE_GEOTAGS;
			aggregateSourceMetadata = QueryAdvancedSettingsConstants.OUTPUT_AGG_AGGREGATE_SOURCE_METADATA;
			aggregateSources = QueryAdvancedSettingsConstants.OUTPUT_AGG_AGGREGATE_SOURCES;
			aggregateTimes = QueryAdvancedSettingsConstants.OUTPUT_AGG_AGGREGATE_TIMES;
		}
	}
}
