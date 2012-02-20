package com.ikanow.infinit.e.widget.library.components
{
	import com.ikanow.infinit.e.widget.library.assets.skins.WidgetOptionsGroupSkin;
	import spark.components.SkinnableContainer;
	
	public class WidgetOptionsGroup extends SkinnableContainer
	{
		
		//======================================
		// constructor 
		//======================================
		
		public function WidgetOptionsGroup()
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
			this.setStyle( "skinClass", Class( WidgetOptionsGroupSkin ) );
		}
	}
}
