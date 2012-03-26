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
package actionscript
{
	import org.un.cava.birdeye.ravis.graphLayout.visual.edgeRenderers.BaseEdgeRenderer;
	import flash.display.Graphics;
	import flash.geom.Point;
	
	import org.un.cava.birdeye.ravis.graphLayout.visual.IVisualEdge;
	import org.un.cava.birdeye.ravis.graphLayout.visual.IVisualGraph;
	import org.un.cava.birdeye.ravis.graphLayout.visual.IVisualNode;
	import org.un.cava.birdeye.ravis.utils.Geometry;
	import org.un.cava.birdeye.ravis.utils.GraphicsWrapper;
	import org.un.cava.birdeye.ravis.utils.LogUtil;
	
	/**
	 * This is a flow edge renderer. It relys on a "flow"
	 * attribute in the edges XML data object.
	 * It uses the flow value, put in relation with some
	 * parameters of the renderer to have an initial edge
	 * thickness. At the target the edge will coverge to a point.
	 * The flow is drawn in the shape of a teardrop with the thick
	 * end near the source and relative to the amount of the flow.
	 * */
	public class AlphaFlowEdgeRenderer extends BaseEdgeRenderer
	{			
		private static const _LOG:String = "graphLayout.visual.edgeRenderers.FlowEdgeRenderer";
		
		/**
		 * This property describes the relation or scale 
		 * for the specified edge flow values. It is assumed
		 * to be a maximum and the drawing thickness will be
		 * oriented on that value. This does not mean that not
		 * larger values can be specified, but they may look ugly.
		 * @default 1000
		 * */
		public var relativeEdgeMagnitude:Number;
		
		/**
		 * This property describes the default maximum base width of the flow.
		 * It should be oriented on the size of the node labels.
		 * @default 100
		 * */
		public var maxBaseWidth:Number;
		
		
		/**
		 * The constructor just initialises some default values
		 * and the graphics object.
		 * @param g The graphics object to draw on.
		 * */
		public function AlphaFlowEdgeRenderer() {
			super();
			relativeEdgeMagnitude = 1000;
			maxBaseWidth = 100;
		}
		
		/**
		 * The draw function, i.e. the main function to be used.
		 * Draws a straight line from one node of the edge to the other.
		 * The colour is determined by a set of edge parameters,
		 * which are stored in an edge object.
		 * @inheritDoc
		 * */
		override public function draw():void {
			
			var fromNode:IVisualNode;
			var toNode:IVisualNode;
			
			var source:Point;
			var target:Point;
			var base1:Point;
			var base2:Point;        
			
			var flow:Number;
			var tdirectionAngle:Number;
			var basedirectionAngle:Number;
			var baseWidth:Number;
			
			/* first get the corresponding nodes */
			fromNode = vedge.edge.node1.vnode;
			toNode = vedge.edge.node2.vnode;
			
			if((vedge.edge.data as XML).attribute("flow").length() > 0) {
				flow = vedge.edge.data.@flow;
			} else {
				throw Error("Edge: "+vedge.edge.id+" does not have flow attribute.");
			}
			
			/* now get some current coordinates and calculate the middle 
			* of the node's view */
			source = fromNode.viewCenter;
			target = toNode.viewCenter;
			
			/* for the source, we now need to establish actually two points
			* which are orthogonal to the direction of the target
			* and have a distance that matches the flow parameter */
			
			/* calculate the angle of the direction of the target */
			tdirectionAngle = Geometry.polarAngle(target.subtract(source));
			//LogUtil.warn(_LOG, "target direction:"+Geometry.rad2deg(tdirectionAngle)+" degrees");
			
			/* calculate the angle of the direction of the base, which is
			* always 90 degrees (PI/2) of tdirection */
			basedirectionAngle = Geometry.normaliseAngle(tdirectionAngle + (Math.PI / 2));
			//LogUtil.warn(_LOG, "base direction:"+Geometry.rad2deg(basedirectionAngle)+" degrees");
			
			/* now calculate the width of the base in relation to the flow */
			baseWidth = (flow * (maxBaseWidth / relativeEdgeMagnitude));
			//LogUtil.warn(_LOG, "flow:"+flow+" base width:"+baseWidth);
			
			/* now calculate the first base point, which is half the width in
			* positive base direction */
			base1 = source.add(Point.polar((baseWidth / 2), basedirectionAngle));
			//LogUtil.warn(_LOG, "base1:"+base1.toString());
			
			/* the second is the same but in negative direction (or negative angle,
			* that should not make a difference */
			base2 = source.add(Point.polar(-(baseWidth / 2), basedirectionAngle));
			//LogUtil.warn(_LOG, "base1:"+base1.toString());
			
			
			/* apply the line style */
			applyLineStyle();
			
			/* now we draw the first curve with base 1 to target */
			g.beginFill(uint(vedge.lineStyle.color),vedge.edge.data.@edgeAlpha);
			g.moveTo(source.x, source.y);
			g.curveTo(
				base1.x,
				base1.y,
				target.x,
				target.y
			);
			
			g.curveTo(
				base2.x,
				base2.y,
				source.x,
				source.y
			);
			g.endFill();
			
			
			/* if the vgraph currently displays edgeLabels, then
			* we need to update their coordinates */
			if(vedge.vgraph.displayEdgeLabels) {
				vedge.setEdgeLabelCoordinates(labelCoordinates());
			}
		}
		
		
		/**
		 * This takes one of the curves which are part of the
		 * curved flow and places the label more or less next to the middle.
		 * 
		 * @inheritDoc
		 * */
		override public function labelCoordinates():Point {
			
			var fromNode:IVisualNode;
			var toNode:IVisualNode;
			
			var source:Point;
			var target:Point;
			var base:Point;
			
			var flow:Number;
			var tdirectionAngle:Number;
			var basedirectionAngle:Number;
			var baseWidth:Number;
			
			/* first get the corresponding nodes */
			fromNode = vedge.edge.node1.vnode;
			toNode = vedge.edge.node2.vnode;
			
			if((vedge.edge.data as XML).attribute("flow").length() > 0) {
				flow = vedge.edge.data.@flow;
			} else {
				throw Error("Edge: "+vedge.edge.id+" does not have flow attribute.");
			}
			
			/* now get some current coordinates and calculate the middle 
			* of the node's view */
			source = fromNode.viewCenter;
			target = toNode.viewCenter;
			
			/* for the source, we now need to establish actually two points
			* which are orthogonal to the direction of the target
			* and have a distance that matches the flow parameter */
			
			/* calculate the angle of the direction of the target */
			tdirectionAngle = Geometry.polarAngle(target.subtract(source));
			//LogUtil.warn(_LOG, "target direction:"+Geometry.rad2deg(tdirectionAngle)+" degrees");
			
			/* calculate the angle of the direction of the base, which is
			* always 90 degrees (PI/2) of tdirection */
			basedirectionAngle = Geometry.normaliseAngle(tdirectionAngle + (Math.PI / 2));
			//LogUtil.warn(_LOG, "base direction:"+Geometry.rad2deg(basedirectionAngle)+" degrees");
			
			/* now calculate the width of the base in relation to the flow */
			baseWidth = (flow * (maxBaseWidth / relativeEdgeMagnitude));
			//LogUtil.warn(_LOG, "flow:"+flow+" base width:"+baseWidth);
			
			/* now calculate the first base point, which is half the width in
			* positive base direction in the curved version we have to add
			* or subtract depending on the direction *
			* the second is the same but in negative direction (or negative angle,
			* that should not make a difference */
			
			base = source.add(Point.polar((baseWidth / 2), basedirectionAngle));
			
			return Geometry.bezierPoint(source,base,target,0.65);
		}
	}
}
