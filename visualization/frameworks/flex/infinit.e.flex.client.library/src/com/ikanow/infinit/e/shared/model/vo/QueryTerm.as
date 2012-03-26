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
	import com.ikanow.infinit.e.shared.model.constant.Constants;
	import com.ikanow.infinit.e.shared.model.constant.types.EditModeTypes;
	import com.ikanow.infinit.e.shared.model.constant.types.QueryTermTypes;
	import com.ikanow.infinit.e.shared.util.QueryUtil;
	import flash.events.EventDispatcher;
	import mx.resources.ResourceManager;
	
	[Bindable]
	public class QueryTerm extends EventDispatcher
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var entityOpt:QueryTermOptions = new QueryTermOptions();
		
		public var entity:String;
		
		public var entityValue:String;
		
		public var entityType:String;
		
		public var etext:String;
		
		public var ftext:String;
		
		public var time:QueryTermTemporal;
		
		public var geo:QueryTermGeoLocation;
		
		public var event:QueryTermEvent;
		
		[Transient]
		public var _id:String;
		
		[Transient]
		public var level:int;
		
		[Transient]
		public var logicIndex:int;
		
		[Transient]
		public var logicOperator:String;
		
		[Transient]
		public var entityArray:Array;
		
		[Transient]
		public var editing:Boolean;
		
		[Transient]
		public var editMode:String = EditModeTypes.UPDATE;
		
		private var _type:String;
		
		public function get type():String
		{
			if ( entity )
				return QueryTermTypes.ENTITY;
			
			if ( etext )
				return QueryTermTypes.EXACT_TEXT;
			
			if ( ftext )
				return QueryTermTypes.FREE_TEXT;
			
			if ( time )
				return QueryTermTypes.TEMPORAL;
			
			if ( geo )
				return QueryTermTypes.GEO_LOCATION;
			
			if ( event )
				return QueryTermTypes.EVENT;
			
			return Constants.BLANK;
		}
		
		public function set type( value:String ):void
		{
			_type = value;
		}
		
		private var _displayLabel:String;
		
		public function get displayLabel():String
		{
			if ( entity )
			{
				var strings:Array = entity.split( Constants.FORWARD_SLASH, 2 );
				return strings[ 0 ];
			}
			
			if ( etext )
				return Constants.DOUBLE_QUOTE + etext + Constants.DOUBLE_QUOTE;
			
			if ( ftext )
				return Constants.DOUBLE_QUOTE + ftext + Constants.DOUBLE_QUOTE;
			
			if ( time )
			{
				var timeString:String = time.startDateString;
				
				if ( time.startDateString != Constants.BLANK && time.endDateString != Constants.BLANK )
					timeString += Constants.SPACE + ResourceManager.getInstance().getString( 'infinite', 'common.to' ).toLowerCase();
				
				timeString += Constants.SPACE + time.endDateString;
				
				return timeString;
			}
			
			if ( geo )
				return geo.centerll + Constants.AMPERSAND + geo.dist;
			
			if ( event )
			{
				var eventDisplay:String = Constants.BLANK;
				
				if ( event && event.entity1 && event.entity1.displayLabel )
					eventDisplay += event.entity1.displayLabel;
				
				eventDisplay += Constants.AMPERSAND;
				
				if ( event && event.verb )
					eventDisplay += event.verb;
				
				eventDisplay += Constants.AMPERSAND;
				
				if ( event && event.entity2 && event.entity2.displayLabel )
					eventDisplay += event.entity2.displayLabel;
				
				return eventDisplay;
			}
			
			return Constants.BLANK;
		}
		
		public function set displayLabel( value:String ):void
		{
			_displayLabel = value;
		}
		
		private var _entityLabel:String;
		
		public function get entityLabel():String
		{
			if ( entity )
			{
				var strings:Array = entity.split( Constants.FORWARD_SLASH, 2 );
				return strings[ 0 ];
			}
			
			if ( etext )
				return etext;
			
			if ( ftext )
				return ftext;
			
			return Constants.BLANK;
		}
		
		public function set entityLabel( value:String ):void
		{
			_entityLabel = value;
		}
		
		private var _eventEntityLabel:String;
		
		public function get eventEntityLabel():String
		{
			if ( entity )
				return entity;
			
			if ( etext )
				return etext;
			
			if ( ftext )
				return ftext;
			
			return Constants.BLANK;
		}
		
		public function set eventEntityLabel( value:String ):void
		{
			_eventEntityLabel = value;
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		public function clone():QueryTerm
		{
			var clone:QueryTerm = new QueryTerm();
			
			clone._id = QueryUtil.getRandomNumber().toString();
			clone.entityOpt = entityOpt.clone();
			clone.entity = entity;
			clone.entityValue = entityValue;
			clone.entityType = entityType;
			clone.etext = etext;
			clone.ftext = ftext;
			
			if ( time )
				clone.time = time.clone();
			
			if ( geo )
				clone.geo = geo.clone();
			
			if ( event )
				clone.event = event.clone();
			
			clone.level = level;
			clone.logicOperator = logicOperator;
			clone.editing = editing;
			clone.editMode = editMode;
			
			return clone;
		}
		
		public function getEntityType():String
		{
			if ( entityType )
			{
				return entityType;
			}
			else if ( entity )
			{
				entityArray = entity.split( Constants.FORWARD_SLASH );
				return entityArray[ 1 ] as String;
			}
			
			return Constants.BLANK;
		}
		
		public function getEntityValue():String
		{
			if ( entityValue )
			{
				return entityValue;
			}
			else if ( entity )
			{
				entityArray = entity.split( Constants.FORWARD_SLASH );
				return entityArray[ 0 ] as String;
			}
			
			return Constants.BLANK;
		}
		
		public function setEntity( value:String ):void
		{
			etext = null;
			ftext = null;
			entity = value;
			event = null;
			time = null;
			geo = null;
		}
		
		public function setEtext( value:String ):void
		{
			etext = value;
			ftext = null;
			entity = null;
			event = null;
			time = null;
			geo = null;
		}
		
		public function setEvent( value:QueryTermEvent ):void
		{
			etext = null;
			ftext = null;
			entity = null;
			event = value;
			time = null;
			geo = null;
		}
		
		public function setFtext( value:String ):void
		{
			etext = null;
			ftext = value;
			entity = null;
			event = null;
			time = null;
			geo = null;
		}
		
		public function setGeo( value:QueryTermGeoLocation ):void
		{
			etext = null;
			ftext = null;
			entity = null;
			event = null;
			time = null;
			geo = value;
		}
		
		public function setTemporal( value:QueryTermTemporal ):void
		{
			etext = null;
			ftext = null;
			entity = null;
			event = null;
			time = value;
			geo = null;
		}
	}
}
