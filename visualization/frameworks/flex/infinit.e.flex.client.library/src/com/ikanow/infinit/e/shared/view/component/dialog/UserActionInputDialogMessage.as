package com.ikanow.infinit.e.shared.view.component.dialog
{
	
	public class UserActionInputDialogMessage extends DialogMessage
	{
		
		//======================================
		// public properties 
		//======================================
		
		[ArrayElementType( "com.ikanow.infinit.e.shared.view.component.dialog.action.DialogInputUserAction" )]
		
		/**
		 * An array of DialogUserAction's related to a specific dialog.
		 */
		public var actions:Array;
		
		/**
		 * The layout of the input dialog
		 */
		public var layout:int;
		
		/**
		 * Maximum number of characters allowed.
		 */
		public var maxChars:int;
		
		/**
		 * Values to limit the input to.
		 */
		public var restrict:String;
		
		//======================================
		// constructor 
		//======================================
		
		public function UserActionInputDialogMessage()
		{
			super();
		}
	}
}
