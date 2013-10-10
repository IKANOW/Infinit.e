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
