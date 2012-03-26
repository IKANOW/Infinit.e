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
/**
 * 
 */
package com.ikanow.infinit.e.api;

import org.restlet.data.Form;
import org.restlet.data.Parameter;

/**
 * Classed used to process all incoming query requests to API
 * (Mostly obsoleted now, used in 2 places so left in, not worth removing)
 * 
 * @author cmorgan
 *
 */
public class Parameters {
	
	 /** 
	  * Private Class Variables
	  */
	// Person interface
	private String wpauth = null;
	private String wpuser = null;
	
	// Login interface
	private String returnURL = null;
	private String username = null;
	private String password = null;
	private Boolean multi = null; // (allows the same user to login multiple times, must be admin)
	private Boolean override = null; // (means existing logins will take precedence over new ones)
	
	/** 
	  * Class Constructor used to setup the query paramters
	  */
	public Parameters(Form form) {
		// Loop through the form object to pull the query parameters
		for (Parameter parameter : form) 
		{
			if ( parameter.getName().toLowerCase().contentEquals("wpauth")) { this.setWpauth(parameter.getValue()); }
			if ( parameter.getName().toLowerCase().contentEquals("wpuser")) { this.setWpuser(parameter.getValue()); }
			
			if ( parameter.getName().toLowerCase().contentEquals("returnurl")) { this.setReturnURL(parameter.getValue()); }
			if ( parameter.getName().toLowerCase().contentEquals("username")) { this.setUsername(parameter.getValue()); }
			if ( parameter.getName().toLowerCase().contentEquals("password")) { this.setPassword(parameter.getValue()); }
			if ( parameter.getName().toLowerCase().contentEquals("multi")) { 
				this.setMulti(parameter.getValue().equalsIgnoreCase("true") || parameter.getValue().equals("1"));
			}
			if ( parameter.getName().toLowerCase().contentEquals("override")) { 
				this.setOverride(parameter.getValue().equalsIgnoreCase("true") || parameter.getValue().equals("1"));
			}
	    }
	}
	
	public void setReturnURL(String returnURL) {
		this.returnURL = returnURL;
	}
	public String getReturnURL() {
		return returnURL;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getUsername() {
		return username;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getPassword() {
		return password;
	}
	public String getWpauth() {
		return wpauth;
	}

	public void setWpauth(String wpauth) {
		this.wpauth = wpauth;
	}

	public String getWpuser() {
		return wpuser;
	}

	public void setWpuser(String wpuser) {
		this.wpuser = wpuser;
	}

	/**
	 * @param multi the multi to set
	 */
	public void setMulti(Boolean multi) {
		this.multi = multi;
	}

	/**
	 * @return the multi
	 */
	public Boolean getMulti() {
		return multi;
	}

	/**
	 * @param override the override to set
	 */
	public void setOverride(Boolean override) {
		this.override = override;
	}

	/**
	 * @return the override
	 */
	public Boolean getOverride() {
		return override;
	}	
}
