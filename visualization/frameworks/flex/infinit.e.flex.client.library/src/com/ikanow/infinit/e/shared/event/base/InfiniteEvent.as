package com.ikanow.infinit.e.shared.event.base
{
	import com.ikanow.infinit.e.shared.model.vo.ui.DialogControl;
	import flash.events.Event;
	
	public class InfiniteEvent extends Event implements IDialogControlEvent
	{
		
		//======================================
		// public properties 
		//======================================
		
		private var _dialogControl:DialogControl = new DialogControl();
		
		public function get dialogControl():DialogControl
		{
			return _dialogControl;
		}
		
		public function set dialogControl( value:DialogControl ):void
		{
			_dialogControl = value;
		}
		
		//======================================
		// constructor 
		//======================================
		
		public function InfiniteEvent( type:String, bubbles:Boolean = true, cancelable:Boolean = false, dialogControl:DialogControl = null )
		{
			super( type, bubbles, cancelable );
			
			if ( dialogControl )
				this.dialogControl = dialogControl;
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		override public function clone():Event
		{
			return new InfiniteEvent( type, bubbles, cancelable, dialogControl );
		}
	}
}
