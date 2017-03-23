/*-*- mode: Java; tab-width:8 -*-*/

package php.java.script;

import java.io.Reader;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptException;

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

    protected Object doEvalPhp(Reader reader, ScriptContext context)
            throws ScriptException {
	if ((continuation != null) || (reader == null))
	    release();
	if (reader == null)
	    return null;

	setNewContextFactory();

	try {
	    this.script = doEval(getArgs(reader), context);
	} catch (Exception e) {
	    Logger.printStackTrace(e);
	    if (e instanceof RuntimeException)
		throw (RuntimeException) e;
	    if (e instanceof ScriptException)
		throw (ScriptException) e;
	    throw new ScriptException(e);
	} finally {

	    // release the engine, so that any error reported by the script can
	    // trigger a Java exception
	    release();
	}

	return resultProxy;
    }
}
