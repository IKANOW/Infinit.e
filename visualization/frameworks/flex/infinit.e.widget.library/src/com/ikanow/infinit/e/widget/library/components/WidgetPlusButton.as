package com.ikanow.infinit.e.widget.library.components
{
	import com.ikanow.infinit.e.widget.library.assets.skins.WidgetPlusButtonSkin;
	import spark.components.Button;
	
	public class WidgetPlusButton extends Button
	{
		
		//======================================
		// constructor 
		//======================================
		
		public function WidgetPlusButton()
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
			this.setStyle( "skinClass", Class( WidgetPlusButtonSkin ) );
		}
	}
}
