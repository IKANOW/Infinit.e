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
		
		//======================================
		// constructor 
		//======================================
		
		public function QueryScoreOptionsRequest( value:QueryScoreOptions )
		{
			this.numAnalyze = value.numAnalyze;
			this.relWeight = value.relWeight;
			this.sigWeight = value.sigWeight;
			this.timeProx = new Object();
			this.timeProx[ QueryConstants.TIME ] = value.timeProx.time;
			this.timeProx[ QueryConstants.DECAY ] = value.timeProx.decay;
			this.geoProx = new Object();
			this.geoProx[ QueryConstants.LAT_LONG ] = value.geoProx.ll;
			this.geoProx[ QueryConstants.DECAY ] = value.geoProx.decay;
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		public function clone():QueryScoreOptionsRequest
		{
			var queryScoreOptions:QueryScoreOptions = new QueryScoreOptions();
			queryScoreOptions.numAnalyze = numAnalyze;
			queryScoreOptions.relWeight = relWeight;
			queryScoreOptions.sigWeight = sigWeight;
			queryScoreOptions.timeProx = new TimeProximity();
			queryScoreOptions.timeProx[ QueryConstants.TIME ] = timeProx[ QueryConstants.TIME ];
			queryScoreOptions.timeProx[ QueryConstants.DECAY ] = timeProx[ QueryConstants.DECAY ];
			queryScoreOptions.geoProx = new GeoProximity();
			queryScoreOptions.geoProx[ QueryConstants.LAT_LONG ] = geoProx[ QueryConstants.LAT_LONG ];
			queryScoreOptions.geoProx[ QueryConstants.DECAY ] = geoProx[ QueryConstants.DECAY ];
			
			var clone:QueryScoreOptionsRequest = new QueryScoreOptionsRequest( queryScoreOptions );
			
			return clone;
		}
	}
}
