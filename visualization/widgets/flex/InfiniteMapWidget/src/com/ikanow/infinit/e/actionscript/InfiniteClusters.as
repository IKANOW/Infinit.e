package com.ikanow.infinit.e.actionscript
{
	/**
	 * This class is used to add marker management to a map by adding lat lon points
	 * to a cluster based on their distance from one another
	*/	
	
	import com.google.maps.LatLng;
	import com.google.maps.LatLngBounds;
	import com.google.maps.interfaces.IMap;
	
	import flash.geom.Point;
	import flash.utils.ByteArray;
	
	import mx.collections.ArrayCollection;
	import mx.controls.Alert;
	
	public class InfiniteClusters
	{
		private var entClusters:ArrayCollection = new ArrayCollection();
		private var bucketClusters:ArrayCollection = new ArrayCollection();
		private var clusters:ArrayCollection = new ArrayCollection();
		private var north:Number;
		private var south:Number;
		private var east:Number;
		private var west:Number;
		private var percentageAway:Number;
		private var g:LatLngBounds;
		private var currZoomLevel:Number;
		
		
		public function InfiniteClusters()
		{
			
		}
		
		/**
		 * cluster function, takes the entities and current dimensions and clusters the points.
		 * 
		 * @param entities The array collection of entities with lat lon information
		 * @param _north The north bound
		 * @param _south The south bound
		 * @param _east The east bound
		 * @param _west The west bound
		 * @param percent The percent of distance allowed between points
		 * @param _g The lat long bounds
		 */ 
		public function cluster(map: IMap, entities:ArrayCollection, _north:Number, _south:Number, _east:Number, _west:Number, percent:Number, _g:LatLngBounds, zoomLevel:Number):void
		{
			
			this.entClusters = entities;
			this.north = _north;
			this.south = _south;
			this.east = _east;
			this.west = _west;
			this.percentageAway = percent;
			this.g = _g;
			this.currZoomLevel = zoomLevel;
			bucketClusters.removeAll();
			clusters.removeAll();
			fillCurrentView(map);
		}		
		
		/**
		 * function to fill the currentview with geotags
		*/
		private function fillCurrentView(map: IMap):void
		{
			//loop through thte entClusters array to get entities that have 
			//latitudes and longitudes that fit into the current view of the maps
			//zoom level and add them to the bucketClusters array
			for each(var ents:Object in entClusters)
			{
				//filter based on what ontology types are currently selected
				if ( InfiniteMapUtils.allowOntologyType(ents.ontology_type) )
					bucketClusters.addItem(ents);				
			}
			
			//call clusterBucket to create clusters
			clusterBucket(map);
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
		
		/**
		 * function to cluster geotags together based on their distance from one another
		*/
		private var iconSize:int = 32;
		
		private function clusterBucket(map: IMap):void
		{
			//create clusters until the bucketClusters array is empty
			while(bucketClusters.length > 0)
			{
				//get the first item in the bucketClusters array
				var firstItem:Object = bucketClusters.removeItemAt(0);
				
				var ll:LatLng = new LatLng(firstItem.geotag.lat, firstItem.geotag.lon)
				var p:Point = map.fromLatLngToPoint(ll);
					// Note p is the top-left of the icon

				var minX:int = p.x - iconSize; // (the worst case of 2 icons directly adjacent)
				var minY:int = p.y - iconSize;
				var maxX:int = p.x + iconSize; 
				var maxY:int = p.y + iconSize;
								
				//add the first item to the firstBucket array to hold for later
				var firstBucket:ArrayCollection = new ArrayCollection();
				firstBucket.addItem(firstItem);
				
				//loop through the bucketClusters array to find points to add to the cluster
				//if they are close enough
				for(var i:int = 0; i < bucketClusters.length; i++)
				{
					var locToCheck:LatLng = new LatLng(bucketClusters.getItemAt(i).geotag.lat,bucketClusters.getItemAt(i).geotag.lon);
					p = map.fromLatLngToPoint(locToCheck);
					if( (p.x > minX) && (p.x < maxX) && (p.y > minY) && (p.y < maxY) ) 
					{
						firstBucket.addItem(bucketClusters.removeItemAt(i));						
						i -= 1;
					}
				}
				
				//add the clusters to the clusters array
				clusters.addItem(firstBucket);
			}
		}	
	}
}