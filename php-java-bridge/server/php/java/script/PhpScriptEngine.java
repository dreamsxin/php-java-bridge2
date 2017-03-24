/*-*- mode: Java; tab-width:8 -*-*/

package php.java.script;

import java.io.Reader;
import java.lang.reflect.Proxy;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptException;

import php.java.bridge.Util;
import php.java.bridge.parser.PhpProcedure;
import php.java.bridge.parser.Request;
import php.java.bridge.util.Logger;

/**
 * This class implements the ScriptEngine.
 * <p>
 * Example:
 * <p>
 * <code>
 * ScriptEngine e = (new ScriptEngineManager()).getEngineByName("php");<br>
 * try { e.eval(&lt;?php foo() ?&gt;"); } catch (ScriptException e) { ... }<br>
 * </code>
 * 
 * @author jostb
 *
 */
public class PhpScriptEngine extends AbstractPhpScriptEngine {
    private static final String X_JAVABRIDGE_INCLUDE = Util.X_JAVABRIDGE_INCLUDE;
    protected static final Object EMPTY_INCLUDE = "@";
    private static final List engines = new LinkedList();
    private static boolean registeredHook = false;
    private static final String PHP_EMPTY_SCRIPT = "<?php ?>";

    /**
     * Create a new ScriptEngine with a default context.
     */
    public PhpScriptEngine() {
	super(new PhpScriptEngineFactory());
    }

    /**
     * Create a new ScriptEngine from a factory.
     * 
     * @param factory
     *            The factory
     * @see #getFactory()
     */
    public PhpScriptEngine(PhpScriptEngineFactory factory) {
	super(factory);
    }

    /**
     * Create a new ScriptEngine with bindings.
     * 
     * @param n
     *            the bindings
     */
    public PhpScriptEngine(Bindings n) {
	this();
	setBindings(n, ScriptContext.ENGINE_SCOPE);
    }
    /*
     * (non-Javadoc)
     * 
     * @see javax.script.Invocable#call(java.lang.String, java.lang.Object[])
     */
    protected Object invoke(String methodName, Object[] args)
            throws ScriptException, NoSuchMethodException {
	if (methodName == null) {
	    release();
	    return null;
	}

	if (scriptClosure == null) {
	    if (Logger.logLevel > 4)
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
    public Object invokeFunction(String methodName, Object[] args)
            throws ScriptException, NoSuchMethodException {
	return invoke(methodName, args);
    }

    private void checkPhpClosure(Object thiz) {
	if (thiz == null)
	    throw new IllegalStateException(
	            "PHP script did not pass its continuation to us!. Please check if the previous call to eval() reported any errors. Or else check if it called OUR continuation.");
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.script.Invocable#call(java.lang.String, java.lang.Object,
     * java.lang.Object[])
     */
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

    /** {@inheritDoc} */
    public Object invokeMethod(Object thiz, String methodName, Object[] args)
            throws ScriptException, NoSuchMethodException {
	return invoke(thiz, methodName, args);
    }

    /** {@inheritDoc} */
    public Object getInterface(Class clasz) {
	checkPhpClosure(script);
	return getInterface(script, clasz);
    }

    /** {@inheritDoc} */
    public Object getInterface(Object thiz, Class clasz) {
	checkPhpClosure(thiz);
	Class[] interfaces = clasz == null ? Util.ZERO_PARAM
	        : new Class[] { clasz };
	return PhpProcedure.createProxy(interfaces,
	        (PhpProcedure) Proxy.getInvocationHandler(thiz));
    }

    
    protected Object doEvalPhp(Reader reader, ScriptContext context)
            throws ScriptException {
	if ((continuation != null) || (reader == null))
	    release();
	if (reader == null)
	    return null;

	setNewContextFactory();
	env.put(X_JAVABRIDGE_INCLUDE, EMPTY_INCLUDE);

	try {
	    this.script = doEval(getArgs(reader), context);
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
	    this.resultProxy = new ResultProxy(this).withResult(ctx.getContext().getExitCode());
	    handleRelease();
	}

	return resultProxy;
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
			            if (engines == null)
				        return;
			            synchronized (engines) {
				        for (Iterator ii = engines
				                .iterator(); ii.hasNext(); ii
				                        .remove()) {
				            PhpScriptEngine e = (PhpScriptEngine) ii
				                    .next();
				            e.releaseInternal();
				        }
			            }
			        }
		            });
		} catch (SecurityException e) {
		    /* ignore */}
	    }
	    engines.add(this);
	}
    }

    private void releaseInternal() {
	super.release();
    }

    /** {@inheritDoc} */
    public void release() {
	synchronized (engines) {
	    releaseInternal();
	    engines.remove(this);
	}
    }

}
