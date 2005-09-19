/*-*- mode: Java; tab-width:8 -*-*/

package php.java.servlet;

import java.util.Hashtable;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import php.java.bridge.ISession;
import php.java.bridge.JavaBridge;
import php.java.bridge.Util;

/**
 * The context may a) keep a promise (a "proxy") which one may evaluate to a
 * session reference (for PHP/JSP session sharing), and/or b) it may
 * reference a "half-executed" bridge for local channel re-directs (for
 * "high speed" communication links).
 *
 * Context instances are kept in an array with no more than 65535
 * entries, after 65535 context creations the context is destroyed if
 * this did not happen explicitly.  A unique [1..65535] context
 * instance should be created for each request and destroyed when the request
 * is done.  

 * The string ID of the instance should be passed to the client, which may
 * pass it back together with the getSession request or the "local
 * channel re-direct". If the former happens, we invoke the promise
 * and return the session object to the client. Different promises can
 * evaluate to the same session object.  For local channel re-directs,
 * the Context is given to a ContextRunner, which handles the
 * local channel communication.
 * @see ContextRunner
 * @see SocketRunner
 */
class Context extends JavaBridge.SessionFactory {
    static final Hashtable contexts = new Hashtable();
    HttpServletRequest sessionPromise;
    JavaBridge bridge;
	
    static short count=0; // If a context survives more than 65535
			  // context creations, that context will be
			  // destroyed.
    String id;
    private static synchronized int getNext() {
	if(++count==0) ++count;
	return count;
    }
    private Context(HttpServletRequest req) {
	this.sessionPromise = req;
	id=String.valueOf(0xFFFF&getNext());
    }
    public void setSession(HttpServletRequest req) {
    	if(this.sessionPromise!=null) throw new IllegalStateException("This context already has a session proxy.");
    	this.sessionPromise = req;
    }
    private Context() {
    	this(null);
    }
    public ISession getSession(String name, boolean clientIsNew, int timeout) {
    	if(sessionPromise==null) throw new NullPointerException("This context "+getId()+" doesn't have a session proxy.");
	return new HttpSessionFacade(sessionPromise.getSession(), timeout);
    }
    public static Context addNew(HttpServletRequest req) {
     	Context ctx = new Context(req);
    	Object old = contexts.put(ctx.getId(), ctx);
    	if(old!=null) {
    	    ((Context)old).remove();
    	    if(Util.logLevel>2) Util.logError("Removed stale context: " +old);
    	}
    	return ctx;
    }
    public static Context get(String id) {
   	return (Context)contexts.get(id);
    }
    
    private boolean removed=false;
    public synchronized void remove() {
	contexts.remove(getId());
	removed=true;
	bridge=null;
	sessionPromise=null;
	notify();
    }
    public static void removeAll() {
	for(Iterator ii=contexts.values().iterator(); ii.hasNext();) {
	    Context ctx = ((Context)ii.next());
	    synchronized(ctx) {
		ctx.removed=true;
		ctx.bridge=null;
		ctx.sessionPromise=null;
		ctx.notify();
	    }
	    ii.remove();
	}
    }
    public synchronized void waitFor() throws InterruptedException {
    	if(!removed) wait();
    }
    public String getId() { 
	return id; 
    }
    public String toString() {
	return "Context# " +id + ", has session proxy: " +sessionPromise==null?"false":"true";
    }
}