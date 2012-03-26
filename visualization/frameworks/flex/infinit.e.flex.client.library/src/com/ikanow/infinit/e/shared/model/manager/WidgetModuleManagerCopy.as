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
package com.ikanow.infinit.e.shared.model.manager
{
	import com.ikanow.infinit.e.shared.model.constant.QueryConstants;
	import com.ikanow.infinit.e.shared.model.manager.base.InfiniteManager;
	import com.ikanow.infinit.e.shared.model.vo.QueryStringRequest;
	import com.ikanow.infinit.e.widget.library.data.SelectedItem;
	import com.ikanow.infinit.e.widget.library.data.WidgetContext;
	import com.ikanow.infinit.e.widget.library.framework.InfiniteMaster;
	import com.ikanow.infinit.e.widget.library.frameworkold.QueryResults;
	import com.ikanow.infinit.e.widget.library.widget.IResultSet;
	import mx.rpc.http.mxml.HTTPService;
	
	/**
	 * Widget Manager
	 */
	public class WidgetModuleManagerCopy extends InfiniteManager implements InfiniteMaster
	{
		
		//======================================
		// public properties 
		//======================================
		
		public var context:WidgetContext;
		
		[Bindable]
		/**
		 * The results returned from a query
		 */
		public var queryResult:Object;
		
		[Bindable]
		/**
		 * The current query string that is being modified for a new query
		 */
		public var currentQueryStringRequest:QueryStringRequest;
		
		//======================================
		// constructor 
		//======================================
		
		public function WidgetModuleManagerCopy()
		{
			super();
			
			context = new WidgetContext( this );
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		public function applyQueryToAllWidgets( queryResults:IResultSet ):void
		{
		
		}
		
		/**
		 * function to follow bread crumb based on the action that was clicked
		 *
		 * @param action The action to be processed
		 */
		public function followBreadcrumb( action:String ):void
		{
		
		}
		
		public function getCurrentQuery():Object
		{
			return currentQueryStringRequest.clone() as Object;
		}
		
		public function invokeQueryEngine( queryObject:Object, widgetHttpService:HTTPService = null ):Boolean
		{
			return false;
		}
		
		public function loadUISetup():void
		{
		}
		
		
		public function parentFlagFilterEvent():void
		{
		
		}
		
		public function parentReceiveSelectedItem( selectedItem:SelectedItem ):void
		{
		
		}
		
		/**
		 * set the current query string
		 * @param value
		 */
		public function setCurrentQueryString( value:QueryStringRequest ):void
		{
			currentQueryStringRequest = value;
		}
		
		/**
		 * set the result returned from a query
		 * @param value
		 */
		public function setQueryResult( value:Object ):void
		{
			queryResult = value;
			
			var queryResults:QueryResults = new QueryResults();
			
			if ( value )
				queryResults.populateQueryResults( value, null, context );
			
			// set the results in the context and the widget modules
			context.onNewQuery( queryResults, QueryConstants.QUERY_RESULTS, currentQueryStringRequest as Object );
		}
		
		
		public function updateCurrentQuery( newQuery:Object, modifiedElements:String ):void
		{
		
		}
	}
}
