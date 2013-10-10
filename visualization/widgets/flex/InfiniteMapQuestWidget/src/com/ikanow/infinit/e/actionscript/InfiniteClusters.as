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
package com.ikanow.infinit.e.actionscript
{
	/**
	 * This class is used to add marker management to a map by adding lat lon points
	 * to a cluster based on their distance from one another
	*/
	import com.mapquest.LatLng;
	import com.mapquest.tilemap.TilemapComponent;
	import flash.geom.Point;
	import flash.utils.ByteArray;
	import mx.collections.ArrayCollection;
	import mx.controls.Alert;
	
	public class InfiniteClusters
	{
		
		//======================================
		// private properties 
		//======================================
		
		private var entClusters:ArrayCollection = new ArrayCollection();
		
		private var bucketClusters:ArrayCollection = new ArrayCollection();
		
		private var clusters:ArrayCollection = new ArrayCollection();
		
		private var north:Number;
		
		private var south:Number;
		
		private var east:Number;
		
		private var west:Number;
		
		private var percentageAway:Number;
		
		private var currZoomLevel:Number;
		
		/**
		 * function to cluster geotags together based on their distance from one another
		*/
		private var iconSize:int = 32;
		
		//======================================
		// constructor 
		//======================================
		
		public function InfiniteClusters()
		{
		
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * cluster function, takes the entities and current dimensions and clusters the points.
		 *
		 * @param entities The array collection of entities with lat lon information
		 * @param _north The north bound
		 * @param _south The south bound
		 * @param _east The east bound
		 * @param _west The west bound
		 * @param percent The percent of distance allowed between points
		 */
		public function cluster( map:TilemapComponent, entities:ArrayCollection, _north:Number, _south:Number, _east:Number, _west:Number, percent:Number, zoomLevel:Number ):void
		{
			
			this.entClusters = entities;
			this.north = _north;
			this.south = _south;
			this.east = _east;
			this.west = _west;
			this.percentageAway = percent;
			this.currZoomLevel = zoomLevel;
			bucketClusters.removeAll();
			clusters.removeAll();
			fillCurrentView( map );
		}
		
		/**
		 * function to return the clusters array
		 *
		 * @return The array collection of clusters
		*/
		public function getClusters():ArrayCollection
		{
			return clusters;
		}
		
		//======================================
		// private methods 
		//======================================
		
		private function clusterBucket( map:TilemapComponent ):void
		{
			//create clusters until the bucketClusters array is empty
			while ( bucketClusters.length > 0 )
			{
				//get the first item in the bucketClusters array
				var firstItem:Object = bucketClusters.removeItemAt( 0 );
				
				var ll:LatLng = new LatLng( firstItem.geotag.lat, firstItem.geotag.lon )
				var p:Point = map.llToPix( ll );
				// Note p is the top-left of the icon
				
				var minX:int = p.x - iconSize; // (the worst case of 2 icons directly adjacent)
				var minY:int = p.y - iconSize;
				var maxX:int = p.x + iconSize;
				var maxY:int = p.y + iconSize;
				
				//add the first item to the firstBucket array to hold for later
				var firstBucket:ArrayCollection = new ArrayCollection();
				firstBucket.addItem( firstItem );
				
				//loop through the bucketClusters array to find points to add to the cluster
				//if they are close enough
				for ( var i:int = 0; i < bucketClusters.length; i++ )
				{
					var locToCheck:LatLng = new LatLng( bucketClusters.getItemAt( i ).geotag.lat, bucketClusters.getItemAt( i ).geotag.lon );
					p = map.llToPix( locToCheck );
					
					if ( ( p.x > minX ) && ( p.x < maxX ) && ( p.y > minY ) && ( p.y < maxY ) )
					{
						firstBucket.addItem( bucketClusters.removeItemAt( i ) );
						i -= 1;
					}
				}
				
				//add the clusters to the clusters array
				clusters.addItem( firstBucket );
			}
		}
		
		/**
		 * function to fill the currentview with geotags
		*/
		private function fillCurrentView( map:TilemapComponent ):void
		{
			//loop through thte entClusters array to get entities that have 
			//latitudes and longitudes that fit into the current view of the maps
			//zoom level and add them to the bucketClusters array
			for each ( var ents:Object in entClusters )
			{
				//filter based on what ontology types are currently selected
				if ( InfiniteMapUtils.allowOntologyType( ents.ontology_type ) )
					bucketClusters.addItem( ents );
			}
			
			//call clusterBucket to create clusters
			clusterBucket( map );
		}
	}
}
