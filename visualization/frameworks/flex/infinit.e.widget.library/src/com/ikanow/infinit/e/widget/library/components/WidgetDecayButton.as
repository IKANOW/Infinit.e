package com.ikanow.infinit.e.widget.library.components
{
	import com.ikanow.infinit.e.widget.library.assets.skins.WidgetDecayButtonSkin;
	import spark.components.Button;
	
	public class WidgetDecayButton extends Button
	{
		
		//======================================
		// constructor 
		//======================================
		
		public function WidgetDecayButton()
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
			this.setStyle( "skinClass", Class( WidgetDecayButtonSkin ) );
		}
	}
}
