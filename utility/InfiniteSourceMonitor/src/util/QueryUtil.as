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
	import com.adobe.utils.StringUtil;
	import mx.collections.ArrayCollection;
	import mx.core.ClassFactory;
	import mx.resources.ResourceManager;
	import objects.Community;
	import util.Constants;
	import util.QueryConstants;
	
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
		 * Returns a random number
		 * @return Number
		 */
		public static function getRandomNumber():Number
		{
			return Math.round( Math.random() * 99999 );
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
	}
}
