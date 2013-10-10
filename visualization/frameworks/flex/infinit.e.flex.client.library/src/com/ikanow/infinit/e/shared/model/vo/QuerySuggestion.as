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
	import com.ikanow.infinit.e.shared.model.constant.types.EntityTypes;
	import com.ikanow.infinit.e.shared.model.constant.types.QueryDimensionTypes;
	import com.ikanow.infinit.e.shared.model.constant.types.QuerySuggestionTypes;
	import flash.events.EventDispatcher;
	import mx.collections.ArrayCollection;
	import mx.resources.ResourceManager;
	import assets.EmbeddedAssets;
	
	[Bindable]
	public class QuerySuggestion extends EventDispatcher
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var dimension:String;
		
		public var value:String;
		
		public var type:String;
		
		public var geotag:Geo;
		
		public var ontology_type:String;
		
		[ArrayCollectionElementType( "String" )]
		public var communityids:ArrayCollection;
		
		[ArrayCollectionElementType( "String" )]
		public var aliases:ArrayCollection;
		
		[Transient]
		public var searchTerm:String;
		
		[Transient]
		public var showHeading:Boolean;
		
		[Transient]
		public var displayValueHTML:String;
		
		[Transient]
		public function get displayValue():String
		{
			return value + "   " + Constants.PARENTHESIS_LEFT + type + Constants.PARENTHESIS_RIGHT;
		}
		
		private var _sortOrder:int;
		
		[Transient]
		public function get sortOrder():int
		{
			return QueryDimensionTypes.getSortOrder( dimension );
		}
		
		public function set sortOrder( value:int ):void
		{
			_sortOrder = value;
		}
		
		public var _dimensionHeading:String;
		
		[Transient]
		public function get dimensionHeading():String
		{
			return QueryDimensionTypes.getLabel( dimension );
		}
		
		public function set dimensionHeading( value:String ):void
		{
			_dimensionHeading = value;
		}
		
		[Transient]
		public var foundColor1:String = "#FFFFFF";
		
		[Transient]
		public var foundColor2:String = "#FF0000";
		
		[Transient]
		public var typeColor1:String = "#A3BBC5";
		
		[Transient]
		public var typeColor2:String = "#000000";
		
		private var _displayValueColor1:String;
		
		[Transient]
		/**
		 * HTML text that has the search term highlighted with foundColor1
		 */
		public function get displayValueColor1():String
		{
			var start:int = searchTerm.length;
			var index:int;
			var newStr:String;
			
			if ( type == Constants.BLANK )
			{
				_displayValueColor1 = value;
			}
			else
			{
				index = value.toLowerCase().indexOf( searchTerm.toLowerCase() );
				
				if ( 0 == index )
				{
					newStr = "<FONT COLOR='" + foundColor1 + "'>" + searchTerm + "</FONT>" + value.substring( Number( start ) ) +
						"<FONT COLOR='" + typeColor1 + "'>&#xA0;&#xA0;&#xA0;(" + type.toLowerCase() + ")</FONT>";
					
					_displayValueColor1 = newStr;
				}
				else if ( index > 0 )
				{
					newStr = value.substr( 0, index ) + "<FONT COLOR='" + foundColor1 + "'>" + value.substring( index, index + Number( start ) ) +
						"</FONT>" + value.substring( index + Number( start ) ) + "<FONT COLOR='" + typeColor1 + "'>&#xA0;&#xA0;&#xA0;(" + type.toLowerCase() + ")</FONT>";
					
					_displayValueColor1 = newStr;
				}
				else
				{
					_displayValueColor1 = value + "<FONT COLOR='" + typeColor1 + "'>&#xA0;&#xA0;&#xA0;(" + type.toLowerCase() + ")</FONT>";
				}
			}
			
			return _displayValueColor1;
		}
		
		public function set displayValueColor1( value:String ):void
		{
			_displayValueColor1 = value;
		}
		
		private var _displayValueColor2:String;
		
		[Transient]
		/**
		 * HTML text that has the search term highlighted with foundColor2
		 */
		public function get displayValueColor2():String
		{
			var start:int = searchTerm.length;
			var index:int;
			var newStr:String;
			
			if ( type == Constants.BLANK )
			{
				_displayValueColor2 = value;
			}
			else
			{
				index = value.toLowerCase().indexOf( searchTerm.toLowerCase() );
				
				if ( 0 == index )
				{
					newStr = "<FONT COLOR='" + foundColor2 + "'>" + searchTerm + "</FONT>" + value.substring( Number( start ) ) +
						"<FONT COLOR='" + typeColor2 + "'>&#xA0;&#xA0;&#xA0;(" + type.toLowerCase() + ")</FONT>";
					
					_displayValueColor2 = newStr;
				}
				else if ( index > 0 )
				{
					newStr = value.substr( 0, index ) + "<FONT COLOR='" + foundColor2 + "'>" + value.substring( index, index + Number( start ) ) +
						"</FONT>" + value.substring( index + Number( start ) ) + "<FONT COLOR='" + typeColor2 + "'>&#xA0;&#xA0;&#xA0;(" + type.toLowerCase() + ")</FONT>";
					
					_displayValueColor2 = newStr;
				}
				else
				{
					_displayValueColor2 = value + "<FONT COLOR='" + typeColor2 + "'>&#xA0;&#xA0;&#xA0;(" + type.toLowerCase() + ")</FONT>";
				}
			}
			
			return _displayValueColor2;
		}
		
		public function set displayValueColor2( value:String ):void
		{
			_displayValueColor2 = value;
		}
		
		private var _headingIcon:Class;
		
		/**
		 * The heading icon to display
		 * @return
		 */
		public function get headingIcon():Class
		{
			return QueryDimensionTypes.getIcon( dimension );
		}
		
		public function set headingIcon( value:Class ):void
		{
			_headingIcon = value;
		}
	}
}
