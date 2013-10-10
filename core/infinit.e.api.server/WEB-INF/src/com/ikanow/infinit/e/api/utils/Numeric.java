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
