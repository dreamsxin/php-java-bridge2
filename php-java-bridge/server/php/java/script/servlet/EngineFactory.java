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

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import php.java.bridge.Util;


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
public class EngineFactory {
    /** The key used to store the factory in the servlet context */
    public static final String ROOT_ENGINE_FACTORY_ATTRIBUTE = EngineFactory.class.getName()+".ROOT";
    /** Only for internal use */
    public EngineFactory() {}
    private Object getScriptEngine(Servlet servlet, 
		     ServletContext ctx, 
		     HttpServletRequest req, 
		     HttpServletResponse res) throws MalformedURLException {
	    return new PhpServletScriptEngine(servlet, ctx, req, res);
    }
    private Object getInvocableScriptEngine(Servlet servlet, 
		     ServletContext ctx, 
		     HttpServletRequest req, 
		     HttpServletResponse res) throws MalformedURLException, URISyntaxException {
	    return new InvocablePhpServletScriptEngine(servlet, ctx, req, res);
    }
    /** 
     * Get an engine factory from the servlet context
     * @param ctx The servlet context
     * @return the factory or null
     */
    public static EngineFactory getEngineFactory(ServletContext ctx) {
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
    public static EngineFactory getRequiredEngineFactory(ServletContext ctx) throws IllegalStateException {
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
     * ScriptEngine scriptEngine = EngineFactory.PhpScriptengine(this, application, request, response);<br>
     * scriptEngine.eval(reader);<br>
     * reader.close();<br>
     * </code>
     * </blockquote>
     * @param servlet the servlet
     * @param ctx the servlet context
     * @param req the request
     * @param res the response
     * @return the PHP JSR 223 ScriptEngine, an instance of the {@link PhpServletScriptEngine}
     * @throws MalformedURLException
     * @throws IllegalStateException
     */
    public static javax.script.ScriptEngine getPhpScriptEngine (Servlet servlet, 
								ServletContext ctx, 
								HttpServletRequest req, 
								HttpServletResponse res) throws 
								    MalformedURLException, IllegalStateException {
	return (javax.script.ScriptEngine)EngineFactory.getRequiredEngineFactory(ctx).getScriptEngine(servlet, ctx, req, res);
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
     * @throws MalformedURLException
     * @throws IllegalStateException
     */
    public static javax.script.ScriptEngine getInvocablePhpScriptEngine (Servlet servlet, 
									 ServletContext ctx, 
									 HttpServletRequest req, 
									 HttpServletResponse res) throws 
									     MalformedURLException, IllegalStateException, URISyntaxException {
	    return (javax.script.ScriptEngine)EngineFactory.getRequiredEngineFactory(ctx).getInvocableScriptEngine(servlet, ctx, req, res);
    }

    private static File getFile(File file, Reader reader) throws IOException {
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
    public static File getPhpScript (String path, Reader reader) {
	try {
	    return getFile(new File(path+"._cache_.php"), reader);
	} catch (IOException e) {
	    Util.printStackTrace(e);
        }
	return null;
    }
    /**
     * Get a PHP script from the given Path. This procedure can be used to cache dynamically-generated scripts
     * @param path the file path which should contain the cached script, must be within the web app directory
     * @return A pointer to the cached PHP script, usually named: path+"._cache_.php"
     * @see #createPhpScriptFileReader(File)
     */
    public static File getPhpScript (String path) {
	return new File(path+"._cache_.php");
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
    public static FileReader createPhpScriptFileReader (File phpScriptFile) {
	try {
	    return new ScriptFileReader(phpScriptFile);
        } catch (IOException e) {
	    Util.printStackTrace(e);
        }
	return null;
    }
}
