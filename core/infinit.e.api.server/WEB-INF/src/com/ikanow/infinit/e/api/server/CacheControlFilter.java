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
package com.ikanow.infinit.e.api.server;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import com.ikanow.infinit.e.api.utils.PropertiesManager;

import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

// This class is only referenced in api.war's web.xml, to append cache-control headers
// (Without which IE caches service requests)

public class CacheControlFilter implements Filter {

	// Used for JSONP transformation
	public class CharResponseWrapper extends HttpServletResponseWrapper {
		private CharArrayWriter output1;
		private BufferedServletOutputStream output2;
		@Override
		public String toString() {
			if (null != output1) {
				return output1.toString();
			}
			else if (null != output2) {
				return new String(output2.getBuffer());
			}
			return null;
		}
		public CharResponseWrapper(HttpServletResponse response){
			super(response);
			
		}
		@Override
		public PrintWriter getWriter(){
			if (null == output1) {
				output1 = new CharArrayWriter();
			}
			return new PrintWriter(output1);
		}	   
		@Override
		public ServletOutputStream getOutputStream() {
			if (null == output2) {
				output2 = new BufferedServletOutputStream();
			}
			return output2;
		}
	}//TESTED (getOutputStream only)
	public class BufferedServletOutputStream extends ServletOutputStream {
	    // the actual buffer
	    private ByteArrayOutputStream bos = new ByteArrayOutputStream( );

	    public byte[] getBuffer( ) {
	        return this.bos.toByteArray( );
	    }
	    public void write(int data) {
	        this.bos.write(data);
	    }
	    public void reset( ) {
	        this.bos.reset( );
	    }
	    public void setBufferSize(int size) {
	        this.bos = new ByteArrayOutputStream(size);
	    }
	}//TESTED
	
	// Used for API key transformation
	public static class ExtendedServletRequest extends HttpServletRequestWrapper {

		String apiKey = null;
		private Cookie[] cookie = new Cookie[1];
		public ExtendedServletRequest(HttpServletRequest request) {
			super(request);
		}
		public void setCookie(String apiKey) {
			this.apiKey = apiKey;
			cookie[0] = new Cookie("infinitecookie", apiKey);
		}
		@Override
		public Cookie[] getCookies() { 
			return cookie;
		}

		@Override
		public String getHeader(String headerName)
		{            
			if (headerName.equalsIgnoreCase("Cookie")) {
				return "infinitecookie=" + apiKey;
			}
			else return super.getHeader(headerName);
		}

		@SuppressWarnings("rawtypes")
		@Override
		public Enumeration getHeaderNames()
		{            
			if (null == super.getHeaderNames()) {
				return new StringTokenizer("Cookie");
			}
			else {
				StringBuffer currHeader = new StringBuffer("Cookie");
				for (Enumeration e = super.getHeaderNames() ; e.hasMoreElements() ;) {
					String field = (String) e.nextElement();
					if (!field.equalsIgnoreCase("Cookie")) {
						currHeader.append('\n').append(field);
					}
				}
				return new StringTokenizer(currHeader.toString());
			}//TESTED
		}

		@SuppressWarnings("rawtypes")
		@Override
		public Enumeration getHeaders(String headerName)
		{            
			if (headerName.equalsIgnoreCase("Cookie")) {
				return new StringTokenizer("infinitecookie=" + apiKey);
			}
			else return super.getHeaders(headerName);
		}
		
	}//TESTED
	
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

    	HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
    	
    	if (!_bCreatedSecurityLayer) {
    		createSecurityPermissions();
    	}
    	if (_bHasSecurityLayer) {
    		if (!isAccessAllowed(req)) {
    			resp.sendError(403, "Locally disabled REST call: " + req.getRemoteAddr() + " / " + req.getRequestURI());
    			return;
    		}
    	}
    	
    	// API key handling:
    	// (can't call getParameter for POSTs because you can only call it once)
    	
    	int nInfiniteApiKey = -1;
    	String queryString = req.getQueryString();
    	if ((null != queryString) && ((nInfiniteApiKey = queryString.indexOf("infinite_api_key=")) >= 0))
    	{
    		if ((0 == nInfiniteApiKey) || ('&' == queryString.charAt(nInfiniteApiKey - 1)) || ('?' == queryString.charAt(nInfiniteApiKey - 1)))
    		{
    			nInfiniteApiKey += 17; // (jumps over attribute size)
    			int nEndApiKey = queryString.indexOf('&', nInfiniteApiKey + 1);
    			String apiKey = null;
    			if (nEndApiKey < 0) {
    				apiKey = queryString.substring(nInfiniteApiKey);    				
    			}
    			else {
    				apiKey = queryString.substring(nInfiniteApiKey, nEndApiKey);
    			}    			
            	ExtendedServletRequest tmpReq = new ExtendedServletRequest(req);
            	tmpReq.setCookie("api:" + apiKey);
            	request = tmpReq;
    		}
    	}//TESTED

    	// JSONP parsing...
    	
    	String jsonpCallbackStr = null;
    	int nJsonpCallback = -1;
    	HttpServletResponse actualResponse = resp;
    	if ((null != queryString) && ((nJsonpCallback = queryString.indexOf("jsonp=")) >= 0))
    	{
    		if ((0 == nJsonpCallback) || ('&' == queryString.charAt(nJsonpCallback - 1))) {
    			nJsonpCallback += 6; // (jumps over attribute size)
    			int nEndJsonp = queryString.indexOf('&', nJsonpCallback + 1);
    			if (nEndJsonp < 0) {
    				jsonpCallbackStr = queryString.substring(nJsonpCallback);    				
    			}
    			else {
    				jsonpCallbackStr = queryString.substring(nJsonpCallback, nEndJsonp);
    			}
    			response = new CharResponseWrapper(resp);
    		}
    		
    	}//TESTED	
    	
