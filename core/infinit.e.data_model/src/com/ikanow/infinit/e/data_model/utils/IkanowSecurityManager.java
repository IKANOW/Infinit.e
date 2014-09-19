package com.ikanow.infinit.e.data_model.utils;

import java.io.File;
import java.io.Reader;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.Permission;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

public class IkanowSecurityManager 
{
	private AccessControlContext _accessControlContext;	//not needed, is null always
	private static InternalSecurityManager ssm;
	private static boolean SECURITY_ACTIVATED = false;
	private boolean _disabled = false;
	
	public IkanowSecurityManager()
	{
		this(false);
	}
	public IkanowSecurityManager(boolean disabled)
	{
		_disabled = disabled;
		if (!disabled) { // (if disabled don't yet need to turn on)
			//Check if security config is on/off
			try {
				PropertiesManager pm = new PropertiesManager();
				SECURITY_ACTIVATED = pm.getSecureMode();
			}
			catch (Exception e) {
				SECURITY_ACTIVATED = true;
			}
			if ( SECURITY_ACTIVATED && ( null == ssm ) )
			{
				Object currSysManager = System.getSecurityManager();				
				if (currSysManager instanceof InternalSecurityManager) {
					ssm =  (InternalSecurityManager) currSysManager;
				}
				else {			
					ssm = new InternalSecurityManager();
					System.setSecurityManager(ssm);
				}
			}//TESTED
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
						ssm.setSecureFlag(true);
						return engine.eval(reader);
					}
				}, _accessControlContext);
				ssm.setSecureFlag(false);
				return retVal;
			}
			catch ( PrivilegedActionException ex )
			{			
				ssm.setSecureFlag(false);
				throw (ScriptException)ex.getException();
			}
			catch ( Exception ex2 ) { // (shouldn't ever occur in practice)
				throw new ScriptException(ex2.getMessage());
			}
			finally {
				ssm.setSecureFlag(false);				
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
						ssm.setSecureFlag(true);
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
				ssm.setSecureFlag(false);				
			}
		}
		else
		{
			//Security OFF
			return engine.eval(script);
		}
	}

	public void setSecureFlag(boolean b) 
	{
		if ( !_disabled && SECURITY_ACTIVATED )
		{
			ssm.setSecureFlag(b);
		}
	}

	//////////////////////////////////////////////////////////
	
	public static class InternalSecurityManager extends SecurityManager 
	{	
		private ThreadLocal<Boolean> javascriptLock = new ThreadLocal<Boolean>();
		
		//DEBUG
		//private static boolean _DEBUG = true;
		private static boolean _DEBUG = false;

		public static void debugDisplayErrorMessage(String message) {
			if (_DEBUG) {
				try {
					throw new RuntimeException("SEC_ERR = " + message);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		public InternalSecurityManager()
		{
			super();
			
			// Currently I'm allowing access to all jar and class files that tomcat can see, which seems
			// a little bit dangerous (eg if you leave a JAR file containing a password kicking around)
		}
		
		public void setSecureFlag(boolean isJavascript)
		{
			javascriptLock.set(isJavascript);
		}

		@Override
		public void checkConnect(String host, int port)
		{	
			Boolean lock = javascriptLock.get();
			if ( lock != null && lock )
			{			
				//if failed we are using our javascript security
				//deny for 10.*
				//deny for 192.186.*
				//deny for 127.*
				if ( host.matches("^(10\\.|127\\.|192\\.168\\.|172\\.1[6-9]|172\\.[2-9][0-9]).*") )
				{
					if (_DEBUG)
						debugDisplayErrorMessage(host + ": " + port);
					throw new SecurityException("Hosts: 10.*, 192.168.*, 127.*, 172.16+ are not allowed to be connected to");
				}
			}
			// Always do this: so we're the union of configured restrictions+the above custom restrictions
			super.checkConnect(host,port);		
		}		
		
		// Infinite loop workaround: http://jira.smartfrog.org/jira/browse/SFOS-236
		protected boolean inReadCheck = false;
		
		@Override
		public synchronized void checkRead(String file) {
			if (inReadCheck) {
				// Infinite loop workaround: http://jira.smartfrog.org/jira/browse/SFOS-236
				return;
			}
			
			Boolean lock = javascriptLock.get();		
			if ( lock != null && lock )
			{
				boolean bExists = false;
				try {
					javascriptLock.set(false);
					bExists = new File(file).exists();
				}
				finally {
					javascriptLock.set(true);				
				}
				
				if (bExists) { // (else don't bother checking)
					// Allow JRE access, and classes and JAR files/native libs and that's it...
					if (!file.startsWith(System.getProperty("java.home"))) {
						if (!(file.endsWith(".class") || file.endsWith(".jar") || file.endsWith(".dll") || file.endsWith(".so"))) {
							debugDisplayErrorMessage(file);
							throw new SecurityException("Read Access is not allowed");				
						}
						else if (file.startsWith("tempJar")) { // (special case: these are the Hadoop plugins)
							debugDisplayErrorMessage(file);
							throw new SecurityException("Read Access is not allowed");									
						}
					}
				}
			}
			try {
				inReadCheck = true;
				super.checkRead(file);
			}
			finally {
				// Infinite loop workaround: http://jira.smartfrog.org/jira/browse/SFOS-236
				inReadCheck = false;
			}
		}
		
		@Override
		public void checkRead(String file, Object context) {
			Boolean lock = javascriptLock.get();
			if ( lock != null && lock )
			{
				boolean bExists = false;
				try {
					javascriptLock.set(false);
					bExists = new File(file).exists();
				}
				finally {
					javascriptLock.set(true);				
				}
				
				if (bExists) { // (else don't bother checking)
					// Allow classes and JAR files and that's it...
					if (!file.endsWith(".class") && !file.endsWith(".jar")) {
						debugDisplayErrorMessage(file);
						throw new SecurityException("Read Access is not allowed");				
					}
					else if (file.startsWith("tempJar")) { // (special case: these are the Hadoop plugins)
						debugDisplayErrorMessage(file);
						throw new SecurityException("Read Access is not allowed");									
					}
				}
			}
			super.checkRead(file, context);
		}
		
		@Override
		public void checkWrite(String file) {
			Boolean lock = javascriptLock.get();
			if ( lock != null && lock )
			{
				debugDisplayErrorMessage(file);
				throw new SecurityException("Write Access is not allowed");
			}
			super.checkWrite(file);
		}
		
		@Override
		public void checkDelete(String file) {
			Boolean lock = javascriptLock.get();
			if ( lock != null && lock )
			{
				debugDisplayErrorMessage(file);
				throw new SecurityException("Delete Access is not allowed");
			}
			super.checkDelete(file);
		}
		
		@Override
		public void checkExec(String cmd) {
			Boolean lock = javascriptLock.get();
			if ( lock != null && lock )
			{
				debugDisplayErrorMessage(cmd);
				throw new SecurityException("Exec is not allowed");
			}
			super.checkExec(cmd);
		}	

		@Override
		public void checkPackageAccess(String packageName) {
			Boolean lock = javascriptLock.get();
			if ( lock != null && lock )
			{
				if (packageName.startsWith("com.ikanow.infinit.e.")) {
					debugDisplayErrorMessage(packageName);
					throw new SecurityException("Not allowed access to these packages: " + packageName);
				}
			}
			super.checkPackageAccess(packageName);
		}//TESTED (by hand)
		
		@Override
		public void checkPermission(Permission permission) { 
			Boolean lock = javascriptLock.get();
			if ( lock != null && lock )
			{
				if (permission instanceof RuntimePermission) {
					RuntimePermission permEx = (RuntimePermission) permission;
					if (permEx.getName().equals("setSecurityManager")) {
						debugDisplayErrorMessage(permEx.getName());
						throw new SecurityException("Not allowed to overwrite security manager: " + permEx.getName());					
					}
				}
			}
			super.checkPermission(permission);
		}//TESTED (by hand)
	
		//TODO (INF-2640): This didn't work, amongst other things it stopped regexes from working
//		@Override
//		public void checkPermission(Permission permission) 
//		{
//			if (permission instanceof ReflectPermission) { 
//				// (i think this might be called in lots of spots so only do anything if it's a reflection)
//				Boolean lock = javascriptLock.get();
//				if ( lock != null && lock )
//				{
//						throw new SecurityException("Reflection is not allowed");				
//				}
//			}
//			// Always do this: so we're the union of configured restrictions+the above custom restrictions
//			super.checkPermission(permission);				
//		}//TESTED (TestSecurityManager.testReflection)
	//	

		/////////////////////////////////////////////////
		
		//CODE TO COMMENT IN AND TEST
		
//		@Override
//		public void checkAccess(Thread t) {
//			Boolean lock = javascriptLock.get();
//			if ( lock != null && lock )
//			{
//				throw new SecurityException("Change thread attributes is not allowed");
//			}
//			super.checkAccess(t);		
//		}//TOTEST
	//
//		@Override
//		public void checkAccess(ThreadGroup g) {
//			Boolean lock = javascriptLock.get();
//			if ( lock != null && lock )
//			{
//				throw new SecurityException("Change thread attributes is not allowed");
//			}
//			super.checkAccess(g);		
//		}//TOTEST
	//
//		@Override
//		public void checkExit(int status) {
//			Boolean lock = javascriptLock.get();
//			if ( lock != null && lock )
//			{
//				throw new SecurityException("Exit is not allowed");
//			}
//			super.checkExit(status);		
//		}//TOTEST
	//	
//		@Override
//		public void checkSecurityAccess(String target) {
//			Boolean lock = javascriptLock.get();
//			if ( lock != null && lock )
//			{
//				if ((target != null) && !target.startsWith("get") && !target.startsWith("print")) {
//					throw new SecurityException("Security Access can only be read only");
//				}
//			}
//			super.checkSecurityAccess(target);		
//		}//TOTEST
	//	
//		@Override
//		public void checkLink(String lib) {
//			Boolean lock = javascriptLock.get();
//			if ( lock != null && lock )
//			{
//				throw new SecurityException("Linking is not allowed");
//			}
//			super.checkLink(lib);		
//		}//TOTEST
	//	
//		@Override
//		public void checkPropertiesAccess() {
//			Boolean lock = javascriptLock.get();
//			if ( lock != null && lock )
//			{
//				throw new SecurityException("Access to system properties is not allowed");
//			}
//			super.checkPropertiesAccess();		
//		}//TOTEST
	//	
//		@Override
//		public void checkPropertyAccess(String key) {
//			Boolean lock = javascriptLock.get();
//			if ( lock != null && lock )
//			{
//				throw new SecurityException("Access to system properties is not allowed");
//			}
//			super.checkPropertyAccess(key);		
//		}//TOTEST
	}
	

}
