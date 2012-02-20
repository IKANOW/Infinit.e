package com.ikanow.infinit.e.shared.util
{
	import com.adobe.utils.StringUtil;
	import com.ikanow.infinit.e.shared.model.constant.Constants;
	import com.ikanow.infinit.e.shared.model.constant.ServiceConstants;
	
	public class ServiceUtil
	{
		
		//======================================
		// constructor 
		//======================================
		
		/**
		 *
		 * Constructor
		 *
		 */
		public function ServiceUtil()
		{
		
		}
		
		
		//======================================
		// public static methods 
		//======================================
		
		/**
		 * Returns a string or a "null" string
		 * @param value
		 * @return String
		 */
		public static function getStringOrNullString( value:String ):String
		{
			return value == Constants.BLANK ? Constants.NULL_STRING : value;
		}
		
		/**
		 * Returns the specified domain or the GUI's own URL if blank
		 * @param value
		 * @return String
		 */
		public static function getLogoutDomain( value:String ):String
		{
			return value == Constants.BLANK ? BrowserUtil.getBrowserURL() : value;
		}
		
		/**
		 * Returns a url encoded string
		 * @param value
		 * @return String
		 */
		public static function getURLEncodedString( value:String ):String
		{
			return urlEncode( replaceMultipleWhitespace( StringUtil.trim( value ) ).toLowerCase() );
		}
		
		/**
		 * Replaces multiple whitespace into a single space  For example
		 * [white  \t\n   house] becomes [white house] and
		 * [obama          white     house] becomes [obama white house]
		 *
		 */
		public static function replaceMultipleWhitespace( toReplace:String ):String
		{
			return toReplace.replace( /\s+/g, " " );
		}
		
		/**
		 * function to encode the url
		 * By default the escape function does not escape the + character
		 * so a regex was added after escaping to add that exception in.
		 *
		 * @param toEncode The url to encode
		 *
		 * @return The encoded url
		 */
		public static function urlEncode( toEncode:String ):String
		{
			return escape( toEncode ).replace( /\+/g, "%2B" ).replace( /\//g, "%2F" );
		}
	}
}
