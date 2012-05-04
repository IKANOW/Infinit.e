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
//
// Presentation layer for the InfiniteMapWidget
//
// Encapsulates the logic that processes data arriving from the model layer
// and sends the resulting "to display" data to the visualization layer
// (and conversely re-processes based on user controls received from the visualization layer)
//

package com.ikanow.infinit.e.actionscript
{
	// Imports
	import com.ikanow.infinit.e.actionscript.InfiniteClusters;
	import com.ikanow.infinit.e.widget.library.widget.IResultSet;
	import com.ikanow.infinit.e.widget.library.widget.IWidgetContext;
	import com.mapquest.LatLng;
	import com.mapquest.tilemap.RectLL;
	import com.mapquest.tilemap.TilemapComponent;
	import flash.geom.Point;
	import flash.utils.setTimeout;
	import mx.collections.ArrayCollection;
	import mx.controls.Alert;
	import system.data.sets.HashSet;
	
	/**
	 * This class contains all the business logic for the Map Widget
	 */
	public class InfiniteMapPresentationLayer
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var _filteringEnabled:Boolean = true;
		
		//======================================
		// private properties 
		//======================================
		
		//threshold for matching lat/lng
		private const ROUND_ERROR:Number = .0001;
		
		// 1] Accessors to the Model and Visualization layers
		
		
		private var _guiLayer:InfiniteMapQuestWidget = null;
		
		private var _modelLayer:InfiniteMapQuestWidget = null;
		
		// 2] State		
		
		// Unfortunately needed for some lat/long conversion functions, don't use for anything
		// display related in the presentation layer!
		private var _mapObj:TilemapComponent = null;
		
		// Map settings:
		private var _currBounds:RectLL = null;
		
		private var _currZoom:Number = -1;
		
		// Data:
		private var _displayData:ArrayCollection = null;
		
		private var _entityMarkers:ArrayCollection = null;
		
		private var _clusters:ArrayCollection = null;
		
		// Filter state:
		private var _filteredGeoNames:HashSet = null;
		
		private var _justFiltered:Boolean = false;
		
		// Heatmap state:
		private var _geoCounts:ArrayCollection = null;
		
		private var _maxGeoValue:Number = 0;
		
		private var _minGeoValue:Number = 0;
		
		// Lat long bounds for auto zoom
		private var _nPointsUsedInAutoZoom:int = 0;
		
		private var _minLat:Number = 180;
		
		private var _minLng:Number = 180;
		
		private var _maxLat:Number = -180;
		
		private var _maxLng:Number = -180;
		
		private var _onlyOnNewQuery:IResultSet = null;
		
		private var ontologyData:ArrayCollection = new ArrayCollection( [ { label: "Point", data: "point", type: "check", toggled: "true" },
																		  { label: "City", data: "city", type: "check", toggled: "true" },
																		  { label: "CountrySubsidiary", data: "countrysubsidiary", type: "check", toggled: "true" },
																		  { label: "Country", data: "country", type: "check", toggled: "true" },
																		  { label: "GeographicalRegion", data: "geographicalregion", type: "check", toggled: "true" },
																		  { label: "Continent", data: "continent", type: "check", toggled: "true" } ] );
		
		private var showOptionsData:ArrayCollection = new ArrayCollection( [ { label: "Heat map overlay", data: "heatMap", type: "check", toggled: "true" },
																			 { label: "Geo-tagged documents", data: "geoDocuments", type: "check", toggled: "true" },
																			 { label: "Geo-tagged entities", data: "geoEntities", type: "check", toggled: "true" },
																			 { label: "Geo-tagged events", data: "geoEvents", type: "check", toggled: "true" } ] );
		
		//======================================
		// constructor 
		//======================================
		
		/**
		 * Constructor
		 *
		 * @param guiLayer: A pointer to the visualization layer interface (ie interface with the GUI)
		 * @param modelLayer: A pointer to the model layer (ie interface with the framework)
		 */
		public function InfiniteMapPresentationLayer( guiLayer:InfiniteMapQuestWidget, modelLayer:InfiniteMapQuestWidget )
		{
			_guiLayer = guiLayer;
			_modelLayer = modelLayer;
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		// 4] Utility functions		
		
		public function doMapDisplayProcessing():void
		{
			if ( ( null != _currBounds ) && ( -1 != _currZoom ) && ( null != _entityMarkers ) && ( null != _mapObj ) )
			{
				InfiniteMapUtils.allowedOntologyData = ontologyData;
				//instantiate the infiniteClusters class to get the group clusters
				var clusterer:InfiniteClusters = new InfiniteClusters();
				clusterer.cluster( _mapObj, _entityMarkers, _currBounds.ul.lat, _currBounds.lr.lat, _currBounds.ul.lng, _currBounds.lr.lng, .1, _currZoom );
				_clusters = clusterer.getClusters();
				
				_guiLayer.renderMapOverlay_fromPresentation( _clusters, _filteredGeoNames );
				
				// Heatmap display:
				doHeatMapProcessing();
			}
		}
		
		public function getAllowedOntologyTypes():ArrayCollection
		{
			return ontologyData;
		}
		
		public function getShowOptions():ArrayCollection
		{
			return showOptionsData;
		}
		// heatMapRedraw_fromGUI - redisplay the heatmap following enable/resize/move
		public function heatMapRedraw_fromGUI():void
		{
			doHeatMapProcessing();
		}
		
		// onFilterClusterFromLatLong_fromGUI - the user has selected a single lat/long corresponding
		//                                      to a cluster - filter on that cluster
		// @param lat: Number
		// @param lon: Number
		
		public function onFilterClusterFromLatLong_fromGUI( lat:Number, lon:Number ):void
		{
			var clickedMarker:ArrayCollection = findCluster( lat, lon );
			
			//send event w/ all id's
			if ( clickedMarker != null )
			{
				var docIds:HashSet = new HashSet();
				_filteredGeoNames = new HashSet();
				_justFiltered = true;
				
				for ( var i:int = 0; i < clickedMarker.length; i++ )
				{
					//make sure clickedMarker is something we can see (not hidden with menu selections)
					var marker:Object = clickedMarker[ i ];
					
					if ( ( marker.object_type == "docgeo" && _guiLayer.SHOW_GEO_DOCUMENTS ) ||
						( marker.object_type == "entity" && _guiLayer.SHOW_GEO_ENTITIES ) ||
						( marker.object_type == "event" && _guiLayer.SHOW_GEO_EVENTS ) )
					{
						_filteredGeoNames.add( clickedMarker[ i ].index );
						
						//first check if this markers feed was already added
						if ( !docIds.contains( clickedMarker[ i ].feed ) )
						{
							docIds.add( clickedMarker[ i ].feed );
						}
					}
				}
				// Build description
				var desc:String = "Geo Filter: ";
				i = 0;
				
				for each ( var s:String in _filteredGeoNames.toArray() )
				{
					if ( 0 != i )
					{
						desc += " OR ";
					}
					desc += s;
					i++;
				}
				_modelLayer.onReceiveFilterRequest_fromPresentation( docIds, desc );
			}
		}
		
		// onMapSizeChanged - respond to a change in the map size / zoom level
		//
		// @param newBounds: LatLngBounds
		
		public function onMapSizeChanged_fromGUI( newBounds:RectLL, newZoom:Number, mapObj:TilemapComponent ):void
		{
			if ( null != newBounds )
			{
				_currBounds = newBounds;
			}
			
			if ( -1 != newZoom )
			{
				_currZoom = newZoom;
			}
			
			if ( null != mapObj )
			{
				_mapObj = mapObj;
			}
			this.doMapDisplayProcessing();
		}
		
		// 3] Public interface functions		
		
		// SUMMARY:
		// onNewMapData_fromModel(displayData, filtered):void
		// onMapSizeChanged_fromGUI(newBounds, newZoom, mapObj):void
		// onFilterClusterFromLatLong_fromGUI(lat, lon):void
		// toggleFilter_fromGUI(filterEnabled):void
		// heatMapRedraw_fromGUI:void
		
		// generateOverlay - creates clusters for the visualization layer to display 
		//
		// @param displayData: ArrayCollection of feed objects
		// @param baseQueryResults: contains aggregation
		// @param filtered: if the call was the result of a filter event (or a query event if "false")
		
		public function onNewMapData_fromModel( displayData:ArrayCollection, baseQueryResults:IResultSet, filtered:Boolean ):void
		{
			// Reset auto-zoom bounds
			_nPointsUsedInAutoZoom = 0;
			_minLat = 180;
			_minLng = 180;
			_maxLat = -180;
			_maxLng = -180;
			
			// If filtering not enabled, and this data is filtered - do nothing
			if ( filtered && !_filteringEnabled )
			{
				return;
			}
			
			if ( !_justFiltered )
			{ // New query or filter, clear filter state
				if ( filtered )
				{
					_filteredGeoNames = handleOldGeoFilter( baseQueryResults );
				}
				else
				{
					_filteredGeoNames = null;
				}
			}
			else
			{ // This was called by us, so retain geo name information
				_justFiltered = false;
			}
			
			_displayData = displayData;
			_entityMarkers = null;
			
			if ( null != _displayData )
			{
				_entityMarkers = new ArrayCollection();
				
				//loop through the knowledge array to get the feeds
				for each ( var feed:Object in _displayData )
				{
					//loop through the feed to get its entities
					for each ( var entity:Object in feed.entities )
					{
						//check to make sure the entity has geotags 
						if ( entity.geotag != null )
						{
							//add the feed id to the entity to know where it came from
							entity.object_type = "entity";
							entity[ "feed" ] = feed._id;
							entity.feedObj = feed;
							//add the entity to the entitymarkers array to be passed into the 
							//infiniteClusters class
							_entityMarkers.addItem( entity );
							
							if ( !filtered )
							{
								_nPointsUsedInAutoZoom++;
								
								if ( entity.geotag.lat < _minLat )
									_minLat = entity.geotag.lat;
								
								if ( entity.geotag.lat > _maxLat )
									_maxLat = entity.geotag.lat;
								
								if ( entity.geotag.lon < _minLng )
									_minLng = entity.geotag.lon;
								
								if ( entity.geotag.lon > _maxLng )
									_maxLng = entity.geotag.lon;
							}
						}
					}
					
					//loop through the feed to get its events
					for each ( var event:Object in feed.associations )
					{
						//check to make sure the event has geotags
						if ( event.geotag != null )
						{
							event.object_type = "event";
							event.feed = feed._id;
							event.feedObj = feed;
							event.index = event.geo_index;
							_entityMarkers.addItem( event );
							
							if ( !filtered )
							{
								_nPointsUsedInAutoZoom++;
								
								if ( event.geotag.lat < _minLat )
									_minLat = event.geotag.lat;
								
								if ( event.geotag.lat > _maxLat )
									_maxLat = event.geotag.lat;
								
								if ( event.geotag.lon < _minLng )
									_minLng = event.geotag.lon;
								
								if ( event.geotag.lon > _maxLng )
									_maxLng = event.geotag.lon;
							}
						}
					}
					
					if ( feed.docGeo != null )
					{
						var docGeoObject:Object = new Object();
						docGeoObject[ "object_type" ] = "docgeo";
						docGeoObject[ "feed" ] = feed._id;
						docGeoObject[ "feedObj" ] = feed;
						docGeoObject[ "geotag" ] = new Object();
						docGeoObject[ "geotag" ][ "lat" ] = feed.docGeo.lat;
						docGeoObject[ "geotag" ][ "lon" ] = feed.docGeo.lon;
						docGeoObject[ "index" ] = "docgeo(" + feed.docGeo.lat + "," + feed.docGeo.lon + ")";
						_entityMarkers.addItem( docGeoObject );
					}
				}
			}
			
			// Re-calculate heat map (only if model has changed, not filter)
			if ( !filtered && ( null != baseQueryResults ) )
			{
				if ( ( null != baseQueryResults.getGeoCounts() ) && ( baseQueryResults.getGeoCounts().length > 0 ) )
				{
					_guiLayer.setHeatMapAvailable_fromPresentation( true );
					doHeatMapProcessing_StateChange( baseQueryResults.getGeoCounts(), baseQueryResults.getGeoMinCount(), baseQueryResults.getGeoMaxCount() );
				}
				else
				{
					_guiLayer.setHeatMapAvailable_fromPresentation( false );
					doHeatMapProcessing_StateChange( null, 0, 0 );
				}
			}
			
			if ( !filtered && ( _onlyOnNewQuery != baseQueryResults ) && ( _nPointsUsedInAutoZoom > 0 ) )
			{
				_onlyOnNewQuery = baseQueryResults;
				_guiLayer.recenterAndZoomMap_fromPresentation( _minLat, _minLng, _maxLat, _maxLng ); //recenter map on new query only
			}
			
			// Display map data
			this.doMapDisplayProcessing();
		}
		
		// Handle requests from the GUI layer to update the query 
		
		public function onUpdateQuery_fromGUI( latlng:LatLng, distance:Number, bAddNotDecay:Boolean ):void
		{
			if ( !bAddNotDecay )
			{
				distance /= 2; // "half-life"
				this._modelLayer.setRegionAsGeoDecay_fromPresentation( latlng, distance );
			}
			else
			{
				this._modelLayer.addRegionToQuery_fromPresentation( latlng, distance );
			}
		}
		
		// onZoomToCluster_fromGUI - the user has selected a single lat/long corresponding
		//                                      to a cluster - zoom to that cluster
		// @param lat: Number
		// @param lon: Number
		
		public function onZoomToCluster_fromGUI( lat:Number, lon:Number ):void
		{
			var clickedMarker:ArrayCollection = findCluster( lat, lon );
			
			//send event w/ all id's
			if ( clickedMarker != null )
			{
				// Reset auto-zoom bounds
				_nPointsUsedInAutoZoom = 0;
				_minLat = 180;
				_minLng = 180;
				_maxLat = -180;
				_maxLng = -180;
				
				for ( var i:int = 0; i < clickedMarker.length; i++ )
				{
					_nPointsUsedInAutoZoom++;
					
					if ( clickedMarker[ i ].geotag.lat < _minLat )
						_minLat = clickedMarker[ i ].geotag.lat;
					
					if ( clickedMarker[ i ].geotag.lat > _maxLat )
						_maxLat = clickedMarker[ i ].geotag.lat;
					
					if ( clickedMarker[ i ].geotag.lon < _minLng )
						_minLng = clickedMarker[ i ].geotag.lon;
					
					if ( clickedMarker[ i ].geotag.lon > _maxLng )
						_maxLng = clickedMarker[ i ].geotag.lon;
				}
				_guiLayer.recenterAndZoomMap_fromPresentation( _minLat, _minLng, _maxLat, _maxLng, true ); //recenter map on new query only
				
				// Display map data
				this.doMapDisplayProcessing();
			}
		}
		public function setAllowedOntologyTypes( ontData:ArrayCollection ):void
		{
			ontologyData = ontData;
		}
		
		// toggleFilter_fromGUI - toggles between filtering showor hidden
		
		public function toggleFilter_fromGUI( filterEnabled:Boolean ):void
		{
			_filteringEnabled = filterEnabled; // (save permanent state)
			
			var filteredGeoNames_saved:HashSet = _filteredGeoNames; // (save transient state)
			
			if ( filterEnabled )
			{
				_justFiltered = true; // (So that won't discard geo-names, used in display logic)
			}
			
			// Re-render the data
			onNewMapData_fromModel( _modelLayer.getData_fromPresentation( filterEnabled ), null, filterEnabled );
			
			// Reset state, if required
			_filteredGeoNames = filteredGeoNames_saved;
		}
		
		//======================================
		// private methods 
		//======================================
		
		// Actual display processing (Every time)
		
		private function doHeatMapProcessing():void
		{
			if ( _guiLayer.isHeatMapEnabled_fromPresentation() )
			{
				if ( null == _geoCounts )
				{
					return;
				}
				else if ( 0 == _geoCounts.length )
				{
					_guiLayer.renderHeatMapOverlay_fromPresentation( null );
					return;
				}
				
				// These seem to work better than 90,10,leave alone? Obv leave  like this for log scale...
				var valueSpread:Number = 100.0;
				var valueFloor:Number = 0.0;
				_minGeoValue = 0.0;
				
				var xmlGeo:XML = <root></root>
					;
				var numAdded:int = 0;
				
				for each ( var geoPoint:Object in _geoCounts )
				{
					if ( InfiniteMapUtils.allowOntologyType( geoPoint[ "type" ] ) )
					{
						var ll:LatLng = new LatLng( geoPoint[ "lat" ], geoPoint[ "lon" ] );
						
						var p:Point = _mapObj.llToPix( ll );
						
						if ( ( p.x >= 0 ) && ( p.y >= 0 ) && ( p.x < _mapObj.width ) && ( p.y < _mapObj.height ) )
						{
							numAdded++;
							var xmlGeoPoint:XML = <point></point>
								;
							xmlGeoPoint.@x = p.x;
							xmlGeoPoint.@y = p.y;
							var nToWrite:int = int( 20 * ( ( geoPoint[ "count" ] as Number ) - _minGeoValue - 0.001 ) / ( _maxGeoValue - _minGeoValue ) );
							
							//-0.001 in conjunction with <= below => max count is 20
							for ( var i:int = 0; i <= nToWrite; ++i )
							{
								xmlGeo.appendChild( xmlGeoPoint );
							}
						}
					}
				}
				
				if ( numAdded > 0 )
				{
					_guiLayer.renderHeatMapOverlay_fromPresentation( xmlGeo.point as XMLList );
				}
				else
				{
					_guiLayer.renderHeatMapOverlay_fromPresentation( null );
				}
			}
		}
		
		// Create the heat map array based on lat/longs and counts
		// Call the GUI layer actually to invoke the heat map display
		
		// State processing (Only when model data changes)
		
		private function doHeatMapProcessing_StateChange( geo:ArrayCollection, maxValue:Number, minValue:Number ):void
		{
			_geoCounts = geo;
			_maxGeoValue = maxValue;
			_minGeoValue = minValue;
			
			if ( null != _geoCounts )
			{
				for each ( var geoPoint:Object in _geoCounts )
				{
					var ll:LatLng = new LatLng( geoPoint[ "lat" ], geoPoint[ "lon" ] );
					
					// Just get bounds
					_nPointsUsedInAutoZoom++;
					
					if ( ll.lat < _minLat )
						_minLat = ll.lat;
					
					if ( ll.lat > _maxLat )
						_maxLat = ll.lat;
					
					if ( ll.lng < _minLng )
						_minLng = ll.lng;
					
					if ( ll.lng > _maxLng )
						_maxLng = ll.lng;
				}
			}
		}
		
		/**
		 * function to find entities/feeds under the given cluster point
		 *
		 * (ISSUE: the other widgets all filter on the underlying data, not
		 *  what happens to be displayed at exactly that moment in time ...
		 *  it's a bit tricky because a cluster is only meaningful in the context
		 *  of what's been clustered, so you'd need to store the clusterings of both filtered
		 *  data and non-filtered data for that to make sense - yuck!)
		 *
		 * @param lat The latitude of the marker
		 * @param lon The longitude of the marker
		 *
		 * @return returns the group of the cluster with the given lat long;
		 * 		   returns null if that cluster can't be found
		 */
		private function findCluster( lat:Number, lon:Number ):ArrayCollection
		{
			//mapquest seems to only allow a certain precision for lat/lng so causing some rounding issues						
			for each ( var group:ArrayCollection in _clusters )
			{
				var latDelta:Number = Math.abs( group[ 0 ].geotag.lat - lat );
				var lonDelta:Number = Math.abs( group[ 0 ].geotag.lon - lon );
				
				if ( latDelta < ROUND_ERROR && lonDelta < ROUND_ERROR )
				{
					return group;
				}
			}
			return null;
		}
		
		// Utility - if we're being opened with a geo filter (not by us just now) then re-calc the names
		
		private function handleOldGeoFilter( baseQueryResults:IResultSet ):HashSet
		{
			var filterDesc:String = baseQueryResults.getDescription();
			
			if ( ( null == filterDesc ) || ( filterDesc.substr( 0, 12 ) != "Geo Filter: " ) )
			{
				return null;
			}
			var indexSet:HashSet = new HashSet();
			
			for each ( var w:String in filterDesc.substr( 12 ).split( " OR " ) )
			{
				indexSet.add( w );
			}
			return indexSet;
		}
	} // end class InfiniteMapPresentationLayer
}
