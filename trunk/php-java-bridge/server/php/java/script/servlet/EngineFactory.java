/*-*- mode: Java; tab-width:8 -*-*/

package php.java.script.servlet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import php.java.bridge.Util;
import php.java.servlet.CGIServlet;
import php.java.servlet.RequestListener;


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

/**
 * Create JSR 223 script engines from a servlet context.
 * @see php.java.servlet.ContextLoaderListener
 * @see php.java.script.servlet.InvocablePhpServletScriptEngine
 * @see php.java.script.servlet.PhpServletScriptEngine
 *
 */
public final class EngineFactory {
    /** The key used to store the factory in the servlet context */
    public static final String ROOT_ENGINE_FACTORY_ATTRIBUTE = EngineFactory.class.getName()+".ROOT";
    private boolean hasCloseable;
    /**
     * Create a new EngineFactory
     */
    public EngineFactory () {
	try {
	    Class.forName("java.io.Closeable");
	    hasCloseable = true;
	} catch (ClassNotFoundException e) {
	    hasCloseable = false;
	}
    }
    private Object getScriptEngine(Servlet servlet, 
		     ServletContext ctx, 
		     HttpServletRequest req, 
		     HttpServletResponse res) throws MalformedURLException {
	URL url = new java.net.URL((req.getRequestURL().toString()));
	return hasCloseable ? 
	    EngineFactoryHelper.newCloseablePhpServletScriptEngine(servlet, ctx, req, res, url.getProtocol(), url.getPort()):
	    new PhpServletScriptEngine(servlet, ctx, req, res, url.getProtocol(), url.getPort());
    }
    private Object getInvocableScriptEngine(Servlet servlet, 
		     ServletContext ctx, 
		     HttpServletRequest req, 
		     HttpServletResponse res) throws MalformedURLException, URISyntaxException {
	URL url = new java.net.URL((req.getRequestURL().toString()));
	return hasCloseable ? 
	    EngineFactoryHelper.newCloseableInvocablePhpServletScriptEngine(servlet, ctx, req, res, url.getProtocol(), url.getPort()) :
	    new InvocablePhpServletScriptEngine(servlet, ctx, req, res, url.getProtocol(), url.getPort());
    }
    private Object getInvocableScriptEngine(Servlet servlet, 
	     ServletContext ctx, 
	     HttpServletRequest req, 
	     HttpServletResponse res, String protocol, int port) throws MalformedURLException, URISyntaxException {
	return hasCloseable ?
	    EngineFactoryHelper.newCloseableInvocablePhpServletLocalHttpServerScriptEngine(servlet, ctx, req, res, protocol, port):
	    new InvocablePhpServletLocalHttpServerScriptEngine(servlet, ctx, req, res, protocol, port);
    }
    private Object getInvocableScriptEngine(Servlet servlet, 
	     ServletContext ctx, 
	     HttpServletRequest req, 
	     HttpServletResponse res, String protocol, int port, String proxy) throws MalformedURLException, URISyntaxException {
	return hasCloseable ?
		EngineFactoryHelper.newCloseableInvocablePhpServletLocalHttpServerScriptEngine(servlet, ctx, req, res, protocol, port, proxy) :
		new InvocablePhpServletLocalHttpServerScriptEngine(servlet, ctx, req, res, protocol, port, proxy);
    }

    /** 
     * Get an engine factory from the servlet context
     * @param ctx The servlet context
     * @return the factory or null
     */
    static EngineFactory getEngineFactory(ServletContext ctx) {
	EngineFactory attr = (EngineFactory) 
	    ctx.getAttribute(php.java.script.servlet.EngineFactory.ROOT_ENGINE_FACTORY_ATTRIBUTE);
	return attr;
    }
    /**
     * Get an engine factory from the servlet context
     * @param ctx The servlet context
     * @return the factory
     * @throws IllegalStateException
     */
    static EngineFactory getRequiredEngineFactory(ServletContext ctx) throws IllegalStateException {
	EngineFactory attr = getEngineFactory (ctx);
	if (attr==null) 
	    throw new IllegalStateException("No EngineFactory found. Have you registered a listener?");
	return attr;
    }

