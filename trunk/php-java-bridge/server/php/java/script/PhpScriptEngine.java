/*-*- mode: Java; tab-width:8 -*-*/

package php.java.script;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

import php.java.bridge.Util;
import php.java.bridge.http.AbstractChannelName;
import php.java.bridge.http.ContextServer;
import php.java.bridge.http.IContext;
import php.java.bridge.http.IContextFactory;
import php.java.bridge.http.WriterOutputStream;
import php.java.bridge.parser.PhpProcedure;
import php.java.bridge.parser.Request;
import php.java.bridge.util.Logger;
import php.java.fastcgi.Continuation;
import php.java.fastcgi.FCGIHeaderParser;

/**
 * This class implements the ScriptEngine.
 * <p>
 * 
 * @see php.java.script.PhpScriptEngine
 */
public abstract class PhpScriptEngine extends AbstractScriptEngine
        implements IPhpScriptEngine, java.io.Closeable, Invocable {

    protected static final HashSet engines = new HashSet();
    private static boolean registeredHook = false;
    private static final String PHP_EMPTY_SCRIPT = "<?php ?>";

    private static final String[] STANDARD_BINDING_KEYS = new String[] {
            ScriptEngine.ARGV, ScriptEngine.ENGINE, ScriptEngine.ENGINE_VERSION,
            ScriptEngine.FILENAME, ScriptEngine.LANGUAGE,
            ScriptEngine.LANGUAGE_VERSION, ScriptEngine.NAME };

    /**
     * The allocated script
     */
    protected Object script = null;
    protected Object scriptClosure = null;

    /**
     * The continuation of the script
     */
    protected Continuation continuation = null;
    protected Map env = null;
    protected IContextFactory ctx = null;

    protected ScriptEngineFactory factory = null;

    protected File scriptFile;
    protected Reader localReader;
    protected ResultProxy resultProxy;

    static HashMap getProcessEnvironment() {
	return new HashMap();
    }

    protected void setStandardBindings() {
	int i = 0;
	for (String s : STANDARD_BINDING_KEYS) {
	    switch (i++) {
	    case 0:
		getBindings(ScriptContext.ENGINE_SCOPE).put(s, new String[] {
		        Util.PHP_EXEC == null ? "php-cgi" : Util.PHP_EXEC });
		break;
	    case 1:
		getBindings(ScriptContext.ENGINE_SCOPE).put(s,
		        factory.getEngineName());
		break;
	    case 2:
		getBindings(ScriptContext.ENGINE_SCOPE).put(s,
		        factory.getEngineVersion());
		break;
	    case 4:
		getBindings(ScriptContext.ENGINE_SCOPE).put(s,
		        factory.getLanguageName());
		break;
	    case 5:
		getBindings(ScriptContext.ENGINE_SCOPE).put(s,
		        factory.getLanguageVersion());
		break;
	    case 6:
		getBindings(ScriptContext.ENGINE_SCOPE).put(s,
		        factory.getEngineName());
		break;
	    }
	}
    }

    /**
     * Set the context id (X_JAVABRIDGE_CONTEXT) and the override flag
     * (X_JAVABRIDGE_OVERRIDE_HOSTS) into env
     * 
     * @param env
     *            the environment which will be passed to PHP
     */
    protected void setStandardEnvironmentValues(Map env) {
	/*
	 * send the session context now, otherwise the client has to call
	 * handleRedirectConnection
	 */
	env.put(Util.X_JAVABRIDGE_CONTEXT, ctx.getId());
	getBindings(ScriptContext.ENGINE_SCOPE).put(STANDARD_BINDING_KEYS[3],
	        env.get("SCRIPT_FILENAME"));
    }

    protected Object invoke(String methodName, Object[] args)
            throws ScriptException, NoSuchMethodException {
	if (methodName == null) {
	    release();
	    return null;
	}

	if (scriptClosure == null) {
	    if (Logger.getLogLevel() > 4)
		Logger.warn(
		        "Evaluating an empty script either because eval() has not been called or release() has been called.");
	    eval(PHP_EMPTY_SCRIPT);
	}
	try {
	    return invoke(scriptClosure, methodName, args);
	} catch (Request.AbortException e) {
	    release();
	    throw new ScriptException(e);
	} catch (NoSuchMethodError e) { // conform to jsr223
	    throw new NoSuchMethodException(String.valueOf(e.getMessage()));
	}
    }

    /** {@inheritDoc} */
    public Object invokeFunction(String methodName, Object... args)
            throws ScriptException, NoSuchMethodException {
	return invoke(methodName, args);
    }

    private void checkPhpClosure(Object thiz) {
	if (thiz == null)
	    throw new IllegalStateException(
	            "PHP script did not pass its continuation to us!. Please check if the previous call to eval() reported any errors. Or else check if it called OUR continuation.");
    }

    protected Object invoke(Object thiz, String methodName, Object[] args)
            throws ScriptException, NoSuchMethodException {
	checkPhpClosure(thiz);
	PhpProcedure proc = (PhpProcedure) (Proxy.getInvocationHandler(thiz));
	try {
	    return proc.invoke(script, methodName, args);
	} catch (ScriptException e) {
	    throw e;
	} catch (NoSuchMethodException e) {
	    throw e;
	} catch (RuntimeException e) {
	    throw e; // don't wrap RuntimeException
	} catch (NoSuchMethodError e) { // conform to jsr223
	    throw new NoSuchMethodException(String.valueOf(e.getMessage()));
	} catch (Error er) {
	    throw er;
	} catch (Throwable e) {
	    throw new PhpScriptException("Invocation threw exception ", e);
	}
    }

    public Object getInterface(Class clasz) {
	checkPhpClosure(script);
	return getInterface(script, clasz);
    }

    public Object getInterface(Object thiz, Class clasz) {
	checkPhpClosure(thiz);
	Class[] interfaces = clasz == null ? Util.ZERO_PARAM
	        : new Class[] { clasz };
	return PhpProcedure.createProxy(interfaces,
	        (PhpProcedure) Proxy.getInvocationHandler(thiz));
    }

    protected Object evalPhp(Reader reader)
            throws ScriptException {
	if ((continuation != null) || (reader == null))
	    release();
	if (reader == null)
	    return null;

	setNewContextFactory((Map) getProcessEnvironment().clone());
	compileScript(reader);

	try {
	    this.script = doEvalPhp(getArgs());
	    if (this.script != null) {
		/*
		 * get the proxy, either the one from the user script or our
		 * default proxy
		 */
		this.scriptClosure = script;
	    }
	} catch (Exception e) {
	    Logger.printStackTrace(e);
	    if (e instanceof RuntimeException)
		throw (RuntimeException) e;
	    if (e instanceof ScriptException)
		throw (ScriptException) e;
	    throw new ScriptException(e);
	} finally {
	    this.resultProxy = new ResultProxy(this)
	            .withResult(ctx.getContext().getExitCode());
	    handleRelease();
	}

	return resultProxy;
    }

    protected String[] getArgs() {
	String[] args = (String[]) get(ScriptEngine.ARGV);
	if (args == null)
	    throw new NullPointerException(
	            "ScriptEngine.ARGV must not be null");
	return args;
    }

    public Object invokeMethod(Object thiz, String methodName, Object... args)
            throws ScriptException, NoSuchMethodException {
	return invoke(thiz, methodName, args);
    }

    protected void addNewContextFactory() {
	ctx = PhpScriptContextFactory.addNew((IContext) getContext());
    }

    protected ContextServer getContextServer() {
	return ((IPhpScriptContext) getContext()).getContextServer();
    }

    protected void setNewContextFactory(Map env) {
	resultProxy = null;
	this.env = env;

	addNewContextFactory();

	// short path S1: no PUT request
	ContextServer contextServer = getContextServer();
	AbstractChannelName channelName = contextServer.getChannelName(ctx);
	if (channelName != null) {
	    env.put("X_JAVABRIDGE_REDIRECT", channelName.getName());
	    ctx.getBridge();
	    contextServer.start(channelName);
	}

	setStandardEnvironmentValues(env);
    }

    protected void compileScript(Reader reader) throws ScriptException {
	IPhpScriptContext phpScriptContext = (IPhpScriptContext) getContext();
	try {
	    if (scriptFile == null) {
	    scriptFile = phpScriptContext.compile(reader);
	    }
	    if (env != null) {
		env.put("SCRIPT_FILENAME", scriptFile.getAbsolutePath());
	    }
	} catch (IOException e) {
	    throw new ScriptException(e);
	}
    }

    public Object eval(Reader reader, ScriptContext context)
            throws ScriptException {

	ScriptContext current = getContext();
	if (current != context)
	    try {
		setContext(context);
		return evalPhp(reader);
	    } finally {
		setContext(current);
	    }
	else
	    return evalPhp(reader);
    }

    private final class SimpleHeaderParser extends FCGIHeaderParser {
	private WriterOutputStream writer;

	public SimpleHeaderParser(WriterOutputStream writer) {
	    this.writer = writer;
	}

	public void parseHeader(String header) {
	    if (header == null)
		return;
	    int idx = header.indexOf(':');
	    if (idx == -1)
		return;
	    String key = header.substring(0, idx).trim().toLowerCase();
	    String val = header.substring(idx + 1).trim();
	    addHeader(key, val);
	}

	public void addHeader(String key, String val) {
	    if (val != null && key.equals("content-type")) {
		int idx = val.indexOf(';');
		if (idx == -1)
		    return;
		String enc = val.substring(idx + 1).trim();
		idx = enc.indexOf('=');
		if (idx == -1)
		    return;
		enc = enc.substring(idx + 1);
		writer.setEncoding(enc);
	    }
	}
    }

    protected Continuation getContinuation(String[] args)
            throws IOException {
	FCGIHeaderParser headerParser = FCGIHeaderParser.DEFAULT_HEADER_PARSER; // ignore
	                                                                        // encoding,
	                                                                        // we
	                                                                        // pass
	                                                                        // everything
	                                                                        // directly
	IPhpScriptContext phpScriptContext = (IPhpScriptContext) getContext();
	OutputStream out = ((PhpScriptWriter) (getContext().getWriter()))
	        .getOutputStream();
	OutputStream err = ((PhpScriptWriter) (getContext().getErrorWriter()))
	        .getOutputStream();

	/*
	 * encode according to content-type charset
	 */
	if (out instanceof WriterOutputStream)
	    headerParser = new SimpleHeaderParser((WriterOutputStream) out);

	Continuation kont = phpScriptContext.createContinuation(args, env, out,
	        err, headerParser);

	phpScriptContext.setContinuation(kont);
	phpScriptContext.startContinuation();
	return kont;
    }

    final protected Object doEvalPhp(String[] args)
            throws Exception {
	continuation = getContinuation(args);
	return continuation.getPhpScript();
    }

    public Object eval(String script, ScriptContext context)
            throws ScriptException {
	if (script == null)
	    return evalPhp(null);

	script = script.trim();
	Reader localReader = new StringReader(script);
	try {
	    return eval(localReader, context);
	} finally {
	    try {
		localReader.close();
	    } catch (IOException e) {
		Logger.printStackTrace(e);
	    }
	}
    }

    /** @inheritDoc */
    public ScriptEngineFactory getFactory() {
	return this.factory;
    }

     public Bindings createBindings() {
	return new SimpleBindings();
    }

    static final void throwNoOutputFile() {
	throw new IllegalStateException(
	        "No compilation output file has been set!");
    }

    static final Reader DUMMY_READER = new Reader() {
	/** {@inheritDoc} */
	public void close() throws IOException {
	    throwNoOutputFile();
	}

	/** {@inheritDoc} */
	public int read(char[] cbuf, int off, int len) throws IOException {
	    throwNoOutputFile();
	    return 0;
	}
    };

    private ScriptContext ctxCache;

    /** {@inheritDoc} */
    protected ScriptContext getScriptContext(Bindings bindings) {
	return new PhpScriptContext(super.getScriptContext(bindings));
    }

    /** {@inheritDoc} */
    public ScriptContext getContext() {
	if (ctxCache == null) {
	    ctxCache = super.getContext();
	    if (!(ctxCache instanceof IPhpScriptContext)) {
		if (ctxCache == null)
		    ctxCache = new SimpleScriptContext();
		ctxCache = new PhpScriptContext(ctxCache);
		super.setContext(ctxCache);
	    }
	}
	return ctxCache;
    }

    /** {@inheritDoc} */
    public void setContext(ScriptContext context) {
	super.setContext(context);
	this.ctxCache = null;
	getContext();
    }



    protected void handleRelease() {
	// make sure to properly release them upon System.exit().
	synchronized (engines) {
	    if (!registeredHook) {
		registeredHook = true;
		try {
		    Runtime.getRuntime()
		            .addShutdownHook(new php.java.bridge.util.Thread() {
			        public void run() {
			            synchronized (engines) {
				        for (Iterator ii = engines
				                .iterator(); ii.hasNext(); ii
				                        .remove()) {
				            PhpScriptEngine e = (PhpScriptEngine) ii
				                    .next();
				            e.releaseInternal(true);
				            if (ctx!=null) ctx.destroy(); // FIXME: necessary?
				                           // shut down the
				                           // SocketContextServer
				        }
				        ((IPhpScriptContext) getContext()).destroy(); // shut
				                                                 // down
				                                                 // the
				                                                 // FastCGI
				                                                 // Server
			            }
			        }
		            });
		} catch (SecurityException e) {
		    /* ignore */}
	    }
	    engines.add(this);
	}
    }

    protected void release() {
	synchronized (engines) {
	    releaseInternal(true);
	    engines.remove(this);
	}
    }

    protected void deleteScript() {
	if (scriptFile != null) {
	    try {
		scriptFile.delete();
	    } catch (Exception e) {
		Logger.printStackTrace(e);
	    }
	    scriptFile = null;
	}
    }

    /**
     * Release the continuation
     */
    protected void releaseInternal(boolean deleteScript) {
	if (continuation != null) {
	    try {
		continuation.release();
		ctx.releaseManaged();

		resultProxy.setResult(ctx.getContext().getExitCode());
	    } catch (InterruptedException e) {
		return;
	    }
	    ctx = null;

	    continuation = null;
	    script = null;
	    scriptClosure = null;

	    try {
		getContext().getWriter().flush();
	    } catch (Exception e) {
		Logger.printStackTrace(e);
	    }
	    try {
		getContext().getErrorWriter().flush();
	    } catch (Exception e) {
		Logger.printStackTrace(e);
	    }
	    if (deleteScript)
		deleteScript();
	}
    }
}
