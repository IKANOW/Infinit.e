package com.ikanow.infinit.e.widget.library.components
{
	import com.ikanow.infinit.e.widget.library.assets.skins.ScrollerLeftButtonSkin;
	
	import spark.components.Button;
	
	public class ScrollerLeftButton extends Button
	{
		public function ScrollerLeftButton()
		{
			super();
			this.buttonMode = false;
		}
		
		/**
		 * Set the default skin class
		 */
		override public function stylesInitialized():void
		{
			super.stylesInitialized();
			this.setStyle( "skinClass", Class( ScrollerLeftButtonSkin ) );
		}
	}
}