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
package com.ikanow.infinit.e.api.server;

import org.restlet.Component;
import org.restlet.data.Protocol;

import com.ikanow.infinit.e.data_model.Globals;

public class StandaloneRestletApp 
{
	public static void main(String[] args) 
	{  
		Globals.setIdentity(Globals.Identity.IDENTITY_API);
		if ( args.length > 0 )
			Globals.overrideConfigLocation(args[0]);

		try 
	    {  	    	
	        // Create a new Component.  
	        Component component = new Component();  
	  
	        // Add a new HTTP server listening on port 8184.  
	        int port = 8184;
	        component.getServers().add(Protocol.HTTP, port);  
	        // Attach the sample application.  
	        component.getDefaultHost().attach(new EmbeddedRestletApp());  
	  
	        // Start the component.  
	        component.start();
	    } 
	    catch (Exception e) 
	    {  
	        // Something is wrong.  
	        e.printStackTrace();  
	    }  
	}  

}
