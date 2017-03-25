package php.java.test;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestPhpScriptEngine {

    private ScriptEngine e;
    private Bindings b;
    private String script;

    @Before
    public void setUp() throws Exception {
	e = ScriptEngineHelper.getPhpScriptEngine4Test();
	b = new SimpleBindings();
	script = "<?php exit(1+2);?>";
    }

    @After
    public void tearDown() throws Exception {
	((Closeable) e).close();
    }

    @Test
    public void testEvalReader() {
	try {
	    Reader r = new StringReader(script);
	    assertTrue("3".equals(String.valueOf(e.eval(r))));
	    r.close();
	} catch (Exception e) {
	    fail(String.valueOf(e));
	}
    }

    @Test
    public void testEvalReaderBindings() {
	try {
	    Reader r = new StringReader(script);
	    assertTrue("3".equals(String.valueOf(e.eval(r, b))));
	    r.close();
	} catch (Exception e) {
	    fail(String.valueOf(e));
	}
    }

    @Test
    public void testEvalString() {
	try {
	    assertTrue("3".equals(String.valueOf(e.eval(script))));
	} catch (ScriptException e) {
	    fail(String.valueOf(e));
	}
    }

    @Test
    public void testEvalStringBindings() {
	try {
	    assertTrue("3".equals(String.valueOf(e.eval(script, b))));
	} catch (ScriptException e) {
	    fail(String.valueOf(e));
	}
    }

    @Test
    public void testEvalCompilableString() {
	try {
	    ByteArrayOutputStream out = new ByteArrayOutputStream();
	    OutputStreamWriter writer = new OutputStreamWriter(out);
	    ScriptEngine e = ScriptEngineHelper.getPhpScriptEngine4Test();

	    e.getContext().setWriter(writer);
	    ((java.io.FileFilter) e).accept(
	            new File(System.getProperty("java.io.tmpdir", "/tmp")
	                    + File.separator + "test.php"));
	    CompiledScript s = ((Compilable) e).compile("<?php echo 1+2;?>");

	    long t1 = System.currentTimeMillis();
	    for (int i = 0; i < 100; i++) {
		s.eval();
		assertTrue("3".equals(out.toString()));
		out.reset();
	    }
	    long t2 = System.currentTimeMillis();
	    System.out.println("testEvalCompilableString time:" + (t2 - t1));

	} catch (Exception e) {
	    fail(String.valueOf(e));
	}
    }

}
