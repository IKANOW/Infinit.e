/**
 * <p>Infinit.e</p>
 *
 * <p>Copyright (c) 2011 IKANOW, llc.</p>
 * <p>http://www.ikanow.com</p> 
 *
 * <p>NOTICE:  IKANOW permits you to use this this file in accordance with the terms of the license agreement 
 * accompanying it.  For information about the licensing and copyright of this Plug-In please contact IKANOW, llc. 
 * at support&#64;ikanow.com.</p>
 *
 * <p>http://www.ikanow.com/terms-conditions/</p>
 * 
 */
package com.ikanow.infinit.e.widget.library.utility
{
	/**
	 * URL encoding helper classes for sending API calls
	 * without characters that will break RESTful calls.
	 */
	public class URLEncoder
	{		
		/**
		 * function to encode the url
		 * By default the escape function does not escape the + character
		 * so a regex was added after escaping to add that exception in.
		 * 
		 * The java api's that urldecode the string
		 * 
		 * @param toEncode The url to encode
		 * 
		 * @return The encoded url
		 */
		public static function encode(toEncode:String):String
		{
			return escape(toEncode).replace(/\+/g,"%2B").replace(/\//g,"%2F"); 
			//return toEncode.replace(/\//g,"%2F").replace(/\+/g,"%2B").replace(/:/g,"%3A").replace(/\?/g,"%3F").replace(/&/g,"%26");
		}
		
		/**
		 * Replaces multiple whitespace into a single space  For example
		 * [white  \t\n   house] becomes [white house] and
		 * [obama          white     house] becomes [obama white house]
		 * 
		 */
		public static function replaceMultipleWhitespace(toReplace:String):String
		{
			return toReplace.replace(/\s+/g," ");
		}
	}
}