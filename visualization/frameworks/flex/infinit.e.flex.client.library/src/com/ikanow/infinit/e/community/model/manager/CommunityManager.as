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
package com.ikanow.infinit.e.community.model.manager
{
	import com.ikanow.infinit.e.shared.control.SetupController;
	import com.ikanow.infinit.e.shared.event.SetupEvent;
	import com.ikanow.infinit.e.shared.event.SourceEvent;
	import com.ikanow.infinit.e.shared.model.constant.Constants;
	import com.ikanow.infinit.e.shared.model.manager.base.InfiniteManager;
	import com.ikanow.infinit.e.shared.model.vo.Community;
	import com.ikanow.infinit.e.shared.model.vo.CommunityApproval;
	import com.ikanow.infinit.e.shared.model.vo.CommunityAttribute;
	import com.ikanow.infinit.e.shared.model.vo.QueryStringRequest;
	import com.ikanow.infinit.e.shared.model.vo.Source;
	import com.ikanow.infinit.e.shared.model.vo.User;
	import com.ikanow.infinit.e.shared.model.vo.ui.DialogControl;
	import com.ikanow.infinit.e.shared.util.CollectionUtil;
	import flash.events.Event;
	import mx.collections.ArrayCollection;
	import mx.collections.SortField;
	import mx.resources.ResourceManager;
	
	/**
	 * Community Manager
	 */
	public class CommunityManager extends InfiniteManager
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Inject]
		public var setupController:SetupController;
		
		[Bindable]
		/**
		 * The collection of communities
		 */
		public var communities:ArrayCollection;
		
		[Bindable]
		/**
		 * The Current User's Communities Collection
		 */
		public var userCommunities:ArrayCollection;
		
		[Bindable]
		/**
		 * The selected communities for the query
		 */
		public var selectedCommunity:Community;
		
		[Bindable]
		/**
		 * The selected/highlighted source in the
		 * sources list
		 */
		public var selectedSource:Source;
		
		/**
		 * The selected communities for the query
		 */
		private var _selectedCommunities:ArrayCollection;
		
		
		[Bindable( event = "selectedCommunitiesChange" )]
		public function get selectedCommunities():ArrayCollection
		{
			return _selectedCommunities;
		}
		
		public function set selectedCommunities( value:ArrayCollection ):void
		{
			if ( _selectedCommunities != value )
			{
				_selectedCommunities = value;
				dispatchEvent( new Event( "selectedCommunitiesChange" ) );
			}
		}
		
		[Bindable]
		/**
		 * Community approval for join
		 */
		public var communityApproval:CommunityApproval;
		
		public var refreshing:Boolean = false;
		
		//======================================
		// protected properties 
		//======================================
		
		/**
		 * The current query string that is being modified for a new query
		 */
		protected var currentQueryStringRequest:QueryStringRequest;
		
		/**
		 * The current user
		 */
		protected var currentUser:User;
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Get Sources Good
		 */
		public function getSourcesGood():void
		{
			var communityIDs:String = Constants.BLANK;
			
			for each ( var community:Community in communities )
			{
				if ( community.isUserMember )
				{
					if ( communityIDs != Constants.BLANK )
						communityIDs += Constants.COMMA;
					
					communityIDs += community._id;
				}
			}
			
			var sourceEvent:SourceEvent = new SourceEvent( SourceEvent.GET_SOURCES_GOOD );
			sourceEvent.communityIDs = communityIDs;
			sourceEvent.dialogControl = DialogControl.create( false, ResourceManager.getInstance().getString( 'infinite', 'sourceService.getSourcesGood' ) );
			dispatcher.dispatchEvent( sourceEvent );
		}
		
		/**
		 * Join Community result handler
		 * @param value
		 */
		public function joinCommunity_resultHandler( value:CommunityApproval ):void
		{
			communityApproval = value;
			
			if ( value.approved )
			{
				selectedCommunity.isUserMember = true;
				selectedCommunity.sortOrder = 0;
				currentUser.communities.addItem( selectedCommunity );
				communities.refresh();
				processCommunities();
				getSourcesGood();
				//added a community, send out for new widgetOptions
				var setupEvent:SetupEvent = new SetupEvent( SetupEvent.GET_WIDGET_OPTIONS );
				setupEvent.dialogControl = DialogControl.create( false, ResourceManager.getInstance().getString( 'infinite', 'setupService.getWidgetOptions' ) );
				dispatcher.dispatchEvent( setupEvent );
				setupController.getWidgetOptions( setupEvent );
			}
		}
		
		/**
		 * Leave Community response handler
		 * @param value
		 */
		public function leaveCommunity_resultHandler():void
		{
			selectedCommunity.isUserMember = false;
			selectedCommunity.selected = false;
			selectedCommunity.sortOrder = 1;
			communities.refresh();
		
			// TODO: remove the community from currentUser.communities,
			// and make sure that the current user in QueryManager is updated, so that 
			// invalid community ids aren't used in a history query
		
			// TODO: update the sources
		}
		
		/**
		 * Reset
		 * Used to reset on logout
		 */
		public function reset():void
		{
			communities = null;
			userCommunities = null;
			selectedCommunities = null;
			selectedCommunity = null;
		}
		
		/**
		 * Select all communities
		 * @param value
		 */
		public function selectAllCommunities():void
		{
			for each ( var community:Community in communities )
			{
				if ( community.isUserMember )
				{
					community.selected = true;
				}
			}
			
			updateSelectedCommunities();
		}
		
		/**
		 * Select no communities
		 * @param value
		 */
		public function selectNoCommunities():void
		{
			for each ( var community:Community in communities )
			{
				if ( community.isUserMember )
				{
					community.selected = false;
				}
			}
			
			updateSelectedCommunities();
		}
		
		/**
		 * Public Communities Collection
		 * @param value
		 */
		public function setCommunities( value:ArrayCollection ):void
		{
			communities = value;
			
			// Process the communities
			processCommunities();
			
			// get the sources
			if ( value && currentUser )
				getSourcesGood();
		}
		
		[Inject( "queryManager.currentQueryStringRequest", bind = "true" )]
		/**
		 * set the current query string
		 * @param value
		 */
		public function setCurrentQueryString( value:QueryStringRequest ):void
		{
			currentQueryStringRequest = value;
			
			// initialize the selected Communities from the query string
			initSelectedCommunitiesFromQueryString();
			
			// Process the communities
			processCommunities();
		}
		
		/**
		 * Person Get response from server
		 * @param value
		 */
		[Inject( "userManager.currentUser", bind = "true" )]
		public function setCurrentUser( value:User ):void
		{
			currentUser = value;
			
			// initialize the selected Communities from the current user
			if ( !selectedCommunities )
				initSelectedCommunitiesFromCurrentUser();
			
			// Process the communities
			processCommunities();
			
			// get the sources
			if ( value && communities && !refreshing )
			{
				getSourcesGood();
			}
			refreshing = false;
		}
		
		/**
		 * The selected community
		 * @param value
		 */
		public function setSelectedCommunity( value:Community ):void
		{
			selectedCommunity = value;
			
			if ( value.isUserMember )
			{
				value.selected = !value.selected;
				updateSelectedCommunities();
			}
		}
		
		/**
		 * The selected source
		 * @param value
		 */
		public function setSelectedSource( value:Source ):void
		{
			selectedSource = value;
		}
		
		//======================================
		// protected methods 
		//======================================
		
		/**
		 * Creates the initial collection of selected communities from the current user
		 */
		protected function initSelectedCommunitiesFromCurrentUser():void
		{
			if ( currentUser && currentUser.communities )
			{
				var selectedCommunitiesNew:ArrayCollection = new ArrayCollection();
				
				for each ( var community:Community in currentUser.communities )
				{
					var newCommunity:Community = new Community();
					newCommunity._id = community._id;
					selectedCommunitiesNew.addItem( newCommunity );
				}
				
				selectedCommunities = selectedCommunitiesNew;
			}
		}
		
		/**
		 * Creates the initial collection of selected communities from the currentQueryStringRequest
		 */
		protected function initSelectedCommunitiesFromQueryString():void
		{
			if ( currentQueryStringRequest )
			{
				var selectedCommunitiesNew:ArrayCollection = new ArrayCollection();
				
				for each ( var communityId:String in currentQueryStringRequest.communityIds )
				{
					var newCommunity:Community = new Community();
					newCommunity._id = communityId;
					
					// make sure that the user is still a member of the community
					if ( isUserCommunity( newCommunity ) )
						selectedCommunitiesNew.addItem( newCommunity );
				}
				
				selectedCommunities = selectedCommunitiesNew;
			}
		}
		
		/**
		 * Determines if a community was used in the last query
		 * @param community
		 * @return Boolean
		 */
		protected function isCommunitySelected( community:Community ):Boolean
		{
			// if the community is found in the selectedCommunities, return true
			if ( selectedCommunities )
			{
				for each ( var selectedCommunity:Community in selectedCommunities )
				{
					if ( selectedCommunity._id == community._id )
					{
						selectedCommunity.name = community.name;
						return true;
					}
				}
			}
			
			return false;
		}
		
		/**
		 * Determines if a user community is a public community
		 * @param community
		 * @return Boolean
		 */
		protected function isPublicCommunity( userCommunity:Community ):Boolean
		{
			if ( userCommunity.communityAttributes != null )
			{
				for each ( var commAttr:CommunityAttribute in userCommunity.communityAttributes )
				{
					if ( commAttr.isPublic.value == "true" )
					{
						return true;
					}
				}
			}
			
			return false;
		}
		
		/**
		 * Determines if the current user is a member of a community
		 * @param community
		 * @return Boolean
		 */
		protected function isUserCommunity( community:Community ):Boolean
		{
			// if the community is found in the user's communities, return true
			if ( currentUser && currentUser.communities )
			{
				if ( community._id == currentUser._id )
					return true;
				else if ( community.isPersonalCommunity )
					return false;
				
				for each ( var userCommunity:Community in currentUser.communities )
				{
					if ( userCommunity._id == community._id )
						return true;
				}
			}
			
			return false;
		}
		
		/**
		 * Updates the sort order of the communities and sets the userCommunities collection
		 */
		protected function processCommunities():void
		{
			var userCommunitiesNew:ArrayCollection = new ArrayCollection();
			var filter:Function = null;
			
			if ( communities && currentUser && selectedCommunities )
			{
				//store and remove a filter if there was one
				filter = communities.filterFunction;
				communities.filterFunction = null;
				communities.refresh();
				
				// create the userCommunities collection and set properties
				var tempCommList:ArrayCollection = new ArrayCollection();
				
				for each ( var community:Community in communities )
				{
					if ( isUserCommunity( community ) )
					{
						community.isUserMember = true;
						community.selected = isCommunitySelected( community );
						community.sortOrder = 0;
						userCommunitiesNew.addItem( community );
						tempCommList.addItem( community );
					}
					else if ( isPublicCommunity( community ) )
					{
						community.isUserMember = false;
						community.sortOrder = 1;
						tempCommList.addItem( community );
					}
				}
				communities.source = tempCommList.source;
				// sort the communities
				communities.filterFunction = filter; //put the filter back on
				CollectionUtil.applySort( communities, [ new SortField( Constants.SORT_ORDER_PROPERTY, false, false, true ), new SortField( Constants.NAME_PROPERTY, true ) ] );
				communities.refresh();
				
				// set and sort the user communities
				userCommunities = userCommunitiesNew;
				CollectionUtil.applySort( userCommunities, [ new SortField( Constants.NAME_PROPERTY, true ) ] );
				userCommunities.refresh();
			}
		}
		
		/**
		 * Creates the collection of selected communities from the communities that are selected
		 */
		protected function updateSelectedCommunities():void
		{
			if ( communities )
			{
				var selectedCommunitiesNew:ArrayCollection = new ArrayCollection();
				
				for each ( var community:Community in communities )
				{
					if ( community.selected )
					{
						selectedCommunitiesNew.addItem( community );
					}
				}
				
				selectedCommunities = selectedCommunitiesNew;
			}
		}
	}
}