    /**
     * Get a PHP JSR 223 ScriptEngine from the servlet context.
     *
     * Example:<br>
     * <blockquote>
     * <code>
     * ScriptEngine scriptEngine = EngineFactory.getPhpScriptEngine(this, application, request, response);<br>
     * scriptEngine.eval(reader);<br>
     * reader.close();<br>
     * </code>
     * </blockquote>
     * @param servlet the servlet
     * @param ctx the servlet context
     * @param req the request
     * @param res the response
     * @return the PHP JSR 223 ScriptEngine, an instance of the {@link PhpServletScriptEngine}
     * @throws Exception 
     * @throws MalformedURLException
     * @throws IllegalStateException
     */
    public static javax.script.ScriptEngine getPhpScriptEngine (final Servlet servlet, 
								final ServletContext ctx, 
								final HttpServletRequest req, 
								final HttpServletResponse res) throws 
								    Exception {
	return (ScriptEngine) AccessController.doPrivileged(new PrivilegedExceptionAction(){ 
	    public Object run() throws Exception {
		return (javax.script.ScriptEngine)EngineFactory.getRequiredEngineFactory(ctx).getScriptEngine(servlet, ctx, req, res);
	    }
	});
    }
    /**
     * Get a PHP JSR 223 ScriptEngine which implements the Invocable interface from the servlet context.
     *
     * Example:<br>
     * <blockquote>
     * <code>
     * ScriptEngine scriptEngine = EngineFactory.getInvocablePhpScriptEngine(this, application, request, response);<br>
     * ...<br>
     * scriptEngine.eval(reader);<br>
     * reader.close ();<br>
     * Invocable invocableEngine = (Invocable)scriptEngine;<br>
     * invocableEngine.invoceFunction("phpinfo", new Object[]{});<br>
     * ...<br>
     * scriptEngine.eval ((Reader)null);<br>
     * </code>
     * </blockquote>
     * @param servlet the servlet
     * @param ctx the servlet context
     * @param req the request
     * @param res the response
     * @return the invocable PHP JSR 223 ScriptEngine, an instance of the {@link InvocablePhpServletScriptEngine}
     * @throws Exception 
     * @throws MalformedURLException
     * @throws IllegalStateException
     */
    public static javax.script.ScriptEngine getInvocablePhpScriptEngine (final Servlet servlet, 
									 final ServletContext ctx, 
									 final HttpServletRequest req, 
									 final HttpServletResponse res) throws 
									     Exception {
	return (ScriptEngine) AccessController.doPrivileged(new PrivilegedExceptionAction(){ 
	    public Object run() throws Exception {
		return (javax.script.ScriptEngine)EngineFactory.getRequiredEngineFactory(ctx).getInvocableScriptEngine(servlet, ctx, req, res);
	    }
	});
    }
    /**
     * Get a PHP JSR 223 ScriptEngine, which implements the Invocable interface, from a HTTP server running on the local host.
     *
     * Example:<br>
     * <blockquote>
     * <code>
     * ScriptEngine scriptEngine = EngineFactory.getInvocablePhpScriptEngine(this, application, request, response, "HTTP", 80);<br>
     * ...<br>
     * scriptEngine.eval(reader);<br>
     * reader.close ();<br>
     * Invocable invocableEngine = (Invocable)scriptEngine;<br>
     * invocableEngine.invoceFunction("phpinfo", new Object[]{});<br>
     * ...<br>
     * scriptEngine.eval ((Reader)null);<br>
     * </code>
     * </blockquote>
     * @param servlet the servlet
     * @param ctx the servlet context
     * @param req the request
     * @param res the response
     * @param protocol either "HTTP" or "HTTPS"
     * @param port the port number
     * @return the invocable PHP JSR 223 ScriptEngine, an instance of the {@link InvocablePhpServletScriptEngine}
     * @throws Exception 
     * @throws MalformedURLException
     * @throws IllegalStateException
     */
    public static javax.script.ScriptEngine getInvocablePhpScriptEngine (final Servlet servlet, 
									 final ServletContext ctx, 
									 final HttpServletRequest req, 
									 final HttpServletResponse res,
									 final String protocol,
									 final int port) throws 
									     Exception {
	return (ScriptEngine) AccessController.doPrivileged(new PrivilegedExceptionAction(){ 
	    public Object run() throws Exception {
	    return (javax.script.ScriptEngine)EngineFactory.getRequiredEngineFactory(ctx).getInvocableScriptEngine(servlet, ctx, req, res, protocol, port);
	    }
	});
    }
    /**
     * Get a PHP JSR 223 ScriptEngine, which implements the Invocable interface, from a HTTP server running on the local host.
     *
     * Example:<br>
     * <blockquote>
     * <code>
     * ScriptEngine scriptEngine = EngineFactory.getInvocablePhpScriptEngine(this, application, request, response, "HTTP", 80);<br>
     * ...<br>
     * scriptEngine.eval(reader);<br>
     * reader.close ();<br>
     * Invocable invocableEngine = (Invocable)scriptEngine;<br>
     * invocableEngine.invoceFunction("phpinfo", new Object[]{});<br>
     * ...<br>
     * scriptEngine.eval ((Reader)null);<br>
     * </code>
     * </blockquote>
     * @param servlet the servlet
     * @param ctx the servlet context
     * @param req the request
     * @param res the response
     * @param protocol either "HTTP" or "HTTPS"
     * @param port the port number
     * @param proxy the name of the PHP proxy, for example "/JavaProxy.php"
     * @return the invocable PHP JSR 223 ScriptEngine, an instance of the {@link InvocablePhpServletScriptEngine}
     * @throws Exception 
     * @throws MalformedURLException
     * @throws IllegalStateException
     */
    public static javax.script.ScriptEngine getInvocablePhpScriptEngine (final Servlet servlet, 
									 final ServletContext ctx, 
									 final HttpServletRequest req, 
									 final HttpServletResponse res,
									 final String protocol,
									 final int port,
									 final String proxy) throws 
									     Exception {
	return (ScriptEngine) AccessController.doPrivileged(new PrivilegedExceptionAction(){ 
	    public Object run() throws Exception {
	    return (javax.script.ScriptEngine)EngineFactory.getRequiredEngineFactory(ctx).getInvocableScriptEngine(servlet, ctx, req, res, protocol, port, proxy);
	    }
	});
    }

