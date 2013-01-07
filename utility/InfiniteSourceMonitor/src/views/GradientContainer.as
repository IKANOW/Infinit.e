/*******************************************************************************
 * Copyright 2012, The Infinit.e Open Source Project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package views
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
