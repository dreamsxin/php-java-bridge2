package php.java.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestBindings {

    private ScriptEngine e;
    private ScriptEngine e2;
    private Bindings b;
    private String script;


    @Before
    public void setUp() throws Exception {
	e = ScriptEngineHelper.getPhpScriptEngine4Test();
	e2 = ScriptEngineHelper.getPhpScriptEngine4Test();

	List<String> asList = new ArrayList<String>(Arrays.asList((String[])e.get("javax.script.argv")));
	asList.addAll(Arrays.asList(new String[] { "1", "2", "3" }));
	e.put("javax.script.argv", asList.toArray(new String[]{}));

	b = new SimpleBindings();
	asList = new ArrayList(Arrays.asList((String[])e.get("javax.script.argv")));
	asList.addAll(Arrays.asList(new String[] { "1", "2", "3" }));
	b.put("javax.script.argv", asList.toArray(new String[]{}));

	script = "<?php " + "$argv = java_context()->get('javax.script.argv');"
	        + "$result = (string)$argv[1]+ (string)$argv[2]+ (string)$argv[3];"
	        + "exit($result);" + "?>";
    }

    @After
    public void tearDown() throws Exception {
	((Closeable) e).close();
    }

    @Test
    public void testEvalReader() {
	try {
	    Reader r = new StringReader(script);
	    assertTrue("6".equals(String.valueOf(e.eval(r))));
	    r.close();
	} catch (Exception e) {
	    fail(String.valueOf(e));
	}
    }

    public void testEvalReaderBindings() {
	try {
	    Reader r = new StringReader(script);
	    assertTrue("6".equals(String.valueOf(e2.eval(r, b))));
	    r.close();
	} catch (Exception e) {
	    fail(String.valueOf(e));
	}
    }

    public void testEvalString() {
	try {
	    assertTrue("6".equals(String.valueOf(e.eval(script))));
	} catch (ScriptException e) {
	    fail(String.valueOf(e));
	}
    }

    public void testEvalStringBindings() {
	try {
	    assertTrue("6".equals(String.valueOf(e2.eval(script, b))));
	} catch (ScriptException e) {
	    fail(String.valueOf(e));
	}
    }

    public void testEvalCompilableString() {
	try {
	    ByteArrayOutputStream out = new ByteArrayOutputStream();
	    OutputStreamWriter writer = new OutputStreamWriter(out);
	    ScriptEngine e = ScriptEngineHelper.getPhpScriptEngine4Test();

	    e.put("javax.script.argv", new String[] { "1", "2", "3" });
	    e.getContext().setWriter(writer);
	    ((java.io.FileFilter) e).accept(
	            new File(System.getProperty("java.io.tmpdir", "/tmp")
	                    + File.separator + "test.php"));
	    CompiledScript s = ((Compilable) e).compile(
	            "<?php " + "$argv = java_context()->get('javax.script.argv');"
	                    + "$result = (string)$argv[0]+ (string)$argv[1]+ (string)$argv[2];"
	                    + "echo($result);" + "?>");

	    long t1 = System.currentTimeMillis();
	    for (int i = 0; i < 100; i++) {
		s.eval();
		assertTrue("6".equals(out.toString()));
		out.reset();
	    }
	    long t2 = System.currentTimeMillis();
	    System.out.println("testEvalCompilableString time:" + (t2 - t1));

	} catch (Exception e) {
	    fail(String.valueOf(e));
	}
    }
}
