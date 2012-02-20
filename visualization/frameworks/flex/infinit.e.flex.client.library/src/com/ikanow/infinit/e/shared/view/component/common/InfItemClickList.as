package com.ikanow.infinit.e.shared.view.component.common
{
	import spark.components.List;
	
	/**
	 *  Dispatched when an item renderer is clicked
	 */
	[Event( name = "itemClick", type = "mx.events.ItemClickEvent" )]
	public class InfItemClickList extends List
	{
		
		//======================================
		// constructor 
		//======================================
		
		public function InfItemClickList()
		{
			super();
		}
	}
}
