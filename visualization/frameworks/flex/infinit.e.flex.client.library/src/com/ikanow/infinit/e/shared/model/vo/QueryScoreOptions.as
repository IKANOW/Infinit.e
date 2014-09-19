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
	import com.ikanow.infinit.e.shared.model.constant.settings.QueryAdvancedSettingsConstants;
	import com.ikanow.infinit.e.shared.util.JSONUtil;
	import flash.events.EventDispatcher;
	import mx.controls.Alert;
	import mx.utils.ObjectUtil;
	import system.data.maps.HashMap;
	
	[Bindable]
	public class QueryScoreOptions extends EventDispatcher
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Transient]
		public var enableScoring:Boolean;
		
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
		
		[Transient]
		public var adjustAggregateSig:int; // (0==auto, 1==true, 2==false)
		
		public var tagWeights:Object; // map<string, double>
		
		[Transient]
		public var tagWeights_cs:String; // the pre-formatted string
		
		public var typeWeights:Object; // map<string, double>
		
		[Transient]
		public var typeWeights_cs:String; // the pre-formatted string
		
		public var sourceWeights:Object; // map<string, double>
		
		[Transient]
		public var sourceWeights_cs:String; // the pre-formatted string
		
		public var scoreEnts:Boolean;
		
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
			enableScoring = options.enableScoring;
			numAnalyze = options.numAnalyze;
			adjustAggregateSig = options.adjustAggregateSig;
			scoreEnts = options.scoreEnts;
			_relWeight = options.relWeight;
			_sigWeight = options.sigWeight; // (use "_" to allow them both to be 0 in the case where the options are disable)
			
			if ( ( relWeight == 0 ) && ( sigWeight == 0 ) )
			{
				enableScoring = false;
			}
			timeProx.apply( options.timeProx );
			geoProx.apply( options.geoProx );
			tagWeights_cs = options.tagWeights_cs;
			typeWeights_cs = options.typeWeights_cs;
			sourceWeights_cs = options.sourceWeights_cs;
			
			if ( null != tagWeights_cs ) // on save
			{
				if ( tagWeights_cs.length > 0 )
				{
					var tmp:String = '{' + options.tagWeights_cs + '}';
					tagWeights = JSONUtil.decode( tmp );
				}
				else // there were previously tag weights, now they've been removed
				{
					tagWeights = null;
				}
			}
			else if ( null != options.tagWeights )  // Copying in other direction, ie to load query
			{
				tagWeights_cs = JSONUtil.encode( options.tagWeights );
				
				if ( ( null != tagWeights_cs ) && ( tagWeights_cs.length > 2 ) )
				{
					var len:int = tagWeights_cs.length;
					tagWeights_cs = tagWeights_cs.substr( 1, len - 2 ); // (remove the first and last char)
				}
				else
				{
					tagWeights_cs = "";
				}
			}
			
			if ( null != typeWeights_cs ) // on save
			{
				if ( typeWeights_cs.length > 0 )
				{
					var tmp2:String = '{' + options.typeWeights_cs + '}';
					typeWeights = JSONUtil.decode( tmp2 );
				}
				else // there were previously tag weights, now they've been removed
				{
					typeWeights = null;
				}
			}
			else if ( null != options.typeWeights ) // Copying in other direction, ie to load query
			{
				typeWeights_cs = JSONUtil.encode( options.typeWeights );
				
				if ( ( null != typeWeights_cs ) && ( typeWeights_cs.length > 2 ) )
				{
					var len2:int = typeWeights_cs.length;
					typeWeights_cs = typeWeights_cs.substr( 1, len2 - 2 ); // (remove the first and last char)
				}
				else
				{
					typeWeights_cs = "";
				}
			}
			
			if ( null != sourceWeights_cs ) // on save
			{
				if ( sourceWeights_cs.length > 0 )
				{
					var tmp3:String = '{' + options.sourceWeights_cs + '}';
					sourceWeights = JSONUtil.decode( tmp3 );
				}
				else // there were previously tag weights, now they've been removed
				{
					sourceWeights = null;
				}
			}
			else if ( null != options.sourceWeights ) // Copying in other direction, ie to load query
			{
				sourceWeights_cs = JSONUtil.encode( options.sourceWeights );
				
				if ( ( null != sourceWeights_cs ) && ( sourceWeights_cs.length > 2 ) )
				{
					var len3:int = sourceWeights_cs.length;
					sourceWeights_cs = sourceWeights_cs.substr( 1, len3 - 2 ); // (remove the first and last char)
				}
				else
				{
					sourceWeights_cs = "";
				}
			}
		}
		
		public function clone():QueryScoreOptions
		{
			var clone:QueryScoreOptions = new QueryScoreOptions();
			clone.apply( this );
			this.tagWeights = ObjectUtil.copy( tagWeights );
			this.typeWeights = ObjectUtil.copy( typeWeights );
			this.sourceWeights = ObjectUtil.copy( sourceWeights );
			
			return clone;
		}
		
		public function disableScoring():void
		{
			enableScoring = false;
			relWeight = QueryAdvancedSettingsConstants.SCORING_REL_WEIGHT;
			sigWeight = QueryAdvancedSettingsConstants.SCORING_SIG_WEIGHT;
		}
		
		public function reset():void
		{
			enableScoring = QueryAdvancedSettingsConstants.SCORING_ENABLE;
			numAnalyze = QueryAdvancedSettingsConstants.SCORING_NUM_ANALYZE;
			adjustAggregateSig = QueryAdvancedSettingsConstants.SCORING_ADJUST_AGGSIG;
			scoreEnts = QueryAdvancedSettingsConstants.SCORING_SCORE_ENTS;
			relWeight = QueryAdvancedSettingsConstants.SCORING_REL_WEIGHT;
			sigWeight = QueryAdvancedSettingsConstants.SCORING_SIG_WEIGHT;
			timeProx = new TimeProximity();
			geoProx = new GeoProximity();
			tagWeights = null;
			tagWeights_cs = null;
			typeWeights = null;
			typeWeights_cs = null;
			sourceWeights = null;
			sourceWeights_cs = null;
		}
	}
}
