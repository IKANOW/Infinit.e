///////////////////////////////////////////////////////////////////////////////
// Licensed Materials - Property of IBM
// 5724-Y31,5724-Z78
// © Copyright IBM Corporation 2007, 2010. All Rights Reserved.
//
// Note to U.S. Government Users Restricted Rights:
// Use, duplication or disclosure restricted by GSA ADP Schedule
// Contract with IBM Corp.
///////////////////////////////////////////////////////////////////////////////
package actionscript
{
	import flash.events.MouseEvent;
	
	import ilog.charts3d.charts3dClasses.CartesianChart3D;
	
	import mx.core.Application;
	import mx.core.FlexGlobals;
	
	public class CartesianChart3DControl
	{
		private var _chart:CartesianChart3D;
		
		private var downX:int;
		private var downY:int;
		private var downElevation:Number;
		private var downRotation:Number;
		
		public function set chart(chart:CartesianChart3D):void
		{
			_chart = chart;
			_chart.addEventListener(MouseEvent.MOUSE_DOWN, downListener);
			_chart.addEventListener(MouseEvent.MOUSE_WHEEL, wheelListener);
		}
		
		public function get chart():CartesianChart3D
		{
			return _chart;
		}
		
		private function downListener(e:MouseEvent):void
		{
			_chart.removeEventListener(MouseEvent.MOUSE_DOWN, downListener);          
			FlexGlobals.topLevelApplication.addEventListener(MouseEvent.MOUSE_UP, upListener);
			FlexGlobals.topLevelApplication.addEventListener(MouseEvent.ROLL_OUT, rollOutListener);
			_chart.addEventListener(MouseEvent.MOUSE_MOVE, moveListener);
			downX = e.stageX;
			downY = e.stageY;
			downElevation = _chart.elevationAngle;
			downRotation = _chart.rotationAngle;
		}
		
		private function rollOverListener(e:MouseEvent):void
		{
			FlexGlobals.topLevelApplication.removeEventListener(MouseEvent.ROLL_OVER, rollOverListener);
			if (! e.buttonDown)
			{
				upListener(e);
			}
		} 
		
		private function rollOutListener(e:MouseEvent):void
		{
			FlexGlobals.topLevelApplication.removeEventListener(MouseEvent.ROLL_OUT, rollOutListener);
			FlexGlobals.topLevelApplication.addEventListener(MouseEvent.ROLL_OVER, rollOverListener);
		}
		
		private function moveListener(e:MouseEvent):void
		{
			var dx:Number = e.stageX - downX;
			var dy:Number = e.stageY - downY;
			_chart.elevationAngle = downElevation + dy/5;
			_chart.rotationAngle = downRotation - dx/5;
		}
		
		private function wheelListener(e:MouseEvent):void
		{
			if (e.delta < 0)
			{
				_chart.zoom = Math.min(2, _chart.zoom + 0.15); 
			}
			else
			{
				_chart.zoom = Math.max(0.1, _chart.zoom - 0.15);
			}
		}
		
		private function upListener(e:MouseEvent):void
		{
			_chart.addEventListener(MouseEvent.MOUSE_DOWN, downListener);          
			FlexGlobals.topLevelApplication.removeEventListener(MouseEvent.MOUSE_UP, upListener);
			_chart.removeEventListener(MouseEvent.MOUSE_MOVE, moveListener);          
		}
	}
}