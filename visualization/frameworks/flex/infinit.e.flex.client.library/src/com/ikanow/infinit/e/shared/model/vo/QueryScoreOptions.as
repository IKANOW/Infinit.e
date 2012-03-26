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
	import com.ikanow.infinit.e.shared.model.constant.settings.QueryAdvancedSettingsConstants;
	import flash.events.EventDispatcher;
	
	[Bindable]
	public class QueryScoreOptions extends EventDispatcher
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var numAnalyze:int;
		
		protected var _relWeight:int;
		
		public function get relWeight():int
		{
			return _relWeight;
		}
		
		public function set relWeight( value:int ):void
		{
			_relWeight = value;
			sigWeight = 100 - relWeight;
		}
		
		protected var _sigWeight:int;
		
		public function get sigWeight():int
		{
			return _sigWeight;
		}
		
		public function set sigWeight( value:int ):void
		{
			_sigWeight = value;
			relWeight = 100 - sigWeight;
		}
		
		public var timeProx:TimeProximity;
		
		public var geoProx:GeoProximity;
		
		//======================================
		// constructor 
		//======================================
		
		public function QueryScoreOptions()
		{
			super();
			reset();
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		public function apply( options:QueryScoreOptions ):void
		{
			numAnalyze = options.numAnalyze;
			relWeight = options.relWeight;
			sigWeight = options.sigWeight;
			timeProx.apply( options.timeProx );
			geoProx.apply( options.geoProx );
		}
		
		public function clone():QueryScoreOptions
		{
			var clone:QueryScoreOptions = new QueryScoreOptions();
			
			clone.numAnalyze = numAnalyze;
			clone.relWeight = relWeight;
			clone.sigWeight = sigWeight;
			clone.timeProx = timeProx.clone();
			clone.geoProx = geoProx.clone();
			
			return clone;
		}
		
		public function reset():void
		{
			numAnalyze = QueryAdvancedSettingsConstants.SCORING_NUM_ANALYZE;
			relWeight = QueryAdvancedSettingsConstants.SCORING_REL_WEIGHT;
			sigWeight = QueryAdvancedSettingsConstants.SCORING_SIG_WEIGHT;
			timeProx = new TimeProximity();
			geoProx = new GeoProximity();
		}
	}
}
