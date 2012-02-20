package com.ikanow.infinit.e.widget.library.components
{
	import com.ikanow.infinit.e.widget.library.assets.skins.WidgetIgnoreFilterToggleButtonSkin;
	
	import spark.components.ToggleButton;
	
	public class WidgetIgnoreFilterToggleButton extends ToggleButton
	{
		
		//======================================
		// constructor 
		//======================================
		
		public function WidgetIgnoreFilterToggleButton()
		{
			super();
			
			this.buttonMode = true;
			this.focusEnabled = false;
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Set the default skin class
		 */
		override public function stylesInitialized():void
		{
			super.stylesInitialized();
			this.setStyle( "skinClass", Class( WidgetIgnoreFilterToggleButtonSkin ) );
		}
	}
}
