package com.ikanow.infinit.e.widget.library.events
{
	import flash.events.Event;
	import mx.collections.ArrayCollection;
	
	public class WidgetDropEvent extends Event
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var entities:ArrayCollection;
		
		public var associations:ArrayCollection;
		
		public var documents:ArrayCollection;
		
		public var dragSource:String;
		
		public var dragWidgetName:String;
		
		public var dragWidgetClass:String;
		
		//======================================
		// constructor 
		//======================================
		
		public function WidgetDropEvent( type:String, entities:ArrayCollection, associations:ArrayCollection, documents:ArrayCollection, dragSource:String, dragWidgetName:String, dragWidgetClass:String )
		{
			super( type );
			this.entities = entities;
			this.associations = associations;
			this.documents = documents;
			this.dragSource = dragSource;
			this.dragWidgetName = dragWidgetName;
			this.dragWidgetClass = dragWidgetClass;
		}
	}
}
