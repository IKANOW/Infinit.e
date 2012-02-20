package com.ikanow.infinit.e.shared.view.component.dialog
{
	import flash.net.URLVariables;
	
	public class DownloadDialogMessage extends DialogMessage
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var defaultFileName:String;
		
		public var path:String;
		
		public var params:URLVariables;
		
		//======================================
		// constructor 
		//======================================
		
		public function DownloadDialogMessage()
		{
			super();
		}
	}
}
