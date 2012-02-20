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
