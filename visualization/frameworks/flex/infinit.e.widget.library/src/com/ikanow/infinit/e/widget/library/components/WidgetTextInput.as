package com.ikanow.infinit.e.widget.library.components
{
	import com.ikanow.infinit.e.widget.library.assets.skins.WidgetTextInputlSkin;
	import spark.components.TextInput;
	
	public class WidgetTextInput extends TextInput
	{
		
		//======================================
		// constructor 
		//======================================
		
		public function WidgetTextInput()
		{
			super();
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
			this.setStyle( "skinClass", Class( WidgetTextInputlSkin ) );
		}
	}
}
