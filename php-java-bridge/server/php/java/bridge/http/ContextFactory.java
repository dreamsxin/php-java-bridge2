/*-*- mode: Java; tab-width:8 -*-*/

package php.java.bridge.http;

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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;

import php.java.bridge.ISession;
import php.java.bridge.JavaBridge;
import php.java.bridge.JavaBridgeClassLoader;
import php.java.bridge.SessionFactory;
import php.java.bridge.Util;


/**
 * Create session, jsr223 contexts.<p>
 * The ContextFactory may keep a promise (a "proxy") which one may evaluate to a
 * session reference (for PHP/JSP session sharing), and/or it may
 * reference a "half-executed" bridge for local channel re-directs (for
 * "high speed" communication links). 
 *
 *<p>
 * A unique context
 * instance should be created for each request and destroyed when the request
 * is done.
 * </p>
 * <p>
 * Clients of the PHP clients may attach additional data and run with
 * a customized ContextFactory by using the visitor pattern, 
 * see {@link #accept(IContextFactoryVisitor)}.
 * </p>
 * <p>
 * The string ID of the instance should be passed to the client, which may
 * pass it back together with the getSession request or the "local
 * channel re-direct". If the former happens, we invoke the promise
 * and return the session object to the client. Different promises can
 * evaluate to the same session object.  For local channel re-directs
 * the ContextFactory is given to a ContextRunner which handles the
 * local channel communication.
 * </p>
 * <p>
 * When a php client is not interested in a context for 5 seconds (checked every 10 minutes), the
 * context is destroyed: a) switching from the HTTP tunnel to the local channel of the
 * ContextRunner or b) switching from the fresh context created by the client of the PHP client to the
 * recycled, persistent context, costs only one round-trip. The time for such a context switch 
 * is usually much less than 10ms unless either the php client or the client that waits for the php client 
 * is traced. If 5 seconds is not enough during debugging, change the ORPHANED_TIMEOUT.
 * </p>
 * <p>
 * In a shared environment with k web contexts there can be up to n*k active JavaBridge/ContextFactory instances 
 * (where n is the number of active php clients). All ContextFactories are kept in a shared, per-loader
 * map. But the map can only be accessed via {@link #get(String, ICredentials)}, which checks if the ContextFactory
 * belongs to the same ContextServer.
 * </p>
 * @see php.java.servlet.ServletContextFactory
 * @see php.java.bridge.http.ContextServer
 * @see php.java.bridge.SessionFactory#TIMER_DURATION
 */
public final class ContextFactory extends SessionFactory implements IContextFactory {

    /** The credentials provided by the web context, usually this ContextFactory. */
    public static interface ICredentials {}

    /** This context name can be used when a ContextFactory is used 
     * outside of a servlet environment */
    public static final String EMPTY_CONTEXT_NAME = "";
    
    /** Use this if you don't care about security. This is used by the {@link php.java.bridge.http.SocketContextServer} */
    public static final ICredentials NO_CREDENTIALS = new ICredentials() {};

    static {
       getTimer().addJob(new Runnable() {public void run() {destroyOrphaned();}});
    }

    private static final HashMap contexts = new HashMap();
    private static final HashMap liveContexts = new HashMap();
    
    private static final long ORPHANED_TIMEOUT = 5000; // every 5 seconds

    private boolean invalid=false;

    private JavaBridge bridge = null;

    private String id;
    private long timestamp;

    private ICredentials contextServer = null;
    private IContextFactory visitor; 

    private static long counter = 0;
    private static synchronized String addNext(String webContext, ContextFactory thiz) {
        String id;
        counter++;
        try {webContext=URLEncoder.encode(webContext, Util.DEFAULT_ENCODING);} catch (UnsupportedEncodingException e) {Util.printStackTrace(e);}
        contexts.put(id=Long.toHexString(counter)+"@"+webContext, thiz);
        return id;
    }
    private static synchronized void remove(String id) {
	Object ob = liveContexts.remove(id);
        if(Util.logLevel>4) Util.logDebug("removed context: " + ob + ", # of contexts: " + contexts.size());
    }
    private static synchronized ContextFactory moveContext(String id) {
        Object o;
        if((o = liveContexts.get(id))!=null) return (ContextFactory)o;
        if((o = contexts.remove(id))!=null) { liveContexts.put(id, o); return (ContextFactory)o; }
        return null;
    }
    public ContextFactory(String webContext) {
      super();
      timestamp = System.currentTimeMillis();
      id=addNext(webContext, this);
      if(Util.logLevel>4) Util.logDebug("new context: " + id + ", # of contexts: " + contexts.size());
    }

