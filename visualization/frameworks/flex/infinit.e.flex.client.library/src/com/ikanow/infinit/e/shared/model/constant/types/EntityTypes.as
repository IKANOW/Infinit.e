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
package com.ikanow.infinit.e.shared.model.constant.types
{
	import mx.resources.ResourceManager;
	import assets.EmbeddedAssets;
	
	/**
	 * Entity Type Constants
	 */
	public class EntityTypes
	{
		
		//======================================
		// public static properties 
		//======================================
		
		public static const EXACT_TEXT:String = ResourceManager.getInstance().getString( 'infinite', 'dimension.exactText' );
		
		public static const FREE_TEXT:String = ResourceManager.getInstance().getString( 'infinite', 'dimension.freeText' );
		
		public static const PERSON:String = "person";
		
		public static const PERSON_VICTIM:String = "personvictim";
		
		public static const PERSON_SUSPECT:String = "personsuspect";
		
		public static const CRIMINAL_ACTIVITY:String = "criminalactivity";
		
		public static const PLACE:String = "place";
		
		public static const CITY:String = "city";
		
		public static const REGION:String = "region";
		
		public static const COUNTRY:String = "country";
		
		public static const CONTINENT:String = "continent";
		
		public static const STATE_OR_COUNTRY:String = "stateorcounty";
		
		public static const PROVINCE_OR_STATE:String = "provinceorstate";
		
		public static const COMPANY:String = "company";
		
		public static const ORGANIZATION:String = "organization";
		
		public static const HEALTH_CONDITION:String = "healthcondition";
		
		public static const DRUG:String = "drug";
		
		public static const SPORT:String = "sport";
		
		public static const SPORTING_EVENT:String = "sportingevent";
		
		public static const FACILITY:String = "facility";
		
		public static const GEOGRAPHIC_FEATURE:String = "geographicfeature";
		
		public static const ENTERTAINMENT_AWARD:String = "entertainmentaward";
		
		public static const MOVIE:String = "movie";
		
		public static const TELEVISION_STATION:String = "televisionstation";
		
		public static const TELEVISION_SHOW:String = "televisionshow";
		
		public static const MUSIC_GROUP:String = "musicgroup";
		
		public static const RADIO_STATION:String = "radiostation";
		
		public static const FINANCIAL_MARKET_INDEX:String = "financialmarketindex";
		
		public static const AUTOMOBILE:String = "automobile";
		
		public static const PRINT_MEDIA:String = "printmedia";
		
		public static const HOLIDAY:String = "holiday";
		
		public static const NATURAL_DISASTER:String = "naturaldisaster";
		
		public static const FIELD_TERMINOLGY:String = "fieldterminology";
		
		public static const TECHNOLOGY:String = "technology";
		
		public static const PRODUCT:String = "product";
		
		
		//======================================
		// public static methods 
		//======================================
		
		/**
		 * function to get the dimension type based on the entity type
		 *
		 * @param type The type of the entity
		 *
		 * @return The dimension of the entity
		 */
		public static function getEntityDimensionType( type:String ):String
		{
			type = type.toLowerCase();
			
			switch ( type )
			{
				case EXACT_TEXT:
					return QueryDimensionTypes.EXACT_TEXT;
					break;
				case FREE_TEXT:
					return QuerySuggestionTypes.FREE_TEXT;
					break;
				case PERSON:
					return QueryDimensionTypes.WHO;
					break;
				case PERSON_VICTIM:
					return QueryDimensionTypes.WHO;
					break;
				case PERSON_SUSPECT:
					return QueryDimensionTypes.WHO;
					break;
				case CRIMINAL_ACTIVITY:
					return QueryDimensionTypes.WHAT;
					break;
				case PLACE:
					return QueryDimensionTypes.WHERE;
					break;
				case CITY:
					return QueryDimensionTypes.WHERE;
					break;
				case REGION:
					return QueryDimensionTypes.WHERE;
					break;
				case COUNTRY:
					return QueryDimensionTypes.WHERE;
					break;
				case CONTINENT:
					return QueryDimensionTypes.WHERE;
					break;
				case STATE_OR_COUNTRY:
					return QueryDimensionTypes.WHERE;
					break;
				case PROVINCE_OR_STATE:
					return QueryDimensionTypes.WHERE;
					break;
				case COMPANY:
					return QueryDimensionTypes.WHO;
					break;
				case ORGANIZATION:
					return QueryDimensionTypes.WHO;
					break;
				case HEALTH_CONDITION:
					return QueryDimensionTypes.WHAT;
					break;
				case DRUG:
					return QueryDimensionTypes.WHAT;
					break;
				case SPORT:
					return QueryDimensionTypes.WHAT;
					break;
				case SPORTING_EVENT:
					return QueryDimensionTypes.WHAT;
					break;
				case FACILITY:
					return QueryDimensionTypes.WHAT;
					break;
				case GEOGRAPHIC_FEATURE:
					return QueryDimensionTypes.WHAT;
					break;
				case ENTERTAINMENT_AWARD:
					return QueryDimensionTypes.WHAT;
					break;
				case MOVIE:
					return QueryDimensionTypes.WHAT;
					break;
				case TELEVISION_STATION:
					return QueryDimensionTypes.WHAT;
					break;
				case TELEVISION_SHOW:
					return QueryDimensionTypes.WHAT;
					break;
				case MUSIC_GROUP:
					return QueryDimensionTypes.WHAT;
					break;
				case RADIO_STATION:
					return QueryDimensionTypes.WHAT;
					break;
				case FINANCIAL_MARKET_INDEX:
					return QueryDimensionTypes.WHAT;
					break;
				case AUTOMOBILE:
					return QueryDimensionTypes.WHAT;
					break;
				case PRINT_MEDIA:
					return QueryDimensionTypes.WHAT;
					break;
				case HOLIDAY:
					return QueryDimensionTypes.WHAT;
					break;
				case NATURAL_DISASTER:
					return QueryDimensionTypes.WHAT;
					break;
				case FIELD_TERMINOLGY:
					return QueryDimensionTypes.WHAT;
					break;
				case TECHNOLOGY:
					return QueryDimensionTypes.WHAT;
					break;
				case PRODUCT:
					return QueryDimensionTypes.WHAT;
					break;
				default:
					return QueryDimensionTypes.WHAT;
					break;
			}
		}
		
		/**
		 * function to get the dimension type based on the entity type
		 *
		 * @param type The type of the entity
		 *
		 * @return The dimension of the entity
		 */
		public static function getEntitySuggestionType( type:String ):String
		{
			type = type.toLowerCase();
			
			switch ( type )
			{
				case EXACT_TEXT:
					return QuerySuggestionTypes.EXACT_TEXT;
					break;
				case FREE_TEXT:
					return QuerySuggestionTypes.FREE_TEXT;
					break;
				case PERSON:
					return QuerySuggestionTypes.PERSON;
					break;
				case PERSON_VICTIM:
					return QuerySuggestionTypes.PERSON;
					break;
				case PERSON_SUSPECT:
					return QuerySuggestionTypes.PERSON;
					break;
				case CRIMINAL_ACTIVITY:
					return QuerySuggestionTypes.THING;
					break;
				case PLACE:
					return QuerySuggestionTypes.PLACE;
					break;
				case CITY:
					return QuerySuggestionTypes.PLACE;
					break;
				case REGION:
					return QuerySuggestionTypes.PLACE;
					break;
				case COUNTRY:
					return QuerySuggestionTypes.PLACE;
					break;
				case CONTINENT:
					return QuerySuggestionTypes.PLACE;
					break;
				case STATE_OR_COUNTRY:
					return QuerySuggestionTypes.PLACE;
					break;
				case PROVINCE_OR_STATE:
					return QuerySuggestionTypes.PLACE;
					break;
				case COMPANY:
					return QuerySuggestionTypes.COMPANY;
					break;
				case ORGANIZATION:
					return QuerySuggestionTypes.COMPANY;
					break;
				case HEALTH_CONDITION:
					return QuerySuggestionTypes.THING;
					break;
				case DRUG:
					return QuerySuggestionTypes.THING;
					break;
				case SPORT:
					return QuerySuggestionTypes.THING;
					break;
				case SPORTING_EVENT:
					return QuerySuggestionTypes.THING;
					break;
				case FACILITY:
					return QuerySuggestionTypes.THING;
					break;
				case GEOGRAPHIC_FEATURE:
					return QuerySuggestionTypes.THING;
					break;
				case ENTERTAINMENT_AWARD:
					return QuerySuggestionTypes.THING;
					break;
				case MOVIE:
					return QuerySuggestionTypes.THING;
					break;
				case TELEVISION_STATION:
					return QuerySuggestionTypes.THING;
					break;
				case TELEVISION_SHOW:
					return QuerySuggestionTypes.THING;
					break;
				case MUSIC_GROUP:
					return QuerySuggestionTypes.THING;
					break;
				case RADIO_STATION:
					return QuerySuggestionTypes.THING;
					break;
				case FINANCIAL_MARKET_INDEX:
					return QuerySuggestionTypes.THING;
					break;
				case AUTOMOBILE:
					return QuerySuggestionTypes.THING;
					break;
				case PRINT_MEDIA:
					return QuerySuggestionTypes.THING;
					break;
				case HOLIDAY:
					return QuerySuggestionTypes.THING;
					break;
				case NATURAL_DISASTER:
					return QuerySuggestionTypes.THING;
					break;
				case FIELD_TERMINOLGY:
					return QuerySuggestionTypes.THING;
					break;
				case TECHNOLOGY:
					return QuerySuggestionTypes.THING;
					break;
				case PRODUCT:
					return QuerySuggestionTypes.THING;
					break;
				default:
					return QuerySuggestionTypes.THING;
					break;
			}
		}
	}
}

