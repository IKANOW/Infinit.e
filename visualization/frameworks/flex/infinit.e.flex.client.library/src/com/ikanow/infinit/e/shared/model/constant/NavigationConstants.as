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
package com.ikanow.infinit.e.shared.model.constant
{
	
	/**
	 * Navigation Constants
	 */
	public class NavigationConstants
	{
		
		//======================================
		// public static properties 
		//======================================
		
		public static const APPLICATION_ID:String = "Infinite";
		
		// application
		
		public static const MAIN_ID:String = APPLICATION_ID + "Main";
		
		// main	
		
		public static const MAIN_LOGIN_ID:String = MAIN_ID + "Login";
		
		public static const MAIN_DASHBOARD_ID:String = MAIN_ID + "Dashboard";
		
		// login
		
		public static const LOGIN_COOKIE_ID:String = MAIN_LOGIN_ID + "Cookie";
		
		public static const LOGIN_FORM_ID:String = MAIN_LOGIN_ID + "Form";
		
		public static const LOGIN_LOADING_DATA_ID:String = MAIN_LOGIN_ID + "LoadingData";
		
		public static const LOGIN_FORGOT_PASSWORD_ID:String = MAIN_LOGIN_ID + "ForgotPassword";
		
		// dashboard
		
		public static const DASHBOARD_HEADER_ID:String = MAIN_DASHBOARD_ID + "Header";
		
		public static const WORKSPACES_ID:String = MAIN_DASHBOARD_ID + "Workspaces";
		
		
		// dashboard header
		
		public static const DASHBOARD_HEADER_QUERY_ID:String = DASHBOARD_HEADER_ID + "Query";
		
		public static const DASHBOARD_HEADER_SOURCES_ID:String = DASHBOARD_HEADER_ID + "Sources";
		
		public static const DASHBOARD_HEADER_HISTORY_ID:String = DASHBOARD_HEADER_ID + "History";
		
		public static const DASHBOARD_HEADER_PROFILE_ID:String = DASHBOARD_HEADER_ID + "Profile";
		
		public static const DASHBOARD_HEADER_MANAGER_ID:String = DASHBOARD_HEADER_ID + "Manager";
		
		public static const DASHBOARD_HEADER_LOGOUT_ID:String = DASHBOARD_HEADER_ID + "Logout";
		
		// workspaces
		
		public static const WORKSPACES_HEADER_ID:String = WORKSPACES_ID + "Header";
		
		public static const WORKSPACES_BODY_ID:String = WORKSPACES_ID + "Body";
		
		public static const WORKSPACES_WELCOME_ID:String = WORKSPACES_ID + "Welcome";
		
		public static const WORKSPACES_QUERY_ID:String = WORKSPACES_ID + "Query";
		
		public static const WORKSPACES_SOURCES_ID:String = WORKSPACES_ID + "Sources";
		
		public static const WORKSPACES_HISTORY_ID:String = WORKSPACES_ID + "History";
		
		// workspaces header
		
		public static const WORKSPACES_HEADER_WORKSPACE_SETTIINGS:String = WORKSPACES_HEADER_ID + "WorkspaceSettings";
		
		public static const WORKSPACES_HEADER_EXPORT_PDF_ID:String = WORKSPACES_HEADER_ID + "ExportPDF";
		
		public static const WORKSPACES_HEADER_EXPORT_JSON_ID:String = WORKSPACES_HEADER_ID + "ExportJSON";
		
		public static const WORKSPACES_HEADER_EXPORT_RSS_ID:String = WORKSPACES_HEADER_ID + "ExportRSS";
		
		// workspaces body
		
		public static const WORKSPACES_BODY_DRAWER_OPEN_ID:String = WORKSPACES_BODY_ID + "Open";
		
		public static const WORKSPACES_BODY_DRAWER_CLOSED_ID:String = WORKSPACES_BODY_ID + "Closed";
		
		public static const WORKSPACES_BODY_WORKSPACE_ID:String = WORKSPACES_BODY_ID + "Workspace";
		
		public static const WORKSPACES_BODY_WIDGETS_ID:String = WORKSPACES_BODY_ID + "Widgets";
		
		// workspace
		
		public static const WORKSPACE_CONTENT_ID:String = WORKSPACES_BODY_WORKSPACE_ID + "Content";
		
		public static const WORKSPACE_LAYOUT_ID:String = WORKSPACES_BODY_WORKSPACE_ID + "Layout";
		
		public static const WORKSPACE_SETTINGS_ID:String = WORKSPACES_BODY_WORKSPACE_ID + "Settings";
		
		// workspace content
		
		public static const WORKSPACE_CONTENT_TILES_ID:String = WORKSPACE_CONTENT_ID + "Tiles";
		
		public static const WORKSPACE_CONTENT_MAXIMIZED_ID:String = WORKSPACE_CONTENT_ID + "Maximized";
		
		// widget
		
		public static const WIDGET_LIST_ID:String = WORKSPACES_BODY_WIDGETS_ID + "List";
		
		public static const WIDGET_EDITOR_ID:String = WORKSPACES_BODY_WIDGETS_ID + "Editor";
		
		// query
		
		public static const QUERY_BUILDER_ID:String = WORKSPACES_QUERY_ID + "Builder";
		
		public static const QUERY_SETTINGS_ID:String = WORKSPACES_QUERY_ID + "Settings";
		
		// query term editor
		
		public static const QUERY_TERM_EDITOR_ID:String = QUERY_BUILDER_ID + "QueryTermEditor";
		
		// query term editor forms
		
		public static const QUERY_TERM_EDITOR_ENTITY_ID:String = QUERY_TERM_EDITOR_ID + "Entity";
		
		public static const QUERY_TERM_EDITOR_EVENT_ID:String = QUERY_TERM_EDITOR_ID + "Event";
		
		public static const QUERY_TERM_EDITOR_GEO_LOCATION_ID:String = QUERY_TERM_EDITOR_ID + "GeoLocation";
		
		public static const QUERY_TERM_EDITOR_TEMPORAL_ID:String = QUERY_TERM_EDITOR_ID + "Temporal";
		
		// sources
		
		public static const SOURCES_COMMUNITY_ID:String = WORKSPACES_SOURCES_ID + "Community";
		
		public static const SOURCES_AVAILABLE_ID:String = WORKSPACES_SOURCES_ID + "Available";
		
		public static const SOURCES_SELECTED_ID:String = WORKSPACES_SOURCES_ID + "Selected";
		
		// community
		
		public static const COMMUNITY_LIST_ID:String = SOURCES_COMMUNITY_ID + "List";
		
		public static const COMMUNITY_REQUEST_ID:String = SOURCES_COMMUNITY_ID + "Request";
		
		// history
		
		public static const HISTORY_LIST_ID:String = WORKSPACES_HISTORY_ID + "List";
		
		// community request
		
		public static const COMMUNITY_REQUEST_JOIN_PROMPT_ID:String = COMMUNITY_REQUEST_ID + "JoinPrompt";
		
		public static const COMMUNITY_REQUEST_REQUESTING_JOIN_ID:String = COMMUNITY_REQUEST_ID + "RequestingJoin";
		
		public static const COMMUNITY_REQUEST_JOIN_APPROVED_ID:String = COMMUNITY_REQUEST_ID + "JoinApproved";
		
		public static const COMMUNITY_REQUEST_JOIN_PENDING_ID:String = COMMUNITY_REQUEST_ID + "JoinPending";
	}
}

