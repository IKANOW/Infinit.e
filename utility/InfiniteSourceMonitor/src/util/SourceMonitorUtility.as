package util
{
	import mx.controls.ToolTip;
	import mx.core.IUIComponent;
	import mx.managers.ToolTipManager;
	
	public class SourceMonitorUtility
	{
		
		//======================================
		// public static properties 
		//======================================
		
		public static var errorMessageToolTip:ToolTip;
		
		//======================================
		// constructor 
		//======================================
		
		public function SourceMonitorUtility()
		{
		}
		
		
		//======================================
		// public static methods 
		//======================================
		
		/**
		 * method to get the error message tool tip
		 *
		 * @param text The text to display
		 * @param x The x coordinate of the component
		 * @param y The y coordinate of the component
		 * @param parent The parent component
		 *
		 * @return The error message tool tip
		 */
		public static function createErrorMessageToolTip( text:String, x:Number, y:Number, parent:IUIComponent ):void
		{
			if ( errorMessageToolTip )
			{
				ToolTipManager.destroyToolTip( errorMessageToolTip );
				errorMessageToolTip = null;
			}
			
			errorMessageToolTip = ToolTipManager.createToolTip( text, x, y, null, parent ) as ToolTip;
			
			if ( errorMessageToolTip.width > 250 )
				errorMessageToolTip.x -= 250;
		}
	}
}
