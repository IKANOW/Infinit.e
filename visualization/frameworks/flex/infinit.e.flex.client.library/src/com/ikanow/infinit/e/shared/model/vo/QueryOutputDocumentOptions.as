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
