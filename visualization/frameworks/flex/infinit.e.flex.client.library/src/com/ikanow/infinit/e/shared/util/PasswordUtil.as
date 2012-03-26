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
