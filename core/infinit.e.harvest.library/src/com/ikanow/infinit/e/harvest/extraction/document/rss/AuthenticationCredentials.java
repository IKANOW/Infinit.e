/**
 * 
 */
package com.ikanow.infinit.e.harvest.extraction.document.rss;

import com.sun.syndication.fetcher.impl.HttpClientFeedFetcher.CredentialSupplier;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.Credentials;
/**
 * Class that allows for the passing of basic authentication information
 * over http.  Specifically used in our application for passing basic
 * authentication credentials to a password protect basic feed such
 * as RSS or a Database requiring credentials
 * 
 * @author cmorgan
 *
 */
public class AuthenticationCredentials implements CredentialSupplier {

	/**
	 * Private variables
	 */
	 private String username= null;
	 private String password = null;

	 /**
	 * Set the username
	 */
	 public void setUsername(String username){
	  this.username=username;
	 }
	 /**
	 * Get the username
	 */
	 public String getUsername(){
	  return this.username;
	 }
	 /**
	 * Set the password
	 */
	 public void setPassword(String password){
	  this.password=password;
	 }
	 /**
	 * Get the password
	 */
	 public String getPassword(){
	  return this.password;
	 }
	 /**
	 *  Constructor for authentication credentials class
	 */
	 public AuthenticationCredentials(){
	 }
	 
	 /**
	 * Get the credentials object
	 * 
	 * @param  username	the username of the user requireing authentication
	 * @param  host		the password of the user requireing authentication
	 */
	 public AuthenticationCredentials(String username,String password){
	  setUsername(username);
	  setPassword(password);
	 }
	 /**
	 * Get the credentials object
	 * 
	 * @param  realm	the realm location
	 * @param  host		the host location
	 */
	 public Credentials getCredentials(String realm, String host){
	  String username= this.username;
	  String password= this.password;
	  return new UsernamePasswordCredentials(username,password);
	 }
}
