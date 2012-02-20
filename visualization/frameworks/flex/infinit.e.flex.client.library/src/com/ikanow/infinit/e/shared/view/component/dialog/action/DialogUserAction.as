package com.ikanow.infinit.e.shared.view.component.dialog.action
{
	
	/**
	 * This class represents a specific user action that can be
	 * performed on a dialog.
	 */
	public class DialogUserAction
	{
		
		//======================================
		// public properties 
		//======================================
		
		/**
		 * List of paramaters to pass to the callback function.
		 * ex. deleteItems( items:Array ):void{}, args = [ items ] not args = items.
		 * ex. add( x:Number, y:Number ):Number{}, args = [ 1, 2 ]
		 * usage - callBack.apply( scope, args );
		 */
		public var args:Array;
		
		/**
		 * The function to be called when the user performs this
		 * action by clicking on the button.
		 */
		public var callback:Function;
		
		/**
		 * The label that will appear on the button in the dialog.
		 */
		public var name:String;
		
		/**
		 * The scope in which to execute the callback function.
		 */
		public var scope:*;
		
		//======================================
		// constructor 
		//======================================
		
		/**
		 * Constructor.
		 */
		public function DialogUserAction( name:String = null, callback:Function = null, scope:* = null, args:Array = null )
		{
			this.name = name;
			this.callback = callback;
			this.scope = scope;
			this.args = args;
		}
	}
}
