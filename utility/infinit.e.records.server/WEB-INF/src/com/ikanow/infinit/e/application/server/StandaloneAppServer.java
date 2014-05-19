/*******************************************************************************
 * Copyright 2012 The Infinit.e Open Source Project sponsored by IKANOW.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.ikanow.infinit.e.application.server;

import org.restlet.Component;
import org.restlet.data.Protocol;

import com.ikanow.infinit.e.data_model.Globals;

public class StandaloneAppServer {

	public static void main(String[] args) 
	{  
		Globals.setIdentity(Globals.Identity.IDENTITY_SERVICE);
		if ( args.length > 0 )
			Globals.overrideConfigLocation(args[0]);
		if (args.length > 1) {
			AppServer.overrideApplicationConfigFile(args[1]);
		}

		try 
	    {  	    	
	        // Create a new Component.  
	        Component component = new Component();  
	  
	        // Add a new HTTP server listening on port 8184.  
	        int port = 8185;
	        component.getServers().add(Protocol.HTTP, port);  
	        // Attach the sample application.  
	        component.getDefaultHost().attach(new AppServer());  
	  
	        // Start the polls
	        AppServer.setupPollingHandlers();
	        
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
