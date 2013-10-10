package com.ikanow.infinit.e.utility.tomcat;

import java.io.IOException;
import java.util.Date;
import java.util.regex.Pattern;

import javax.servlet.ServletException;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.AccessLogValve;

// Differs from AccessLogValue in 2 ways:
// 1] always logs an HTTP error (>=400)
// 2] condition is reversed, ie _only_ logs if condition is set
// 3] logs if "infinit.e.log" is set in request
// 4] sanitizes the URLs

public class ExtendedAccessLogValve extends AccessLogValve {

	protected String _savedMessage = null;
	
	protected boolean _isError = false;
	Pattern _errorOverrideRegex = null;
	
	@Override
	public void invoke(Request request, Response response) throws ServletException, IOException {
		// Sanitize the URLs 
		for (int i = 0; i < logElements.length; i++) {
			if (logElements[i] instanceof RequestElement) {
				logElements[i] = new SanitizedRequestElement();
			}
			else if (logElements[i] instanceof RequestURIElement) {
				logElements[i] = new SanitizedRequestURIElement();
			}
			else if (logElements[i] instanceof HttpStatusCodeElement) {
				logElements[i] = new ExtendedHttpStatusCodeElement();
			}
		}//TESTED
		
		_savedMessage = null;
		super.invoke(request, response);
		_isError = (response.getStatus() >= 400);
		
		if (!_isError) {
			if (null != request.getAttribute("infinit.e.log")) { // an infint.e servlet has requested logging
				_isError = true;
			}//TOTEST
		}
		else if (null != _errorOverrideRegex){ // check vs error override 
			if (_errorOverrideRegex.matcher(request.getDecodedRequestURI()).find()) {
				_isError = false;
			}//TESTED
		}
		if (_isError && (null != _savedMessage)) {
			log(_savedMessage);
		}//TESTED
		_savedMessage = null;
	}//TESTED
	
	@Override
	public void setCondition(String condition) {
		if (null != condition) {
			_errorOverrideRegex = Pattern.compile(condition, Pattern.CASE_INSENSITIVE);
		}
		//(don't pass on)
	}//TESTED
	
	@Override
	public void log(String message) {
		if (null != _savedMessage) {
			super.log(message);
		}
		else {
			_savedMessage = message;
		}
	}//TESTED
	
	// Override request element and do sanitization on it...
	protected class SanitizedRequestElement implements AccessLogElement {
		public void addElement(StringBuffer buf, Date date, Request request,
				Response response, long time) {
			if (request != null) {
				buf.append(request.getMethod());
				buf.append(' ');
				buf.append(sanitize(request.getRequestURI()));
				// (don't show the query string)
				//if (request.getQueryString() != null) {
				//	buf.append('?');
				//	buf.append(request.getQueryString());
				//}
				buf.append(' ');
				buf.append(request.getProtocol());
			} else {
				buf.append("- - ");
			}
		}
	}//(TESTED)
	protected class SanitizedRequestURIElement implements AccessLogElement {
		public void addElement(StringBuffer buf, Date date, Request request,
				Response response, long time) {
			if (request != null) {
				buf.append(sanitize(request.getRequestURI()));
			} else {
				buf.append('-');
			}
		}
	}//(copy and paste from AccessLogValve)
	
	static protected String sanitize(String url) {
		if (url.startsWith("/api/auth/login/")) { // remove password
			int index2 = url.indexOf('/', 17); // (the end of the username)
			if (index2 >= 0) {
				url = url.substring(0, index2);
			}
		}
		return url;
	}//TESTED
	
	protected class ExtendedHttpStatusCodeElement implements AccessLogElement {
		public void addElement(StringBuffer buf, Date date, Request request,
				Response response, long time)
		{
			if (response != null) {
				if (null == response.getHeader("X-infinit.e.log")) {
					buf.append(response.getStatus());
				}
				else {
					buf.append("550");
				}
			} else {
				buf.append('-');
			}
		}
	}//TOTEST
}