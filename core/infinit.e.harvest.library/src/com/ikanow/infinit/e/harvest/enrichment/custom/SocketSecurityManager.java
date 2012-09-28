package com.ikanow.infinit.e.harvest.enrichment.custom;

public class SocketSecurityManager extends SecurityManager 
{	
	private ThreadLocal<Boolean> javascriptLock = new ThreadLocal<Boolean>();
		
	public SocketSecurityManager()
	{
		super();
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
	
}
