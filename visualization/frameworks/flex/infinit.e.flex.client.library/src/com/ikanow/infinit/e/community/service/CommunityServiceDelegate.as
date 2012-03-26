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
package com.ikanow.infinit.e.community.service
{
	import com.ikanow.infinit.e.shared.event.CommunityEvent;
	import com.ikanow.infinit.e.shared.model.constant.ServiceConstants;
	import com.ikanow.infinit.e.shared.model.vo.Community;
	import com.ikanow.infinit.e.shared.model.vo.CommunityApproval;
	import com.ikanow.infinit.e.shared.model.vo.Source;
	import com.ikanow.infinit.e.shared.model.vo.ui.ServiceResult;
	import com.ikanow.infinit.e.shared.service.base.InfiniteDelegate;
	import com.ikanow.infinit.e.shared.util.ObjectTranslatorUtil;
	import mx.collections.ArrayCollection;
	import mx.rpc.AsyncToken;
	import mx.rpc.http.HTTPService;
	
	public class CommunityServiceDelegate extends InfiniteDelegate implements ICommunityServiceDelegate
	{
		
		//======================================
		// public properties 
		//======================================
		
		[Inject( "communityService" )]
		public var service:HTTPService;
		
		//======================================
		// constructor 
		//======================================
		
		public function CommunityServiceDelegate()
		{
		}
		
		
		//======================================
		// public methods 
		//======================================
		
		/**
		 * Get Communities Public
		 * Retrieves the public communities
		 * @param event
		 * @return AsyncToken
		 */
		public function getCommunitiesPublic( event:CommunityEvent ):AsyncToken
		{
			var url:String = ServiceConstants.GET_COMMUNITIES_PUBLIC_URL;
			var params:Object = { action: ServiceConstants.GET_COMMUNITIES_PUBLIC_ACTION, dialogControl: event.dialogControl };
			var token:AsyncToken = makeCall( service, url, params, default_resultHandler, default_faultHandler );
			
			return token;
		}
		
		/**
		 * Join Community
		 * @param event
		 * @return AsyncToken
		 */
		public function joinCommunity( event:CommunityEvent ):AsyncToken
		{
			var url:String = ServiceConstants.JOIN_COMMUNITY_URL + event.communityID;
			var params:Object = { action: ServiceConstants.JOIN_COMMUNITY_ACTION, dialogControl: event.dialogControl, communityID: event.communityID };
			var token:AsyncToken = makeCall( service, url, params, default_resultHandler, default_faultHandler );
			
			return token;
		}
		
		/**
		 * Leave Community
		 * @param event
		 * @return AsyncToken
		 */
		public function leaveCommunity( event:CommunityEvent ):AsyncToken
		{
			var url:String = ServiceConstants.LEAVE_COMMUNITY_URL + event.communityID;
			var params:Object = { action: ServiceConstants.LEAVE_COMMUNITY_ACTION, dialogControl: event.dialogControl, communityID: event.communityID };
			var token:AsyncToken = makeCall( service, url, params, default_resultHandler, default_faultHandler );
			
			return token;
		}
		
		//======================================
		// protected methods 
		//======================================
		
		/**
		 * Called from translateResult method in super() if the result has data
		 * @param serviceResult - the raw object returned from the service
		 * @param result - the generated service result object
		 * @return Object
		 */
		override protected function translateServiceResultData( serviceResult:Object, result:ServiceResult ):void
		{
			var action:String = getResponseAction( serviceResult );
			
			switch ( action )
			{
				case ServiceConstants.GET_COMMUNITIES_PUBLIC_ACTION:
					result.data = new ArrayCollection( ObjectTranslatorUtil.translateArrayObjects( serviceResult.data, Community ) );
					break;
				case ServiceConstants.JOIN_COMMUNITY_ACTION:
					result.data = ObjectTranslatorUtil.translateObject( serviceResult.data, new CommunityApproval );
					break;
			}
		}
	}
}
