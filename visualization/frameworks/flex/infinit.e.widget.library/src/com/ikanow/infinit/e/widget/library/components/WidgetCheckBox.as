package com.ikanow.infinit.e.widget.library.components
{
	import spark.components.CheckBox;
	
	public class WidgetCheckBox extends CheckBox
	{
		
		//======================================
		// constructor 
		//======================================
		
		public function WidgetCheckBox()
		{
			super();
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		public function setHovered( value:Boolean ):void
		{
			hovered = value;
		}
	}
}
