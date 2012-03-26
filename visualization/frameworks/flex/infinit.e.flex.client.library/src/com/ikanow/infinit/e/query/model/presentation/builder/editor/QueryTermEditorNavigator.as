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
package com.ikanow.infinit.e.query.model.presentation.builder.editor
{
	import com.ikanow.infinit.e.shared.model.constant.NavigationConstants;
	import com.ikanow.infinit.e.shared.model.constant.types.NavigationItemTypes;
	import com.ikanow.infinit.e.shared.model.presentation.base.Navigator;
	import com.ikanow.infinit.e.shared.model.vo.ui.NavigationItem;
	import com.ikanow.infinit.e.shared.util.StateUtil;
	import mx.collections.ArrayCollection;
	import mx.core.UIComponent;
	import mx.resources.ResourceManager;
	import assets.EmbeddedAssets;
	
	/**
	 * Query Term Editor Navigator
	 */
	public class QueryTermEditorNavigator extends Navigator
	{
		
		//======================================
		// public static properties 
		//======================================
		
		public static const ENTITY_ID:String = NavigationConstants.QUERY_TERM_EDITOR_ENTITY_ID;
		
		public static const EVENT_ID:String = NavigationConstants.QUERY_TERM_EDITOR_EVENT_ID;
		
		public static const GEO_LOCATION_ID:String = NavigationConstants.QUERY_TERM_EDITOR_GEO_LOCATION_ID;
		
		public static const TEMPORAL_ID:String = NavigationConstants.QUERY_TERM_EDITOR_TEMPORAL_ID;
		
		
		//======================================
		// public properties 
		//======================================
		
		/**
		 * Presentation Model
		 */
		[Bindable]
		[Inject]
		public var model:QueryTermEditorModel;
		
		//======================================
		// constructor 
		//======================================
		
		/**
		 * Constructor
		 */
		public function QueryTermEditorNavigator()
		{
			navigatorId = NavigationConstants.QUERY_TERM_EDITOR_ID;
			parentNavigatorId = NavigationConstants.QUERY_BUILDER_ID;
		}
		
		
		//======================================
		// public static methods 
		//======================================
		
		/**
		 * Update View States
		 * Used to override the states of a view with the
		 * full state navigation item ids from the
		 * associated Navigator.
		 * @param component - the component to update states
		 * @param state - the current state to set after the update
		 */
		public static function updateViewStates( component:UIComponent, state:String = "" ):void
		{
			StateUtil.setStates( component, [ ENTITY_ID, EVENT_ID, GEO_LOCATION_ID, TEMPORAL_ID ], state );
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		override public function navigate( navigationItem:* ):void
		{
			if ( model )
				model.clearSuggestionsList();
			
			super.navigate( navigationItem );
		}
		
		/**
		 * Show Entity View
		 */
		public function showEntityView():void
		{
			navigateById( ENTITY_ID );
		}
		
		//======================================
		// protected methods 
		//======================================
		
		/**
		 * Create States
		 */
		override protected function createStates():void
		{
			var navStates:ArrayCollection = new ArrayCollection();
			var navigationItem:NavigationItem;
			
			// entity
			navigationItem = new NavigationItem();
			navigationItem.navigatorId = navigatorId;
			navigationItem.id = ENTITY_ID;
			navigationItem.type = NavigationItemTypes.STATE;
			navigationItem.state = ENTITY_ID;
			navigationItem.toolTip = ResourceManager.getInstance().getString( 'infinite', 'queryTermEditor.searchTerm' );
			navigationItem.icon = EmbeddedAssets.ENTITY_GENERIC;
			navigationItem.altIcon = EmbeddedAssets.ENTITY_GENERIC_ON;
			navStates.addItem( navigationItem );
			
			// event
			navigationItem = new NavigationItem();
			navigationItem.navigatorId = navigatorId;
			navigationItem.id = EVENT_ID;
			navigationItem.type = NavigationItemTypes.STATE;
			navigationItem.state = EVENT_ID;
			navigationItem.toolTip = ResourceManager.getInstance().getString( 'infinite', 'queryTermEditor.event' );
			navigationItem.icon = EmbeddedAssets.ENTITY_EVENT;
			navigationItem.altIcon = EmbeddedAssets.ENTITY_EVENT_ON;
			navStates.addItem( navigationItem );
			
			// geolocation
			navigationItem = new NavigationItem();
			navigationItem.navigatorId = navigatorId;
			navigationItem.id = GEO_LOCATION_ID;
			navigationItem.type = NavigationItemTypes.STATE;
			navigationItem.state = GEO_LOCATION_ID;
			navigationItem.toolTip = ResourceManager.getInstance().getString( 'infinite', 'queryTermEditor.geolocation' );
			navigationItem.icon = EmbeddedAssets.ENTITY_GEO_LOCATION;
			navigationItem.altIcon = EmbeddedAssets.ENTITY_GEO_LOCATION_ON;
			navStates.addItem( navigationItem );
			
			// temporal
			navigationItem = new NavigationItem();
			navigationItem.navigatorId = navigatorId;
			navigationItem.id = TEMPORAL_ID;
			navigationItem.type = NavigationItemTypes.STATE;
			navigationItem.state = TEMPORAL_ID;
			navigationItem.toolTip = ResourceManager.getInstance().getString( 'infinite', 'queryTermEditor.temporal' );
			navigationItem.icon = EmbeddedAssets.ENTITY_TEMPORAL;
			navigationItem.altIcon = EmbeddedAssets.ENTITY_TEMPORAL_ON;
			navStates.addItem( navigationItem );
			
			// set states - default entity
			setStates( navStates, ENTITY_ID );
		}
	}
}

