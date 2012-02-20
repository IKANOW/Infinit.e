package com.ikanow.infinit.e.shared.model.vo.ui
{
	import flash.events.EventDispatcher;
	
	[Bindable]
	public class DialogControl
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var _id:Number;
		
		public var show:Boolean;
		
		public var title:String;
		
		public var message:String;
		
		public var description:String;
		
		public var startTime:Date
		
		public var endTime:Date;
		
		public function get duration():int
		{
			return endTime.time - startTime.time;
		}
		
		
		//======================================
		// public static methods 
		//======================================
		
		public static function create( showDilaog:Boolean, dialogMessage:String = "", dialogTitle:String = "" ):DialogControl
		{
			var dialogControl:DialogControl = new DialogControl();
			dialogControl.show = showDilaog;
			dialogControl.message = dialogMessage;
			dialogControl.title = dialogTitle;
			return dialogControl;
		}
	}
}
