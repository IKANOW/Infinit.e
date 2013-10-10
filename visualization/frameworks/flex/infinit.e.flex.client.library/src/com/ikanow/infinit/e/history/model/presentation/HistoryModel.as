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
package com.ikanow.infinit.e.history.model.presentation
{
	import com.ikanow.infinit.e.shared.event.QueryEvent;
	import com.ikanow.infinit.e.shared.model.presentation.base.PresentationModel;
	import com.ikanow.infinit.e.shared.model.vo.TypedQueryString;
	import flash.events.Event;
	import mx.collections.ArrayCollection;
	
	/**
	 *  History Presentation Model
	 */
	public class HistoryModel extends PresentationModel
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Bindable]
		[Inject]
		public var navigator:HistoryNavigator;
		
		/**
		 * The collection of recent queries
		 */
		private var _recentQueries:ArrayCollection;
		
		[Bindable( event = "recentQueriesChange" )]
		public function get recentQueries():ArrayCollection
		{
			return _recentQueries;
		}
		
		public function set recentQueries( value:ArrayCollection ):void
		{
			if ( _recentQueries != value )
			{
				_recentQueries = value;
				dispatchEvent( new Event( "recentQueriesChange" ) );
			}
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Run the selected query
		 * @param value
		 */
		public function runHistoryQuery( typedQueryString:TypedQueryString ):void
		{
			var queryEvent:QueryEvent = new QueryEvent( QueryEvent.RUN_HISTORY_QUERY );
			queryEvent.typedQueryString = typedQueryString;
			dispatcher.dispatchEvent( queryEvent );
		}
		
		[Inject( "historyManager.recentQueries", bind = "true" )]
		/**
		 * set the collection of recent queries
		 */
		public function setRecentQueries( value:ArrayCollection ):void
		{
			recentQueries = value;
		}
	}
}

