package com.ikanow.infinit.e.shared.model.constant
{
	import flash.external.ExternalInterface;
	
	/**
	 * Application Constants
	 */
	public class Constants
	{
		
		//======================================
		// public static properties 
		//======================================
		
		// session timeout
		public static const ENCRIPTION_ALGORITHM:String = "sha256";
		
		// session timeout
		public static const SESSION_TIMEOUT:int = ExternalInterface.call( "getTimeoutSeconds" ) * 1000;
		
		// session keep alive timer
		public static const SESSION_KEEP_ALIVE_TIMER:uint = 60000;
		
		// session update timer - check for new documents
		public static const SESSION_UPDATE_TIMER:uint = 60000;
		
		// transition constants
		public static const TRANSITION_DURATION:int = 500;
		
		public static const TRANSITION_EASER_EXPONENT:Number = 4;
		
		// modal constants
		public static const MODAL_ALPHA:Number = 1.0;
		
		// drawer constants
		public static const DRAWER_TRANSITION_DURATION:Number = 500;
		
		public static const DRAWER_BUTTON_WIDTH:Number = 32;
		
		public static const DRAWER_WIDTH:Number = 270;
		
		// widget constants
		public static const WIDGET_HEADER_HEIGHT:int = 29;
		
		// widget constants
		public static const WIDGET_DONE_LOADING:String = "Done Loading";
		
		// string array delimiter
		public static const STRING_ARRAY_DELIMITER:String = ",";
		
		// null string
		public static const NULL_STRING:String = "null";
		
		// question mark
		public static const QUESTION_MARK:String = "?";
		
		// parenthesis left
		public static const PARENTHESIS_LEFT:String = "(";
		
		// parenthesis right
		public static const PARENTHESIS_RIGHT:String = ")";
		
		// wildcard
		public static const WILDCARD:String = "*";
		
		// wildcard forward slash
		public static const WILDCARD_FORWARD_SLASH:String = "*/";
		
		// ampersand
		public static const AMPERSAND:String = "&";
		
		// forward slash
		public static const FORWARD_SLASH:String = "/";
		
		// line break
		public static const LINE_BREAK:String = "\n";
		
		// double quote
		public static const DOUBLE_QUOTE:String = "''";
		
		// period
		public static const PERIOD:String = ".";
		
		// blank
		public static const BLANK:String = "";
		
		// space
		public static const SPACE:String = " ";
		
		// comma
		public static const COMMA:String = ",";
		
		// colon
		public static const COLON:String = ":";
		
		// plus
		public static const PLUS:String = "+";
		
		// kilometers
		public static const KILOMETERS_ABBREV:String = "km";
		
		// default id property
		public static const DEFAULT_ID_PROPERTY:String = "_id";
		
		// default sort order property
		public static const DEFAULT_SORT_ORDER_PROPERTY:String = "sortOrder";
		
		// default date time format
		public static const DEFAULT_DATE_TIME_FORMAT:String = "MM/DD/YYYY";
		
		// default time format
		public static const DEFAULT_TIME_FORMAT:String = "hh:mm a";
		
		// sort order property
		public static const SORT_ORDER_PROPERTY:String = "sortOrder";
		
		// sort order property
		public static const POSITION_INDEX_PROPERTY:String = "positionIndex";
		
		// time sort order property
		public static const TIME_SORT_ORDER_PROPERTY:String = "time";
		
		// name property
		public static const NAME_PROPERTY:String = "name";
	}
}

