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
	import flash.events.Event;
	
	import mx.controls.Alert;
	import mx.controls.Image;
	import mx.core.UIComponent;
	
	import org.un.cava.birdeye.ravis.assets.icons.primitives.Circle;
	import org.un.cava.birdeye.ravis.components.renderers.RendererIconFactory;
	import org.un.cava.birdeye.ravis.components.renderers.nodes.BaseNodeRenderer;
	
	
	public class AlphaCircleNodeRenderer extends BaseNodeRenderer
	{
		public function AlphaCircleNodeRenderer()
		{
			super();
		}
		
		override protected function initComponent(e:Event):void
		{
			initTopPart();
			if ("0" == this.data.data.@nodeAlpha) {
				var cc:UIComponent = createCircle(10,this.data.data.@nodeColor,this.data.data.@nodeAlpha);;			
				this.addChild(cc);				
			}
			else {
				var img:Image = new Image();
				var type:String = this.data.data.@name;
				var pos:Number = type.lastIndexOf("/");
				if (pos > 0) {
					type = type.substr(pos + 1);
				}
				else {
					type = "unknown";
				}
				if ("1" == this.data.data.@geo) {
					img.source=EmbeddedImages.findIcon(type, "Where");					
				}
				else {
					img.source=EmbeddedImages.findIcon(type, "What");
				}
				this.addChild(img);				
			}
			this.toolTip = this.data.data.@name;
			initLinkButton();
		}
		
		private function createCircle(size:int, color:int, alpha:Number=1.0):UIComponent
		{
			var img:UIComponent
			img = new Circle();
			(img as Circle).color = color;
			img.setStyle("color", color);
			img.alpha = alpha;
			img.width = this.data.data.@nodeSize;
			img.height = this.data.data.@nodeSize;
			return img;
		}
	}
}
