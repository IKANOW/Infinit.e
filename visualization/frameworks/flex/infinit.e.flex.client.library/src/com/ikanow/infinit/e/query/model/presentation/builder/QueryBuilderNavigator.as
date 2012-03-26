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
package com.ikanow.infinit.e.query.model.presentation.builder
{
	import com.ikanow.infinit.e.shared.model.constant.NavigationConstants;
	import com.ikanow.infinit.e.shared.model.presentation.base.Navigator;
	
	/**
	 * Query Builder Navigator
	 */
	public class QueryBuilderNavigator extends Navigator
	{
		
		//======================================
		// public properties 
		//======================================
		
		/**
		 * Presentation Model
		 */
		[Bindable]
		[Inject]
		public var model:QueryBuilderModel;
		
		//======================================
		// constructor 
		//======================================
		
		/**
		 * Constructor
		 */
		public function QueryBuilderNavigator()
		{
			navigatorId = NavigationConstants.QUERY_BUILDER_ID;
			parentNavigatorId = NavigationConstants.WORKSPACES_QUERY_ID;
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Hide Query Term Editor
		 */
		public function hideQueryTermEditor():void
		{
			model.showQueryTermEditor = false;
		}
		
		/**
		 * Show Query Builder View
		 */
		public function showQueryBuilderView():void
		{
			navigateById( NavigationConstants.QUERY_BUILDER_ID );
			navigateById( NavigationConstants.WORKSPACES_QUERY_ID );
		}
		
		/**
		 * Show Query Term Editor
		 */
		public function showQueryTermEditor():void
		{
			model.showQueryTermEditor = true;
		}
		
		/**
		 * Show Query Term Editor Entity View
		 */
		public function showQueryTermEditorEntityView():void
		{
			navigateById( NavigationConstants.QUERY_TERM_EDITOR_ENTITY_ID );
		}
		
		/**
		 * Show Query Term Editor Event View
		 */
		public function showQueryTermEditorEventView():void
		{
			navigateById( NavigationConstants.QUERY_TERM_EDITOR_EVENT_ID );
		}
		
		/**
		 * Show Query Term Editor Geo Location View
		 */
		public function showQueryTermEditorGeoLocationView():void
		{
			navigateById( NavigationConstants.QUERY_TERM_EDITOR_GEO_LOCATION_ID );
		}
		
		/**
		 * Show Query Term Editor Temporal View
		 */
		public function showQueryTermEditorTemporalView():void
		{
			navigateById( NavigationConstants.QUERY_TERM_EDITOR_TEMPORAL_ID );
		}
	}
}
