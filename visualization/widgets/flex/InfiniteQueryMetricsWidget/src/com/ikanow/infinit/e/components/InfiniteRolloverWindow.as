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
package com.ikanow.infinit.e.components
{
	import com.mapquest.tilemap.RolloverWindow;
	import com.mapquest.tilemap.TileMap;
	
	public class InfiniteRolloverWindow extends RolloverWindow
	{
		
		//======================================
		// public properties 
		//======================================
		
		override public function set content( value:Object ):void
		{
		
		}
		
		//======================================
		// private properties 
		//======================================
		
		private var content
		
		//======================================
		// constructor 
		//======================================
		
		public function InfiniteRolloverWindow( map:TileMap )
		{
			super( map );
		}
	}
}
