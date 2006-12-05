/*-*- mode: Java; tab-width:8 -*-*/

package php.java.servlet;

/*
 * Copyright (C) 2006 Jost Boekemeier
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER(S) OR AUTHOR(S) BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import php.java.bridge.ISession;

/**
 * Wraps the J2EE session interface
 */
public class HttpSessionFacade implements ISession {

    private HttpSession session;
    private int timeout;
    private HttpServletRequest req=null;
    private HttpSession sessionCache=null;
    private boolean isNew;
    private ServletContext ctx;
    private HttpServletResponse res;
    
    private HttpSession getSession() {
	if(sessionCache!=null) return sessionCache;
	sessionCache = session;
	sessionCache.setMaxInactiveInterval(timeout);
	return sessionCache;
    }
    protected HttpSessionFacade (ServletContext ctx, HttpServletRequest req, HttpServletResponse res, int timeout) {
	this.session = req.getSession();
	this.req = req;
	this.ctx = ctx;
	this.res = res;
	this.timeout = timeout;
	this.isNew = session.isNew();
    }
    
    /**
     * Returns the HttpServletRequest
     * @return The HttpServletRequest.
     */
    public HttpServletRequest getHttpServletRequest() {
    	return this.req;
    }
    
    /**
     * Returns the ServletContext
     * @return The ServletContext.
     */
    public ServletContext getServletContext() {
        return this.ctx;
    }
    
    /**
     * Returns the ServletResponse
     * @return The ServletResponse.
     */
    public HttpServletResponse getHttpServletResponse() {
        return this.res;
    }
    /**@inheritDoc*/
    public Object get(Object ob) {
	return getSession().getAttribute(String.valueOf(ob));
    }

    /**@inheritDoc*/
    public void put(Object ob1, Object ob2) {
	getSession().setAttribute(String.valueOf(ob1), ob2);
    }

    /**@inheritDoc*/
   public Object remove(Object ob) {
	String key = String.valueOf(ob);
	Object o = getSession().getAttribute(key);
	if(o!=null)
	    getSession().removeAttribute(key);
	return o;
    }

   /**@inheritDoc*/
    public void setTimeout(int timeout) {
	getSession().setMaxInactiveInterval(timeout);
    }

    /**@inheritDoc*/
    public int getTimeout() {
	return getSession().getMaxInactiveInterval();
    }

    /**@inheritDoc*/
    public int getSessionCount() {
	return -1;
    }

    /**@inheritDoc*/
    public boolean isNew() {
	return isNew;
    }

    /**@inheritDoc*/
    public void destroy() {
	getSession().invalidate();
    }

    /**@inheritDoc*/
    public void putAll(Map vars) {
	for(Iterator ii = vars.keySet().iterator(); ii.hasNext();) {
	    Object key = ii.next();
	    Object val = vars.get(key);
	    put(key, val);
	}
    }

    /**@inheritDoc*/
    public Map getAll() {
	HttpSession session = getSession();
	HashMap map = new HashMap();
	for(Enumeration ee = session.getAttributeNames(); ee.hasMoreElements();) {
	    Object key = ee.nextElement();
	    Object val = get(key);
	    map.put(key, val);
	}
	return map;
    }
    /**@inheritDoc*/
    public long getCreationTime() {
      return getSession().getCreationTime();
    }
    /**@inheritDoc*/
    public long getLastAccessedTime() {
      return getSession().getLastAccessedTime();
    }
}