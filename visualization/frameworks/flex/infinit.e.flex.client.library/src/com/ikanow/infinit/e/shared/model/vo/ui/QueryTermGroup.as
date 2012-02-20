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
