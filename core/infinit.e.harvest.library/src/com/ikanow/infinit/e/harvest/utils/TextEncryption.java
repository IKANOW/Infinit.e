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
