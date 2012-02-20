package com.ikanow.infinit.e.shared.event
{
	import flash.events.Event;
	
	public class DialogCloseEvent extends Event
	{
		
		//======================================
		// public static properties 
		//======================================
		
		public static const DIALOG_CLOSE:String = "dialogClose";
		
		
		//======================================
		// public properties 
		//======================================
		
		/**
		 * The label of the associated button that triggered the
		 * DialogCloseEvent.
		 */
		public var label:String;
		
		/**
		 * Additional arguments to pass from an input dialog.
		 */
		public var additionalArguments:Array;
		
		//======================================
		// constructor 
		//======================================
		
		/**
		 * Constructor.
		 */
		public function DialogCloseEvent( type:String, label:String )
		{
			super( type, false, false );
			this.label = label;
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * @inheritDoc
		 */
		override public function clone():Event
		{
			return new DialogCloseEvent( type, label );
		}
	}
}
