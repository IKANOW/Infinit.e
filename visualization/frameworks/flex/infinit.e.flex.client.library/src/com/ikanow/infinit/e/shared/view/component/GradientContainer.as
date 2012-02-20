package com.ikanow.infinit.e.shared.view.component
{
	import spark.components.SkinnableContainer;
	
	[Style( name = "startRatio", type = "Number", format = "Number", inherit = "yes", theme = "spark" )]
	[Style( name = "startColor", type = "uint", format = "Color", inherit = "yes", theme = "spark" )]
	[Style( name = "endColor", type = "uint", format = "Color", inherit = "yes", theme = "spark" )]
	/**
	 * Gradient Container
	 */
	public class GradientContainer extends SkinnableContainer
	{
		
		//======================================
		// constructor 
		//======================================
		
		public function GradientContainer()
		{
			super();
		}
	}
}
