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
package com.ikanow.infinit.e.actionscript
{
	import flash.display.DisplayObject;
	import flash.display.Sprite;
	import flash.text.TextField;
	import flash.text.TextFieldAutoSize;
	
	import spark.components.Label;
	
	/**
	 * This class gets the icons for clusters based on the number of 
	 * markers it has underneath it
	*/
	public class InfiniteClusterIcons extends Sprite
	{
		[Embed('../assets/icons/green.png')]
		private var greenImg:Class;
		[Embed('../assets/icons/blue.png')]
		private var blueImg:Class;
		[Embed('../assets/icons/magenta.png')]
		private var magentaImg:Class;
		[Embed('../assets/icons/orange.png')]
		private var orangeImg:Class;
		[Embed('../assets/icons/red.png')]
		private var redImg:Class;
		[Embed('../assets/icons/orangewithflag.png')]
		private var orangeFlagImg:Class;
		[Embed('../assets/icons/redwithflag.png')]
		private var redFlagImg:Class;
		[Embed('../assets/icons/greenwithflag.png')]
		private var greenFlagImg:Class;
		[Embed('../assets/icons/bluewithflag.png')]
		private var blueFlagImg:Class;
		
		/**
		 * Constructor
		 * 
		 * @param _label The string representation of how many points are at this cluster
		*/
		public function InfiniteClusterIcons(_label:String, numEvent:Number=0, maxSize:Number=1)
		{
			addChild(getIcon(Number(_label), numEvent, maxSize));
			
			var iconLabel:TextField = new TextField;
			iconLabel.autoSize = TextFieldAutoSize.LEFT;
			iconLabel.selectable = false;
			iconLabel.border = false;
			iconLabel.embedFonts = false;
			iconLabel.mouseEnabled = false;
			iconLabel.width = 60;
			iconLabel.height = 60;
			iconLabel.text = _label;
			
			if(_label.length == 2)
			{
				iconLabel.x = 9;
			}
			else if(_label.length == 3)
			{
				iconLabel.x = 7;
			}
			else
			{
				iconLabel.x = 11;
			}
			if ( numEvent == 0 )
				iconLabel.y = 6;
			else
				iconLabel.y = 20;
			
			addChild(iconLabel);
			cacheAsBitmap = true;
		}
		
		/**
		 * function to get the icon based on the number of points
		 * within a cluster
		 * 
		 * @param numCluster The number of points at this cluster
		 * 
		 * @return The icon for the given cluster
		*/
		
		private function getIcon(numCluster:Number, numEvent:Number = 0, maxSize:Number=1):DisplayObject
		{
			var icon:DisplayObject;
			var normalizeSize:Number = numCluster / maxSize * 100;
			if ( numEvent == 0 )
			{
				if ( normalizeSize > 75 )
					icon = new redImg();
				else if ( normalizeSize > 50 )
					return new orangeImg();
				else if ( normalizeSize > 25 )
					return new greenImg();
				else
					return new blueImg();
			}
			else
			{
				if ( normalizeSize > 75 )
					icon = new redFlagImg();
				else if ( normalizeSize > 50 )
					return new orangeFlagImg();
				else if ( normalizeSize > 25 )
					return new greenFlagImg();
				else
					return new blueFlagImg();
			}
			return icon;
		}
	}
}
