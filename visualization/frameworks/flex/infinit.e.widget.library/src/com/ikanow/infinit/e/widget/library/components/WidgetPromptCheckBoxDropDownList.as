package com.ikanow.infinit.e.widget.library.components
{
	import com.ikanow.infinit.e.widget.library.assets.skins.WidgetPromptCheckBoxDropDownListSkin;
	import spark.components.DropDownList;
	
	public class WidgetPromptCheckBoxDropDownList extends DropDownList
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Bindable]
		public var listWidth:int = 0;
		
		//======================================
		// constructor 
		//======================================
		
		public function WidgetPromptCheckBoxDropDownList()
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
			this.setStyle( "skinClass", Class( WidgetPromptCheckBoxDropDownListSkin ) );
		}
		//======================================
		// protected methods 
		//======================================
	}
}
