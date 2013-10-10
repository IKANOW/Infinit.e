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
package com.ikanow.infinit.e.widget.library.components
{
	// Extension of https://github.com/flex-users/flex-iframe/blob/master/library/src/com/google/code/flexiframe/IFrame.as
	
	import com.google.code.flexiframe.IFrame;
	import flash.display.DisplayObject;
	import flash.display.DisplayObjectContainer;
	import flash.events.TimerEvent;
	import flash.geom.Point;
	import flash.utils.Timer;
	import flashx.textLayout.tlf_internal;
	import mx.controls.Alert;
	import mx.core.IChildList;
	import mx.core.UIComponent;
	import mx.managers.ISystemManager;
	import system.data.Set;
	
	public class InfiniteFlexIFrame extends IFrame
	{
		public function getDivId():String {
			return _iframeId;
		}
		
		//DEBUG: THIS IS NEEDED TO MAKE IT WORK IN FF LOCAL MODE...
		override protected function childrenCreated():void{
			IFrame.applicationId = "Index";
		}
		
		//======================================
		// private properties 
		//======================================
		
		//DEBUG
		//(to "undebug": do the following replaces:
		// "//DEBUG trace(" -> "//DEBUG //DEBUG trace("
		// "htmlDebug" -> "//DEBUG htmlDebug"		
		//public var htmlDebug:String = "";
		
		private var timer:Timer;
		
		private var foundMe:Boolean = false;
		
		private var foundOverlapper:Boolean = false;
		
		private var parentSet:Object = null;
		
		//======================================
		// constructor 
		//======================================
		
		public function InfiniteFlexIFrame( id:String = null )
		{
			super( id );
			
			timer = new Timer( 100 );
			timer.addEventListener( TimerEvent.TIMER, onTick );
			timer.start();
		}
		
		override public function removeIFrame():void
		{
			timer.stop();
			super.removeIFrame();
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		public function onTick( event:TimerEvent ):void
		{
			this.checkExistingPopUps();
		}
		
		//======================================
		// protected methods 
		//======================================
		
		override protected function checkExistingPopUps():void
		{
			// Cache list of parents:
			this.parentSet = new Object();
			var parentCheck:DisplayObjectContainer = this.parent;
			
			while ( null != parentCheck )
			{
				parentSet[ parentCheck ] = parentCheck;
				parentCheck = parentCheck.parent;
			}
			
			//DEBUG
			//DEBUG htmlDebug = "<body>7\n" + this.toString() + "\n";
			
			// run through each child of systemManager and if it's a popup, check it for overlay
			var sm:ISystemManager = systemManager;
			var n:int = sm.rawChildren.numChildren;
			
			this.foundMe = false;
			this.foundOverlapper = false;
			
			// (loop backwards through children so anything overlapping is always on top)
			for ( var i:int = n - 1; i >= 0; i-- )
			{
				var child:UIComponent = sm.rawChildren.getChildAt( i ) as UIComponent;
				
				if ( child && child.isPopUp )
				{
					//DEBUG
					//DEBUG trace( "POPUP: " + child.toString() );
					//DEBUG htmlDebug "<p/>POPUP: " + child.toString() + "\n";
					
					checkOverlay( child );
				}
				else if ( child )
				{
					if ( !this.foundMe && !this.foundOverlapper )
					{
						checkExistingPopUps_2( child );
					}
				}
			}
			var onTop:Boolean = ( this.overlapCount == 0 ) && !this.foundOverlapper;
			
			//DEBUG
			//DEBUG trace( "Visibility: " + onTop + " = " + this.overlapCount + " , " + this.foundOverlapper );
			//DEBUG htmlDebug "<p/>Visibility: " + onTop + " = " + this.overlapCount + " , " + this.foundOverlapper + "\n";
			
			updateFrameVisibility( onTop );
			
			//DEBUG
			//DEBUG htmlDebug "</body>";
		}
		
		protected function checkExistingPopUps_2( child:UIComponent ):void
		{
			var n:int = child.numChildren;
			
			//DEBUG
			//DEBUG trace( "RECURSE: " + child.toString()  + ": " + n );
			//DEBUG htmlDebug "<p/>RECURSE: " + child.toString() + ": " + n + "\n";
			
			// (loop backwards through children so anything overlapping is always on top)
			for ( var i:int = n - 1; i >= 0; i-- )
			{
				var child2:UIComponent = child.getChildAt( i ) as UIComponent;
				
				var hitTest:Boolean = false;
				
				if ( child2 )
				{
					if ( child2 == this )
					{
						//DEBUG
						//DEBUG trace( "FOUND ME!!!" );
						//DEBUG htmlDebug "<p/>FOUND ME!!! " + child2.toString() + "\n";
						
						this.foundMe = true;
						return; // (anything after this point is behind me, so we're done here)
					}
					
					if ( !child2.visible )
					{
						//DEBUG
						//DEBUG trace( "INVISIBLE..." );
						//DEBUG htmlDebug "<p/>INVISIBLE... " + child2.toString() + "\n";
						
						continue; // Keep checking children but don't recurse (since this guy's children are invisible)
					}
					
					// One subtletly: I can be "overlapped" by my parent 
					// Ugly test for this:
					var parentCheck:Boolean = this.parentSet.hasOwnProperty( child2 );
					
					if ( parentCheck )
					{
						//DEBUG
						//DEBUG trace( "PARENT TRAP!" );
						//DEBUG htmlDebug "<p/>PARENT TRAP!\n";
					}
					
					if ( !parentCheck )
					{
						hitTest = hitTestStageObject_2( child2 );
					}
					
					// (if child2 has mask it gets complicated because it can be a circle etc etc
					//  in Infinite, we happen to know that won't be an issue so can just ignore these components)
					
					
					if ( hitTest )
					{
						//DEBUG
						//DEBUG trace( hitTest + ": " + child2.toString() + "..." + this.depth + " vs " + child2.depth );
						//DEBUG htmlDebug "<p/>" + hitTest + ": " + child2.toString() + "..." + child2.depth + "/" + child2.alpha + "/" + child2.mask + " vs " + this.depth + "\n";
						
						// Nasty hack: there's some masking going on that I don't understand
						// I'm ignoring everything with masks (see below)
						// But the mask itself is also causing problems, so going to discard that by name
						if ( child2.toString().indexOf( "workspacesView.dialogsMask" ) > 0 )
						{
							//DEBUG
							//DEBUG trace( "IGNORE DIALOG MASK!" );
							//DEBUG htmlDebug "<p/>IGNORE DIALOG MASK!\n";
							
							continue;
						}
						
						// Similar problem...
						if ( child2.toString().indexOf( "workspacesView.modalBackground" ) > 0 )
						{
							//DEBUG
							//DEBUG trace( "IGNORE MODAL BACKGROUND!" );
							//DEBUG htmlDebug "<p/>IGNORE MODAL BACKGROUND!\n";
							
							continue;
						}
						
						if ( ( null == child2.mask ) && ( child2.alpha > 0.0 ) )
							// mask: see above, still check children though; alpha: else isn't currently visible - still need to recurse from here
						{
							//DEBUG
							//DEBUG trace( "FOUND OVERLAPPER!" );
							//DEBUG htmlDebug "<p/>FOUND OVERLAPPER!\n";
							
							this.foundOverlapper = true;
							return;
						}
						
					}//(end if hitTest)
					else
					{
						//DEBUG
						//DEBUG trace( hitTest + ": " + child2.toString() + "..." + this.depth + " vs " + child2.depth );
						//DEBUG htmlDebug "<p/>MISS..." + hitTest + ": " + child2.toString() + "..." + child2.depth + "/" + child2.alpha + "/" + child2.mask + "\n";
					}
					
				} // (child2 is UIComponent)
				else
				{
					//DEBUG
					//DEBUG trace( "STOP AT NON-UICOMPONENT: " + child.toString() + ": " + child.numChildren );
					//DEBUG htmlDebug "<p/>STOP AT NON-UICOMPONENT: " + child.toString() + ": " + child.numChildren + "\n";
				}
				
				// Recurse if: 
				// - masked hit
				// - parent hit (but is actually a parent of the target)
				// - it's a top-level container (very hacky, but this is only for performance anyway)
				if ( child2 && ( hitTest || parentCheck || ( !child2.x && !child2.y ) ) )
				{
					checkExistingPopUps_2( child2 );
					
					if ( this.foundMe || this.foundOverlapper )
					{
						return; // (anything after this point is behind me, so we're done here)
					}
					
				}
			}//(end loop over children)
		}
		
		// OK I think this is getting the global wrong?! Probably need to check out localToGlobal (eg diff between this.parent.L2G and this.L2G...)
		
		protected function hitTestStageObject_2( o:DisplayObject ):Boolean
		{
			var overlapX:Boolean = false;
			var overlapY:Boolean = false;
			
			var localMe:Point = new Point( this.x, this.y );
			var globalMe:Point = this.parent.localToGlobal( localMe );
			
			//DEBUG
			//DEBUG trace( "ME: global: " + globalMe.x + " , " + globalMe.y + " / local: " + localMe.x + " , " + localMe.y );
			//DEBUG htmlDebug "<p/>ME: global: " + globalMe.x + " , " + globalMe.y + " / local: " + localMe.x + " , " + localMe.y + "|" + this.width + "," + this.height + "\n";
			
			var localYou:Point = new Point( o.x, o.y );
			var globalYou:Point = o.parent.localToGlobal( localYou );
			
			//DEBUG
			//DEBUG trace( "YOU: global: " + globalYou.x + " , " + globalYou.y + " / local: " + localYou.x + " , " + localYou.y );
			//DEBUG htmlDebug "<p/>YOU: global: " + globalYou.x + " , " + globalYou.y + " / local: " + localYou.x + " , " + localYou.y + "|" + o.width + "," + o.height + "\n";
			
			var myLeft:int = globalMe.x;
			var myRight:int = globalMe.x + this.width - 1;
			var myTop:int = globalMe.y;
			var myBottom:int = globalMe.y + this.height - 1;
			
			// Shrink the target object to allow a small overlap (eg shadows)
			var oLeft:int = globalYou.x + 5;
			var oRight:int = globalYou.x + o.width - 6;
			var oTop:int = globalYou.y + 5;
			var oBottom:int = globalYou.y + o.height - 6;
			
			// Does object's left edge fall between my left and right edges?
			overlapX = ( oLeft >= myLeft ) && ( oLeft <= myRight );
			// Or does my left edge fall between object's left and right edges?
			overlapX ||= ( oLeft <= myLeft ) && ( oRight >= myLeft );
			
			// Does object's top edge fall between my top and bottom edges?
			overlapY = ( oTop >= myTop ) && ( oTop <= myBottom );
			// Or does my top edge fall between object's top and bottom edges?
			overlapY ||= ( oTop <= myTop ) && ( oBottom >= myTop );
			
			return overlapX && overlapY;
		}
	}//(end class)
}