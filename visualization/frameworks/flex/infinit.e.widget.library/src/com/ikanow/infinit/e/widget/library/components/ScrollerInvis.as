package com.ikanow.infinit.e.widget.library.components
{
	import com.ikanow.infinit.e.widget.library.assets.skins.InfScrollerInvisSkin;
	
	import spark.components.Scroller;
	
	public class ScrollerInvis extends Scroller
	{
		public function ScrollerInvis()
		{
			super();			
		}
		
		/**
		 * Set the default skin class
		 */
		override public function stylesInitialized():void
		{
			super.stylesInitialized();
			this.setStyle( "skinClass", Class( InfScrollerInvisSkin ) );
		}
	}
}