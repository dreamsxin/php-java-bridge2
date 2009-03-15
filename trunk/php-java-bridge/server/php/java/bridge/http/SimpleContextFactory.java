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

import java.net.URI;
import java.net.URISyntaxException;

import php.java.bridge.ISession;
import php.java.bridge.JavaBridge;
import php.java.bridge.NotImplementedException;
import php.java.bridge.Util;
import php.java.bridge.SimpleJavaBridgeClassLoader;


/**
 * Base of a set of visitors which can extend the standard ContextFactory.
 * 
 * Instances of this class are thrown away at the end of the request. 
 *
 * @see php.java.servlet.ServletContextFactory
 * @see php.java.script.PhpScriptContextFactory
 */
public class SimpleContextFactory implements IContextFactoryVisitor {
    
    /**
     * The session object
     */
    protected ISession session;
    
    /**
     * The visited ContextFactory
     */
    protected ContextFactory visited;

    /**
     * The jsr223 context or the emulated jsr223 context.
     */
    protected IContext context;
    
    private boolean isContextRunnerRunning = false;
    private boolean isValid = true;
    private boolean isManaged;
    
    protected SimpleContextFactory(String webContext, boolean isManaged) {
	this.isManaged = isManaged;
  	visited = new ContextFactory(webContext, isManaged);
  	visited.accept(this);
    	setClassLoader(Util.getContextClassLoader());
    }
    
    /**{@inheritDoc}*/
   public void recycle(String id) {
        visited.recycle(id);
    }

   /**{@inheritDoc}*/
    public void destroy() {
        visited.destroy();
        session = null;
    }
    
    /**{@inheritDoc}*/
    public synchronized void invalidate() {
	    isValid = false;
	    notifyAll(); // notify waitForContextRunner() and waitFor()
    }
    /**{@inheritDoc}*/
    public synchronized void initialize () {
	isContextRunnerRunning = true;
        getContext().setAttribute(IContext.JAVA_BRIDGE, getBridge(), IContext.ENGINE_SCOPE);
    }
    /**
     * Wait for the context factory to finish, then release
     * @throws InterruptedException 
     */
    public synchronized void releaseManaged() throws InterruptedException {
	if(Util.logLevel>4) Util.logDebug("contextfactory: servlet is waiting for ContextRunner " +System.identityHashCode(this));
	if (isContextRunnerRunning) {
	    while(isValid) wait();
	    if(Util.logLevel>4) Util.logDebug("contextfactory: servlet done waiting for ContextRunner " +System.identityHashCode(this));
	} else {
	    if(Util.logLevel>4) Util.logDebug("contextfactory: servlet done w/o ContextRunner " +System.identityHashCode(this));
	    if (isManaged)
		destroy();
	    else
		release();
	}
    }
    /**
     * Wait for the context factory to finish. 
     * @param timeout The timeout
     * @throws InterruptedException 
     */
    public synchronized void waitFor(long timeout) throws InterruptedException {
	if(Util.logLevel>4) Util.logDebug("contextfactory: servlet waitFor() ContextFactory " +System.identityHashCode(this) + " for " +timeout+" ms");
	if (isValid) wait(timeout);
	if(Util.logLevel>4) Util.logDebug("contextfactory: servlet waitFor() ContextRunner " +System.identityHashCode(this));
	if (isContextRunnerRunning && isValid) wait();
	if(Util.logLevel>4) Util.logDebug("contextfactory: servlet done waitFor() ContextRunner " +System.identityHashCode(this));
    }    
    /**{@inheritDoc}*/
    public String getId() { 
        return visited.getId();
    }
    /**{@inheritDoc}*/
    public String toString() {
	return "ContextFactory: " + visited + ", SimpleContextFactory: " +getClass() + ", current loader: " + loader;
    }
    /**
     * Create a new context. The default implementation
     * creates a dummy context which emulates the JSR223 context.
     * @return The context.
     */
    protected IContext createContext() {
      return new Context();
    }
    /**{@inheritDoc}*/
    public IContext getContext() {
	if(context==null) setContext(createContext());
        return context;
    }

    /**{@inheritDoc}*/
    public boolean isNew () {
	return visited.isNew();
    }
    /**{@inheritDoc}*/
    public JavaBridge getBridge() {
        return visited.getBridge();
    }
    /**{@inheritDoc}*/
    public void visit(ContextFactory visited) {
        this.visited=visited;
    }
    /**{@inheritDoc}*/
    public ISession getSession(String name, boolean clientIsNew, int timeout) {
	if(session != null) return session;
	return session = visited.getSimpleSession(name, clientIsNew, timeout);
    }
    /**{@inheritDoc}*/
    public ISession getSession(boolean clientIsNew, int timeout) {
	return visited.getSimpleSession(clientIsNew, timeout);
    }
    /**{@inheritDoc}*/
    public void setContext(IContext context) {
        this.context = context;
    }
    /**{@inheritDoc}*/
    public void release() {
        visited.release();
    }

    /**
     * Called by recycle at the end of the script
     */
    public void recycle() {
	visited.recycle();
    }

    private ClassLoader loader;
    /**
     * Return the current class loader.
     * @return the current DynamicJavaBridgeClassLoader
     */
    public ClassLoader getClassLoader() {
	return loader;
    }

    /**
     * Return the JavaBridgeClassLoader, which wraps the
     * DynamicJavaBridgeClassLoader
     * @return The class loader
     */
    public SimpleJavaBridgeClassLoader getJavaBridgeClassLoader() {
	return visited.getJavaBridgeClassLoader();
    }

    /**
     * Set the current class loader
     * @param loader The DynamicJavaBridgeClassLoader
     */
    public void setClassLoader(ClassLoader loader) {
	if(loader==null) 
	    throw new NullPointerException("loader");
	this.loader = loader;
    }

    /**{@inheritDoc}*/
    public String getSocketName() {
	throw new NotImplementedException("Use the JSR 223 API or a servlet environment instead");
    }
    /**{@inheritDoc}*/
    public String getRedirectString() {
	return getRedirectString("/JavaBridge");
    }
    /**{@inheritDoc}*/
    public String getRedirectString(String webPath) {
	try {
	    StringBuffer buf = new StringBuffer();
	    buf.append(getSocketName());
	    buf.append("/");
	    buf.append(webPath);
	    URI uri = new URI("h:127.0.0.1", buf.toString(), null);
	    return (uri.toASCIIString()+".phpjavabridge");
	} catch (URISyntaxException e) {
	    Util.printStackTrace(e);
        }
	StringBuffer buf = new StringBuffer("h:127.0.0.1:");
	buf.append(getSocketName()); 
	buf.append('/');
	buf.append(webPath);
	buf.append(".phpjavabridge");
	return buf.toString();
    }
}
