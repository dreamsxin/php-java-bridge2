/*-*- mode: Java; tab-width:8 -*-*/

package php.java.servlet;

/*
 * Copyright (C) 2003-2007 Jost Boekemeier
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


import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import php.java.bridge.ISession;
import php.java.bridge.http.IContext;

/**
 * Create session contexts for servlets.<p> In addition to the
 * standard ContextFactory this manager keeps a reference to the
 * HttpServletRequest.
 *
 * @see php.java.bridge.http.ContextFactory
 * @see php.java.bridge.http.ContextServer
 */
public class RemoteServletContextFactory extends ServletContextFactory {
    protected RemoteServletContextFactory(Servlet servlet, ServletContext ctx, HttpServletRequest proxy, HttpServletRequest req, HttpServletResponse res) {
	super(servlet, ctx, proxy, req, res);
    }

    /**
     * Set the HttpServletRequest for session sharing.
     *  @param req The HttpServletRequest
     */
    protected void setSessionFactory(HttpServletRequest req) {
    	this.proxy = req;
    }

    public ISession getSession(String name, boolean clientIsNew, int timeout) {
	if(session!=null) return session;
	 // if name != null return a "named" php session which is not shared with jsp
	if(name!=null) return session = visited.getSimpleSession(name, clientIsNew, timeout);
	
    	if(proxy==null) throw new NullPointerException("This context "+getId()+" doesn't have a session proxy.");
	return session = new RemoteHttpSessionFacade(this, kontext, proxy, res, clientIsNew, timeout);
    }
    
    /**
     * Create and add a new ContextFactory.
     * @param req The HttpServletRequest
     * @param res The HttpServletResponse
     * @return The created ContextFactory
     */
    public static ServletContextFactory addNew(Servlet servlet, ServletContext kontext, HttpServletRequest proxy, HttpServletRequest req, HttpServletResponse res) {
        RemoteServletContextFactory ctx = new RemoteServletContextFactory(servlet, kontext, proxy, req, res);
    	return ctx;
    }
    
    /**
     * Return an emulated JSR223 context.
     * @return The context.
     * @see php.java.servlet.Context
     */
    public IContext createContext() {
	IContext ctx = new Context(kontext, req, res);
	ctx.setAttribute(IContext.SERVLET_CONTEXT, kontext, IContext.ENGINE_SCOPE);
	ctx.setAttribute(IContext.SERVLET_CONFIG, servlet.getServletConfig(), IContext.ENGINE_SCOPE);
	ctx.setAttribute(IContext.SERVLET, servlet, IContext.ENGINE_SCOPE);

	ctx.setAttribute(IContext.SERVLET_REQUEST, new RemoteHttpServletRequest(this, req), IContext.ENGINE_SCOPE);
	ctx.setAttribute(IContext.SERVLET_RESPONSE, new RemoteHttpServletResponse(), IContext.ENGINE_SCOPE);
	
	return ctx;
    }
    /**
     * Wait for the context factory to finish. 
     */
    public void waitFor() throws InterruptedException {
        visited.waitFor();
    }
    /**
     * Wait for the context factory to finish. 
     */
    public void waitFor(long timeout) throws InterruptedException {
	visited.waitFor(timeout);
    }    
}
