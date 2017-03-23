package php.java.test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import junit.framework.TestCase;

public class TestScript extends TestCase {

    public TestScript(String name) {
	super(name);
    }

    protected void setUp() throws Exception {
	super.setUp();
    }

    protected void tearDown() throws Exception {
	super.tearDown();
    }

    public void test() throws IOException, ScriptException {
	ScriptEngine eng = (new ScriptEngineManager()).getEngineByName("php");
	String[] args = new String[] {
	        new File(new File("server/WEB-INF/cgi"), "php-cgi")
	                .getAbsolutePath() };
	eng.put(ScriptEngine.ARGV, args);

	OutputStream out = new ByteArrayOutputStream();
	Writer w = new OutputStreamWriter(out);
	eng.getContext().setWriter(w);
	eng.getContext().setErrorWriter(w);

	eng.eval(
	        "<?php if(java_is_true(java_context()->call(java_closure()))) print('test okay'); exit(9); ?>");
	eng.eval((String) null);

	assertTrue("test okay".equals(String.valueOf(out)));
    }

}
