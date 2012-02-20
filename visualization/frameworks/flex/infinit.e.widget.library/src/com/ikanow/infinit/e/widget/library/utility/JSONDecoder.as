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
	import com.adobe.serialization.json.JSONDecoder;
	
	/**
	 * Helper class used to decoded JSON returned from
	 * infinit.e API calls.
	 */
	public class JSONDecoder
	{
		
		//======================================
		// public static methods 
		//======================================
		
		/**
		 * Deserializes a json string into an actionscript object.
		 * Fields can then be accessed using object.field or object[field]
		 * calls.
		 *
		 * @param jsonData The json string to be deserialized.
		 * @return Object created from the json string.
		 */
		public static function decode( jsonData:String ):Object
		{
			var jd:com.adobe.serialization.json.JSONDecoder = new com.adobe.serialization.json.JSONDecoder( jsonData, true );
			return jd.getValue();
		}
	}
}
