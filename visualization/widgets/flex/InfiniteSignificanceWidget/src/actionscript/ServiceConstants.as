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
package actionscript
{
	import flash.external.ExternalInterface;
	
	/**
	 * Service Constants
	 */
	public class ServiceConstants
	{
		
		//======================================
		// public static properties 
		//======================================
		
		// service fault
		public static const SERVICE_FAULT:String = "Service Fault";
		
		// post
		public static const SERVICE_METHOD_POST:String = "POST";
		
		// accept
		public static const SERVICE_HEADER_ACCEPT:String = "Accept";
		
		// content type
		public static const SERVICE_CONTENT_TYPE:String = "application/json";
		
		// url delimiter
		public static const SERVICE_RIGHT_BRACE_DELIMITER:String = "%5D";
		
		// server
		public static const SERVER_URL_SUBSTITUTE:String = "$infinite/";
		
		public static const SERVER_URL:String = "http://infinite.ikanow.com/api/";
		
		public static const DOMAIN_URL:String = ExternalInterface.call( "getDomainUrl" );
		
		public static const BLANK_URL:String = "_blank";
		
		public static const SELF_URL:String = "_self";
		
		public static const HTTP:String = "http";
		
		public static const APP_HTML_LOCAL:String = "Index.html";
		
		public static const APP_HTML_SERVER:String = "index.html";
		
		public static const BIN_DEBUG:String = "bin-debug";
		
		public static const API_URL:String = "/api";
		
		public static const PDF_URL:String = "CreatePDFServlet";
		
		// login service
		public static const FORGOT_PASSWORD_URL:String = ExternalInterface.call( "getForgottenPasswordUrl" ) + "?username=";
		
		public static const DOMAIN_LOGOUT_URL:String = ExternalInterface.call( "getDomainLogoutUrl" );
		
		public static const GET_COOKIE_URL:String = SERVER_URL + "auth/keepalive/";
		
		public static const GET_COOKIE_ACTION:String = "Get Cookie";
		
		public static const LOGIN_URL:String = SERVER_URL + "auth/login/";
		
		public static const LOGIN_ACTION:String = "Login";
		
		public static const LOGOUT_URL:String = SERVER_URL + "auth/logout/";
		
		public static const LOGOUT_ACTION:String = "Logout";
		
		public static const KEEP_ALIVE_URL:String = SERVER_URL + "auth/keepalive/";
		
		public static const KEEP_ALIVE_ACTION:String = "Get Cookie";
		
		// user service
		public static const GET_USER_URL:String = SERVER_URL + "social/person/get/";
		
		public static const GET_USER_ACTION:String = "Person Info";
		
		// setup service
		public static const GET_SETUP_URL:String = SERVER_URL + "social/gui/uisetup/get/";
		
		public static const GET_WIDGET_OPTIONS_ACTION:String = "Share";
		
		public static const GET_WIDGET_OPTIONS_URL:String = SERVER_URL + "social/share/search/?type=widgetsave&ignoreAdmin=true";
		
		public static const GET_SETUP_ACTION:String = "UISetup";
		
		public static const UPDATE_SETUP_URL:String = SERVER_URL + "social/gui/uisetup/update/";
		
		public static const UPDATE_SETUP_ACTION:String = "Update UISetup";
		
		public static const GET_MODULES_ALL_URL:String = SERVER_URL + "social/gui/modules/get/";
		
		public static const GET_MODULES_ALL_ACTION:String = "Get Modules";
		
		public static const GET_MODULES_USER_URL:String = SERVER_URL + "social/gui/modules/user/get/";
		
		public static const GET_MODULES_USER_ACTION:String = "Get User Modules";
		
		public static const SET_MODULES_USER_URL:String = SERVER_URL + "social/gui/modules/user/set/";
		
		public static const SET_MODULES_USER_ACTION:String = "Save Modules";
		
		// community service
		public static const GET_COMMUNITIES_PUBLIC_URL:String = SERVER_URL + "social/community/getpublic/";
		
		public static const GET_COMMUNITIES_PUBLIC_ACTION:String = "Community Info";
		
		public static const LEAVE_COMMUNITY_URL:String = SERVER_URL + "social/community/member/leave/";
		
		public static const LEAVE_COMMUNITY_ACTION:String = "Leave Community";
		
		public static const JOIN_COMMUNITY_URL:String = SERVER_URL + "social/community/member/join/";
		
		public static const JOIN_COMMUNITY_ACTION:String = "Join Community";
		
		// source service
		public static const GET_SOURCES_GOOD_URL:String = SERVER_URL + "config/source/good/";
		
		public static const GET_SOURCES_GOOD_ACTION:String = "Good Sources";
		
		// query service
		public static const QUERY_URL:String = SERVER_URL + "knowledge/document/query/";
		
		public static const QUERY_ACTION:String = "Query";
		
		public static const GET_QUERY_SUGGESTIONS_URL:String = SERVER_URL + "knowledge/feature/entitySuggest/";
		
		public static const GET_QUERY_SUGGESTIONS_ACTION:String = "Suggestions";
		
		public static const GET_QUERY_ASSOC_SUGGESTIONS_URL:String = SERVER_URL + "knowledge/feature/eventSuggest/";
		
		public static const GET_QUERY_ASSOC_SUGGESTIONS_ACTION:String = "Association Suggestions";
		
		public static const GET_SOCIAL_SHARE_ADD_JSON_URL:String = SERVER_URL + "social/share/update/json/";
		
		public static const GET_SOCIAL_SHARE_URL:String = SERVER_URL + "social/share/get/";
	}
}

