/*******************************************************************************
 * Copyright 2012, The Infinit.e Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package actionscript
{
	import org.un.cava.birdeye.ravis.components.renderers.edgeLabels.BaseEdgeLabelRenderer;
	
	public class EdgeLabelTooltipRenderer extends BaseEdgeLabelRenderer
	{
		public function EdgeLabelTooltipRenderer()
		{
			super();
		}
		
		/**
		 * @inheritDoc
		 * */
		override protected function initTopPart():void {
			
			/* create the top part using super class */
			super.initTopPart();
			
			this.toolTip = this.data.data.@tooltip;
		}
	}
}
