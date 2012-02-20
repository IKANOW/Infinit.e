package com.ikanow.infinit.e.shared.view.component.common
{
	import com.ikanow.infinit.e.shared.view.component.GradientContainer;
	
	[Style( name = "color", type = "uint", format = "Color", inherit = "yes", theme = "spark" )]
	[Style( name = "angle", type = "Number", format = "int", inherit = "yes", theme = "spark" )]
	[Style( name = "blurX", type = "Number", format = "int", inherit = "yes", theme = "spark" )]
	[Style( name = "blurY", type = "Number", format = "int", inherit = "yes", theme = "spark" )]
	[Style( name = "distance", type = "Number", format = "int", inherit = "yes", theme = "spark" )]
	[Style( name = "inner", type = "Boolean", format = "Boolean", inherit = "yes", theme = "spark" )]
	/**
	 * A simple drop shadow container
	 */
	public class InfDropShadowGradientContainer extends GradientContainer
	{
		
		//======================================
		// constructor 
		//======================================
		
		public function InfDropShadowGradientContainer()
		{
			super();
		}
	}
}
