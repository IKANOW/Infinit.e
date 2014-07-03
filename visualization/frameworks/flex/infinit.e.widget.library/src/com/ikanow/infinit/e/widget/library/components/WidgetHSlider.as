package com.ikanow.infinit.e.widget.library.components
{	
	import com.ikanow.infinit.e.widget.library.assets.skins.WidgetHSliderSkin;
	
	import spark.components.HSlider;
	
	public class WidgetHSlider extends HSlider
	{
		[Bindable] public var label:String = "Value: ";
		[Bindable] public var mymax:Number;
		
		public function set bindable_maximum(max:Number):void
		{
			maximum = max;
			mymax = max;
		}
		
		public function WidgetHSlider()
		{
			super();
		}
		/**
		 * @private
		 */
		override protected function initializationComplete():void
		{
			super.initializationComplete();
			mymax = maximum;
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