    private static ScriptFile getFile(ScriptFile file, Reader reader) throws IOException {
	FileOutputStream fout = new FileOutputStream(file);
	OutputStreamWriter writer = new OutputStreamWriter(fout);
	char[] cbuf = new char[Util.BUF_SIZE];
	int length;
	while((length=reader.read(cbuf, 0, cbuf.length))>0) 
	    writer.write(cbuf, 0, length);
	writer.close();
	return file;
    }
    
    /**
     * Get a PHP script from the given Path. This procedure can be used to cache dynamically-generated scripts
     * @param path the file path which should contain the cached script, must be within the web app directory
     * @param reader the JSR 223 script reader
     * @return A pointer to the cached PHP script, named: path+"._cache_.php"
     * @see #createPhpScriptFileReader(File)
     */
    public static ScriptFile getPhpScript (final String path, final Reader reader) {
	if (path==null) throw new NullPointerException("path");
	return (ScriptFile) AccessController.doPrivileged(new PrivilegedAction(){ 
	    public Object run() {
        	try {
        	    return getFile(new ScriptFile(path+"._cache_.php"), reader);
        	} catch (IOException e) {
        	    Util.printStackTrace(e);
                }
        	return null;
	    }
	});
    }
    /**
     * Get a PHP script from the given Path. This procedure can be used to cache dynamically-generated scripts
     * @param webPath the web path of the script or the web path of a resource within the current context
     * @param path the file path which should contain the cached script
     * @param reader the JSR 223 script reader
     * @return A pointer to the cached PHP script, named: path+"._cache_.php"
     * @see #createPhpScriptFileReader(File)
     */
    public static ScriptFile getPhpScript (final String webPath, final String path, final Reader reader) {
	if (path==null) throw new NullPointerException("path");
	return (ScriptFile) AccessController.doPrivileged(new PrivilegedAction(){ 
	    public Object run() {
        	try {
        	    return getFile(new ScriptFile(webPath, path+"._cache_.php"), reader);
        	} catch (IOException e) {
        	    Util.printStackTrace(e);
                }
        	return null;
	    }
	});
    }
   /**
     * Get a PHP script from the given Path. This procedure can be used to cache dynamically-generated scripts
     * @param path the file path which should contain the cached script, must be within the web app directory
     * @return A pointer to the cached PHP script, usually named: path+"._cache_.php"
     * @see #createPhpScriptFileReader(File)
     */
    public static ScriptFile getPhpScript (final String path) {
	if (path==null) throw new NullPointerException("path");
	return (ScriptFile) AccessController.doPrivileged(new PrivilegedAction(){ 
	    public Object run() {
		return new ScriptFile(path+"._cache_.php");
	    }
	});
    }
    /**
     * Get a PHP script from the given Path. This procedure can be used to cache dynamically-generated scripts
     * @param webPath the web path of the script or the web path of a resource within the current context
     * @param path the file path which should contain the cached script
     * @return A pointer to the cached PHP script, usually named: path+"._cache_.php"
     * @see #createPhpScriptFileReader(File)
     */
    public static ScriptFile getPhpScript (final String webPath, final String path) {
	if (path==null) throw new NullPointerException("path");
	return (ScriptFile) AccessController.doPrivileged(new PrivilegedAction(){ 
	    public Object run() {
		return new ScriptFile(webPath, path+"._cache_.php");
	    }
	});
    }
    
