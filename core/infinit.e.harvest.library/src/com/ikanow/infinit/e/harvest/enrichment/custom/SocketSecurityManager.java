package com.ikanow.infinit.e.harvest.enrichment.custom;

import java.io.File;

public class SocketSecurityManager extends SecurityManager 
{	
	private ThreadLocal<Boolean> javascriptLock = new ThreadLocal<Boolean>();
	
	public SocketSecurityManager()
	{
		super();
		
		//TODO (INF-1802): Normal Java... get the classpath and compare vs that:
		// - have 2 different maps, one containing files - direct comparison;
		//   one containing directories - extract just the path and compare
		// Tomcat: seems to want to access:
		// - java.home/lib, java.home/lib/ext, "java.lib.ext"
		// - /usr/share/tomcat6 (not clear where it gets this from... only feature is that i think it's HOME)
		// - webapp home directory (not clear how i can get this without having tomcat context available)
		
		// Currently I'm allowing access to all jar and class files that tomcat can see, which seems
		// a little bit dangerous (eg if you leave a JAR file containing a password kicking around)
	}
	
	public void setJavascriptFlag(boolean isJavascript)
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
			if ( host.matches("^(10\\.|127\\.|192.168\\.).*") )
			{
				throw new SecurityException("Hosts: 10.*, 192.168.*, 127.* are not allowed to be connected to");
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
				// Allow classes and JAR files and that's it...
				if (!file.endsWith(".class") && !file.endsWith(".jar")) {
					throw new SecurityException("Read Access is not allowed");				
				}
				else if (file.startsWith("tempJar")) { // (special case: these are the Hadoop plugins)
					throw new SecurityException("Read Access is not allowed");									
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
					throw new SecurityException("Read Access is not allowed");				
				}
				else if (file.startsWith("tempJar")) { // (special case: these are the Hadoop plugins)
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
			throw new SecurityException("Write Access is not allowed");
		}
		super.checkWrite(file);
	}
	
	@Override
	public void checkDelete(String file) {
		Boolean lock = javascriptLock.get();
		if ( lock != null && lock )
		{
			throw new SecurityException("Delete Access is not allowed");
		}
		super.checkDelete(file);
	}
	
	@Override
	public void checkExec(String cmd) {
		Boolean lock = javascriptLock.get();
		if ( lock != null && lock )
		{
			throw new SecurityException("Exec is not allowed");
		}
		super.checkExec(cmd);
	}	
}
