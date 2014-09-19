package com.ikanow.infinit.e.widget.library.components.DocViewer
{
	import com.ikanow.infinit.e.widget.library.enums.EntityMatchTypeEnum;
	import com.ikanow.infinit.e.widget.library.enums.FilterDataSetEnum;
	import com.ikanow.infinit.e.widget.library.enums.IncludeEntitiesEnum;
	
	import flash.events.Event;
	
	import system.data.Map;
	import system.data.Set;
	
	public class FilterEntsEvent extends Event
	{
		public var filterDataSetEnum:FilterDataSetEnum;
		public var ents:Set;
		public var entityMatchTypeEnum:EntityMatchTypeEnum;
		public var includeEntitiesEnum:IncludeEntitiesEnum;
		public var desc:String;
		
		public function FilterEntsEvent(type:String, filterDataSetEnum:FilterDataSetEnum, ents:Set, entityMatchTypeEnum:EntityMatchTypeEnum, includeEntitiesEnum:IncludeEntitiesEnum, desc:String )
		{
			super(type,true);
			this.filterDataSetEnum = filterDataSetEnum;
			this.ents = ents;
			this.entityMatchTypeEnum = entityMatchTypeEnum;
			this.includeEntitiesEnum = includeEntitiesEnum;
			this.desc = desc;
		}
	}
}