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
package com.ikanow.infinit.e.shared.util
{
	import com.adobe.utils.StringUtil;
	import com.ikanow.infinit.e.shared.model.constant.Constants;
	import com.ikanow.infinit.e.shared.model.constant.ServiceConstants;
	import mx.core.FlexGlobals;
	
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
		 * Returns the specified domain or the GUI's own URL if blank
		 * @param value
		 * @return String
		 */
		public static function getLogoutDomain( value:String ):String
		{
			return value == Constants.BLANK ? BrowserUtil.getBrowserURL() : value;
		}
		
		/**
		 * Returns the URL of the manager
		 * @param value
		 * @return String
		 */
		public static function getManagerUrl():String
		{
			return BrowserUtil.getBrowserDomain() + Constants.MANAGER_PAGE;
		}
		
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
