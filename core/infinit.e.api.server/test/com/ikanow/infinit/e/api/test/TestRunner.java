package com.ikanow.infinit.e.api.test;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import com.ikanow.infinit.e.api.social.sharing.ShareV2ApiTest;
import com.ikanow.infinit.e.data_model.Globals;

public class TestRunner
{

	public static void main(String[] args) 
	{
		//init config
		Globals.setIdentity(Globals.Identity.IDENTITY_API);
		if ( args.length > 0 )
			Globals.overrideConfigLocation(args[0]);
		
		//run tests
		Result result = JUnitCore.runClasses(ShareV2ApiTest.class);
		for ( Failure failure : result.getFailures() )
		{
			System.out.println(failure.toString());
		}
		System.out.println(result.wasSuccessful());
	}

}
