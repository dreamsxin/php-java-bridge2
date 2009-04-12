/*-*- mode: Java; tab-width:8 -*-*/

package php.java.script.servlet;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import javax.script.ScriptException;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import php.java.bridge.Util;
import php.java.bridge.http.AbstractChannelName;
import php.java.bridge.http.ContextServer;
import php.java.bridge.http.IContext;
import php.java.bridge.http.IContextFactory;
import php.java.script.IPhpScriptContext;
import php.java.servlet.PhpJavaServlet;

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
 * This script engine connects a remote PHP container with the current servlet container.
 * 
 * There must not be a firewall in between, and both components should be behind a firewall. The remote
 * PHP application must have the PHP code from the PHP file <code>JavaProxy.php</code> embedded, otherwise invocation
 * will fail. <code>JavaProxy.php</code> has the ability to inject PHP code dynamically into a running PHP application,
 * provided that the administrator has set the php.ini option <code>allow_url_include = On</code>.
 * <br>	
 * 
 * PHP scripts are evaluated as follows:
 * <ol>
 * <li> JavaProxy.php is requested from Java<br>
 * <li> Your script is included and then evaluated
 * <li> &lt;?php java_context()-&gt;call(java_closure());?&gt; is called in order to make the script invocable<br>
 * </ol>
 * In order to evaluate PHP methods follow these steps:<br>
 * <ol>
 * <li> Create a factory which creates a PHP script file from a reader using the methods from {@link EngineFactory}:
 * <blockquote>
 * <code>
 * private static File script;<br>
 * private static final File getScriptF() {<br>
 * &nbsp;&nbsp; if (script!=null) return script;<br><br>
 * &nbsp;&nbsp; String webCacheDir = ctx.getRealPath(req.getServletPath());<br>
 * &nbsp;&nbsp; Reader reader = new StringReader ("&lt;?php function f($v) {return "passed:".$v;} ?&gt;");<br>
 * &nbsp;&nbsp; return EngineFactory.getPhpScript(webCacheDir, reader);<br>
 * }<br>
 * </code>
 * </blockquote>
 * <li> Acquire a PHP invocable script engine from the {@link EngineFactory}. The following example links the PHP app server "diego" with the current Java app server "timon":
 * <blockquote>
 * <code>
 * ScriptEngine scriptEngine = EngineFactory.getInvocablePhpScriptEngine(this, ctx, req, res, new java.net.URI("http://diego.intern.com:80/phpApp/JavaProxy.php"), "timon.intern.com"));
 * </code>
 * </blockquote> 
 * <li> Optional: Create a FileReader for the created script file:
 * <blockquote>
 * <code>
 * <strike>Reader readerF = EngineFactory.createPhpScriptFileReader(getScriptF());</strike>
 * </code>
 * </blockquote>
 * <li> Optional: Evaluate the engine:
 * <blockquote>
 * <code>
 * <strike>scriptEngine.eval(readerF);</strike>
 * </code>
 * </blockquote> 
 * <li> Optional: Close the reader obtained from the {@link EngineFactory}:
 * <blockquote>
 * <code>
 * <strike>readerF.close();</strike>
 * </code>
 * </blockquote> 
 * <li> Cast the engine to Invocable:
 * <blockquote>
 * <code>
 * Invocable invocableEngine = (Invocable)scriptEngine;
 * </code>
 * </blockquote> 
 * <li> Call PHP functions or methods:
 * <blockquote>
 * <code>
 * System.out.println("result from PHP:" + invocableEngine.invoceFunction(f, new Object[]{"arg1"}));
 * </code>
 * </blockquote> 
 * <li> Release the invocable:
 * <blockquote>
 * <code>
 * ((Closeable)scriptEngine).close();
 * </code>
 * </blockquote> 
 * </ol>
 * Injecting code into a foreign remote PHP application using <code>scriptEngine.eval(readerF);</code> 
 * requires that the PHP administrator has set the php.ini option <code>allow_url_include=On</code> for the remove PHP application, 
 * the PHP code is fetched from your Java app using <code>require_once("your PHP code")</code>.
 * <br>
 */
public class InvocablePhpServletRemoteHttpServerScriptEngine extends InvocablePhpServletLocalHttpServerScriptEngine {
    
    /** The official FIXED(!) IP# of the current host, */
    protected String localName;
    protected ContextServer contextServer;
    
    protected InvocablePhpServletRemoteHttpServerScriptEngine(Servlet servlet, 
		   ServletContext ctx, 
		   HttpServletRequest req, 
		   HttpServletResponse res,
    		   URI uri,
    		   String localName) throws MalformedURLException, URISyntaxException {
	super(servlet, ctx, req, res);

	this.localName = localName;
	
	this.protocol = uri.getScheme();
	this.port = uri.getPort();
	this.proxy = uri.getPath();
	this.url = uri.toURL();
	
	this.contextServer = PhpJavaServlet.getContextServer(ctx);
    }
    protected IContextFactory getPhpScriptContextFactory (IPhpScriptContext context) {
	IContextFactory ctx = InvocableRemotePhpServletContextFactory.addNew((IContext)context, servlet, servletCtx, req, res, localName);
    	// short path S1: no PUT request
    	AbstractChannelName channelName = contextServer.getFallbackChannelName(null, ctx);
    	if (channelName != null) {
    	    env.put("X_JAVABRIDGE_REDIRECT", channelName.getName());
    	    ctx.getBridge();
    	    contextServer.start(channelName);
    	}
    	return ctx;
    }
    /**
     * Create a new context ID and a environment map which we send to the client.
     * @throws IOException 
     *
     */
    protected void setNewScriptFileContextFactory(ScriptFileReader fileReader) throws IOException, ScriptException {
        IPhpScriptContext context = (IPhpScriptContext)getContext(); 
	env = (Map) processEnvironment.clone();

	ctx = getPhpScriptContextFactory(context);

	/* send the session context now, otherwise the client has to 
	 * call handleRedirectConnection */
	setStandardEnvironmentValues(env);
	
	ScriptFile file = fileReader.getFile();
	URI include;
        try {
	    include = new URI(req.getScheme(), null, localName, req.getServerPort(), file.getWebPath(file.getName(), req, servletCtx), null, null);
        } catch (URISyntaxException e) {
           Util.printStackTrace(e);
	   throw new ScriptException(e);
        }
	env.put("X_JAVABRIDGE_INCLUDE", include.toASCIIString());
    }
}
