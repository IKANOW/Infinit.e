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
	
	import com.google.maps.LatLng;
	import com.google.maps.LatLngBounds;
	import com.google.maps.Map;
	import com.google.maps.extras.xmlparsers.kml.*;
	import com.google.maps.overlays.GroundOverlay;
	import com.google.maps.overlays.Marker;
	import com.google.maps.overlays.MarkerOptions;
	import com.google.maps.overlays.Polygon;
	import com.google.maps.overlays.PolygonOptions;
	import com.google.maps.overlays.Polyline;
	import com.google.maps.styles.FillStyle;
	import com.google.maps.styles.StrokeStyle;
	
	import flash.display.Loader;
	import flash.events.Event;
	import flash.net.URLRequest;
	
	import mx.core.BitmapAsset; 
	
	/** 
	 * This class is based on the open source KMLParser.mxml written by 
	 Google employee, Pamela Fox. 
	 * Jeff Smith adapted it into this class (OOP) form and added 
	 support for alternative kml namespaces, setting the opacity, 
	 * turning on/off polygon edges, adding custom marker icons (for 
	 placemarks). 
	 * Required Lib: GoogleMapsAPIUtilityLibrary_04262009.swc 
	 */ 
	public class GoogleMapKMLLoader
	{

		
		private var map : Map; 
		private var markerTooltipMsg : String; 
		private var kmlObj : Object; 
		private const KML_NAMESPACE : String = "<kml xmlns='http://earth.google.com/kml/2.2' xmlns:gx='http://www.google.com/kml/ext/2.2' xmlns:atom='http://www.w3.org/2005/Atom'>"; 
		public var showPolygonEdges : Boolean = true; 
		public var opacity : Number = -1; 
		//[Embed(source="blue-dot.png")] 
		//[Bindable]public var imgCls : Class; 
		
		public function GoogleMapKMLLoader(map : Map, markerTooltipMsg : 
										   String) 
		{ 
			this.map = map; 
			this.markerTooltipMsg = markerTooltipMsg; 
		} 
		
		
		private function fixKMLNamespace(kmlStr : String) : String 
		{ 
			var startPos : int = kmlStr.indexOf("<kml"); 
			var endPos : int = kmlStr.indexOf(">", startPos); 
			var fixedKmlStr : String = kmlStr.substring(0, startPos) + 
				KML_NAMESPACE + kmlStr.substring(endPos+1, kmlStr.length); 
			return(fixedKmlStr); 
		} 
		public function loadKML(kmlStr : String):void 
		{ 
			// (don't clear overlays here, done by calling infrastructure)
			kmlObj = new Object(); 
			var fixedKmlStr : String = fixKMLNamespace(kmlStr); 
			var kml:Kml22 = new Kml22(fixedKmlStr); 
		
			//var kml:Kml22 = new Kml22(kmlStr);
			var rootFeature:Feature = kml.feature; 
			kmlObj.name = rootFeature.name; 
			kmlObj.mapObjs = new Array(); 
			kmlObj.bounds = new LatLngBounds(); 
			if (canContainFeatures(rootFeature)) 
				kmlObj.children = getChildrenFeatures(Container(rootFeature)); 
			else 
				associateWithMapObject(kmlObj, rootFeature); 
		} 
		private function getChildrenFeatures(container:Container):Array 
		{ 
			var childrenFeatures:Array = new Array(); 
			for (var i:Number = 0; i < container.features.length; i++) 
			{ 
				var feature:Feature = container.features[i]; 
				var childObj:Object = new Object(); 
				childObj.mapObjs = new Array(); 
				childObj.name = feature.name; 
				if (childObj.name == null) 
					childObj.name = getAlternateName(feature); 
				if (canContainFeatures(feature)) 
					childObj.children = getChildrenFeatures(Container(feature)); 
				else 
					associateWithMapObject(childObj, feature); 
				childrenFeatures.push(childObj); 
			} 
			return childrenFeatures; 
		} 
		private function getAlternateName(feature:Feature):String 
		{ 
			if (feature is Folder) 
				return "Unnamed Folder"; 
			else if (feature is Document) 
				return "Unnamed Document"; 
			else if (feature is Placemark) 
			{ 
				var placemark:Placemark = Placemark(feature); 
				if (placemark.geometry != null) 
				{ 
					if (placemark.geometry is KmlPoint) 
						return "Unnamed Point"; 
					else if (placemark.geometry is LineString) 
						return "Unnamed Linestring"; 
					else if (placemark.geometry is LinearRing) 
						return "Unnamed LinearRing"; 
					else if (placemark.geometry is KmlPolygon) 
						return "Unnamed Polygon"; 
				} 
				return "Unnamed Placemark"; 
			} 
			else if (feature is 
				com.google.maps.extras.xmlparsers.kml.GroundOverlay::KmlGroundOverlay) 
				return "Unnamed GroundOverlay"; 
			return "Unnamed Feature"; 
		} 
		private function associateWithMapObject(obj:Object, 
												feature:Feature):void 
		{ 
			// at this point it can either be a placemark or a groundoverlay 
			if (feature is Placemark) 
			{ 
				var placemark:Placemark = Placemark(feature); 
				if (placemark.geometry != null) 
				{ 
					var placemarkDesc : String = "";
					
					// Added to ensure that name for the placemark is used vs the description field
					// Needs additional parsing to ensure for long descriptions it is not added ot the tooltip but to
					// some other type of visualization
					//placemarkDesc = placemark.description;
			
					if (placemark.name != null) {
						placemarkDesc = placemark.name; 
					}
					
					placemarkDesc = placemarkDesc.replace("<br>", 
						"\n").replace("<br/>", "\n"); 
					associateGeometryWithMapObject(obj, placemarkDesc, 
						placemark.geometry, placemark.styleUrl); 
				} 
			} 
			else if (feature is KmlGroundOverlay) 
			{ 
				var groundOverlay:KmlGroundOverlay = 
					KmlGroundOverlay(feature); 
				var latLngBounds:LatLngBounds = new LatLngBounds(new 
					LatLng(groundOverlay.latLonBox.south,groundOverlay.latLonBox.west), 
					new 
					LatLng(groundOverlay.latLonBox.north,groundOverlay.latLonBox.east)); 
				updateLatLngBounds(obj, latLngBounds); 
				var testLoader:Loader = new Loader(); 
				var urlRequest:URLRequest = new 
					URLRequest(groundOverlay.icon.href); 
				testLoader.contentLoaderInfo.addEventListener 
					( 
						Event.COMPLETE, 
						function(e:Event):void 
						{ 
							obj.mapObject = new 
							com.google.maps.overlays.GroundOverlay::GroundOverlay(testLoader, 
								latLngBounds); 
							map.addOverlay(obj.mapObject); 
						}); 
				testLoader.load(urlRequest); 
			} 
		} 
		private function getRGBFromKMLStyle(styleURL : String) : Number 
		{ 
			if (styleURL == null) 
				return(uint("0x008080")); 
			else 
			{ 
				var red : String = styleURL.substring(6, 8); 
				var green : String = styleURL.substring(4, 6); 
				var blue :  String = styleURL.substring(2, 4); 
				var fillColor : Number = uint("0x" + red + green + blue); 
				return(fillColor); 
			} 
		} 
		private function associateGeometryWithMapObject(obj:Object, 
														placemarkDesc: String, geometry:Geometry, styleURL : String):void 
		{ 
			var multiGeometry : MultiGeometry = null; 
			var isMultigeometryOfOnePolygon : Boolean = false; 
			if (geometry is MultiGeometry) 
			{ 
				multiGeometry = MultiGeometry(geometry); 
				if (multiGeometry.geometries.length == 1) 
					isMultigeometryOfOnePolygon = true; 
			} 
			var polyline:Polyline; 
			var fillAlpha : Number; 
			if (styleURL != null && styleURL.charAt(0) == '#') 
			{ 
				styleURL = styleURL.substr(1, styleURL.length); 
				fillAlpha = parseInt(styleURL.substr(0, 2), 16) / 256.0; 
			} 
			else 
			{ 
				styleURL = "FF0000a1"; 
				fillAlpha = 0.75; 
			} 
			if (opacity >= 0)  //if opacity level set, then use it (overriding what is in the KML) 
				fillAlpha = opacity; 
			var thisFillStyle: FillStyle = new FillStyle(); 
			thisFillStyle.alpha = fillAlpha; 
			thisFillStyle.color = getRGBFromKMLStyle(styleURL); 
			var thisStrokeStyle : StrokeStyle = new StrokeStyle(); 
			if (showPolygonEdges) 
				thisStrokeStyle.alpha = 0.8; 
			else 
				thisStrokeStyle.alpha = 0.0; 
			thisStrokeStyle.color = 0xffffff; 
			if (geometry is KmlPoint) 
			{ 
				var point:KmlPoint = KmlPoint(geometry); 
				var latlng:LatLng = new 
					LatLng(point.coordinates.coordsList[0].lat, 
						point.coordinates.coordsList[0].lon); 
				var markerOptions : MarkerOptions = new MarkerOptions(); 
				
				// Chris Commented Out
				//var planeIcon : BitmapAsset = new imgCls() as BitmapAsset; 
				//markerOptions.icon = planeIcon; 
				
				
				var fillStyle:FillStyle = new FillStyle();
				fillStyle.alpha = Number("0.5");
				markerOptions.fillStyle = fillStyle;
				
				
				markerOptions.tooltip = markerTooltipMsg + latlng; 
				markerOptions.iconAlignment = 
					MarkerOptions.ALIGN_HORIZONTAL_CENTER; 
				markerOptions.hasShadow = true; 
				markerOptions.radius = 6; 
				var marker: Marker = new Marker(latlng, markerOptions); 
				obj.mapObjs.push(marker); 
				updateLatLngBounds(obj, new LatLngBounds(latlng, latlng)); 
				map.addOverlay(obj.mapObjs[obj.mapObjs.length -1]); 
			} 
			else if (geometry is LineString) 
			{ 
				var lineString:LineString = LineString(geometry); 
				polyline = new 
					Polyline(getCoordinatesLatLngs(lineString.coordinates)); 
				obj.mapObjs.push(polyline); 
				updateLatLngBounds(obj, polyline.getLatLngBounds()); 
				obj.center = polyline.getLatLngBounds().getCenter(); 
				obj.bounds = polyline.getLatLngBounds(); 
				map.addOverlay(polyline); 
			} 
			else if (geometry is LinearRing) 
			{ 
				var linearRing:LinearRing = LinearRing(geometry); 
				polyline = new 
					Polyline(getCoordinatesLatLngs(linearRing.coordinates)); 
				obj.mapObjs.push(polyline); 
				updateLatLngBounds(obj, polyline.getLatLngBounds()); 
				map.addOverlay(polyline); 
			} 
			else if (geometry is KmlPolygon || isMultigeometryOfOnePolygon) 
			{ 
				var kmlPolygon : KmlPolygon; 
				if (isMultigeometryOfOnePolygon) 
				{ 
					multiGeometry = MultiGeometry(geometry); 
					kmlPolygon = multiGeometry.geometries[0]; 
				} 
				else 
					kmlPolygon = KmlPolygon(geometry); 
				var polygonOptions : PolygonOptions = new 
					PolygonOptions({fillStyle:thisFillStyle, strokeStyle:thisStrokeStyle, 
						tooltip:placemarkDesc}); 
				var polygon : com.google.maps.overlays.Polygon = new 
					com.google.maps.overlays.Polygon(getCoordinatesLatLngs(kmlPolygon.outerBoundaryIs.linearRing.coordinates), 
						polygonOptions); 
				obj.mapObjs.push(polygon); 
				updateLatLngBounds(obj, polygon.getLatLngBounds()); 
				map.addOverlay(polygon); 
			} 
			else if (geometry is MultiGeometry) 
			{ 
				multiGeometry = MultiGeometry(geometry); 
				for (var i:uint = 0; i < multiGeometry.geometries.length; i++) 
				{ 
					associateGeometryWithMapObject(obj, placemarkDesc, 
						multiGeometry.geometries[i], styleURL); 
				} 
			} 
		} 
		private function 
			getCoordinatesLatLngs(coordinates:Coordinates):Array 
		{ 
			var latlngs:Array = new Array(); 
			for (var i:Number = 0; i < coordinates.coordsList.length; i++) 
			{ 
				var coordinate:Object = coordinates.coordsList[i]; 
				latlngs.push(new LatLng(Number(coordinate.lat), 
					Number(coordinate.lon))); 
			} 
			return latlngs; 
		} 
		private function updateLatLngBounds(obj:Object, 
											bounds:LatLngBounds):void 
		{ 
			if (obj.bounds) 
				obj.bounds.union(bounds); 
			else 
				obj.bounds = bounds; 
			kmlObj.bounds.union(bounds); 
		} 
		private function canContainFeatures(feature:Feature):Boolean 
		{ 
			return (feature is Container); 
		} 
		//this function is useful for debugging. It is not currently called. 
		public function getSampleKML() : String 
		{ 
			return("<?xml version='1.0' encoding='UTF-8'?> " + 
				"<kml xmlns='http://earth.google.com/kml/2.1'> " + 
				"<Document> " + 
				"<name>FIM Earth Grid Level 5</name> " + 
				"<Style id='7F008000'><LineStyle><color>60FFFFFF</color><width>1.0</width></LineStyle><PolyStyle><color>7F008000</color></PolyStyle></Style> " + 
				"<Placemark> " + 
				"  <name>FIM Grid Cell 1</name> " + 
				"  <description>TEMP=295.9104></description> " + 
				"  <open>false</open> " + 
				"  <styleUrl>#7F008000</styleUrl> " + 
				"  <MultiGeometry>  " + 
				"    <Polygon> " + 
				"      <altitudeMode>absolute</altitudeMode> " + 
				"      <outerBoundaryIs> " + 
				"      <LinearRing> " + 
				"        <coordinates>-98,41.953,35000 -98,40.723,35000 -96.46,39.997,35000 -94.883,40.681,35000 -94.826,41.911,35000 -96.393,42.659,35000 -98,41.953,35000</coordinates> " + 
				"      </LinearRing> " + 
				"      </outerBoundaryIs> " + 
				"    </Polygon> " + 
				"  </MultiGeometry> " + 
				"</Placemark> " + 
				"</Document> " + 
				"</kml>"); 
		} 
		
	}
}
