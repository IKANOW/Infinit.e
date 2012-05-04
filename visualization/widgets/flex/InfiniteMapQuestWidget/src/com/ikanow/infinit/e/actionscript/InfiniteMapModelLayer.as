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
import com.ikanow.infinit.e.widget.library.enums.*;
import com.ikanow.infinit.e.widget.library.utility.JSONEncoder;
import com.ikanow.infinit.e.widget.library.widget.IWidget;
import com.ikanow.infinit.e.widget.library.widget.IWidgetContext;
import com.mapquest.LatLng;
import mx.collections.ArrayCollection;
import system.data.Set;
import system.data.sets.HashSet;


//======================================
// private properties 
//======================================

private var _context:IWidgetContext;


//======================================
// public methods 
//======================================

// Update query - with new geo term

public function addRegionToQuery_fromPresentation( latlng:LatLng, nRadiusKm:Number ):void
{
	var query:Object = _context.getCurrentQuery();
	var queryTerms:ArrayCollection = new ArrayCollection( query[ "qt" ] );
	var newTerm:Object = new Object();
	var newGeoTerm:Object = new Object();
	newGeoTerm[ "centerll" ] = new String( "" + latlng.lat + "," + latlng.lng );
	newGeoTerm[ "dist" ] = new String( "" + nRadiusKm );
	newTerm[ "geo" ] = newGeoTerm;
	queryTerms.addItem( newTerm );
	_context.setCurrentQuery( query, "qt" );
}

// getData_fromPresentation - Returns the current query/filtered data to the presentation layer
//
// @param bFilteredIfAvailable: If the data has been filtered, return just the filtered data
// @returns the requested dataset

public function getData_fromPresentation( filteredIfAvailable:Boolean ):ArrayCollection
{
	if ( filteredIfAvailable )
	{
		var returnCandidate:ArrayCollection = _context.getQuery_FilteredResults().getTopDocuments();
		
		if ( null == returnCandidate )
		{
			return _context.getQuery_TopResults().getTopDocuments();
		}
		return returnCandidate;
	}
	else
	{
		return _context.getQuery_TopResults().getTopDocuments();
	}
}
//private var _presentationLayer:InfiniteMapPresentationLayer declared in the mxml file

// 2] Interface with framework

/**
 * IWidget interface to receive data object (IWidgetContext).
 * Store the iwidgetcontext so we can receieve data later.
 */
public function onInit( context:IWidgetContext ):void
{
	_context = context;
}

// 3] Interface with model layer

// SUMMARY:
// onReceiveFilterRequest_fromPresentation(docIdSet):void
// getData_fromPresentation(filteredIfAvailable):ArrayCollection 

// onReceiveFilterRequest_fromPresentation - processes a filter request from the presentation layer
//
// @param docIdSet: HashSet of doc ids to filter on

public function onReceiveFilterRequest_fromPresentation( docIdSet:Set, description:String ):void
{
	_context.filterByDocField( FilterDataSetEnum.FILTER_GLOBAL_DATA, docIdSet, "_id", description );
}

/**
 * IWidget interface that fires when a new filter is done (including from ourself)
 * We can access the data fromt he filter by using our
 * iwidgetcontext object _context.getFilterResults().
 */
public function onReceiveNewFilter():void
{
	if ( !internalFilterUpdate )
	{
		_presentationLayer._filteringEnabled = true;
		hasFilter = true;
		setTimeout( resetIgnoreLocalFilter, 1000 );
	}
	_presentationLayer.onNewMapData_fromModel( _context.getQuery_FilteredResults().getTopDocuments(), _context.getQuery_FilteredResults(), true );
	// this generates the clusters and then invokes the visualization layer
}

/**
 * IWidget interface that fires when a new query is done.
 * We can access the data from the query by using our
 * iwidgetcontext object _context.getQueryResults().
 */
public function onReceiveNewQuery():void
{
	if ( !internalFilterUpdate )
	{
		hasFilter = false;
		setTimeout( resetIgnoreLocalFilter, 1000 );
	}
	_presentationLayer.onNewMapData_fromModel( _context.getQuery_TopResults().getTopDocuments(), _context.getQuery_AllResults(), false );
	// this generates the clusters and then invokes the visualization layer
}

// Update query options - with geo decay

public function setRegionAsGeoDecay_fromPresentation( latlng:LatLng, nRadiusKm:Number ):void
{
	var query:Object = _context.getCurrentQuery();
	var queryScoring:Object = query[ "score" ];
	
	if ( null == queryScoring )
	{
		queryScoring = new Object();
		query[ "score" ] = queryScoring;
	}
	var geoScore:Object = new Object();
	geoScore[ "ll" ] = new String( "" + latlng.lat + "," + latlng.lng );
	geoScore[ "decay" ] = Number( nRadiusKm ).toString();
	queryScoring[ "geoProx" ] = geoScore;
	_context.setCurrentQuery( query, "score" );
}
