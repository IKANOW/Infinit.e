package com.ikanow.infinit.e.widget.library.data
{
	import system.data.Map;
	import system.data.maps.HashMap;
	
	public class WidgetSaveObject
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var shareid:String;
		
		public var userSave:Object;
		
		public var communitySave:Map;
		
		//======================================
		// constructor 
		//======================================
		
		public function WidgetSaveObject()
		{
			communitySave = new HashMap();
		}
	}
}
