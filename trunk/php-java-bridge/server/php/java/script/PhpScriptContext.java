/*-*- mode: Java; tab-width:8 -*-*/

package php.java.script;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;

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

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;

import javax.script.ScriptContext;

import php.java.bridge.Util;
import php.java.bridge.http.ContextServer;
import php.java.bridge.http.IContext;
import php.java.bridge.http.JavaBridgeRunner;
import php.java.bridge.util.Logger;
import php.java.bridge.util.NotImplementedException;
import php.java.fastcgi.ConnectionException;
import php.java.fastcgi.Continuation;
import php.java.fastcgi.FCGIConnectionPool;
import php.java.fastcgi.FCGIHeaderParser;
import php.java.fastcgi.FCGIHelper;
import php.java.fastcgi.FCGIProcessException;
import php.java.fastcgi.FCGIProxy;

/**
 * This class implements a simple script context for PHP. It starts a standalone 
 * <code>JavaBridgeRunner</code> which listens for requests from php instances.<p>
 * 
 * In a servlet environment please use a <code>php.java.script.http.PhpSimpleHttpScriptContext</code> instead.
 * @see php.java.script.PhpScriptContext
 * @see php.java.bridge.JavaBridgeRunner
 * @author jostb
 *
 */

public final class PhpScriptContext extends AbstractPhpScriptContext {
    private FCGIHelper helper = new FCGIHelper();
    
    public PhpScriptContext(ScriptContext ctx) {
	super(ctx);
    }
    /**{@inheritDoc}*/
    public Object init(Object callable) throws Exception {
	return php.java.bridge.http.Context.getManageable(callable);
    }
    /**{@inheritDoc}*/
    public void onShutdown(Object closeable) {
	php.java.bridge.http.Context.handleManaged(closeable);
    }
    /**
     * Throws IllegalStateException
     * @return none
     */
    public Object getHttpServletRequest() {
	throw new IllegalStateException("PHP not running in a servlet environment");
    }
    
    /**
     * Throws IllegalStateException
     * @return none
     */
    public Object getServletContext() {
	throw new IllegalStateException("PHP not running in a servlet environment");
    }
    
    /**
     * Throws IllegalStateException
     * @return none
     */
    public Object getHttpServletResponse() {
	throw new IllegalStateException("PHP not running in a servlet environment");
    }
    /**
     * Throws IllegalStateException
     * @return none
     */
    public Object getServlet() {
	throw new IllegalStateException("PHP not running in a servlet environment");
    }
    /**
     * Throws IllegalStateException
     * @return none
     */
    public Object getServletConfig() {
	throw new IllegalStateException("PHP not running in a servlet environment");
    }

    /**{@inheritDoc}*/
    public String getRealPath(String path) {
	return php.java.bridge.http.Context.getRealPathInternal(path);
    }
    /**{@inheritDoc}*/
    public Object get(String key) {
	  return getBindings(IContext.ENGINE_SCOPE).get(key);
    }
    /**{@inheritDoc}*/
    public void put(String key, Object val) {
	  getBindings(IContext.ENGINE_SCOPE).put(key, val);
    }
    /**{@inheritDoc}*/
    public void remove(String key) {
	  getBindings(IContext.ENGINE_SCOPE).remove(key);
    }
    /**{@inheritDoc}*/
    public void putAll(Map map) {
	  getBindings(IContext.ENGINE_SCOPE).putAll(map);
    }
    /**{@inheritDoc}*/
    public Map getAll() {
	return Collections.unmodifiableMap(getBindings(IContext.ENGINE_SCOPE));
    }
    
    private static final Object globalCtxLock = new Object();
    private static FCGIConnectionPool fcgiConnectionPool = null;
 
    private FCGIConnectionPool getConnectionPool(String[] args, Map env)
            throws FCGIProcessException, ConnectionException {
	
	synchronized (globalCtxLock) {
	    if (fcgiConnectionPool != null)
		return fcgiConnectionPool;
	    return fcgiConnectionPool = FCGIConnectionPool
	            .createConnectionPool(args, env, helper);

	}
    }

    
    /**{@inheritDoc}
     * @throws ConnectionException */
    public Continuation createContinuation(String[] args, Map env,
            OutputStream out, OutputStream err, FCGIHeaderParser headerParser) throws FCGIProcessException, ConnectionException {

	return new FCGIProxy(env, out,  err, headerParser, args==null?fcgiConnectionPool:getConnectionPool(args, env)); 
    }
    private static JavaBridgeRunner httpServer;
    private static synchronized final JavaBridgeRunner getHttpServer() {
	if (httpServer!=null) return httpServer;
	try {
	    return httpServer = JavaBridgeRunner.getRequiredInstance();
        } catch (IOException e) {
            Logger.printStackTrace(e);
            return null;
        }
    }
    /**{@inheritDoc}*/
    public String getSocketName() {
	return getHttpServer().getSocket().getSocketName();
    }
    /**@deprecated*/
    public String getRedirectString() {
	throw new NotImplementedException();
    }
    /**@deprecated*/
    public String getRedirectString(String webPath) {
	throw new NotImplementedException();
    }
    /**{@inheritDoc}*/
    public String getRedirectURL(String webPath) {
	return "http://127.0.0.1:"+getSocketName()+webPath;
    }
    /**{@inheritDoc}*/
    public ContextServer getContextServer() {
	return getHttpServer().getContextServer();
    }
    @Override
    public void destroy() {
	synchronized(globalCtxLock) { 
	    if (fcgiConnectionPool!=null)
		fcgiConnectionPool.destroy();
	    fcgiConnectionPool=null;
	}
    }
    
    
    
