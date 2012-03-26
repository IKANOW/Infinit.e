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
	import mx.collections.ArrayCollection;
	
	[Bindable]
	public class QueryStringRequest extends EventDispatcher
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var input:Object;
		
		public var logic:String = "1";
		
		public var output:QueryOutputRequest;
		
		public var qtOptions:Object;
		
		public var qt:Array;
		
		public var communityIds:Array;
		
		public var score:QueryScoreOptionsRequest;
		
		//======================================
		// constructor 
		//======================================
		
		public function QueryStringRequest( score:QueryScoreOptions, docs:QueryOutputDocumentOptions, aggregation:Object )
		{
			this.score = new QueryScoreOptionsRequest( score );
			this.output = new QueryOutputRequest();
			this.output.docs = docs;
			this.output.aggregation = aggregation;
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		public function clone():QueryStringRequest
		{
			var scoreOptions:QueryScoreOptions = new QueryScoreOptions();
			
			scoreOptions.numAnalyze = score.numAnalyze;
			scoreOptions.relWeight = score.relWeight;
			scoreOptions.sigWeight = score.sigWeight;
			scoreOptions.timeProx = new TimeProximity();
			scoreOptions.timeProx.time = score.timeProx[ QueryConstants.TIME ];
			scoreOptions.timeProx.decay = score.timeProx[ QueryConstants.DECAY ];
			
			var geo:GeoProximity = new GeoProximity();
			geo.ll = score.geoProx.ll;
			geo.decay = score.geoProx.decay;
			
			scoreOptions.geoProx = geo;
			
			var clone:QueryStringRequest = new QueryStringRequest( scoreOptions, output.docs, output.aggregation );
			
			clone.input = input;
			clone.logic = logic.toString();
			clone.output.format = output.format.toString();
			clone.qtOptions = qtOptions;
			clone.communityIds = communityIds;
			
			clone.qt = [];
			
			for each ( var queryTerm:QueryTerm in qt )
			{
				clone.qt.push( queryTerm.clone() );
			}
			
			return clone;
		}
	}
}
