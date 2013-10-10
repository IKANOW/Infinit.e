package com.ikanow.infinit.e.harvest.enrichment.custom;

import java.io.Reader;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import com.ikanow.infinit.e.harvest.utils.PropertiesManager;

public class JavascriptSecurityManager 
{
	private AccessControlContext _accessControlContext;	//not needed, is null always
	private static SocketSecurityManager ssm;
	private boolean SECURITY_ACTIVATED = false;
	
	//TODO (INF-2118) don't eval if running the source as admin (have a non static user passed into c'tor, do a auth check)
	// ... probably need to investigate why it's intermittently failing first?
	// ALSO NEED TO DISABLE REFLECTION
	
	public JavascriptSecurityManager()
	{
		//Check if security config is on/off
		PropertiesManager pm = new PropertiesManager();
		SECURITY_ACTIVATED = pm.getHarvestSecurity();
		if ( SECURITY_ACTIVATED && ( null == ssm ) )
		{
			ssm = new SocketSecurityManager();
			System.setSecurityManager(ssm);
		}
	}
	
	public Object eval(final ScriptEngine engine, final Reader reader) throws ScriptException
	{	
		if ( SECURITY_ACTIVATED )
		{
			//Security ON
			try
			{				
				Object retVal = AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
					@Override
					public Object run() throws ScriptException
					{
						ssm.setJavascriptFlag(true);
						return engine.eval(reader);
					}
				}, _accessControlContext);
				ssm.setJavascriptFlag(false);
				return retVal;
			}
			catch ( PrivilegedActionException ex )
			{			
				ssm.setJavascriptFlag(false);
				throw (ScriptException)ex.getException();
			}
			catch ( Exception ex2 ) { // (shouldn't ever occur in practice)
				throw new ScriptException(ex2.getMessage());
			}
			finally {
				ssm.setJavascriptFlag(false);				
			}
		}
		else
		{
			//security OFF
			return engine.eval(reader);
		}
	}
	
	public Object eval(final ScriptEngine engine, final String script) throws ScriptException
	{			
		if ( SECURITY_ACTIVATED )
		{
			//Security ON
			try
			{
				Object retVal = AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
					@Override
					public Object run() throws ScriptException
					{
						ssm.setJavascriptFlag(true);
						return engine.eval(script);
					}
				}, _accessControlContext);
				return retVal;
			}
			catch ( PrivilegedActionException ex )
			{			
				throw (ScriptException)ex.getException();
			}
			catch ( Exception ex2 ) { // (shouldn't ever occur in practice)
				throw new ScriptException(ex2.getMessage());
			}
			finally {
				ssm.setJavascriptFlag(false);				
			}
		}
		else
		{
			//Security OFF
			return engine.eval(script);
		}
	}

	public void setJavascriptFlag(boolean b) 
	{
		if ( SECURITY_ACTIVATED )
		{
			ssm.setJavascriptFlag(b);
		}
	}
}