    protected String getStandardHeader(String filePath) throws IOException {
	return filePath == null ? getEmbeddedStandardHeader(filePath)
	        : getSimpleStandardHeader(filePath);
    }
    private static final String STANDARD_HEADER = new String(
            "<?php require_once(\"/java/Java.inc\");"
                    + "$java_bindings = java_context()->getBindings(100);"
                    + "$java_scriptname = @java_values($java_bindings['javax.script.filename']);"
                    + "if(!isset($argv)) $argv = @java_values($java_bindings['javax.script.argv']);"
                    + "if(!isset($argv)) $argv=array();\n"
                    + "$_SERVER['SCRIPT_FILENAME'] =  isset($java_scriptname) ? $java_scriptname : '';"
                    + "array_unshift($argv, $_SERVER['SCRIPT_FILENAME']);"
                    + "if (!isset($argc)) $argc = count($argv);"
                    + "$_SERVER['argv'] = $argv;" + "?>");
    private String cachedSimpleStandardHeader;
    private String getSimpleStandardHeader(String filePath) {
	if (cachedSimpleStandardHeader != null)
	    return cachedSimpleStandardHeader;
	StringBuffer buf = new StringBuffer(STANDARD_HEADER);
	buf.insert(20, filePath);
	return cachedSimpleStandardHeader = buf.toString();
    }

    private static final String STANDARD_HEADER_EMBEDDED = new String(
            "<?php " + "$java_bindings = java_context()->getBindings(100);"
                    + "$java_scriptname = @java_values($java_bindings['javax.script.filename']);"
                    + "if(!isset($argv)) $argv = @java_values($java_bindings['javax.script.argv']);"
                    + "if(!isset($argv)) $argv=array();\n"
                    + "$_SERVER['SCRIPT_FILENAME'] =  isset($java_scriptname) ? $java_scriptname : '';"
                    + "array_unshift($argv, $_SERVER['SCRIPT_FILENAME']);"
                    + "if (!isset($argc)) $argc = count($argv);"
                    + "$_SERVER['argv'] = $argv;" + "?>");
    private String cachedEmbeddedStandardHeader;
    private String getEmbeddedStandardHeader(String filePath)
            throws IOException {
	if (cachedEmbeddedStandardHeader != null)
	    return cachedEmbeddedStandardHeader;
	try {
	    ByteArrayOutputStream out = new ByteArrayOutputStream();
	    Field f = Util.JAVA_INC.getField("bytes");
	    byte[] buf = (byte[]) f.get(Util.JAVA_INC);
	    out.write(buf);

	    OutputStreamWriter writer = new OutputStreamWriter(out);
	    writer.write(STANDARD_HEADER_EMBEDDED);
	    writer.close();
	    return cachedEmbeddedStandardHeader = out.toString(Util.ASCII);
	} catch (Exception e) {
	    IOException ex = new IOException("Cannot create standard header");
	    ex.initCause(e);
	    throw ex;
	}
    }

    private static final char[] PHP_JAVA_CONTEXT_CALL_JAVA_CLOSURE = "<?php java_context()->call(java_closure()); ?>"
            .toCharArray();
    private static final char[] HEADER = "\n<?php $java_scriptengine_script=<<<'JAVA_EOF'\n"
            .toCharArray();
    private static final char[] FOOTER = "\nJAVA_EOF;\njava_eval($java_scriptengine_script);?>"
            .toCharArray();

    @Override
    public File compile(Reader reader) throws IOException {
	File scriptFile = File.createTempFile("pjbtmp", ".php")
	        .getAbsoluteFile();
	FileWriter writer = new FileWriter(scriptFile);
	int count;
	boolean embedJavaInc = helper.isPhpIncludeJava();
	char[] cbuf = new char[Util.BUF_SIZE];
	String stdHeader = getStandardHeader(embedJavaInc ? null
	        : ((IContext) this)
	                .getRedirectURL("/JavaBridge"));

	writer.write(stdHeader);
	writer.write(HEADER);
	/* the script: */
	while ((count = reader.read(cbuf)) > 0)
	    writer.write(cbuf, 0, count);
	writer.write(FOOTER);

	while ((count = reader.read(cbuf)) != -1) {
	    writer.write(cbuf, 0, count);
	}
	writer.write(PHP_JAVA_CONTEXT_CALL_JAVA_CLOSURE);
	writer.close();
	return scriptFile;
    }
}
