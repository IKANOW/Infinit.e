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

// It's important to understand QueryScoreOptions and QueryScoreOptionsRequest
// QueryScoreOptionsRequest is only ever transmitted to the API, never received
// QueryScoreOptions is used for internal storage (eg _cs fields)
// And is also used to receive objects that were originally transmitted as
// QueryScoreOptionsRequest
// CUSTOM LOGIC TO MAP BETWEEN THE INTERNAL REPRESENTATION OF OBJECTS AND THE API VERSION SHOULD RESIDE IN OBJECTTRANSLATORUTIL AND QUERYUTIL
// (SEE setAggregationOptions/setScoringOptions)

package com.ikanow.infinit.e.shared.model.vo
{
	import com.ikanow.infinit.e.shared.model.constant.QueryConstants;
	import flash.events.EventDispatcher;
	
	[Bindable]
	public class QueryScoreOptionsRequest extends EventDispatcher
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var numAnalyze:int;
		
		public var relWeight:int;
		
		public var sigWeight:int;
		
		public var timeProx:Object;
		
		public var geoProx:Object;
		
		public var tagWeights:Object;
		
		public var typeWeights:Object;
		
		public var sourceWeights:Object;
		
		public var adjustAggregateSig:*;
		
		public var scoreEnts:Boolean;
		
		//======================================
		// constructor 
		//======================================
		
		public function QueryScoreOptionsRequest( value:QueryScoreOptions )
		{
			this.numAnalyze = value.numAnalyze;
			
			if ( value.enableScoring )
			{
				this.relWeight = value.relWeight;
				this.sigWeight = value.sigWeight;
			}
			else
			{
				this.relWeight = 0;
				this.sigWeight = 0;
			}
			this.timeProx = new Object();
			this.timeProx[ QueryConstants.TIME ] = value.timeProx.time;
			this.timeProx[ QueryConstants.DECAY ] = value.timeProx.decay;
			this.geoProx = new Object();
			this.geoProx[ QueryConstants.LAT_LONG ] = value.geoProx.ll;
			this.geoProx[ QueryConstants.DECAY ] = value.geoProx.decay;
			this.tagWeights = value.tagWeights;
			this.typeWeights = value.typeWeights;
			this.sourceWeights = value.sourceWeights;
			
			switch ( value.adjustAggregateSig )
			{
				case 0: // null, ie auto
					this.adjustAggregateSig = null;
					break;
				case 1: // true
					this.adjustAggregateSig = true;
					break;
				default: // false
					this.adjustAggregateSig = false;
					break;
			}
			this.scoreEnts = value.scoreEnts;
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		public function getOptions():QueryScoreOptions
		{
			var queryScoreOptions:QueryScoreOptions = new QueryScoreOptions();
			queryScoreOptions.numAnalyze = numAnalyze;
			
			if ( ( relWeight == 0 ) && ( sigWeight == 0 ) )
			{
				queryScoreOptions.enableScoring = false;
			}
			else
			{
				queryScoreOptions.enableScoring = true;
			}
			queryScoreOptions.relWeight = relWeight;
			queryScoreOptions.sigWeight = sigWeight;
			queryScoreOptions.timeProx = new TimeProximity();
			queryScoreOptions.timeProx[ QueryConstants.TIME ] = timeProx[ QueryConstants.TIME ];
			queryScoreOptions.timeProx[ QueryConstants.DECAY ] = timeProx[ QueryConstants.DECAY ];
			queryScoreOptions.geoProx = new GeoProximity();
			queryScoreOptions.geoProx[ QueryConstants.LAT_LONG ] = geoProx[ QueryConstants.LAT_LONG ];
			queryScoreOptions.geoProx[ QueryConstants.DECAY ] = geoProx[ QueryConstants.DECAY ];
			queryScoreOptions.tagWeights = this.tagWeights;
			queryScoreOptions.typeWeights = this.typeWeights;
			queryScoreOptions.sourceWeights = this.sourceWeights;
			queryScoreOptions.scoreEnts = this.scoreEnts;
			
			if ( null == this.adjustAggregateSig )
			{
				queryScoreOptions.adjustAggregateSig = 0;
			}
			else
			{
				queryScoreOptions.adjustAggregateSig = ( this.adjustAggregateSig as Boolean ) ? 1 : 2;
			}
			return queryScoreOptions;
		}
	}
}
