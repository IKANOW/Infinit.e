package com.ikanow.infinit.e.widget.library.components.DocViewer
{
	import flash.events.TimerEvent;
	import flash.utils.Timer;
	
	import mx.collections.ArrayCollection;
	import mx.controls.LinkButton;
	
	public class ScrollingLinkButton extends LinkButton
	{
		private var _mainText:String = "";
		private var _listText:ArrayCollection = null;
		private var _displayField:String = null;
		private var _scrollTimer:Timer;
		private var currIndex:int = -1;
		
		public function ScrollingLinkButton()
		{
			super();			
		}
		
		private function startScrolling():void
		{
			currIndex = 0;
			this.label = getText(currIndex);	
			_scrollTimer = new Timer(3000);
			_scrollTimer.addEventListener(TimerEvent.TIMER,scrollText);
			_scrollTimer.start();
		}
		
		private function scrollText(event:TimerEvent):void
		{
			currIndex = ( currIndex + 1 ) % _listText.length;
			this.label = getText(currIndex);			
		}
		
		private function getText(index:int):String
		{
			var item:Object = _listText.getItemAt(currIndex);
			if ( _displayField != null )
				return _mainText + item[_displayField];
			else
				return _mainText + item;
		}
		
		
		public function set displayField(val:String):void
		{
			_displayField = val;
		}
		
		public function get displayField():String
		{
			return _displayField;
		}
		
		public function set mainText(val:String):void
		{
			_mainText = val;
			this.label = _mainText;
		}
		
		public function get mainText():String
		{
			return _mainText;
		}
		
		public function set listText(val:ArrayCollection):void
		{
			_listText = val;
			if ( val != null && val.length > 0 )
			{
				startScrolling();	
			}				
		}
		
		public function get listText():ArrayCollection
		{
			return _listText;
		}
	}
}