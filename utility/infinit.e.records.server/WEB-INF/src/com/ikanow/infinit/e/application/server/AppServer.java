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

import javax.servlet.ServletContext;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.xml.DOMConfigurator;
import org.restlet.Application;
import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.resource.ServerResource;
import org.restlet.routing.Router;
import org.restlet.routing.Template;

import com.ikanow.infinit.e.application.handlers.actions.RecordInterface;
import com.ikanow.infinit.e.application.handlers.polls.LogstashHarvestPollHandler;
import com.ikanow.infinit.e.application.handlers.polls.LogstashIndexAgeOutPollHandler;
import com.ikanow.infinit.e.application.handlers.polls.LogstashSourceDeletionPollHandler;
import com.ikanow.infinit.e.application.handlers.polls.LogstashTestRequestPollHandler;
import com.ikanow.infinit.e.application.handlers.polls.PollingThread;
import com.ikanow.infinit.e.application.handlers.polls.V2SynchronizationPollHandler;
import com.ikanow.infinit.e.data_model.Globals;

public class AppServer extends Application {

	// ACTION HANDLERS
	
	protected void setupActionHandlers(Router router) {
        // EXAMPLE - ATTACH ExampleAction to URL "/test"
        attachPrefix(router, "/proxy/v2/{proxyterms}", RecordInterface.class); 
        attachPrefix(router, "/proxy/{proxyterms}", RecordInterface.class); 
    }
	
    // POLLING THREADS

	public static void setupPollingHandlers()
	{
        PollingThread poll1 = new PollingThread(new LogstashTestRequestPollHandler(), 2*1000L); 
        poll1.startThread();
        
        PollingThread poll2 = new PollingThread(new LogstashHarvestPollHandler(), 10*1000L); 
        poll2.startThread();
        
        PollingThread poll3 = new PollingThread(new LogstashIndexAgeOutPollHandler(), 3600*1000L); 
        poll3.startThread();
        
        PollingThread poll4 = new PollingThread(new LogstashSourceDeletionPollHandler(), 10*1000L); 
        poll4.startThread();
        
        PollingThread poll5 = new PollingThread(new V2SynchronizationPollHandler(), 5*1000L);
        poll5.startThread();
	}
	
	///////////////////////////////////////////////////////////////////////
	
	// UTILITY CODE
	
	public static String getApplicationConfigFile() {
		return _applicationFile;
	}
	public static void overrideApplicationConfigFile(String filePath) {
		_applicationFile = filePath;
	}
	private static String _applicationFile = "/opt/infinite-install/config/infinite.configuration.properties";
	
	public static void intializeInfiniteConfig(Context restContext, ServletContext servletContext) {
		
		if (Globals.Identity.IDENTITY_NONE == Globals.getIdentity()) {
			
			Globals.setIdentity(Globals.Identity.IDENTITY_API);
			String configpath = null;
			// First try override from system
			if (null == (configpath = (String) System.getProperties().get("INFINITE_CONFIG_HOME"))) { 
				if (null != restContext) {
					configpath = restContext.getParameters().getFirstValue("base_configpath");
				}
				else {
					configpath = (String) servletContext.getInitParameter("base_configpath");
				}
			}
	    	if(configpath != null) {
	    		Globals.overrideConfigLocation(configpath);
	    	}
			if (null == (configpath = (String) System.getProperties().get("APPLICATION_CONFIG_FILE"))) { 
				if (null != restContext) {
					_applicationFile = restContext.getParameters().getFirstValue("app_configfile");
				}
				else {
					_applicationFile = (String) servletContext.getInitParameter("app_configfile");
				}
			}
		}
		java.io.File file = new java.io.File(Globals.getLogPropertiesLocation() + ".xml");
		if (file.exists()) {
    		DOMConfigurator.configure(Globals.getLogPropertiesLocation() + ".xml");
		}
		else {
    		PropertyConfigurator.configure(Globals.getLogPropertiesLocation());
		}
		
    	Logger logger = Logger.getLogger(AppServer.class);
   
    	if (logger.getEffectiveLevel() != Level.DEBUG)
        {
        	java.util.logging.Logger.getLogger("org.restlet.Component.LogService").setLevel(java.util.logging.Level.OFF);
        }
	}	
	
    /** 
     * Creates a root Restlet that will receive all incoming calls. 
     */  
    @Override  
    public Restlet createRoot() { 
    	
    	intializeInfiniteConfig(getContext(), null);
    	
        // Create a router Restlet that routes each call to a  
        // new instance of HelloWorldResource.
    	
        Router router = new Router(getContext());

        setupActionHandlers(router);
        return router;  
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    
    // UTILITY
    
    @SuppressWarnings("unused")
	static private void attach(Router router, String url,  Class<? extends ServerResource> clazz) {
    	if (url.endsWith("/")) {
    		throw new IllegalArgumentException("Trailing / is automatically added as option");
    	}
    	router.attach(url, clazz);
    	router.attach(url + "/", clazz);
    }
    static private void attachPrefix(Router router, String url,  Class<? extends ServerResource> clazz) {
    	if (url.endsWith("/")) {
    		throw new IllegalArgumentException("Trailing / is automatically added as option");
    	}
    	router.attach(url, clazz).setMatchingMode(Template.MODE_STARTS_WITH);
    }
}
