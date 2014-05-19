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
//TODO reuse 

package com.ikanow.infinit.e.application.server;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

// NO NEED TO EDIT THIS - JUST CALLS BACK INTO APP SERVER

public class PollServer implements ServletContextListener
{
	///////////////////////////////////////////////////////////////////////

	// UTILITY CODE

	@Override
	public void contextDestroyed(ServletContextEvent contextEvent) {
	}

	@Override
	public void contextInitialized(ServletContextEvent contextEvent)
	{
		AppServer.intializeInfiniteConfig(null, contextEvent.getServletContext());
		AppServer.setupPollingHandlers();         
	}
}
