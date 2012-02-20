package com.ikanow.infinit.e.shared.view.component.dialog.action
{
	
	public class DialogInputUserAction extends DialogUserAction
	{
		
		//======================================
		// public properties 
		//======================================
		
		/**
		 * Flags whether or not the input is required for the selected action.
		 */
		public var inputRequired:Boolean;
		
		//======================================
		// constructor 
		//======================================
		
		public function DialogInputUserAction( name:String = null, callback:Function = null, scope:* = null, inputRequired:Boolean = false, args:Array = null )
		{
			super( name, callback, scope, args );
			this.inputRequired = inputRequired;
		}
	}
}
