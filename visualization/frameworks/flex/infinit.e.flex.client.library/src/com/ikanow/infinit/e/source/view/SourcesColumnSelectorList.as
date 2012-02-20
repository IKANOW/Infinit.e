package com.ikanow.infinit.e.source.view
{
	import com.ikanow.infinit.e.shared.view.component.common.InfItemClickList;
	
	/**
	 *  Dispatched when an column selector item is clicked
	 */
	[Event( name = "addItemToFilter", type = "mx.events.ItemClickEvent" )]
	public class SourcesColumnSelectorList extends InfItemClickList
	{
		
		//======================================
		// constructor 
		//======================================
		
		public function SourcesColumnSelectorList()
		{
			super();
			
			this.focusEnabled = false;
		}
	}
}
