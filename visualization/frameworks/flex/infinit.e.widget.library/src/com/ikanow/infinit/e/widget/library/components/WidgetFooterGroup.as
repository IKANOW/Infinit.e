package com.ikanow.infinit.e.widget.library.components
{
	import com.ikanow.infinit.e.widget.library.assets.skins.WidgetFooterGroupSkin;
	import spark.components.SkinnableContainer;
	
	public class WidgetFooterGroup extends SkinnableContainer
	{
		
		//======================================
		// constructor 
		//======================================
		
		public function WidgetFooterGroup()
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
			this.setStyle( "skinClass", Class( WidgetFooterGroupSkin ) );
			
			if ( isNaN( getStyle( "contentBackgroundColor" ) ) )
				this.setStyle( "contentBackgroundColor", 0xE8E9E9 );
			
			invalidateDisplayList();
		}
	}
}
