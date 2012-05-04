package
{
	import system.data.Map;
	
	public class GroupStoreObject
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var keys:Array;
		
		public var values:Array;
		
		//======================================
		// constructor 
		//======================================
		
		public function GroupStoreObject( map:Map )
		{
			keys = map.getKeys();
			values = map.getValues();
		}
	}
}