    /**
     * Wrapper for {@link ServletContext#getRealPath(String)}, throws an IllegalArgumentException if the path could not be determined.
     * @param ctx ServletContext
     * @param path the resource path
     * @return The full path to the resource.
     */
    public static String getRealPath (ServletContext ctx, String path) {
	return CGIServlet.getRealPath(ctx, path);
    }
    /**
     * Create a Reader from a given PHP script file. This procedure can be used to create
     * a reader from a cached script
     *
     * Example:<br>
     * <blockquote>
     * <code>
     * private static File script;<br>
     * private static final File getScript() {<br>
     * &nbsp;&nbsp; if (script!=null) return script;<br>
     * &nbsp;&nbsp; return EngineFactory.getPhpScript(ctx.getRealPath(req.getServletPath(),new StringReader("&lt;?php phpinfo();?&gt;"));<br>
     * }<br>
     * ... <br>
     * FileReader reader = EngineFactory.createPhpScriptFileReader(getScript());<br>
     * scriptEngine.eval (reader);<br>
     * reader.close();<br>
     * ...<br>
     * </code>
     * </blockquote>
     * @param phpScriptFile the file containing the cached script, obtained from {@link #getPhpScript(String, Reader)} or {@link #getPhpScript(String)}
     * @return A pointer to the cached PHP script, usually named: path+"._cache_.php"
     */
    public static FileReader createPhpScriptFileReader (final ScriptFile phpScriptFile) {
	return (FileReader) AccessController.doPrivileged(new PrivilegedAction(){ 
	    public Object run() {
        	try {
        	    return new ScriptFileReader(phpScriptFile);
                } catch (IOException e) {
        	    Util.printStackTrace(e);
                }
        	return null;
	    }
	});
    }
    /** Use {@link #createPhpScriptFileReader(ScriptFile)} instead 
     * @param phpScriptFile 
     * @return A new FileReader
     */
    public static FileReader createPhpScriptFileReader (final File phpScriptFile) {
	return createPhpScriptFileReader((ScriptFile)phpScriptFile);
    }
    /**
     * Release all managed script engines. Will be called automatically at the end of each request,
     * if a RequestListener has been declared.
     * @param list the list from the request attribute {@link RequestListener#ROOT_ENGINES_COLLECTION_ATTRIBUTE}
     */
    public void releaseScriptEngines(final List list) {
	AccessController.doPrivileged(new PrivilegedAction(){ 
	    public Object run() {
        	for (Iterator ii=list.iterator(); ii.hasNext(); ) {
        	    InvocablePhpServletLocalHttpServerScriptEngine engine = (InvocablePhpServletLocalHttpServerScriptEngine) ii.next();
        	    engine.releaseReservedContinuation();
        	    engine.release();
        	}
        	list.clear();
        	return null;
	    }
	});
    }
    /**
     * Manage a script engine
     * @param req the servlet request
     * @param engine the engine to manage
     * @throws ScriptException 
     * @see #releaseScriptEngines(List)
     */
    public static void addManaged(HttpServletRequest req,
		InvocablePhpServletLocalHttpServerScriptEngine engine) throws ScriptException {
	try {
	    addManagedInternal(req, engine);
	} catch (PrivilegedActionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) throw (RuntimeException)cause;
            throw (ScriptException) e.getCause();
	}
    }
    private static void addManagedInternal(final HttpServletRequest req,
	final InvocablePhpServletLocalHttpServerScriptEngine engine) throws PrivilegedActionException {
	AccessController.doPrivileged(new PrivilegedExceptionAction() { 
	    public Object run() throws Exception {
		
		ArrayList list = null;
		try {
		    list = EngineFactoryHelper.getManagedEngineList(req);
		} catch (NoClassDefFoundError e) { /**ignore for jdk 1.4*/ }
		
		if (list!=null) {
		    list.add(engine);
		    
		    // check
		    engine.reserveContinuation();
		}
		return null;
	    }
	});
    }
}