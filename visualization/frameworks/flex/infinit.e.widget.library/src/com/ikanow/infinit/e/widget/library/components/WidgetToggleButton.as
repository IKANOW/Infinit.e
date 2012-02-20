package com.ikanow.infinit.e.widget.library.components
{
	import com.ikanow.infinit.e.widget.library.assets.skins.WidgetToggleButtonSkin;
	import spark.components.ToggleButton;
	
	public class WidgetToggleButton extends ToggleButton
	{
		
		//======================================
		// constructor 
		//======================================
		
		public function WidgetToggleButton()
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
			this.setStyle( "skinClass", Class( WidgetToggleButtonSkin ) );
		}
	}
}
