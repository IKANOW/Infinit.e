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
package com.ikanow.infinit.e.harvest.utils;

import org.jasypt.util.text.BasicTextEncryptor;

public class TextEncryption {
	// Basic Text Encryptor Object
	private static BasicTextEncryptor encryptor = null;
	// Encryptor Password String
	private String entryptorPassword = "infinit.e";
	
	/**
	 *  Constructor for password encryption class
	 */
	public TextEncryption() {
		encryptor = new BasicTextEncryptor();
		encryptor.setPassword(entryptorPassword);           // we HAVE TO set a password

	}
	/**
	 *  Encrypt the password
	 */
	public String encrypt(String password) {
		return encryptor.encrypt(password);
	}
	/**
	 *  Decrypting the password
	 */
	public String decrypt(String password) {
		return encryptor.decrypt(password);
	}

}
