package php.java.test;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.junit.Test;

public class TestException  {

    @Test
    public void test() throws Exception {
	ScriptEngine e = ScriptEngineHelper.getPhpScriptEngine4Test();
	OutputStream out = new ByteArrayOutputStream();
	Writer w = new OutputStreamWriter(out);
	e.getContext().setWriter(w);
	e.getContext().setErrorWriter(w);
	try {
	    e.eval("<?php bleh();?>");
	} catch (Throwable ex) {
	    if (out.toString().length() == 0)
		throw new Exception("test failed");
	    return;
	}
	fail("test failed");
    }
}
