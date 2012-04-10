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
	import com.ikanow.infinit.e.widget.library.widget.IResultSet;
	import mx.collections.ArrayCollection;
	import system.data.sets.HashSet;
	
	public class GraphMLRenderer
	{
		
		//======================================
		// public static properties 
		//======================================
		
		public static const CXDIM_FULL:int = 0;
		
		public static const CXDIM_PARTIAL:int = 1;
		
		public static const CXDIM_PURE:int = 2;
		
		
		//======================================
		// private properties 
		//======================================
		
		private var nodeCount_:int = 0;
		
		private var edgeCount_:int = 0;
		
		//======================================
		// constructor 
		//======================================
		
		public function GraphMLRenderer()
		{
		}
		
		
		//======================================
		// private static methods 
		//======================================
		
		private static function getPseudoDimension( type:String, isGeo:Boolean ):String
		{
			if ( isGeo )
			{
				return "Where";
			}
			else
			{
				if ( ( -1 != type.indexOf( "company" ) || ( -1 != type.indexOf( "organization" ) ) ) || ( -1 != type.indexOf( "facility" ) ) )
				{
					return "Who(organization)";
				}
				else if ( -1 != type.indexOf( "person" ) )
				{
					return "Who(person)";
				}
				else if ( ( -1 != type.indexOf( "city" ) ) || ( -1 != type.indexOf( "country" ) ) || ( -1 != type.indexOf( "region" ) ) || ( -1 != type.indexOf( "continent" ) ) )
				{
					return "Where";
				}
				else if ( ( -1 != type.indexOf( "geographicalfeature" ) ) || ( -1 != type.indexOf( "naturalfeature" ) ) || ( -1 != type.indexOf( "province" ) ) )
				{
					return "Where";
				}
				else
				{
					return "What";
				}
			}
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		public function createGraphML( eventResults:IResultSet, factResults:IResultSet, userFilter:RegExp, crossDimensional:int ):XML
		{
			nodeCount_ = 0;
			edgeCount_ = 0;
			
			// Root node and base attributes:
			var graphML:XML = <graphml xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://graphml.graphdrawing.org/xmlns http://graphml.graphdrawing.org/xmlns/1.0/graphml.xsd"></graphml>
				;
			
			// Declare attributes
			
			graphML.appendChild(
				<key id="sig" for="node" attr.name="significance" attr.type="double"></key>
				);
			graphML.appendChild(
				<key id="name" for="node" attr.name="name" attr.type="string"></key>
				);
			graphML.appendChild(
				<key id="type" for="node" attr.name="type" attr.type="string"></key>
				);
			graphML.appendChild(
				<key id="dim" for="node" attr.name="dimension" attr.type="string"></key>
				);
			graphML.appendChild(
				<key id="evsig" for="edge" attr.name="significance" attr.type="double"></key>
				);
			graphML.appendChild(
				<key id="docs" for="edge" attr.name="document count" attr.type="long"></key>
				);
			graphML.appendChild(
				<key id="verb" for="edge" attr.name="edge relation" attr.type="string"></key>
				);
			
			var mapOfNodeMaps:Object = new Object();
			
			if ( CXDIM_FULL == crossDimensional )
			{
				var mainGraph:XML = <graph id="mixed" edgedefault="directed"></graph>
					;
				graphML.appendChild( mainGraph );
				
				mapOfNodeMaps[ mainGraph.@id ] = new Object();
			}
			
			if ( null != eventResults )
			{
				addNodesAndEdges( graphML, mapOfNodeMaps, eventResults.getEvents(), userFilter, crossDimensional );
			}
			
			if ( null != factResults )
			{
				addNodesAndEdges( graphML, mapOfNodeMaps, factResults.getFacts(), userFilter, crossDimensional );
			}
			
			return graphML;
		}
		
		//======================================
		// private methods 
		//======================================
		
		private function addNodesAndEdges( xmlObj:XML, mapOfHashSets:Object, source:ArrayCollection, userFilter:RegExp, crossDimensional:int ):void
		{
			
			// Main processing
			
			var mainGraph:XML = null;
			var mainNodeMap:Object = null;
			
			if ( CXDIM_FULL == crossDimensional )
			{
				mainGraph = xmlObj.graph[ 0 ];
				mainNodeMap = mapOfHashSets[ mainGraph.@id ];
			}
			
			for each ( var assoc:Object in source )
			{
				// Get attributes for easy access
				
				var ent1:String = assoc.entity1_index;
				var ent1_sig:Number = assoc.entity1_sig;
				
				if ( null == assoc.entity1_sig )
				{
					ent1_sig = 0.0;
				}
				var ent2:String = assoc.entity2_index;
				
				var ent2_sig:Number = assoc.entity2_sig;
				
				if ( null == assoc.entity2_sig )
				{
					ent2_sig = 0.0;
				}
				var geo:String = assoc.geo_index;
				var geo_sig:Number = assoc.geo_sig;
				
				if ( null == assoc.geo_sig )
				{
					geo_sig = 0.0;
				}
				var edgeName:String = assoc.verb_category;
				
				if ( ( null == edgeName ) || ( edgeName == "null" ) )
				{
					edgeName = "unknown";
				}
				var assoc_sig:Number = assoc.assoc_sig;
				var assoc_doccount:Number = assoc.doccount;
				
				// Some further decomposition on the names:
				
				var ent1_name:String = null;
				var ent1_type:String = null;
				var ent1_dim:String = null;
				var ent2_name:String = null;
				var ent2_type:String = null;
				var ent2_dim:String = null;
				var geo_name:String = null;
				var geo_type:String = null;
				var geo_dim:String = null;
				var index:int = 0;
				
				if ( null != ent1 )
				{
					index = ent1.lastIndexOf( "/" );
					
					if ( index > 0 )
					{
						ent1_name = ent1.substr( 0, index );
						ent1_type = ent1.substr( index + 1 );
						ent1_dim = getPseudoDimension( ent1_type, false );
					}
				}
				
				if ( null != ent2 )
				{
					index = ent2.lastIndexOf( "/" );
					
					if ( index > 0 )
					{
						ent2_name = ent2.substr( 0, index );
						ent2_type = ent2.substr( index + 1 );
						ent2_dim = getPseudoDimension( ent2_type, false );
					}
				}
				
				if ( null != geo )
				{
					index = geo.lastIndexOf( "/" );
					
					if ( index > 0 )
					{
						geo_name = geo.substr( 0, index );
						geo_type = geo.substr( index + 1 );
						geo_dim = getPseudoDimension( geo_type, true );
					}
				}
				
				if ( null != userFilter )
				{
					var s:String = ent1_name + " " + ent1_type + " " + ent2_name + " " + ent2_type + " " + geo_name + " " + geo_type + " " + edgeName;
					
					if ( !userFilter.test( s ) )
					{
						continue;
					}
				}
				
				// Pick the graph to which to append (create if necessary)
				
				if ( null != mainGraph )
				{
					createEdgesAndNode( ent1_name, ent1_type, ent1_dim, ent1, ent1_sig, ent2_name, ent2_type, ent2_dim, ent2, ent2_sig, edgeName, assoc_sig, assoc_doccount, mainGraph, mainNodeMap );
					createEdgesAndNode( ent1_name, ent1_type, ent1_dim, ent1, ent1_sig, geo_name, geo_type, geo_dim, geo, geo_sig, edgeName, assoc_sig, assoc_doccount, mainGraph, mainNodeMap );
					createEdgesAndNode( ent2_name, ent2_type, ent2_dim, ent2, ent2_sig, geo_name, geo_type, geo_dim, geo, geo_sig, edgeName, assoc_sig, assoc_doccount, mainGraph, mainNodeMap );
				}
				else
				{
					addNodesAndEdges_graph( ent1_name, ent1_type, ent1_dim, ent1, ent1_sig, ent2_name, ent2_type, ent2_dim, ent2, ent2_sig, edgeName, assoc_sig, assoc_doccount, xmlObj, mapOfHashSets, crossDimensional );
					addNodesAndEdges_graph( ent1_name, ent1_type, ent1_dim, ent1, ent1_sig, geo_name, geo_type, geo_dim, geo, geo_sig, edgeName, assoc_sig, assoc_doccount, xmlObj, mapOfHashSets, crossDimensional );
					addNodesAndEdges_graph( ent2_name, ent2_type, ent2_dim, ent2, ent2_sig, geo_name, geo_type, geo_dim, geo, geo_sig, edgeName, assoc_sig, assoc_doccount, xmlObj, mapOfHashSets, crossDimensional );
				}
				
			}
		}
		
		private function addNodesAndEdges_graph( name1:String, type1:String, dim1:String, index1:String, sig1:Number, name2:String, type2:String, dim2:String, index2:String, sig2:Number, edgeName:String, edgeSig:Number, edgeCount:Number, xmlObj:XML, mapOfHashSets:Object, crossDimensional:int ):void
		{
			if ( ( null == name1 ) || ( null == name2 ) )
			{
				return; // nothing to do
			}
			
			var graphName:String;
			
			if ( CXDIM_PARTIAL == crossDimensional )
			{
				if ( dim1.localeCompare( dim2 ) > 0 )
				{
					graphName = dim1 + "/" + dim2;
				}
				else
				{
					graphName = dim2 + "/" + dim1;
				}
			}
			else // CXDIM_PURE
			{
				if ( type1.localeCompare( type2 ) > 0 )
				{
					graphName = type1 + "/" + type2;
				}
				else
				{
					graphName = type2 + "/" + type1;
				}
				
			}
			
			var nodeMap:Object = mapOfHashSets[ graphName ] as Object;
			
			var newGraph:XML = null;
			
			if ( null == nodeMap )
			{
				// First time this type pairing has been seen
				
				// Deduplication set for nodes:
				nodeMap = new Object();
				mapOfHashSets[ graphName ] = nodeMap;
				
				// Graph:
				newGraph = <graph edgedefault="directed"></graph>
					;
				newGraph.@id = graphName;
				xmlObj.appendChild( newGraph );
				
			}
			else
			{
				newGraph = xmlObj.graph.( @id == graphName )[ 0 ];
			}
			createEdgesAndNode( name1, type1, dim1, index1, sig1, name2, type2, dim2, index2, sig2, edgeName, edgeSig, edgeCount, newGraph, nodeMap );
		} // end addNodesAndEdges_graph
		
		private function createEdgesAndNode( name1:String, type1:String, dim1:String, index1:String, sig1:Number, name2:String, type2:String, dim2:String, index2:String, sig2:Number, edgeName:String, edgeSig:Number, edgeCount:Number, graphXML:XML, nodeMap:Object ):void
		{
			if ( ( null == name1 ) || ( null == name2 ) )
			{
				return; // nothing to do
			}
			
			// Create node:
			
			var nId1:String = createNode( name1, type1, dim1, index1, sig1, graphXML, nodeMap );
			var nId2:String = createNode( name2, type2, dim2, index2, sig2, graphXML, nodeMap );
			
			// Create edge:
			
			// Useful info from the main function:
			//xmlObj.appendChild(<key id="evsig" for="edge" attr.name="significance" attr.type="double"></key>);
			//xmlObj.appendChild(<key id="docs" for="edge" attr.name="document count" attr.type="long"></key>);
			//xmlObj.appendChild(<key id="verb" for="edge" attr.name="edge relation" attr.type="string"></key>);
			
			// Create element
			var sigNode:XML = <data key="evsig"></data>
				;
			sigNode.appendChild( edgeSig );
			var nameNode:XML = <data key="docs"></data>
				;
			nameNode.appendChild( edgeCount );
			var typeNode:XML = <data key="verb"></data>
				;
			typeNode.appendChild( edgeName );
			
			var edge:XML = <edge></edge>
				;
			edge.@id = "e" + int( ++edgeCount_ ).toString();
			edge.@source = nId1;
			edge.@target = nId2;
			edge.appendChild( sigNode );
			edge.appendChild( nameNode );
			edge.appendChild( typeNode );
			
			graphXML.appendChild( edge );
		
		} // end createEdgesAndNode
		
		private function createNode( name:String, type:String, dim:String, index:String, sig:Number, graphXML:XML, nodeMap:Object ):String
		{
			// Useful info from the main function:
			//xmlObj.appendChild(<key id="sig" for="node" attr.name="significance" attr.type="double"></key>);
			//xmlObj.appendChild(<key id="name" for="node" attr.name="name" attr.type="string"></key>);
			//xmlObj.appendChild(<key id="type" for="node" attr.name="type" attr.type="string"></key>);
			//xmlObj.appendChild(<key id="dim" for="node" attr.name="dimension" attr.type="string"></key>);
			
			var nodeId:String = nodeMap[ index ];
			
			// This indicates that the node has already been create for this graph
			if ( null == nodeId )
			{
				
				// Create element
				var sigNode:XML = <data key="sig"></data>
					;
				sigNode.appendChild( sig );
				var nameNode:XML = <data key="name"></data>
					;
				nameNode.appendChild( name );
				var typeNode:XML = <data key="type"></data>
					;
				typeNode.appendChild( type );
				var dimNode:XML = <data key="dim"></data>
					;
				dimNode.appendChild( dim );
				var node:XML = <node></node>
					;
				nodeId = "n" + int( ++nodeCount_ ).toString();
				node.@id = nodeId;
				node.appendChild( sigNode );
				node.appendChild( nameNode );
				node.appendChild( typeNode );
				node.appendChild( dimNode );
				
				graphXML.appendChild( node );
				nodeMap[ index ] = nodeId;
			}
			return nodeId;
		
		} // end createNode
	} // (end class)
} // end package)
