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
/**
 * <p>Infinit.e</p>
 *
 * <p>Copyright (c) 2011 IKANOW, llc.</p>
 * <p>http://www.ikanow.com</p> 
 *
 * <p>NOTICE:  IKANOW permits you to use this this file in accordance with the terms of the license agreement 
 * accompanying it.  For information about the licensing and copyright of this Plug-In please contact IKANOW, llc. 
 * at support&#64;ikanow.com.</p>
 *
 * <p>http://www.ikanow.com/terms-conditions/</p>
 * 
 */
package com.ikanow.infinit.e.widget.library.framework
{
	import com.ikanow.infinit.e.widget.library.data.SelectedItem;
	import mx.rpc.http.mxml.HTTPService;
	import com.ikanow.infinit.e.widget.library.widget.IResultSet;
	/**
	 * This interface is used to create an infinite master to be the parent over all infinite modules
	 * in the environment
	*/
	
	public interface InfiniteMaster
	{
		//______________________________________________________________________________________
		//
		// QUERIES
		
		/**
		 * Apply the provided results to all widgets (except those with specific data selected) 
		 * 
		 * @param queryResults the results to apply
		 */
		function applyQueryToAllWidgets(queryResults:IResultSet):void;
		
		/**
		 * Performs a query request  
		 * 
		 * @param widgetHttpService (optional) - if non-null, performs a local query using the specified object
		 * (otherwise performs a global query just like if the search button had been pressed
		 * 
		 * @returns Whether the query was successful (nothing to do with the response, which happens via callbacks)
		 */
		function invokeQueryEngine(queryObject:Object, widgetHttpService:HTTPService = null):Boolean;  
		
		//______________________________________________________________________________________
		//
		// FILTERING 
		
		/**
		 * function to pass selected items to any children modules loaded into 
		 * the environment
		 * 
		 * @param selectedItem The selected item being passed from a module
		*/
		function parentReceiveSelectedItem(selectedItem:SelectedItem):void;
		
		/**
		 * flags the filter event as being done by the new context object
		 */
		function parentFlagFilterEvent():void;
		
		//______________________________________________________________________________________
		//
		// FRAMEWORK INTERFACE 
		
		// Query state
		
		/**
		 * Retrieves the current query object (not necessarily the same one that was last run, eg if updated from the UI)
		 * 
		 * @returns the current query object from the UI
		 */
		function getCurrentQuery():Object;
		
		/**
		 * Update the current query settings with a widget-modified version
		 * 
		 * @param action The action to be processed
		 */
		function updateCurrentQuery(newQuery:Object, modifiedElements:String):void;
		
		//______________________________________________________________________________________
		//
		// UNKNOWN (PRE-BETA?)
		
		/**
		 * function to load the ui setup of the user
		 */
		function loadUISetup():void;
		
		/**
		 * function to follow bread crumb based on the action that was clicked
		 * 
		 * @param action The action to be processed
		 */
		function followBreadcrumb(action:String):void;		
	}
}
