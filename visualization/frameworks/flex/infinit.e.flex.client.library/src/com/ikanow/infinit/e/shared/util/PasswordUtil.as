package com.ikanow.infinit.e.shared.util
{
	import com.hurlant.crypto.Crypto;
	import com.hurlant.crypto.hash.IHash;
	import com.hurlant.util.Base64;
	import com.hurlant.util.Hex;
	import com.ikanow.infinit.e.shared.model.constant.Constants;
	import flash.utils.ByteArray;
	
	public class PasswordUtil
	{
		
		//======================================
		// constructor 
		//======================================
		
		/**
		 *
		 * Constructor
		 *
		 */
		public function PasswordUtil()
		{
		
		}
		
		
		//======================================
		// public static methods 
		//======================================
		
		/**
		 * Hashes a string so it can be passed to the authentication API
		 * calls.
		 */
		public static function hashPassword( password:String ):String
		{
			var c:Crypto = new Crypto();
			var cipher:IHash = Crypto.getHash( Constants.ENCRIPTION_ALGORITHM );
			var data:ByteArray = Hex.toArray( Hex.fromString( password ) );
			var hashed:String = Base64.encodeByteArray( cipher.hash( data ) );
			return hashed;
		}
	}
}
