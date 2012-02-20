package com.ikanow.infinit.e.api.utils;

/**
 * Class used to parse strings to various numbers.  Used for validation purposes.
 * @author root
 *
 */
public class Numeric {

	/**
	 * Checks to see if a string is a integer
	 * @param input
	 * @return
	 */
	public static boolean isInteger( String input )  {  
	   try  {  
	      Integer.parseInt( input );  
	      return true;  
	   }  
	   catch( Exception e)  
	   {  
	      return false;  
	   }  
	} 
	
	/**
	 * Checks to see if a string is a long
	 * @param input
	 * @return
	 */
	public static boolean isLong(String input) {
		try {
			Long.parseLong( input );
			return true;
		}
		catch (Exception e) {
			return false;
		}
	}
	/**
	 * Checks to see if a string is a double
	 * @param input
	 * @return
	 */
	public static boolean isDouble( String input ) {
		try {
			Double.parseDouble(input);
			return true;
		}
		catch (Exception e) {
			return false;
		}
	}
	/**
	 * Checks to see if a string is a float
	 * @param input
	 * @return
	 */
	public static boolean isFloat( String input ) {
		try {
			Float.parseFloat(input);
			return true;
		}
		catch (Exception e) {
			return false;
		}
	}
	
}
