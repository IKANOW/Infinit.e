package com.ikanow.infinit.e.widget.library.data
{
	import mx.collections.ArrayCollection;
	
	public class WidgetDragObject
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var entities:ArrayCollection = new ArrayCollection();
		
		public var associations:ArrayCollection = new ArrayCollection();
		
		public var documents:ArrayCollection = new ArrayCollection();
		
		public var dragSource:String = null;
		
		//======================================
		// constructor 
		//======================================
		
		public function WidgetDragObject()
		{
		
		}
	}
}
