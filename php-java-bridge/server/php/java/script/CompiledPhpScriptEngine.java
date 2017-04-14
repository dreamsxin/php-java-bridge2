package php.java.script;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptException;

import php.java.bridge.util.Logger;

public class CompiledPhpScriptEngine extends PhpScriptEngine
        implements Compilable {

    public CompiledPhpScriptEngine(Bindings n) {
	this();
	setBindings(n, ScriptContext.ENGINE_SCOPE);
    }

    public CompiledPhpScriptEngine() {
	this(new PhpScriptEngineFactory());
    }

    public CompiledPhpScriptEngine(PhpScriptEngineFactory factory) {
	super();
	this.factory = factory;
	getContext(); // update context in parent as a side effect
	setStandardBindings();
    }

    public Object evalCompiled(ScriptContext context) throws ScriptException {
	ScriptContext current = getContext();
	if (current != context)
	    try {
		setContext(context);
		return evalCompiledPhp();
	    } finally {
		setContext(current);
	    }
	else
	    return evalCompiledPhp();
    }

    public CompiledScript compile(final Reader reader) throws ScriptException {
	try {
	    compilePhp(reader);
	    return new CompiledPhpScript(this);
	} catch (IOException e) {
	    throw new ScriptException(e);
	}
    }

    private Object evalCompiledPhp() throws ScriptException {
	if ((continuation != null))
	    releaseCompiled();

	setNewContextFactory(env);
	env.put("SCRIPT_FILENAME", scriptFile.getAbsolutePath());

	try {
	    this.script = doEvalPhp(null);
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

    private boolean compiled;
    private boolean compile;
    private File emptyScriptFile;

    public boolean isCompiled() {
	return compiled && continuation != null;
    }

    protected void compilePhp(Reader reader)
            throws IOException, ScriptException {
	compile = true;
	evalPhp(reader);
	compiled = true;
    }

    protected void compileScript(Reader reader) throws ScriptException {
	super.compileScript(reader);
	if (compile) {
	    try {
		emptyScriptFile = File.createTempFile("pjbempty", ".php");
	    } catch (IOException e) {
		throw new ScriptException(e);
	    }

	    env.put("SCRIPT_FILENAME", emptyScriptFile.getAbsolutePath());
	}
    }

    public CompiledScript compile(String script) throws ScriptException {
	Reader reader = new StringReader(script);
	try {
	    return compile(reader);
	} finally {
	    try {
		reader.close();
	    } catch (IOException e) {
		Logger.printStackTrace(e);
	    }
	}
    }

    protected void deleteScript() {
	super.deleteScript();
	if (emptyScriptFile != null) {
	    try {
		emptyScriptFile.delete();
	    } catch (Exception e) {
		Logger.printStackTrace(e);
	    }
	    emptyScriptFile = null;
	}
    }

    protected void releaseCompiled() {
	synchronized (engines) {
	    releaseInternal(false);
	    engines.remove(this);
	}
    }

    public void close() throws IOException {
	if (!compiled)
	    release();
	else
	    releaseCompiled();
    }
}
