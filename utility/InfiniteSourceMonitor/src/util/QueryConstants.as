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
package util
{
	
	/**
	 * Query Constants
	 */
	public class QueryConstants
	{
		
		//======================================
		// public static properties 
		//======================================
		
		public static const QUERY_RESULTS:String = "Query Results";
		
		// search type constants
		
		public static const SIMPLE_SEARCH:String = "simpleSearch";
		
		public static const ADVANCED_SEARCH:String = "advancedSearch";
		
		// general constants
		
		public static const OUTPUT_FORMAT:String = "json";
		
		public static const SRC_INCLUDE:String = "srcInclude";
		
		public static const SOURCES:String = "sources";
		
		public static const SOURCE_KEY:String = "key";
		
		public static const NAME:String = "name";
		
		public static const TITLE:String = "title";
		
		public static const NOW:String = "now";
		
		public static const DRAG_DATA:String = "queryTerm";
		
		public static const BLANK:String = "";
		
		public static const SPACE:String = " ";
		
		public static const PARENTHESIS_LEFT:String = "(";
		
		public static const PARENTHESIS_RIGHT:String = ")";
		
		public static const DEFAULT_QUERY_LOGIC:String = "1";
		
		// scoring constants
		
		public static const TIME_PROXIMITY:String = "timeProx";
		
		public static const GEO_PROXIMITY:String = "geoProx";
		
		public static const TIME:String = "time";
		
		public static const LAT_LONG:String = "ll";
		
		public static const DECAY:String = "decay";
		
		// aggregation constants
		
		public static const ENTITIES_NUMBER_RETURN:String = "entsNumReturn";
		
		public static const EVENTS_NUMBER_RETURN:String = "eventsNumReturn";
		
		public static const FACTS_NUMBER_RETURN:String = "factsNumReturn";
		
		public static const GEO_LOCATIONS_NUMBER_RETURN:String = "geoNumReturn";
		
		public static const SOURCE_METADATA:String = "sourceMetadata";
		
		public static const TIMES_INTERVAL:String = "timesInterval";
		
		// query string constants
		
		public static const COMMUNITY_IDS:String = "communityIds";
		
		public static const INPUT_OPTIONS:String = "input";
		
		public static const OUTPUT_OPTIONS:String = "output";
		
		public static const SCORING_OPTIONS:String = "score";
		
		public static const QUERY_TERM_LOGIC:String = "logic";
		
		public static const QUERY_TERM_OPTIONS:String = "qtOptions";
		
		public static const QUERY_TERMS:String = "qt";
		
		public static const QUERY_TERM_ENTITY_OPTIONS:String = "entityOpt";
		
		public static const SAVE_STRING:String = "saveString";
		
		public static const WIDGET_OPTIONS:String = "widgetOptions";
		
		public static const QUERY_STRING:String = "queryString";
		
		public static const AGGREGATION:String = "aggregation";
		
		// query term constants
		
		public static const EVENT_ENTITY_1:String = "entity1";
		
		public static const EVENT_ENTITY_2:String = "entity2";
		
		public static const EVENT_VERB:String = "verb";
		
		public static const TEMPORAL_MIN:String = "min";
		
		public static const TEMPORAL_MAX:String = "max";
		
		public static const GEO_CENTER_LL:String = "centerll";
		
		public static const GEO_DISTANCE:String = "dist";
	}
}