        resp.setHeader("Expires", "0");
        resp.setHeader("Last-Modified", new Date().toString());
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0, post-check=0, pre-check=0");
        resp.setHeader("Pragma", "no-cache");

        chain.doFilter(request, response);
        
        // Logging:
        if (_loggingEnabled) {
	        boolean apiLogSet = false;        
	        if (resp.containsHeader("X-infinit.e.log")) {
	        	apiLogSet = true;
	        }//TESTED        
        	if (!_loggingErrorsOnly || apiLogSet) {
        		if (null != _loggingRegex) { // if regex enabled then apply it
        			String url = req.getRequestURI();
        			if (!_loggingErrorsOnly && apiLogSet) { // if it's an error and we're doing positive selection then allow selection on that also 
        				url += "&X-infinit.e.log";
        			}//TESTED
	    			if (_loggingRegex.matcher(url).find()) {
	    				req.setAttribute("infinit.e.log", "1");
	    			}//TESTED
        		}
        		else if (apiLogSet) { // if regex not enabled, only forward on error 
    				req.setAttribute("infinit.e.log", "1");        			
        		}//TESTED
        	}//TESTED
        	// if logging errors only then only process if the API has set "infinit.e.log" (and 
        	// if !(logging errors only)
        } //TESTED
        
        if (null != jsonpCallbackStr) {
        	actualResponse.setHeader("Content-Type", "application/javascript");
        	
        	// This isn't the fastest, but I think RESTlet uses the writer().write() code
        	// so it gets written into the string not the output stream so we have to go via
        	// a string and pay this (possibly xMB) copy penalty
        	String json = ((CharResponseWrapper)response).toString();
        	byte[] jsonpBytes = jsonpCallbackStr.getBytes();
        	byte[] responseBytes = json.getBytes();
        	
        	actualResponse.setContentLength(jsonpBytes.length + responseBytes.length + 3);
        	actualResponse.getOutputStream().write(jsonpBytes);
        	actualResponse.getOutputStream().write("(".getBytes());
        	actualResponse.getOutputStream().write(responseBytes);
        	actualResponse.getOutputStream().write(");".getBytes());
        	actualResponse.getOutputStream().flush();
        	
        }//TESTED
        
    }

    public void destroy() {
    }
    
    protected FilterConfig _config;
    
    public void init(FilterConfig arg0) throws ServletException {
    	_config = arg0;
    }

    public void setFilterConfig(FilterConfig config) {
      this._config = config;
    }

    public FilterConfig getFilterConfig() {
      return _config;
    }
    
    ////////////////////////////////////////////////////////////////////////////////////
    
    // REMOTE ACCESS SECURITY
    
	private static boolean _bCreatedSecurityLayer = false;
	private static boolean _bHasSecurityLayer = false;
	private static Pattern _allowRegex = null;
	private static Pattern _denyRegex = null;
	private static boolean _loggingEnabled = false;
	private static boolean _loggingErrorsOnly = true;
	private static Pattern _loggingRegex = null;
	
	private void createSecurityPermissions() {    	
    	
		EmbeddedRestletApp.intializeInfiniteConfig(null, _config.getServletContext());
		
    	PropertiesManager pm = new PropertiesManager();
    	
    	// Logging:
    	
    	_loggingEnabled = pm.getApiLoggingEnabled();
    	if (_loggingEnabled) {
    		String loggingRegex = pm.getApiLoggingRegex();
        	if ((null != loggingRegex) && !loggingRegex.trim().isEmpty()) {
        		if (loggingRegex.startsWith("*")) { // (log everything that matches the regex)
        			_loggingErrorsOnly = false;
        			loggingRegex = loggingRegex.substring(1);
        		}
        		else if (loggingRegex.startsWith("!")) { // (default -log errors only- just remove this char)
        			loggingRegex = loggingRegex.substring(1);        			
        		}
        		if (!loggingRegex.isEmpty()) {
        			_loggingRegex = Pattern.compile(loggingRegex.trim(), Pattern.CASE_INSENSITIVE);
        		}
        	}
    	}//(else only log failures)
    	//TESTED
    	
    	// Security:
    	
    	String allow = pm.getRemoteAccessAllow();
    	String deny = pm.getRemoteAccessDeny();
    	if (((null == allow) || allow.trim().isEmpty()) && ((null == deny) || deny.trim().isEmpty())) {
        	_bCreatedSecurityLayer = true;
    		return; // no remote access security
    	}
    	if ((null != allow) && !allow.trim().isEmpty()) {
    		_allowRegex = Pattern.compile(allow.trim(), Pattern.CASE_INSENSITIVE);
    	}
    	if ((null != deny) && !deny.trim().isEmpty()) {
    		_denyRegex = Pattern.compile(deny.trim(), Pattern.CASE_INSENSITIVE);
    	}
    	if ((null != _allowRegex) || (null != _denyRegex)) {
    		_bHasSecurityLayer = true;
    	}
    	_bCreatedSecurityLayer = true;
    }
    
    private boolean isAccessAllowed(HttpServletRequest request) {

    	// Is this a local request?
    	
    	String ipAddr = request.getRemoteAddr(); 
    	
    	if (!ipAddr.startsWith("127.0.0.") && !ipAddr.startsWith("0:0:0:0:0:0:0:1")) {
    		
    		if (null != _allowRegex) {
    			if (!_allowRegex.matcher(request.getRequestURI()).find()) {
    				return false;
    			}
    		}
    		if (null != _denyRegex) {
    			if (_denyRegex.matcher(request.getRequestURI()).find()) {
    				return false;
    			}    			
    		}    		
    	}
    	return true;
    }
    
}
