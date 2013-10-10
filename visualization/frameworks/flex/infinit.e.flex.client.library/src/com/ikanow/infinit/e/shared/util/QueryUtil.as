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
package com.ikanow.infinit.e.shared.util
{
	import com.adobe.utils.StringUtil;
	import com.ikanow.infinit.e.query.view.builder.QueryTermGroupItemRenderer;
	import com.ikanow.infinit.e.query.view.builder.QueryTermListItemRenderer;
	import com.ikanow.infinit.e.shared.model.constant.Constants;
	import com.ikanow.infinit.e.shared.model.constant.QueryConstants;
	import com.ikanow.infinit.e.shared.model.constant.ServiceConstants;
	import com.ikanow.infinit.e.shared.model.constant.types.EntityTypes;
	import com.ikanow.infinit.e.shared.model.constant.types.QueryDimensionTypes;
	import com.ikanow.infinit.e.shared.model.constant.types.QueryLogicTypes;
	import com.ikanow.infinit.e.shared.model.constant.types.QueryOperatorTypes;
	import com.ikanow.infinit.e.shared.model.constant.types.QueryStringTypes;
	import com.ikanow.infinit.e.shared.model.constant.types.QuerySuggestionTypes;
	import com.ikanow.infinit.e.shared.model.constant.types.QueryTermTypes;
	import com.ikanow.infinit.e.shared.model.vo.Community;
	import com.ikanow.infinit.e.shared.model.vo.QueryObject;
	import com.ikanow.infinit.e.shared.model.vo.QueryOutputAggregationOptions;
	import com.ikanow.infinit.e.shared.model.vo.QueryScoreOptions;
	import com.ikanow.infinit.e.shared.model.vo.QueryString;
	import com.ikanow.infinit.e.shared.model.vo.QueryStringRequest;
	import com.ikanow.infinit.e.shared.model.vo.QuerySuggestion;
	import com.ikanow.infinit.e.shared.model.vo.QuerySuggestions;
	import com.ikanow.infinit.e.shared.model.vo.QueryTerm;
	import com.ikanow.infinit.e.shared.model.vo.QueryTermEntity;
	import com.ikanow.infinit.e.shared.model.vo.QueryTermEvent;
	import com.ikanow.infinit.e.shared.model.vo.TypedQueryString;
	import com.ikanow.infinit.e.shared.model.vo.ui.QueryTermGroup;
	import mx.collections.ArrayCollection;
	import mx.core.ClassFactory;
	import mx.resources.ResourceManager;
	
	public class QueryUtil
	{
		
		//======================================
		// constructor 
		//======================================
		
		/**
		 *
		 * Constructor
		 *
		 */
		public function QueryUtil()
		{
		
		}
		
		
		//======================================
		// public static methods 
		//======================================
		
		public static function advancedQueryList_itemRendererFunction( item:Object ):ClassFactory
		{
			var itemRendererClass:Class;
			
			if ( item is QueryTermGroup )
				itemRendererClass = QueryTermGroupItemRenderer;
			else
				itemRendererClass = QueryTermListItemRenderer;
			
			return new ClassFactory( itemRendererClass );
		}
		
		/**
		 * convert a query term to a query term event
		 */
		public static function convertQueryTermToGroup( queryTerm:QueryTerm ):QueryTermGroup
		{
			var queryTermGroup:QueryTermGroup = new QueryTermGroup();
			queryTermGroup.children = new ArrayCollection( [ queryTerm ] );
			queryTermGroup._id = getRandomNumber().toString();
			queryTermGroup.logicOperator = queryTerm.logicOperator;
			queryTermGroup.level = queryTerm.level;
			
			return queryTermGroup;
		}
		
		/**
		 * convert the query term suggestion into a query object
		 * @param value
		 */
		public static function doesQueryTermExistInCollection( value:QueryTerm, collection:ArrayCollection ):Boolean
		{
			var found:Boolean;
			var queryTerm:QueryTerm;
			
			for each ( queryTerm in collection )
			{
				if ( queryTerm.type.toLowerCase() == value.type.toLowerCase() )
				{
					switch ( value.type.toLowerCase() )
					{
						case QueryTermTypes.EXACT_TEXT:
							if ( queryTerm.etext == value.etext )
								found = true;
							break;
						case QueryTermTypes.FREE_TEXT:
							if ( queryTerm.ftext == value.ftext )
								found = true;
							break;
						case QueryTermTypes.ENTITY:
							if ( queryTerm.entity == value.entity )
								found = true;
							break;
						case QueryTermTypes.EVENT:
							var entity1Found:Boolean;
							var verbFound:Boolean;
							var entity2Found:Boolean;
							
							if ( ( queryTerm.event.entity1 == null && value.event.entity1 == null ) || ( queryTerm.event.entity1.displayLabel == value.event.entity1.displayLabel ) )
								entity1Found = true;
							if ( ( queryTerm.event.verb == null && value.event.verb == null ) || ( queryTerm.event.verb == value.event.verb ) )
								verbFound = true;
							if ( ( queryTerm.event.entity2 == null && value.event.entity2 == null ) || ( queryTerm.event.entity2.displayLabel == value.event.entity2.displayLabel ) )
								entity2Found = true;
							
							found = entity1Found && verbFound && entity2Found;
							break;
						case QueryTermTypes.TEMPORAL:
							if ( queryTerm.time.startDateString == value.time.startDateString && queryTerm.time.endDateString == value.time.endDateString )
								found = true;
							break;
						case QueryTermTypes.GEO_LOCATION:
							if ( queryTerm.geo.centerll == value.geo.centerll && queryTerm.geo.dist == value.geo.dist )
								found = true;
							break;
					}
				}
			}
			
			return found;
		}
		
		/**
		 * Since the service expects only those properties that the user wants to include
		 * in the query, we're forced to create a generic object for the aggregation options
		 */
		public static function getAggregationOptionsObject( options:QueryOutputAggregationOptions ):Object
		{
			var aggregationOptionsObject:Object = new Object();
			
			if ( options.aggregateEntities )
				aggregationOptionsObject[ QueryConstants.ENTITIES_NUMBER_RETURN ] = options.entsNumReturn;
			
			if ( options.aggregateEvents )
				aggregationOptionsObject[ QueryConstants.EVENTS_NUMBER_RETURN ] = options.eventsNumReturn;
			
			if ( options.aggregateFacts )
				aggregationOptionsObject[ QueryConstants.FACTS_NUMBER_RETURN ] = options.factsNumReturn;
			
			if ( options.aggregateGeotags )
				aggregationOptionsObject[ QueryConstants.GEO_LOCATIONS_NUMBER_RETURN ] = options.geoNumReturn;
			
			if ( options.aggregateSourceMetadata )
				aggregationOptionsObject[ QueryConstants.SOURCE_METADATA ] = 20;
			
			if ( options.aggregateSources )
				aggregationOptionsObject[ QueryConstants.SOURCES ] = 20;
			
			if ( options.aggregateTimes )
				aggregationOptionsObject[ QueryConstants.TIMES_INTERVAL ] = options.timesInterval;
			
			return aggregationOptionsObject;
		}
		
		/**
		 * Returns a query results object
		 * if the value is null, a initialized object will be returned
		 * * @param value
		 * @return Object
		 */
		public static function getInititalizedQueryResultsObject( value:Object ):Object
		{
			if ( !value )
			{
				value = new Object();
				value[ "data" ] = null;
				value[ "facets" ] = [];
				value[ "times" ] = [];
				value[ "timeInterval" ] = 0;
				value[ "geo" ] = [];
				value[ "maxGeoCount" ] = 0;
				value[ "minGeoCount" ] = 0;
				value[ "entities" ] = [];
				value[ "eventsTimeline" ] = [];
				value[ "facts" ] = [];
				value[ "sources" ] = [];
				value[ "sourceMetaTags" ] = [];
				value[ "sourceMetaTypes" ] = [];
				value[ "moments" ] = [];
				value[ "momentInterval" ] = 0;
				value[ "stats" ] = new Object();
				value[ "stats" ][ "found" ] = false;
			}
			
			return value;
		}
		
		/**
		 * Returns an array of string items from a logic string
		 * @return Array
		 */
		public static function getLogicStringArray( logicString:String ):Array
		{
			var logicStringArray:Array = [];
			
			if ( logicString && logicString != Constants.BLANK )
			{
				var logicStringArrayTemp:Array = logicString.split( Constants.SPACE );
				
				for ( var i:int = 0; i < logicStringArrayTemp.length; i++ )
				{
					if ( logicStringArrayTemp[ i ] != Constants.BLANK && logicStringArrayTemp[ i ] != Constants.SPACE )
					{
						logicStringArray.push( logicStringArrayTemp[ i ] );
					}
				}
				
				return logicStringArray;
			}
			
			return null;
		}
		
		/**
		 * Returns the query logic string and populates the query terms
		 */
		public static function getQueryLogicAndSetQueryTerms( collection:ArrayCollection, queryLogic:String, queryTerms:ArrayCollection ):String
		{
			for each ( var term:* in collection )
			{
				// clean up the logic operators
				if ( collection.getItemIndex( term ) == 0 )
					term.logicOperator = QueryConstants.BLANK;
				else if ( term.logicOperator == QueryConstants.BLANK )
					term.logicOperator = QueryOperatorTypes.AND;
				
				// add the logic operator to the string
				if ( term.logicOperator != QueryConstants.BLANK )
					queryLogic += term.logicOperator;
				
				if ( term is QueryTermGroup )
				{
					// handle nested terms
					queryLogic += QueryConstants.SPACE + QueryConstants.PARENTHESIS_LEFT;
					queryLogic = getQueryLogicAndSetQueryTerms( term.children, queryLogic, queryTerms );
					queryLogic += QueryConstants.PARENTHESIS_RIGHT + QueryConstants.SPACE;
				}
				else
				{
					// add the query term
					queryLogic += QueryConstants.SPACE + term.logicIndex + QueryConstants.SPACE;
					queryTerms.addItem( term );
				}
			}
			
			return queryLogic;
		}
		
		/**
		 * Since the service expects only those properties that the user wants to include
		 * in the query, we're forced to create a generic object for the query string
		 */
		public static function getQueryStringObject( queryString:QueryStringRequest ):Object
		{
			var queryStringObject:Object = new Object();
			
			if ( queryString.communityIds )
				queryStringObject[ QueryConstants.COMMUNITY_IDS ] = queryString.communityIds;
			
			if ( queryString.input )
				queryStringObject[ QueryConstants.INPUT_OPTIONS ] = queryString.input;
			
			if ( queryString.output )
				queryStringObject[ QueryConstants.OUTPUT_OPTIONS ] = queryString.output;
			
			if ( queryString.score )
				queryStringObject[ QueryConstants.SCORING_OPTIONS ] = queryString.score;
			
			if ( queryString.logic )
				queryStringObject[ QueryConstants.QUERY_TERM_LOGIC ] = queryString.logic.replace( QueryOperatorTypes.NOT, QueryOperatorTypes.AND_NOT );
			
			if ( queryString.qt )
				queryStringObject[ QueryConstants.QUERY_TERMS ] = getQueryTermObjects( new ArrayCollection( queryString.qt ) );
			
			queryStringObject[ QueryConstants.QUERY_TERM_OPTIONS ] = null;
			
			return queryStringObject;
		}
		
		public static function getQueryStringSettingsSummary( queryString:QueryString ):String
		{
			var querySummary:String = "";
			
			if ( null != queryString.score )
			{
				if ( null != queryString.score.geoProx )
				{
					if ( ( null != queryString.score.geoProx.decay ) && ( null != queryString.score.geoProx.ll ) )
					{
						if ( ( queryString.score.geoProx.decay.length > 0 ) && ( queryString.score.geoProx.ll.length > 1 ) )
						{
							if ( querySummary.length > 0 )
								querySummary += " | ";
							querySummary += "geo";
						}
					}
				}
				
				if ( null != queryString.score.timeProx )
				{
					if ( ( null != queryString.score.timeProx.time ) && ( null != queryString.score.timeProx.decay ) )
					{
						if ( ( queryString.score.timeProx.time.length > 0 ) && ( queryString.score.timeProx.decay.length > 0 ) )
						{
							if ( querySummary.length > 0 )
								querySummary += " | ";
							
							if ( queryString.score.timeProx.time == "now" )
							{
								querySummary += "recent";
							}
							else
							{
								querySummary += "time";
							}
						}
					}
				}
				
				if ( ( null != queryString.score.sourceWeights ) || ( null != queryString.score.tagWeights ) || ( null != queryString.score.typeWeights ) )
				{
					if ( querySummary.length > 0 )
						querySummary += " | ";
					querySummary += "source";
				}
			}
			
			if ( null != queryString.output )
			{
				if ( null != queryString.output.filter )
				{
					if ( ( null != queryString.output.filter.assocVerbs ) || ( null != queryString.output.filter.entityTypes ) )
					{
						if ( querySummary.length > 0 )
							querySummary += " | ";
						querySummary += "filter";
					}
				}
			}
			
			if ( 0 == querySummary.length )
				querySummary = null;
			
			return querySummary;
		}
		
		/**
		 * Gets an query string summary
		 */
		public static function getQueryStringSummary( queryTerms:ArrayCollection, logic:String ):String
		{
			var querySummary:String = "";
			var queryTerm:QueryTerm;
			var logicArray:Array = getLogicStringArray( logic );
			var logicType:String;
			
			for ( var i:int = 0; i < logicArray.length; i++ )
			{
				logicType = QueryLogicTypes.getType( logicArray[ i ] );
				
				switch ( logicType )
				{
					case QueryLogicTypes.GROUP_START:
						querySummary += logicArray[ i ].toString();
						break;
					case QueryLogicTypes.OPERATOR:
						querySummary += logicArray[ i ].toString();
						break;
					case QueryLogicTypes.PLACE_HOLDER:
						queryTerm = queryTerms.getItemAt( int( logicArray[ i ] ) - 1 ) as QueryTerm;
						querySummary += queryTerm.displayLabel;
						break;
					case QueryLogicTypes.GROUP_END:
						querySummary += logicArray[ i ].toString();
						break;
				}
				
				querySummary += QueryConstants.SPACE;
			}
			
			return querySummary;
		}
		
		/**
		 * Gets an html formated query string summary
		 */
		public static function getQueryStringSummaryHTML( queryTerms:ArrayCollection, logic:String, color:String, termColor:String ):String
		{
			var querySummary:String = "";
			var queryTerm:QueryTerm;
			var logicArray:Array = getLogicStringArray( logic );
			var logicType:String;
			
			for ( var i:int = 0; i < logicArray.length; i++ )
			{
				logicType = QueryLogicTypes.getType( logicArray[ i ] );
				
				switch ( logicType )
				{
					case QueryLogicTypes.GROUP_START:
						querySummary += "<FONT COLOR='" + color + "'>" + logicArray[ i ].toString() + "</FONT>";
						break;
					case QueryLogicTypes.OPERATOR:
						querySummary += QueryConstants.SPACE + "<FONT COLOR='" + color + "'>" + logicArray[ i ].toString() + "</FONT>";
						break;
					case QueryLogicTypes.PLACE_HOLDER:
						queryTerm = queryTerms.getItemAt( int( logicArray[ i ] ) - 1 ) as QueryTerm;
						querySummary += "<FONT COLOR='" + termColor + "'>" + queryTerm.displayLabel + "</FONT>";
						break;
					case QueryLogicTypes.GROUP_END:
						querySummary += QueryConstants.SPACE + "<FONT COLOR='" + color + "'>" + logicArray[ i ].toString() + "</FONT>";
						break;
				}
				
				querySummary += QueryConstants.SPACE;
			}
			
			return querySummary;
		}
		
		/**
		 * convert the query terms into query objects
		 * @param value
		 */
		public static function getQuerySuggestionsFromStrings( strings:ArrayCollection ):QuerySuggestions
		{
			var querySuggestions:QuerySuggestions = new QuerySuggestions();
			var suggestions:ArrayCollection = new ArrayCollection();
			var suggestion:QuerySuggestion;
			var stringArray:Array;
			
			// create the query term objects
			for each ( var string:String in strings )
			{
				suggestion = new QuerySuggestion();
				suggestion.value = string;
				stringArray = [];
				
				if ( string.lastIndexOf( Constants.PARENTHESIS_LEFT ) > -1 )
				{
					// verbs
					stringArray[ 0 ] = string.substring( 0, string.lastIndexOf( Constants.PARENTHESIS_LEFT ) - 1 );
					stringArray[ 1 ] = string.substring( string.lastIndexOf( Constants.PARENTHESIS_LEFT ) + 1, string.lastIndexOf( Constants.PARENTHESIS_RIGHT ) );
				}
				else
				{
					// entities
					if ( string.lastIndexOf( Constants.FORWARD_SLASH ) > -1 )
					{
						stringArray[ 0 ] = string.substring( 0, string.lastIndexOf( Constants.FORWARD_SLASH ) );
						stringArray[ 1 ] = string.substring( string.lastIndexOf( Constants.FORWARD_SLASH ) + 1, string.length );
					}
				}
				
				if ( stringArray.length == 2 )
				{
					suggestion.value = stringArray[ 0 ];
					suggestion.type = stringArray[ 1 ];
					suggestion.dimension = EntityTypes.getEntityDimensionType( suggestion.type );
				}
				else
				{
					suggestion.type = QuerySuggestionTypes.EXACT_TEXT;
				}
				
				suggestions.addItem( suggestion );
			}
			
			querySuggestions.currentSuggestions = strings;
			querySuggestions.dimensions = suggestions;
			
			return querySuggestions;
		}
		
		/**
		 * convert the event query terms into a query object
		 * @param value
		 */
		public static function getQueryTermEventObject( event:QueryTermEvent ):Object
		{
			var eventObject:Object = new Object();
			
			// entity1 -------------------------------
			if ( event.entity1 && event.entity1.type )
			{
				eventObject[ QueryConstants.EVENT_ENTITY_1 ] = new Object();
				
				switch ( event.entity1.type )
				{
					case QueryTermTypes.EXACT_TEXT:
						eventObject[ QueryConstants.EVENT_ENTITY_1 ][ QueryTermTypes.EXACT_TEXT ] = event.entity1.etext;
						break;
					case QueryTermTypes.FREE_TEXT:
						eventObject[ QueryConstants.EVENT_ENTITY_1 ][ QueryTermTypes.FREE_TEXT ] = event.entity1.ftext;
						break;
					case QueryTermTypes.ENTITY:
						eventObject[ QueryConstants.EVENT_ENTITY_1 ][ QueryTermTypes.ENTITY ] = event.entity1.entity;
						break;
				}
			}
			
			// verb ----------------------------------
			if ( event.verb )
				eventObject[ QueryConstants.EVENT_VERB ] = event.verb;
			
			// entity2 -------------------------------
			if ( event.entity2 && event.entity2.type )
			{
				eventObject[ QueryConstants.EVENT_ENTITY_2 ] = new Object();
				
				switch ( event.entity2.type )
				{
					case QueryTermTypes.EXACT_TEXT:
						eventObject[ QueryConstants.EVENT_ENTITY_2 ][ QueryTermTypes.EXACT_TEXT ] = event.entity2.etext;
						break;
					case QueryTermTypes.FREE_TEXT:
						eventObject[ QueryConstants.EVENT_ENTITY_2 ][ QueryTermTypes.FREE_TEXT ] = event.entity2.ftext;
						break;
					case QueryTermTypes.ENTITY:
						eventObject[ QueryConstants.EVENT_ENTITY_2 ][ QueryTermTypes.ENTITY ] = event.entity2.entity;
						break;
				}
			}
			
			return eventObject;
		}
		
		/**
		 * convert the query term suggestion into a query object
		 * @param value
		 */
		public static function getQueryTermFromSuggestion( querySuggestion:QuerySuggestion ):QueryTerm
		{
			if ( !querySuggestion )
			{
				querySuggestion = new QuerySuggestion();
				querySuggestion.dimension = QueryDimensionTypes.EXACT_TEXT;
				querySuggestion.value = Constants.WILDCARD;
			}
			
			var queryTerm:QueryTerm = new QueryTerm();
			var dimension:String = querySuggestion.dimension ? querySuggestion.dimension : querySuggestion.type;
			
			switch ( dimension.toLowerCase() )
			{
				case QueryDimensionTypes.EXACT_TEXT:
					queryTerm.etext = querySuggestion.value;
					break;
				case QueryDimensionTypes.FREE_TEXT:
					queryTerm.ftext = querySuggestion.value;
					break;
				default:
					queryTerm.entity = querySuggestion.value + Constants.FORWARD_SLASH + querySuggestion.type;
					break;
			}
			
			return queryTerm;
		}
		
		/**
		 * convert the query terms into query objects
		 * @param value
		 */
		public static function getQueryTermObjects( queryTerms:ArrayCollection ):Array
		{
			var queryTermObjects:Array = [];
			var qo:Object;
			
			// create the query term objects
			for each ( var queryTerm:QueryTerm in queryTerms )
			{
				qo = new Object();
				
				// set the appropriate query term type and value
				switch ( queryTerm.type )
				{
					case QueryTermTypes.EXACT_TEXT:
						qo[ QueryTermTypes.EXACT_TEXT ] = queryTerm.etext;
						break;
					case QueryTermTypes.FREE_TEXT:
						qo[ QueryTermTypes.FREE_TEXT ] = queryTerm.ftext;
						break;
					case QueryTermTypes.ENTITY:
						qo[ QueryTermTypes.ENTITY ] = queryTerm.entity;
						if ( null != queryTerm.sentiment )
						{
							qo[ QueryTermTypes.SENTIMENT ] = queryTerm.sentiment;
						}
						break;
					case QueryTermTypes.EVENT:
						qo[ QueryTermTypes.EVENT ] = getQueryTermEventObject( queryTerm.event );
						break;
					case QueryTermTypes.GEO_LOCATION:
						qo[ QueryTermTypes.GEO_LOCATION ] = new Object();
						qo[ QueryTermTypes.GEO_LOCATION ][ QueryConstants.GEO_CENTER_LL ] = queryTerm.geo.centerll;
						qo[ QueryTermTypes.GEO_LOCATION ][ QueryConstants.GEO_DISTANCE ] = queryTerm.geo.dist;
						break;
					case QueryTermTypes.TEMPORAL:
						qo[ QueryTermTypes.TEMPORAL ] = new Object();
						qo[ QueryTermTypes.TEMPORAL ][ QueryConstants.TEMPORAL_MIN ] = queryTerm.time.startDateString;
						qo[ QueryTermTypes.TEMPORAL ][ QueryConstants.TEMPORAL_MAX ] = queryTerm.time.endDateString;
						break;
				}
				
				// set the entity options
				if ( queryTerm.entityOpt.expandAlias )
					qo[ QueryConstants.QUERY_TERM_ENTITY_OPTIONS ] = queryTerm.entityOpt;
				
				queryTermObjects.push( qo );
			}
			
			return queryTermObjects;
		}
		
		/**
		 * if a group has only one child, return the child, else return null
		 */
		public static function getQueryTermOrphan( queryTermGroup:QueryTermGroup ):*
		{
			if ( queryTermGroup.children && queryTermGroup.children.length == 1 )
			{
				var term:* = queryTermGroup.children.getItemAt( 0 );
				term.logicOperator = queryTermGroup.logicOperator;
				return term;
			}
			else
			{
				return null;
			}
		}
		
		/**
		 * Returns a random number
		 * @return Number
		 */
		public static function getRandomNumber():Number
		{
			return Math.round( Math.random() * 99999 );
		}
		
		/**
		 * Get a typed query string - used for query history
		 */
		public static function getTypedQueryString( queryString:QueryString, type:String = "query" ):TypedQueryString
		{
			var typedQueryString:TypedQueryString = new TypedQueryString();
			typedQueryString.queryString = queryString;
			typedQueryString.type = type;
			
			return typedQueryString;
		}
		
		/**
		 * Return an array string of community Ids that the user is a member of
		 */
		public static function getUserCommunityIdsArrayFromArray( communityIds:Array, userCommunities:ArrayCollection ):Array
		{
			var communityIdsNew:Array = [];
			var isUserCommunity:Boolean;
			
			for each ( var communityId:String in communityIds )
			{
				isUserCommunity = false;
				
				for each ( var userCommunity:Community in userCommunities )
				{
					if ( communityId == userCommunity._id )
						isUserCommunity = true;
				}
				
				if ( isUserCommunity )
					communityIdsNew.push( communityId );
			}
			
			return communityIdsNew;
		}
		
		/**
		 * Return an array string of community Ids that the user is a member of
		 */
		public static function getUserCommunityIdsArrayStringFromArray( communityIds:Array, userCommunities:ArrayCollection, delimeter:String = "," ):String
		{
			var communityIdsNew:Array = [];
			var isUserCommunity:Boolean;
			
			for each ( var communityId:String in communityIds )
			{
				isUserCommunity = false;
				
				for each ( var userCommunity:Community in userCommunities )
				{
					if ( communityId == userCommunity._id )
						isUserCommunity = true;
				}
				
				if ( isUserCommunity )
					communityIdsNew.push( communityId );
			}
			
			return communityIdsNew.join( delimeter );
		}
		
		/**
		 * Returns "null" id the term is blank
		 * * @param value
		 * @return String
		 */
		public static function getValueOrNullString( value:String ):String
		{
			if ( value == null || value == Constants.BLANK )
				return Constants.NULL_STRING;
			else
				return value;
		}
		
		/**
		 * Iterate through the groups and roll-up any orphan children caused by
		 * removing a query term or query term group
		 */
		public static function normalizeQueryTermOrphans( collection:ArrayCollection ):void
		{
			for each ( var term:* in collection )
			{
				if ( term is QueryTermGroup )
				{
					if ( QueryUtil.getQueryTermOrphan( term ) )
						collection.setItemAt( getQueryTermOrphan( term ), collection.getItemIndex( term ) );
					else
						normalizeQueryTermOrphans( term.children );
				}
			}
		}
		
		/**
		 * Removes an item from the query term groups collection
		 */
		public static function removeQueryTerm( collection:ArrayCollection, term:* ):void
		{
			if ( CollectionUtil.doesCollectionContainItem( collection, term ) )
			{
				CollectionUtil.removeItem( collection, term );
			}
			else
			{
				for each ( var group:* in collection )
				{
					if ( group is QueryTermGroup )
					{
						removeQueryTerm( group.children, term );
					}
				}
			}
		}
		
		/**
		 * Updates the aggregation options based on the values returned in the query string raw object
		 */
		public static function setAggregationOptions( options:QueryOutputAggregationOptions, optionsRaw:Object ):QueryOutputAggregationOptions
		{
			options.aggregateEntities = optionsRaw.hasOwnProperty( QueryConstants.ENTITIES_NUMBER_RETURN );
			options.aggregateEvents = optionsRaw.hasOwnProperty( QueryConstants.EVENTS_NUMBER_RETURN );
			options.aggregateFacts = optionsRaw.hasOwnProperty( QueryConstants.FACTS_NUMBER_RETURN );
			options.aggregateGeotags = optionsRaw.hasOwnProperty( QueryConstants.GEO_LOCATIONS_NUMBER_RETURN );
			options.aggregateSourceMetadata = optionsRaw.hasOwnProperty( QueryConstants.SOURCE_METADATA );
			options.aggregateSources = optionsRaw.hasOwnProperty( QueryConstants.SOURCES );
			options.aggregateTimes = optionsRaw.hasOwnProperty( QueryConstants.TIMES_INTERVAL );
			
			return options;
		}
		
		/**
		 * Updates the scoring options based on the values returned in the query string raw object
		 */
		public static function setScoringOptions( options:QueryScoreOptions, optionsRaw:Object ):QueryScoreOptions
		{
			// 1] enableScoring vs relWeight/sigWeight
			if ( optionsRaw.hasOwnProperty( QueryConstants.REL_WEIGHT ) && ( options.sigWeight == 0 ) )
			{
				var relWeight:int = optionsRaw[ QueryConstants.REL_WEIGHT ] as int;
				
				if ( relWeight == 0 )
				{
					options.disableScoring();
				}
			}
			
			// 2] adjustAggregateSig
			if ( !optionsRaw.hasOwnProperty( QueryConstants.ADJUST_AGGREGATE_SIG ) || ( null == optionsRaw[ QueryConstants.ADJUST_AGGREGATE_SIG ] ) )
			{
				options.adjustAggregateSig = 0; // auto
			}
			else
			{
				var tmp:Boolean = optionsRaw[ QueryConstants.ADJUST_AGGREGATE_SIG ] as Boolean;
				
				if ( tmp )
				{
					options.adjustAggregateSig = 1; // always
				}
				else
				{
					options.adjustAggregateSig = 2; // never
				}
			}
			
			return options;
		}
	}
}
