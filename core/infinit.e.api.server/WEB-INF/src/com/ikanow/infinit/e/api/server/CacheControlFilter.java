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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ikanow.infinit.e.api.utils.PropertiesManager;

import java.io.IOException;
import java.util.Date;
import java.util.regex.Pattern;

// This class is only referenced in api.war's web.xml, to append cache-control headers
// (Without which IE caches service requests)

public class CacheControlFilter implements Filter {

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
    	
        resp.setHeader("Expires", "0");
        resp.setHeader("Last-Modified", new Date().toString());
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0, post-check=0, pre-check=0");
        resp.setHeader("Pragma", "no-cache");

        chain.doFilter(request, response);
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
	
	private void createSecurityPermissions() {    	
    	
		EmbeddedRestletApp.intializeInfiniteConfig(null, _config.getServletContext());
		
    	PropertiesManager pm = new PropertiesManager();
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
