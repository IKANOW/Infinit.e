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
package assets
{
	
	[Bindable]
	public class EmbeddedAssets
	{
		
		//======================================
		// public static properties 
		//======================================
		
		/*[Embed( source = "/assets/images/Add_Query_Term.png" )]
		public static const ADD_QUERY_TERM_BUTTON:Class;
		
		[Embed( source = "/assets/images/Add_Widget_Button.png" )]
		public static const ADD_WIDGET_BUTTON:Class;
		
		[Embed( source = "/assets/images/NAV-AdvQuery_Off.png" )]
		public static const ADVANCED_OFF:Class;
		
		[Embed( source = "/assets/images/NAV-AdvQuery_On.png" )]
		public static const ADVANCED_ON:Class;*/
		
		[Embed( source = "/assets/images/CheckMark.png" )]
		public static const CHECK_MARK:Class;
		
		[Embed( source = "/assets/images/Clear_Off.png" )]
		public static const CLEAR_OFF:Class;
		
		[Embed( source = "/assets/images/Clear_On.png" )]
		public static const CLEAR_ON:Class;
		
		[Embed( source = "/assets/images/Close_Off.png" )]
		public static const CLOSE_OFF:Class;
		
		[Embed( source = "/assets/images/Close_On.png" )]
		public static const CLOSE_ON:Class;
		
		[Embed( source = "/assets/images/Nav-Community_Hover.png" )]
		public static const COMMUNITY_HOVER:Class;
		
		[Embed( source = "/assets/images/Community_Add_Hover.png" )]
		public static const COMMUNITY_ADD:Class;
		
		[Embed( source = "/assets/images/Community_Add.png" )]
		public static const COMMUNITY_ADD_HOVER:Class;
		
		[Embed( source = "/assets/images/Nav-Community_Off.png" )]
		public static const COMMUNITY_OFF:Class;
		
		[Embed( source = "/assets/images/Nav-Community_DD.png" )]
		public static const COMMUNITY_ON:Class;
		
		[Embed( source = "/assets/images/NAV-Refresh_Off.png" )]
		public static const REFRESH_OFF:Class;
		
		[Embed( source = "/assets/images/NAV-Refresh_On.png" )]
		public static const REFRESH_ON:Class;
		
		[Embed( source = "/assets/images/NAV-Refresh_Hover.png" )]
		public static const REFRESH_HOVER:Class;
		
		/*[Embed( source = "/assets/images/DkGrey_Stripe.png" )]
		public static const DK_GREY_STRIPE:Class;
		
		[Embed( source = "/assets/images/Delete_Black.png" )]
		public static const DELETE_BLACK:Class;
		
		[Embed( source = "/assets/images/Favorite_Off.png" )]
		public static const FAVORITE_OFF:Class;
		
		[Embed( source = "/assets/images/Favorite_On.png" )]
		public static const FAVORITE_ON:Class;*/
		
		[Embed( source = "/assets/images/Filter.png" )]
		public static const FILTER:Class;
		
		/*
		[Embed( source = "/assets/images/Green_CrossHatch.png" )]
		public static const GREEN_CROSS_HATCH:Class;
		
		[Embed( source = "/assets/images/NAV-History_On.png" )]
		public static const HISTORY_OFF:Class;
		
		[Embed( source = "/assets/images/NAV-History_Off.png" )]
		public static const HISTORY_ON:Class;
		
		[Embed( source = "/assets/images/Info.png" )]
		public static const INFO:Class;
		
		[Embed( source = "/assets/images/CheckPassword-No.png" )]
		public static const INPUT_VALID:Class;
		
		[Embed( source = "/assets/images/CheckPassword-Yes.png" )]
		public static const INPUT_INVALID:Class;
		
		[Embed( source = "/assets/images/Email_Small.png" )]
		public static const EMAIL_SMALL:Class;
		
		[Embed( source = "/assets/images/entity/EntityTypeIcon_Entity_Company.png" )]
		public static const ENTITY_COMPANY:Class;
		
		[Embed( source = "/assets/images/entity/EntityTypeIcon_Entity_Generic.png" )]
		public static const ENTITY_GENERIC:Class;
		
		[Embed( source = "/assets/images/entity/EntityTypeIcon_Entity_Generic_ON.png" )]
		public static const ENTITY_GENERIC_ON:Class;
		
		[Embed( source = "/assets/images/entity/EntityTypeIcon_Entity_Person.png" )]
		public static const ENTITY_PERSON:Class;
		
		[Embed( source = "/assets/images/entity/EntityTypeIcon_Entity_Person_ON.png" )]
		public static const ENTITY_PERSON_ON:Class;
		
		[Embed( source = "/assets/images/entity/EntityTypeIcon_Event.png" )]
		public static const ENTITY_EVENT:Class;
		
		[Embed( source = "/assets/images/entity/EntityTypeIcon_Event_ON.png" )]
		public static const ENTITY_EVENT_ON:Class;
		
		[Embed( source = "/assets/images/entity/EntityTypeIcon_GeoLocation.png" )]
		public static const ENTITY_GEO_LOCATION:Class;
		
		[Embed( source = "/assets/images/entity/EntityTypeIcon_GeoLocation_ON.png" )]
		public static const ENTITY_GEO_LOCATION_ON:Class;
		
		[Embed( source = "/assets/images/entity/EntityTypeIcon_Temporal.png" )]
		public static const ENTITY_TEMPORAL:Class;
		
		[Embed( source = "/assets/images/entity/EntityTypeIcon_Temporal_ON.png" )]
		public static const ENTITY_TEMPORAL_ON:Class;
		
		[Embed( source = "/assets/images/entity/EntityTypeIcon_Text_Exact.png" )]
		public static const ENTITY_TEXT_EXACT:Class;
		
		[Embed( source = "/assets/images/entity/EntityTypeIcon_Text_Free.png" )]
		public static const ENTITY_TEXT_FREE:Class;
		
		[Embed( source = "/assets/images/IKANOW_Logo.png" )]
		public static const LOGO:Class;
		
		[Embed( source = "/assets/images/Max_on.png" )]
		public static const MAXIMIZE_ON:Class;
		
		[Embed( source = "/assets/images/Max_off.png" )]
		public static const MAXIMIZE_OFF:Class;
		
		[Embed( source = "/assets/images/Min_on.png" )]
		public static const MINIMIZE_ON:Class;
		
		[Embed( source = "/assets/images/Min_off.png" )]
		public static const MINIMIZE_OFF:Class;
		
		[Embed( source = "/assets/images/Move_Widget_Handle.png" )]
		public static const MOVE_WIDGET_HANDLE:Class;
		
		[Embed( source = "/assets/images/Workspace_Options.png" )]
		public static const OPTIONS_OFF:Class;
		
		[Embed( source = "/assets/images/Workspace_Hover.png" )]
		public static const OPTIONS_ON:Class;
		
		[Embed( source = "/assets/images/Phone_Small.png" )]
		public static const PHONE_SMALL:Class;
		
		[Embed( source = "/assets/images/Query_History_Search.png" )]
		public static const QUERY_HISTORY_SEARCH:Class;
		
		[Embed( source = "/assets/images/Query_History_Settings.png" )]
		public static const QUERY_HISTORY_SETTINGS:Class;
		
		[Embed( source = "/assets/images/QuerySummary_ClearButton.png" )]
		public static const QUERY_SUMMARY_CLEAR_BUTTON:Class;
		
		[Embed( source = "/assets/images/Remove_Widget_Button.png" )]
		public static const REMOVE_WIDGET_BUTTON:Class;
		
		[Embed( source = "/assets/images/Run_Query.png" )]
		public static const RUN_QUERY:Class;*/
		
		[Embed( source = "/assets/images/Search.png" )]
		public static const SEARCH:Class;
		
		/*
		[Embed( source = "/assets/images/Search_White_Over.png" )]
		public static const SEARCH_WHITE_OVER:Class;
		
		[Embed( source = "/assets/images/Search_White_Up.png" )]
		public static const SEARCH_WHITE_UP:Class;
		
		[Embed( source = "/assets/images/Search_White_Arrow_Over.png" )]
		public static const SEARCH_WHITE_ARROW_OVER:Class;
		
		[Embed( source = "/assets/images/Search_White_Arrow_Up.png" )]
		public static const SEARCH_WHITE_ARROW_UP:Class;
		
		[Embed( source = "/assets/images/Slider_Off.png" )]
		public static const SLIDER_OFF:Class;
		
		[Embed( source = "/assets/images/Slider_Over.png" )]
		public static const SLIDER_OVER:Class;
		
		[Embed( source = "/assets/images/Slider_Thumb.png" )]
		public static const SLIDER_THUMB_UP:Class;
		
		[Embed( source = "/assets/images/Slider_Thumb_Over.png" )]
		public static const SLIDER_THUMB_OVER:Class;
		
		[Embed( source = "/assets/images/NAV-Sources_Off.png" )]
		public static const SOURCES_OFF:Class;
		
		[Embed( source = "/assets/images/NAV-Sources_On.png" )]
		public static const SOURCES_ON:Class;
		
		[Embed( source = "/assets/images/Tan_CrossHatch.png" )]
		public static const TAN_CROSS_HATCH:Class;*/
		
		[Embed( source = "/assets/images/SelectAll_Icon_Off.png" )]
		public static const SELECT_ALL_OFF:Class;
		
		[Embed( source = "/assets/images/SelectAll_Icon_On.png" )]
		public static const SELECT_ALL_ON:Class;
	/*[Embed( source = "/assets/images/Sources_AddNew_On.png" )]
	public static const SOURCE_ADD_NEW_ON:Class;
	[Embed( source = "/assets/images/Sources_AddNew_Off.png" )]
	public static const SOURCE_ADD_NEW_OFF:Class;
	[Embed( source = "/assets/images/Widget_Options_Button.png" )]
	public static const WIDGET_OPTIONS_BUTTON:Class;*/
	}
}