    /**
     * Create a new ContextFactory and add it to the list of context factories kept by this classloader.
     * @param webContext The current web context, use ContextFactory.EMPTY_CONTEXT 
     * if servlet web contexts are not available, config.getServletContext().getRealPath("") otherwise.
     * @return The created ContextFactory.
     * @see php.java.bridge.http.ContextFactory#get(String, ICredentials)
     */
    public static IContextFactory addNew(String webContext) {
    	ContextFactory factory = new ContextFactory(webContext);
    	factory.visitor = factory;
    	return factory;
    }
    /**
     * Create a new simple ContextFactory (a factory which creates an emulated JSR223 context) and add it 
     * to the list of context factories kept by this classloader.
     * @return The created ContextFactory.
     * @see php.java.bridge.http.ContextFactory#get(String, ICredentials)
     */
    public static IContextFactory addNew() {
    	return new SimpleContextFactory(EMPTY_CONTEXT_NAME);
    }
    
    private void init(ICredentials cred) {
	contextServer = cred;
	invalid = false;
    }
   /**
     * Only for internal use.
     *  
     * Returns the context factory associated with the given <code>id</code>
     * @param id The ID
     * @param server Your context server.
     * @return The ContextFactory or null.
     * @see #addNew(String)
     * @throws SecurityException if id belongs to a different ContextServer.
     */
    /* See PhpJavaServlet#contextServer, http.ContextRunner#contextServer and 
     * JavaBridgeRunner#ctxServer. */
    public static IContextFactory get(String id, ICredentials server) {
        if(server == null) throw new NullPointerException("server");

        ContextFactory factory = moveContext(id); 
        if(factory==null) return null;
        
        if(factory.contextServer==null) factory.init(server);
        if(factory.contextServer != server) 
            throw new SecurityException("Illegal access");
        return factory.visitor;
    }
    /* (non-Javadoc)
     * @see php.java.bridge.http.IContextFactory#recycle(php.java.bridge.http.ContextFactory)
     */
    public void recycle(ContextFactory target) {}
    /* (non-Javadoc)
     * @see php.java.bridge.http.IContextFactory#recycle(java.lang.String)
     */
    public void recycle(String id) throws SecurityException {
        IContextFactory target = ((ContextFactory)contexts.get(id)).visitor;
        target.removeOrphaned();
        target.recycle(this);
    }
    /**
     * Recycle the factory for new reqests.
     */    
    public synchronized void recycle() {
	super.recycle();
	visitor.finishContext();
	contextServer=null;
	invalid=true;
	notify();
    }
    
    /* (non-Javadoc)
     * @see php.java.bridge.http.IContextFactory#destroy()
     */
    public synchronized void destroy() {
	if(Util.logLevel>4) Util.logDebug("context finished: " +getId());
	remove(getId());
	bridge=null;
	invalid=true;
	notify();
   }
    
