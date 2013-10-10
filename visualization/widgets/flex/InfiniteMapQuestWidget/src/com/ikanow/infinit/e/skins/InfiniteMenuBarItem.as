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
package com.ikanow.infinit.e.skins {
	import mx.controls.MenuBar;
	import mx.controls.menuClasses.MenuBarItem;
	import mx.controls.Alert;
	
	public class InfiniteMenuBarItem extends MenuBarItem
	{
		override public function set menuBarItemState(value:String):void
		{
			super.menuBarItemState = value;
			
			if (!label)
				return;
			
			if (value == "itemOverSkin")
				label.textColor = 0x000000;
			else if (value == "itemDownSkin")
				label.textColor = 0x000000;
			else
				label.textColor = data.@color;
		}    		
	};
	
}
