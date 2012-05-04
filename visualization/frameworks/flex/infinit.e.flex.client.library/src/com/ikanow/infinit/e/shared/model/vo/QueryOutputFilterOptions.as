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
	/**/
	import mx.controls.Alert;
	
	[Bindable]
	public class QueryOutputFilterOptions extends EventDispatcher
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var entityTypes:Array;
		
		public var assocVerbs:Array;
		
		[Transient]
		public var entityTypes_cs:String;
		
		[Transient]
		public var assocVerbs_cs:String;
		
		//======================================
		// constructor 
		//======================================
		
		public function QueryOutputFilterOptions()
		{
			super();
			reset();
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		public function apply( options:QueryOutputFilterOptions ):void
		{
			entityTypes_cs = options.entityTypes_cs;
			assocVerbs_cs = options.assocVerbs_cs;
			
			if ( ( null != entityTypes_cs ) && ( entityTypes_cs.length > 0 ) )
			{
				entityTypes = entityTypes_cs.split( /\s*,\s*/ );
			}
			else if ( null != options.entityTypes ) // Copying in other direction 
			{
				entityTypes_cs = "";
				
				for each ( var entType:String in options.entityTypes )
				{
					if ( entityTypes_cs.length > 0 )
					{
						entityTypes_cs += ",";
					}
					entityTypes_cs += entType;
				}
			}
			else // There's just no filters to apply
			{
				entityTypes = null;
			}
			
			if ( ( null != assocVerbs_cs ) && ( assocVerbs_cs.length > 0 ) )
			{
				assocVerbs = assocVerbs_cs.split( /\s*,\s*/ );
			}
			else if ( null != options.assocVerbs ) // Copying in other direction 
			{
				assocVerbs_cs = "";
				
				for each ( var assocVerb:String in options.assocVerbs )
				{
					if ( assocVerbs_cs.length > 0 )
					{
						assocVerbs_cs += ",";
					}
					assocVerbs_cs += assocVerb;
				}
			}
			else // There's just no filters to apply
			{
				assocVerbs = null;
			}
		}
		
		public function clone():QueryOutputFilterOptions
		{
			var clone:QueryOutputFilterOptions = new QueryOutputFilterOptions();
			clone.apply( this );
			
			return clone;
		}
		
		public function reset():void
		{
			entityTypes = null;
			assocVerbs = null;
			entityTypes_cs = QueryAdvancedSettingsConstants.OUTPUT_FILTER_ENTTYPES;
			assocVerbs_cs = QueryAdvancedSettingsConstants.OUTPUT_FILTER_ASSOCVERBS;
		}
	}
}
