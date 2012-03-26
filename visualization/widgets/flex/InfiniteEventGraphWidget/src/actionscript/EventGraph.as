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
	import mx.controls.Alert;

	public class EventGraph
	{
		private var xml:String = "";		
		
		public function EventGraph()
		{
			//instantiate graph
			xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n";
			xml += "<graph>\n";
			xml += "<Node id=" + "\"" + "1" + "\" significance=\"0\" nodeSize=\"10\" url=\"\" nodeAlpha=\"0\" nodeColor=\"0xFF0000\"/>\n";			
		}
		
		public function createNode(name:String, geo:String, nodeID:Number):void
		{			
			xml += "<Node id=" + "\"" + nodeID + "\" significance=\"0\" nodeSize=\"10\" url=\"\" nodeAlpha=\"1\" name=" + "\"" + name + "\"" + "geo=\"" + geo + "\"" + "/>\n";
		}
		
		public function createEdge(name:String, tooltip:String, fromNodeID:String, toNodeID:String, flow:Number=100, alpha:Number=1, color:uint=0x800517):void
		{	
			xml += "<Edge fromID=\"" + fromNodeID + "\" toID=\""+ toNodeID +"\" ";
			xml += "flow=\""+flow+"\" alpha=\""+alpha+"\" edgeAlpha=\"" + alpha + "\" edgeLabel=\"" + name + "\" color=\"" + color + "\" tooltip=\"" + tooltip + "\"/>\n";
		}
		
		public function getGraph():XML
		{
			//close graph and return
			xml += "</graph>\n";
			return XML(xml);
		}
	}
}
