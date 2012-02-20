package com.ikanow.infinit.e.shared.view.component.dialog
{
	
	/**
	 * UserAction Dialog Message
	 */
	public class UserActionDialogMessage extends DialogMessage
	{
		
		//======================================
		// public properties 
		//======================================
		
		[ArrayElementType( "com.ikanow.infinit.e.shared.view.component.dialog.action.DialogUserAction" )]
		/**
		 * An array of DialogUserAction's related to a specific dialog.
		 */
		public var actions:Array;
		
		//======================================
		// constructor 
		//======================================
		
		/**
		 * Constructor.
		 */
		public function UserActionDialogMessage()
		{
			super();
		}
	}
}
