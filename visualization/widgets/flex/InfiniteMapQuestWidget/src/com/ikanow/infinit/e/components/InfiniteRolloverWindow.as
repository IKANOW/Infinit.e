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
