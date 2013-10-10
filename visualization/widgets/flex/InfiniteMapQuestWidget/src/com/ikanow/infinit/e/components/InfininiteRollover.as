package com.ikanow.infinit.e.components
{
	import com.mapquest.tilemap.DefaultRolloverWindow;
	import com.mapquest.tilemap.TileMap;
	import flash.text.TextFormat;
	
	public class InfininiteRollover extends DefaultRolloverWindow
	{
		
		//======================================
		// private properties 
		//======================================
		
		private var backgroundHeight:Number;
		
		private var backgroundWidth:Number;
		
		private var shadowOffsetX:int = 4;
		
		private var shadowOffsetY:int = 3;
		
		//======================================
		// constructor 
		//======================================
		
		public function InfininiteRollover( map:TileMap )
		{
			super( map );
		}
		
		
		//======================================
		// protected methods 
		//======================================
		
		override protected function draw():void
		{
			this.graphics.clear();
			
			_borderSize = 1;
			_backgroundColor = 0x223344;
			_backgroundAlpha = 0.8;
			backgroundHeight = this.height;
			backgroundWidth = this.width;
			
			//draw the shadow
			/*this.graphics.beginFill( 0x000000, .2 );
			this.graphics.drawRect( 0, 0, this.width, this.height );
			this.graphics.drawEllipse( shadowOffsetX - 40, shadowOffsetY - 15, backgroundWidth + 95, backgroundHeight + 30 );
			this.graphics.endFill();*/
			
			//draw the window
			this.graphics.beginFill( _backgroundColor, _backgroundAlpha );
			this.graphics.drawRect( 0, 0, backgroundWidth + 20, backgroundHeight + 20 );
			this.graphics.endFill();
			
			//create a text format object and set it to the window
			var tf:TextFormat = new TextFormat();
			//tf.font = "Helvetica";
			tf.size = 10;
			tf.bold = true;
			tf.color = 0xFFFFFF;
			this.setTextFormat( tf );
		}
	}
}
