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
	private AccessControlContext _accessControlContext;	
	private SocketSecurityManager ssm;
	private boolean SECURITY_ACTIVATED = false;
	
	public JavascriptSecurityManager()
	{
		//Check if security config is on/off
		PropertiesManager pm = new PropertiesManager();
		SECURITY_ACTIVATED = pm.getHarvestSecurity();
		if ( SECURITY_ACTIVATED )
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
				ssm.setJavascriptFlag(true);
				Object retVal = AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
					@Override
					public Object run() throws ScriptException
					{
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
			ssm.setJavascriptFlag(true);
			try
			{
				Object retVal = AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
					@Override
					public Object run() throws ScriptException
					{
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
