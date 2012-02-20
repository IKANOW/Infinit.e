package com.ikanow.infinit.e.widget.library.components
{
	import com.ikanow.infinit.e.widget.library.assets.skins.WidgetPromptDropDownListSkin;
	import spark.components.DropDownList;
	import spark.events.DropDownEvent;
	
	public class WidgetPromptDropDownList extends DropDownList
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Bindable]
		public var listWidth:int = 60;
		
		//======================================
		// constructor 
		//======================================
		
		public function WidgetPromptDropDownList()
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
			this.setStyle( "skinClass", Class( WidgetPromptDropDownListSkin ) );
		}
		//======================================
		// protected methods 
		//======================================
	}
}
