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
package com.ikanow.infinit.e.core.mapreduce;


public class CheckMapReduceJobs {

	/**
	 * @param args
	 */
	public static void main(String[] args) 
	{
		com.ikanow.infinit.e.data_model.Globals.setIdentity(com.ikanow.infinit.e.data_model.Globals.Identity.IDENTITY_API);
		
		String configloc = args[0];
		com.ikanow.infinit.e.data_model.Globals.overrideConfigLocation(configloc);
		
		int flags = 0;
		String jobOverride = null;
		if ( args.length > 1 )
			flags = Integer.parseInt(args[1]);
		if ( args.length > 2 )
			jobOverride = args[2];
		
		HadoopJobRunner hdr = new HadoopJobRunner();
		//run any jobs that are ready
		if ( (flags & 1) == 1 )
		{			
			hdr.runScheduledJobs(jobOverride);
		}
		
		//check status on any jobs that were running
		if ( (flags & 2) == 2 )
		{			
			hdr.updateJobStatus();
		}
	}
}
