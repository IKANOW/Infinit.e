package com.ikanow.infinit.e.widget.library.components
{
	import com.ikanow.infinit.e.widget.library.assets.skins.ScrollerRightButtonSkin;
	
	import spark.components.Button;
	
	public class ScrollerRightButton extends Button
	{
		public function ScrollerRightButton()
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
			this.setStyle( "skinClass", Class( ScrollerRightButtonSkin ) );
		}
	}
}