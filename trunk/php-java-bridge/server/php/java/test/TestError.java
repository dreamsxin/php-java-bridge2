package php.java.test;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.junit.Test;

public class TestError extends ScriptEngineTestBase {

    @Test
    public void test() throws Exception {
	OutputStream out = new ByteArrayOutputStream();
	OutputStream err = new ByteArrayOutputStream();
	Writer ou = new OutputStreamWriter(out);
	Writer er = new OutputStreamWriter(err);
	e.getContext().setWriter(ou);
	e.getContext().setErrorWriter(er);
	Object result = e
	        .eval("<?php error_log('bleh'); echo 1+2; error_log('blah'); error_log('blub');?>");
	assertEquals("", result.toString());
	assertEquals("bleh\nblah\nblub\n", err.toString());
	assertEquals("3", out.toString());

    }
}
