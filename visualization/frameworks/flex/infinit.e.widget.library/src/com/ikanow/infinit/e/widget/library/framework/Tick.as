/**
 * <p>Infinit.e</p>
 *
 * <p>Copyright (c) 2011 IKANOW, llc.</p>
 * <p>http://www.ikanow.com</p> 
 *
 * <p>NOTICE:  IKANOW permits you to use this this file in accordance with the terms of the license agreement 
 * accompanying it.  For information about the licensing and copyright of this Plug-In please contact IKANOW, llc. 
 * at support&#64;ikanow.com.</p>
 *
 * <p>http://www.ikanow.com/terms-conditions/</p>
 * 
 */
package com.ikanow.infinit.e.widget.library.framework {
	import flash.display.Sprite;
	
	import mx.effects.Fade;
	
	public class Tick extends Sprite {
		private var tickFade:Fade = new Fade(this);
		
			
		public function Tick(fromX:Number, fromY:Number, toX:Number, toY:Number, tickWidth:int, tickColor:uint) {
			this.graphics.lineStyle(tickWidth, tickColor, 1.0, false, "normal", "rounded");
			this.graphics.moveTo(fromX, fromY);
			this.graphics.lineTo(toX, toY);
		}
		
			
		public function fade(duration:Number):void {
			tickFade.alphaFrom = 1.0;
			tickFade.alphaTo = 0.1;
			tickFade.duration = duration;
			tickFade.play();
		}
	}
}