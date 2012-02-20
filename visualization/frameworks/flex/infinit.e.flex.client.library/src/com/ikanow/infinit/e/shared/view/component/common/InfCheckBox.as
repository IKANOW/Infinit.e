package com.ikanow.infinit.e.shared.view.component.common
{
	import spark.components.CheckBox;
	
	public class InfCheckBox extends CheckBox
	{
		
		//======================================
		// constructor 
		//======================================
		
		public function InfCheckBox()
		{
			super();
			
			this.buttonMode = true;
			this.focusEnabled = false;
		}
	}
}
