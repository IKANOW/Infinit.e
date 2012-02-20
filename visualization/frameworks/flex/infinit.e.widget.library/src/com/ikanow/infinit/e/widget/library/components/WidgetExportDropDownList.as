package com.ikanow.infinit.e.widget.library.components
{
	import com.ikanow.infinit.e.widget.library.assets.skins.WidgetExportDropDownListSkin;
	import spark.components.DropDownList;
	import spark.events.DropDownEvent;
	
	public class WidgetExportDropDownList extends DropDownList
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Bindable]
		public var listWidth:int = 0;
		
		//======================================
		// constructor 
		//======================================
		
		public function WidgetExportDropDownList()
		{
			super();
			
			this.buttonMode = true;
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
			this.setStyle( "skinClass", Class( WidgetExportDropDownListSkin ) );
		}
		//======================================
		// protected methods 
		//======================================
	}
}
