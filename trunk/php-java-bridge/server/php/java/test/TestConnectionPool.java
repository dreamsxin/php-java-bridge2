package php.java.test;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.junit.Test;

public class TestConnectionPool {

    private void testfunc(CompiledScript script) throws Exception {
	ScriptEngine e = script.getEngine();
	OutputStream out = new ByteArrayOutputStream();
	Writer w = new OutputStreamWriter(out);
	e.getContext().setWriter(w);
	script.eval();

	Invocable i = (Invocable) e;
	i.invokeFunction("phpinfo", new Object[0]);
	((Closeable) e).close();
	String res = out.toString();
	if (res.length() <= 20) {
	    System.out.println(res);
	    throw new Exception("testConnectionPoolSmoke failed");
	}
	String tail = res.substring(res.length() - 20);
	assertEquals("</div></body></html>", tail);
    }

    @Test
    public void testConnectionPoolSmoke() throws Exception {
	ScriptEngine eng = ScriptEngineHelper.getPhpScriptEngine4Test();
	CompiledScript script = ((Compilable) eng).compile("");

	for (int i = 0; i < 2000; i++) {
	    try {
		testfunc(script);
	    } catch (Exception e) {
		ScriptException scriptException = new ScriptException(
		        "test failed after " + i + " iterations");
		scriptException.initCause(e);
		throw scriptException;
	    }
	}
    }

    private void testfunc2() throws Exception {
	ScriptEngine e = ScriptEngineHelper.getPhpScriptEngine4Test();
	OutputStream out = new ByteArrayOutputStream();
	Writer w = new OutputStreamWriter(out);
	e.getContext().setWriter(w);
	Invocable i = (Invocable) e;
	i.invokeFunction("phpinfo", new Object[0]);
	((Closeable) e).close();
	String res = out.toString();
	if (res.length() <= 20) {
	    System.out.println(res);
	    throw new Exception("testConnectionPoolSmoke failed");
	}
	String tail = res.substring(res.length() - 20);
	assertEquals("</div></body></html>", tail);
    }

    @Test
    public void testConnectionPoolSmoke2() throws Exception {
	for (int i = 0; i < 2000; i++) {
	    try {
		testfunc2();
	    } catch (Exception e) {
		throw new ScriptException(
		        "test failed after " + i + " iterations");
	    }
	}
    }

}
