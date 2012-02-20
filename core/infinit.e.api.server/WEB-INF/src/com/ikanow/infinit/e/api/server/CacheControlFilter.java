package com.ikanow.infinit.e.api.server;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

// This class is only referenced in api.war's web.xml, to append cache-control headers
// (Without which IE caches service requests)

public class CacheControlFilter implements Filter {

    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletResponse resp = (HttpServletResponse) response;
        resp.setHeader("Expires", "0");
        resp.setHeader("Last-Modified", new Date().toString());
        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0, post-check=0, pre-check=0");
        resp.setHeader("Pragma", "no-cache");

        chain.doFilter(request, response);
    }

    public void destroy() {
    }

    public void init(FilterConfig arg0) throws ServletException {
    }
}
