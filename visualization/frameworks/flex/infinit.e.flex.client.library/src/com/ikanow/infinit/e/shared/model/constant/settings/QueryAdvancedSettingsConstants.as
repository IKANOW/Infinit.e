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
package com.ikanow.infinit.e.shared.model.constant.settings
{
	
	public class QueryAdvancedSettingsConstants
	{
		
		//======================================
		// public static properties 
		//======================================
		
		// scoring
		
		public static const SCORING_ENABLE:Boolean = true;
		
		public static const SCORING_NUM_ANALYZE:int = 1000;
		
		public static const SCORING_ADJUST_AGGSIG:int = 0;
		
		public static const SCORING_SCORE_ENTS:Boolean = true;
		
		public static const SCORING_REL_WEIGHT:int = 33;
		
		public static const SCORING_SIG_WEIGHT:int = 67;
		
		public static const SCORING_TIME_PROX_DECAY:String = "1m";
		
		public static const SCORING_GEO_PROX_DECAY:String = "100";
		
		// output document
		
		public static const OUTPUT_DOC_ENABLE:Boolean = true;
		
		public static const OUTPUT_DOC_ENTS:Boolean = true;
		
		public static const OUTPUT_DOC_EVENTS:Boolean = true;
		
		public static const OUTPUT_DOC_EVENTS_TIMELINE:Boolean = false;
		
		public static const OUTPUT_DOC_FACTS:Boolean = true;
		
		public static const OUTPUT_DOC_GEO:Boolean = true;
		
		public static const OUTPUT_DOC_NUM_RETURN:int = 100;
		
		public static const OUTPUT_DOC_SKIP:int = 0;
		
		public static const OUTPUT_DOC_SUMMARIES:Boolean = true;
		
		public static const OUTPUT_DOC_METADATA:Boolean = true;
		
		// output aggregation
		
		public static const OUTPUT_AGG_ENTS_NUM_RETURN:int = 250;
		
		public static const OUTPUT_AGG_EVENTS_NUM_RETURN:int = 100;
		
		public static const OUTPUT_AGG_FACTS_NUM_RETURN:int = 100;
		
		public static const OUTPUT_AGG_GEO_NUM_RETURN:int = 1000;
		
		public static const OUTPUT_AGG_SOURCE_METADATA:int = 20;
		
		public static const OUTPUT_AGG_SOURCES:int = 20;
		
		public static const OUTPUT_AGG_TIMES_INTERVAL:String = "1w";
		
		public static const OUTPUT_AGG_AGGREGATE_ENTITIES:Boolean = true;
		
		public static const OUTPUT_AGG_AGGREGATE_EVENTS:Boolean = true;
		
		public static const OUTPUT_AGG_AGGREGATE_FACTS:Boolean = true;
		
		public static const OUTPUT_AGG_AGGREGATE_GEOTAGS:Boolean = true;
		
		public static const OUTPUT_AGG_AGGREGATE_SOURCE_METADATA:Boolean = false;
		
		public static const OUTPUT_AGG_AGGREGATE_SOURCES:Boolean = false;
		
		public static const OUTPUT_AGG_AGGREGATE_TIMES:Boolean = true;		
	}
}
