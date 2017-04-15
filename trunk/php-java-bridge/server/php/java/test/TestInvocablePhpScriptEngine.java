package php.java.test;

import static org.junit.Assert.assertTrue;

import java.io.Closeable;
import java.io.Reader;
import java.io.StringReader;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestInvocablePhpScriptEngine {

    private ScriptEngine e;
    private Bindings b;
    private String script;
    private String invocableScript;

    @Before
    public void setUp() throws Exception {
	e = ScriptEngineHelper.getPhpScriptEngine4Test();
	b = e.getBindings(ScriptContext.ENGINE_SCOPE);
	script = "<?php function f($arg) {return 1 + (string)$arg;}; exit(1+2); ?>";
	invocableScript = "<?php function f($arg) {return 1 + (string)$arg;}; ?>"; // no
	                                                                           // exit()

    }

    @After
    public void tearDown() throws Exception {
	((Closeable) e).close();
    }

    @Test
    public void testEvalReader() throws Exception {
	    Reader r = new StringReader(script);
	    assertTrue("3".equals(String.valueOf(e.eval(r))));
	    r.close();
    }

    @Test
    public void testEvalReaderBindings() throws Exception {
	    Reader r = new StringReader(script);
	    assertTrue("3".equals(String.valueOf(e.eval(r, b))));
	    r.close();
    }

    @Test
    public void testEvalString() throws ScriptException {
	    assertTrue("3".equals(String.valueOf(e.eval(script))));
    }

    @Test
    public void testEvalStringBindings() throws Exception {
	    assertTrue("3".equals(String.valueOf(e.eval(script, b))));
    }


    @Test
    public void testInvokeFunction() throws Exception {
	e.eval(invocableScript);
	assertTrue(6 == ((Integer) ((Invocable) e).invokeFunction("f",
	        new Object[] { "5" })).intValue());
    }

    @Test
    public void testCompile() throws Exception {

	CompiledScript c = ((Compilable) e).compile(invocableScript);
	c.eval();
	assertTrue(6 == ((Integer) ((Invocable) e).invokeFunction("f",
	        new Object[] { "5" })).intValue());
    }

    @Test
    public void testInvokeFunctionCompiled() throws Exception {
	ScriptEngine e = ScriptEngineHelper.getPhpScriptEngine4Test();

	CompiledScript c = ((Compilable) e).compile(invocableScript);
	c.eval();
	assertTrue(6 == ((Integer) ((Invocable) e).invokeFunction("f",
	        new Object[] { "5" })).intValue());
	c.eval();
	assertTrue(6 == ((Integer) ((Invocable) e).invokeFunction("f",
	        new Object[] { "5" })).intValue());
	((Closeable) e).close();
    }

    // public void testInvokeMethod() {
    // fail("Not yet implemented");
    // }
    //
    // public void testGetInterfaceClass() {
    // fail("Not yet implemented");
    // }
    //
    // public void testGetInterfaceObjectClass() {
    // fail("Not yet implemented");
    // }
    // public void testInvokeFunctionCompiled() {
    // fail("Not yet implemented");
    // }
    //
    // public void testInvokeMethodCompiled() {
    // fail("Not yet implemented");
    // }
    //
    // public void testGetInterfaceClassCompiled() {
    // fail("Not yet implemented");
    // }
    //
    // public void testGetInterfaceObjectClassCompiled() {
    // fail("Not yet implemented");
    // }
}
