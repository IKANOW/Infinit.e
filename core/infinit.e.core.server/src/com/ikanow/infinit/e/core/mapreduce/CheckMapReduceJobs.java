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
		if ( args.length > 1 )
			flags = Integer.parseInt(args[1]);
		
		
		HadoopJobRunner hdr = new HadoopJobRunner();
		//run any jobs that are ready
		if ( (flags & 1) == 1 )
		{			
			hdr.runScheduledJobs();
		}
		
		//check status on any jobs that were running
		if ( (flags & 2) == 2 )
		{			
			hdr.updateJobStatus();
		}
	}
}
