package com.ikanow.infinit.e.utility
{
	import flash.display.InteractiveObject;
	import flash.display.Stage;
	import flash.events.MouseEvent;
	import flash.external.ExternalInterface;
	
	/**
	 * Code pulled from: https://code.google.com/p/flex-wmode-mousewheel-handler-example/
	 * 
	 * full credit goes to: 
	 * http://blog.earthbrowser.com/2009/01/simple-solution-for-mousewheel-events.html
	 **/
	public class MouseWheelEnabler
	{
		static private var initialised : Boolean = false;
		static private var currentItem : InteractiveObject;
		static private var browserMouseEvent : MouseEvent;
		
		public static function init( stage : Stage ) : void
		{
			if( !initialised )
			{
				initialised = true;
				registerListenerForMouseMove( stage );
				registerJS();
			}
		}
		
		private static function registerListenerForMouseMove( stage : Stage ) : void
		{
			stage.addEventListener
				(
					MouseEvent.MOUSE_MOVE, 
					function( e : MouseEvent ) : void
					{
						currentItem = InteractiveObject( e.target );
						browserMouseEvent = MouseEvent( e );
					}
				);
		}
		
		
		private static function registerJS() : void
		{
			if( ExternalInterface.available )
			{
				var id:String = 'eb_' + Math.floor(Math.random()*1000000);
				ExternalInterface.addCallback(id, function():void{});
				ExternalInterface.call(MouseWheelEnabler_JavaScript.CODE);
				ExternalInterface.call("eb.InitMacMouseWheel", id);
				ExternalInterface.addCallback('externalMouseEvent', handleExternalMouseEvent);  
			}
		}
		
		private static function handleExternalMouseEvent(delta:Number):void
		{
			if(currentItem && browserMouseEvent)
			{
				currentItem.dispatchEvent(new MouseEvent(MouseEvent.MOUSE_WHEEL, true, false, 
					browserMouseEvent.localX, browserMouseEvent.localY, browserMouseEvent.relatedObject,
					browserMouseEvent.ctrlKey, browserMouseEvent.altKey, browserMouseEvent.shiftKey, browserMouseEvent.buttonDown,
					int(delta)));
			}
		}
		
	}
}