package com.ikanow.infinit.e.widget.library.components
{
	import com.ikanow.infinit.e.widget.library.assets.skins.WidgetLayersToggleButtonSkin;
	import spark.components.ToggleButton;
	
	public class WidgetLayersToggleButton extends ToggleButton
	{
		
		//======================================
		// constructor 
		//======================================
		
		public function WidgetLayersToggleButton()
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
			this.setStyle( "skinClass", Class( WidgetLayersToggleButtonSkin ) );
		}
	}
}
