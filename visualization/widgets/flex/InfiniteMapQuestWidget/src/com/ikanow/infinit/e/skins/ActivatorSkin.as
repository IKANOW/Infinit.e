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
////////////////////////////////////////////////////////////////////////////////
//
// ActivatorSkin - written from scratch based on inspection of Adobe original
//
////////////////////////////////////////////////////////////////////////////////

package com.ikanow.infinit.e.skins
{
	
	import flash.display.DisplayObject;
	import flash.display.GradientType;
	import flash.filters.BlurFilter;
	import flash.utils.getQualifiedClassName;
	import flash.utils.describeType;
//	import mx.core.ApplicationGlobals;
	import mx.skins.Border;
	import mx.styles.IStyleClient;
	import mx.utils.ColorUtil;
	
	/**
	 *  Defines the up, down, and over states for MenuBarItem objects.
	 */
	public class ActivatorSkin extends Border
	{
		private static var cache:Object = []; 
		
		public function ActivatorSkin()
		{
			super();
		}
		
		override protected function updateDisplayList(w:Number, h:Number):void
		{
			super.updateDisplayList(w, h);
	
			// Example code:
			//var fillColors:Array = getStyle("fillColors");
			
			graphics.clear();
			switch (this.name)
			{
				case "itemUpSkin": // up/disabled
				{
					// invisible hit area
					drawRoundRect(
						x, y, w, h, 2,
						0xFFFFFF, 0);
					break;
				}
					
				case "itemDownSkin":
				{
					drawRoundRect(
						x, y, w, h, 2,
						0xA8C6EE, 1);
					break;
				}
					
				case "itemOverSkin":
				{
					drawRoundRect(
						x, y, w, h, 2,
						0xCEDBEF, 1);
					break;			
				}
			}
			return;
		}
	}	
}
