package com.ikanow.infinit.e.widget.library.components
{	
	import com.ikanow.infinit.e.widget.library.assets.skins.WidgetHSliderSkin;
	
	import spark.components.HSlider;
	
	public class WidgetHSlider extends HSlider
	{
		[Bindable] public var label:String = "Value: ";
		
		public function WidgetHSlider()
		{
			super();
		}
		
		/**
		 * Set the default skin class
		 */
		override public function stylesInitialized():void
		{
			super.stylesInitialized();
			this.setStyle( "skinClass", Class( WidgetHSliderSkin ) );
		}
	}
}