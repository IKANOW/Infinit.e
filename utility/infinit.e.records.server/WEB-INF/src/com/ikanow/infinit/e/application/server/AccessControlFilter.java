package com.ikanow.infinit.e.application.server;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AccessControlFilter implements Filter 
{
	private FilterConfig config;
	
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException 
	{
		Boolean crossDomainAllowed = Boolean.parseBoolean( config.getInitParameter("crossDomainAllowed") );
		if ( crossDomainAllowed )
		{
			//grab the origin header and use it for who is allowed so UM can
			//use multiple development sites
			HttpServletResponse resp = (HttpServletResponse) response;
			HttpServletRequest req = (HttpServletRequest) request;
			String origin = req.getHeader("Origin");		
			resp.setHeader("Access-Control-Allow-Origin", origin);
			//resp.setHeader("Access-Control-Allow-Origin", "http://localhost:8080");
			resp.setHeader("Access-Control-Allow-Credentials", "true");
			resp.setHeader("Access-Control-Allow-Methods", "OPTIONS, GET, POST, PUT, DELETE");
			resp.setHeader("Access-Control-Allow-Headers", "accept, content-type");
		}
		
		chain.doFilter(request, response);
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException 
	{
		config = arg0;		
	}

	@Override
	public void destroy() 
	{		
		//do nothing
	}
}