package com.ikanow.infinit.e.widget.library.components
{
	import com.ikanow.infinit.e.widget.library.assets.skins.WidgetOptionsToggleButtonSkin;
	import spark.components.ToggleButton;
	
	public class WidgetOptionsToggleButton extends ToggleButton
	{
		
		//======================================
		// constructor 
		//======================================
		
		public function WidgetOptionsToggleButton()
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
			this.setStyle( "skinClass", Class( WidgetOptionsToggleButtonSkin ) );
		}
	}
}