    /**
     * @deprecated
     * @see #destroyAll()
     */
    public static void removeAll() {
        destroyAll();
    }
    private boolean isInitialized() {
        return (contextServer!=null);
    }
    /**
     * Orphaned contexts may appear when the PHP client has no interest in the new context
     * that the client of the PHP client has allocated, for example when
     * the php instance already has a connection and there is no need access the new context
     * (php script contains no "java_session" and no "java_context"). 
     * 
     * Orphaned contexts will be automatically removed after 5 seconds. -- Even 10ms would 
     * be sufficient because contexts only bridge the gap between a) the first statement executed
     * via the HTTP tunnel and the second statement executed via the ContextRunner or 
     * b) they pass initial information from a client of a PHP client to the PHP script. If one round-trip
     * costs more than 10ms, then there's something wrong with the connection. 
     */
    private static synchronized void destroyOrphaned() {
	long timestamp = System.currentTimeMillis();
	
        for(Iterator ii=contexts.values().iterator(); ii.hasNext();) {
	    ContextFactory ctx = ((ContextFactory)ii.next());
	    if(ctx.timestamp+ORPHANED_TIMEOUT<timestamp) {
	        synchronized(ctx) {
	            ctx.invalid=true;
	            ctx.notify();
	        }
	        if(Util.logLevel>4) Util.logDebug("Orphaned context: " + ctx.getId() + " removed.");
	        ii.remove();
	    }
	}        
    }
    /**
     * Remove all context factories from the classloader.
     * May only be called by the ContextServer.
     * @see php.java.bridge.http.ContextServer
     */
    public static synchronized void destroyAll() {
	destroyOrphaned();
	for(Iterator ii=contexts.values().iterator(); ii.hasNext();) {
	    ContextFactory ctx = ((ContextFactory)ii.next());
	    synchronized(ctx) {
		ctx.invalid=true;
		ctx.notify();
	    }
	    ii.remove();
	}
    }
    
    /* (non-Javadoc)
     * @see php.java.bridge.http.IContextFactory#waitFor()
     */
    public synchronized void waitFor() throws InterruptedException {
    	if(!invalid) wait();
    }
    /* (non-Javadoc)
     * @see php.java.bridge.http.IContextFactory#waitFor()
     */
    public synchronized void waitFor(long timeout) throws InterruptedException {
	if(!invalid) wait(timeout);
    }
    /* (non-Javadoc)
     * @see php.java.bridge.http.IContextFactory#getId()
     */
    public String getId() { 
	return id; 
    }
    /* (non-Javadoc)
     * @see php.java.bridge.http.IContextFactory#toString()
     */
    public String toString() {
	return "Context# " +id + ", isInitialized: " + isInitialized();
    }
    /**
     * Returns the context.
     * @return The context or null.
     */
    public IContext getContext() {
	return visitor.getContext();
    }
	
    /**
     * Set the Context into this factory.
     * Should be called by Context.addNew() only.
     * @param context The context.
     * @see #addNew(String)
     */
    public void setContext(IContext context) {
        visitor.setContext(context);
    }
    /* (non-Javadoc)
     * @see php.java.bridge.http.IContextFactory#setBridge(php.java.bridge.JavaBridge)
     */
    public void setBridge(JavaBridge bridge) {
	this.bridge = bridge;
    }
    /* (non-Javadoc)
     * @see php.java.bridge.http.IContextFactory#getBridge()
     */
    public JavaBridge getBridge() {
        if(bridge != null) return bridge;
        setBridge(new JavaBridge());
        bridge.setClassLoader(new JavaBridgeClassLoader());
        bridge.setSessionFactory(this);
	return bridge;
    }
    private void setVisitor(IContextFactory newVisitor) {
        visitor = newVisitor;
    }
    /**
     * Use this method to attach a visitor to the ContextFactory.
     * @param visitor The custom ContextFactory
     */
    public void accept(IContextFactoryVisitor visitor) {
        visitor.visit(this);
        setVisitor(visitor);
    }
    
    /**
     * Return a simple session which cannot be shared with JSP
     * @param name The session name
     * @param clientIsNew true, if the client wants a new session
     * @param timeout expires in n seconds
     */
    public ISession getSimpleSession(String name, boolean clientIsNew, int timeout) {
        return super.getSession(name, clientIsNew, timeout);
    }
    /**
     * Return a standard session, shared with JSP
     * @param name The session name
     * @param clientIsNew true, if the client wants a new session
     * @param timeout expires in n seconds
     */
    public ISession getSession(String name, boolean clientIsNew, int timeout) {
	return visitor.getSession(name, clientIsNew, timeout);
    }
    public synchronized void removeOrphaned() {
	Object ob = contexts.remove(id);
        if(Util.logLevel>4) Util.logDebug("removed empty context: " + ob + ", # of contexts: " + contexts.size());
	invalid=true;
	notify();
    }
    /** Called by recycle at the end of the script */
    public void finishContext() {}
}