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
	import com.ikanow.infinit.e.shared.model.constant.Constants;
	import com.ikanow.infinit.e.shared.model.constant.QueryConstants;
	import com.ikanow.infinit.e.shared.model.vo.Community;
	import com.ikanow.infinit.e.shared.model.vo.CommunityApproval;
	import com.ikanow.infinit.e.shared.model.vo.CommunityAttribute;
	import com.ikanow.infinit.e.shared.model.vo.LatLong;
	import com.ikanow.infinit.e.shared.model.vo.Link;
	import com.ikanow.infinit.e.shared.model.vo.Member;
	import com.ikanow.infinit.e.shared.model.vo.QueryOutputAggregationOptions;
	import com.ikanow.infinit.e.shared.model.vo.QueryOutputDocumentOptions;
	import com.ikanow.infinit.e.shared.model.vo.QuerySuggestion;
	import com.ikanow.infinit.e.shared.model.vo.QuerySuggestions;
	import com.ikanow.infinit.e.shared.model.vo.QueryTerm;
	import com.ikanow.infinit.e.shared.model.vo.Type;
	import com.ikanow.infinit.e.shared.model.vo.UserAttribute;
	import com.ikanow.infinit.e.shared.model.vo.WidgetSummary;
	import flash.utils.*;
	import mx.charts.LineChart;
	import mx.collections.ArrayCollection;
	import mx.controls.AdvancedDataGrid;
	
	public class ObjectTranslatorUtil
	{
		
		//======================================
		// private properties 
		//======================================
		
		// unfortunately, these classes must be referenced in order to be translated
		
		private var lineChart:LineChart;
		
		private var adg:AdvancedDataGrid;
		
		private var community:Community;
		
		private var communityApproval:CommunityApproval;
		
		private var communityAttribute:CommunityAttribute;
		
		private var latLong:LatLong;
		
		private var link:Link;
		
		private var member:Member;
		
		private var output:QueryOutputAggregationOptions;
		
		private var docs:QueryOutputDocumentOptions;
		
		private var querySuggestion:QuerySuggestion;
		
		private var querySuggestions:QuerySuggestions;
		
		private var queryTerm:QueryTerm;
		
		private var type:Type;
		
		private var userAttribute:UserAttribute;
		
		private var widgetSummary:WidgetSummary;
		
		//======================================
		// constructor 
		//======================================
		
		public function ObjectTranslatorUtil()
		{
		
		}
		
		
		//======================================
		// public static methods 
		//======================================
		
		/**
		 * Create a clone of an object
		 * @param source
		 * @return *
		 */
		public static function clone( source:Object ):*
		{
			var myBA:ByteArray = new ByteArray();
			myBA.writeObject( source );
			myBA.position = 0;
			return ( myBA.readObject() );
		}
		
		/**
		 * Convert common text values into a Boolean value
		 * @param bValue
		 * @return Boolean
		 */
		public static function getBooleanValue( bValue:Object ):Boolean
		{
			var returnBoolean:Boolean = false;
			var stringBoolean:String = bValue.toString().toLowerCase();
			
			if ( stringBoolean == "true" || stringBoolean == "yes" || stringBoolean == "1" || stringBoolean == "-1" )
			{
				returnBoolean = true;
			}
			return returnBoolean;
		}
		
		/**
		 * Parse date from a string
		 * @param dateString
		 * @return Date
		 */
		public static function parseDateFromString( dateString:String ):Date
		{
			var returnDate:Date;
			
			// check to see if we have a utc date as a string
			// guessing that dateString should be at least 6 chars long in order to be 
			// considered a viable date.... this could be a problem though, because theoretically,
			// a time of zero should still be valid.
			if ( dateString.length > 6 && parseInt( dateString ).toString().length > 6 )
			{
				// we probaly have a utc date here
				returnDate = parseUtcDate( parseInt( dateString ) );
			}
			else
			{
				//  we didn't get a utc date so let's try to parse it...
				returnDate = parseUtcDate( Date.parse( dateString ) );
			}
			
			return returnDate;
		}
		
		/**
		 * Parse a Date from a UTC number
		 * @param utcNumber
		 * @return Date
		 */
		public static function parseUtcDate( utcNumber:Number ):Date
		{
			var returnDate:Date;
			
			// there should be a minimum length here for a valid utc Date
			if ( utcNumber.toString().length > 6 )
			{
				returnDate = new Date;
				returnDate.setTime( utcNumber );
			}
			return returnDate;
		}
		
		/**
		 * Translate an ArrayCollection
		 * @param collection
		 * @param translateToClass
		 * @param upperCaseFromVar
		 * @return ArrayCollection
		 */
		public static function translateArrayCollection( collection:ArrayCollection, translateToClass:Class, upperCaseFromVar:Boolean = false ):ArrayCollection
		{
			// if an array collection is expected, this function will check to make sure 
			// that it is not null.  it returns a completely new collection, which may or may not be what is desired.
			var returnCollection:ArrayCollection = new ArrayCollection;
			
			if ( collection != null )
			{
				if ( collection.length > 0 )
				{
					returnCollection.source = translateArrayObjects( collection.source, translateToClass, upperCaseFromVar );
				}
			}
			return returnCollection;
		}
		
		/**
		 * Translate an ArrayCollection and return the source Array
		 * @param collection
		 * @param translateToClass
		 * @param upperCaseFromVar
		 * @return Array
		 */
		public static function translateArrayCollectionAsArray( collection:ArrayCollection, translateToClass:Class, upperCaseFromVar:Boolean = false ):Array
		{
			// if an array collection is expected, this function will check to make sure 
			// that it is not null.  it returns a completely new Array, which may or may not be what is desired
			var returnArray:Array = new Array;
			
			if ( collection != null )
			{
				if ( collection.length > 0 )
				{
					returnArray = translateArrayObjects( collection.source, translateToClass, upperCaseFromVar );
				}
			}
			return returnArray;
		}
		
		/**
		 * Taranslate individual Array Objects
		 * @param arrayOfObjects
		 * @param translateToClass
		 * @param upperCaseFromVar
		 * @return Array
		 */
		public static function translateArrayObjects( arrayOfObjects:Array, translateToClass:Class, upperCaseFromVar:Boolean = false ):Array
		{
			// describeType describes a class and all of its properties
			var classInfo:XML = describeType( translateToClass );
			// get the class name, in order to instantiate more classes
			var className:String = classInfo.@name;
			var i:Number = new Number;
			var returnArray:Array = new Array;
			// get a reference to the class to create new classes 
			var classRef:Class = Class( getDefinitionByName( className ) );
			var tempObj:Object = new classRef;
			
			if ( arrayOfObjects != null )
			{
				for ( i = 0; i < arrayOfObjects.length; i++ )
				{
					// for each new object, create a new class of the class type that was passed in.
					tempObj = new classRef();
					
					// send it off to translateObject function. send it the class to populate
					if ( className == "String" )
					{
						returnArray.push( arrayOfObjects[ i ].toString() );
					}
					else
					{
						returnArray.push( translateObject( arrayOfObjects[ i ], tempObj, classInfo, upperCaseFromVar ) );
					}
					
				}
			}
			return returnArray;  // return the translated array
		}
		
		
		/**
		 * Translate Objects into strongly-typed Class objects
		 * @param translateFrom
		 * @param translateTo
		 * @param classInfo
		 * @param upperCaseFromVar
		 * @return Object
		 */
		public static function translateObject( translateFrom:Object, translateTo:Object, classInfo:XML = null, upperCaseFromVar:Boolean = false, override:Boolean = false ):Object
		{
			var itemType:String;
			var itemName:String;
			var subClassInfo:XML;
			var tempArray:Array;
			var vType:String;
			var vName:String;
			var vClass:Class
			var fromName:String;
			var arrayCollectionElementClass:Class;
			
			// if class information has already been sent in, we don't need to re-fetch it.
			if ( classInfo == null )
			{
				classInfo = describeType( translateTo );
			}
			
			for each ( var v:XML in classInfo..accessor )
			{
				try
				{
					vName = v.@name;
					vType = v.@type;
					
					if ( upperCaseFromVar )
					{
						fromName = vName.toUpperCase();
					}
					else
					{
						fromName = vName;
					}
					
					// check to see if the untyped object has a property that matches the typed property
					// if it does, then translate it.  if not, skip it.
					// this is done to maintain integrity with the application and the internal classes.
					// internal classes are static (unless they've been made dynamic) an error will result if
					// and unknown variable is set in it.  
					if ( translateFrom.hasOwnProperty( vName ) && translateFrom[ vName ] != null )
					{
						// Only need to check for a few types here. variables should come across 
						// in a limited number of base object types, ie numbers, ints, dates, strings, etc.
						// anything else is assumed to be a class and will be sent for translation as well.
						// the one case that is not dealt with here is when an array of objects underneath
						// an array of objects.  This can be theoretically set up, but there's no clear 
						// an array of objects has to be a special type of array, which in most cases only limits
						// the objects that can be store in it to a specific class.  the simplest way around 
						// this is to send back a simple array of untyped objects which can then be translated
						// at the point that they are needed.
						
						switch ( vType )
						{
							case "Array":
								if ( translateFrom[ vName ] is ArrayCollection )
								{
									translateTo[ vName ] = translateFrom[ fromName ].source;
								}
								else if ( translateFrom[ fromName ] is Array )
								{
									translateTo[ vName ] = translateFrom[ fromName ];
								}
								break;
							case "mx.collections::ArrayCollection":
								// for this application all ArrayCollection must have ArrayCollectionElementType metadata in order to be translated
								for each ( var arrayCollectionElement:XML in v..metadata )
								{
									if ( arrayCollectionElement.@name == "ArrayCollectionElementType" )
										arrayCollectionElementClass = getDefinitionByName( arrayCollectionElement..arg.@value ) as Class;
								}
								
								if ( translateFrom[ fromName ] is ArrayCollection )
								{
									if ( arrayCollectionElementClass )
										translateTo[ vName ] = ObjectTranslatorUtil.translateArrayCollection( translateFrom[ fromName ] as ArrayCollection, arrayCollectionElementClass );
									else
										translateTo[ vName ] = translateFrom[ fromName ];
								}
								else if ( translateFrom[ fromName ] is Array )
								{
									if ( arrayCollectionElementClass )
										translateTo[ vName ] = ObjectTranslatorUtil.translateArrayCollection( new ArrayCollection( translateFrom[ fromName ] ), arrayCollectionElementClass );
									else
										translateTo[ vName ].source = translateFrom[ fromName ];
								}
								break;
							case "Number":
								// sometimes numbers come across as a complexString.  this can 
								// cause a mess at runtime if there is a need to check against a 
								// simple number type.
								translateTo[ vName ] = new Number( translateFrom[ fromName ] );
								break;
							case "int":
								// ditto for ints
								translateTo[ vName ] = new int( translateFrom[ fromName ] );
								break;
							case "uint":
								// ditto for uints
								translateTo[ vName ] = new uint( translateFrom[ fromName ] );
								break;
							case "Date":
								// ditto for dates.  what's nice about this function is that with a little 
								// work, dates can automatically be transfered from a UTC format or a string 
								// format (used sometimes for easy transport)  into an actual date format
								// which should be easier to use.
								if ( translateFrom[ fromName ] is Date )
								{
									translateTo[ vName ] = translateFrom[ fromName ];
								}
								else if ( translateFrom[ fromName ] is String )
								{
									translateTo[ vName ] = parseDateFromString( translateFrom[ fromName ] );
								}
								else if ( translateFrom[ fromName ] is Number || translateFrom[ fromName ] is int || translateFrom[ fromName ] is uint )
								{
									translateTo[ vName ] = parseUtcDate( translateFrom[ fromName ] );
								}
								break;
							case "Boolean":
								// Booleans get typed as a complexString sometimes too.
								translateTo[ vName ] = getBooleanValue( translateFrom[ fromName ] );
								break;
							case "String":
								translateTo[ vName ] = new String( translateFrom[ fromName ] );
								break;
							case "Object":
								if ( fromName == QueryConstants.WIDGET_OPTIONS && translateFrom[ fromName ] != null && translateFrom[ fromName ] != Constants.BLANK )
								{
									translateFrom[ fromName ] = JSONUtil.decode( translateFrom[ fromName ] );
								}
								translateTo[ vName ] = translateFrom[ fromName ];
								break;
							default:
								// assuming that anything that makes it here is a sub class...
								// send it to the this function again to decode it...
								
								// hack because this property contains a JSON string
								if ( fromName == QueryConstants.QUERY_STRING && !override )
								{
									translateFrom[ fromName ] = JSONUtil.decode( translateFrom[ fromName ] );
								}
								
								vClass = getDefinitionByName( vType ) as Class;
								translateTo[ vName ] = ObjectTranslatorUtil.translateObject( translateFrom[ fromName ], new vClass );
								
								// hack because this object contains optional properites that control Boolean values
								if ( fromName == QueryConstants.AGGREGATION )
								{
									QueryUtil.setAggregationOptions( translateTo[ vName ], translateFrom[ fromName ] );
								}
								if ( fromName == QueryConstants.SCORING_OPTIONS )
								{
									QueryUtil.setScoringOptions( translateTo[ vName ], translateFrom[ fromName ] );
								}
								
								break;
						}
					}
				}
				catch ( e:Error )
				{
					// do nothing // we will loose data, but our app will stay in tact;
					//if (translateFrom.hasOwnProperty(v.@name)) 	{
					//	translateTo[v.@name] = translateFrom[v.@name];
					//}
					trace( "an error occured in ObjectTranslator. The following fields were not captured correctly - to:" + vName + "  from:" + fromName );
					trace( translateTo[ vName ] );
					trace( translateFrom[ fromName ] );
				}
				
			}
			return translateTo;
		}
	}
}
