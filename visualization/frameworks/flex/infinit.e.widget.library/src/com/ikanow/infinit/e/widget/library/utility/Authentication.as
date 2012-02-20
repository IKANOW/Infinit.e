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
	import com.hurlant.crypto.Crypto;
	import com.hurlant.crypto.hash.IHash;
	import com.hurlant.util.Base64;
	import com.hurlant.util.Hex;
	
	import flash.utils.ByteArray;

	/**
	 * Contains methods for authication with the infinit.e system
	 */
	public class Authentication
	{			
		/**
		 * Hashes a string so it can be passed to the authentication API
		 * calls.
		 */
		public static function hashPassword(password:String):String
		{
			var c:Crypto = new Crypto();
			var cipher:IHash = Crypto.getHash("sha256");			
			var data:ByteArray = Hex.toArray(Hex.fromString(password));
			var hashed:String = Base64.encodeByteArray(cipher.hash(data));			
			return hashed;
		}		
	}
}