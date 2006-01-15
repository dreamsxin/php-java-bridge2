/*-*- mode: Java; tab-width:8 -*-*/

package php.java.servlet;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import php.java.bridge.ISession;

/**
 * Manage session contexts for servlets.<p>
 * In addition to the standard ContextFactory this manager keeps a reference to the HttpServletRequest.
 * @see php.java.bridge.http.ContextFactory
 * @see php.java.bridge.http.ContextServer
 */
public class ContextFactory extends php.java.bridge.http.ContextFactory {
    private HttpServletRequest proxy;
    private HttpServletResponse res;
    private ServletContext ctx;
	
    protected ContextFactory(ServletContext ctx, HttpServletRequest proxy, HttpServletRequest req, HttpServletResponse res) {
    	super();
    	this.ctx = ctx;
    	this.proxy = proxy;
    	this.res = res;
    }
    
    /**
     * Set the HttpServletRequest for session sharing. 
     * @param req The HttpServletRequest
     * @throws IllegalStateException When the ContextFactory was created with a HttpServletRequest or when this method was called twice.
     */
    public void setSession(HttpServletRequest req) {
    	if(this.proxy!=null) throw new IllegalStateException("This context already has a session proxy.");
    	this.proxy = req;
    }
    public ISession getSession(String name, boolean clientIsNew, int timeout) {
    	if(proxy==null) throw new NullPointerException("This context "+getId()+" doesn't have a session proxy.");
	return new HttpSessionFacade(ctx, proxy, res, timeout);
    }
    
    /**
     * Create and add a new ContextFactory.
     * @param req The HttpServletRequest
     * @param res The HttpServletResponse
     * @return The created ContextFactory
     */
    public static ContextFactory addNew(ServletContext kontext, HttpServletRequest proxy, HttpServletRequest req, HttpServletResponse res) {
    	ContextFactory ctx = new ContextFactory(kontext, proxy, req, res);
    	ctx.add();
    	ctx.setContext(new Context(kontext,req,res));
    	return ctx;
    }	

    
    public synchronized void remove() {
    	super.remove();
    	proxy=null;
    }
    public String toString() {
	return super.toString() + ", has proxy: " +(proxy==null?"false":"true");
    }
}
