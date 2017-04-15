package php.java.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.script.ScriptException;

import org.junit.Test;

public class TestScript extends ScriptEngineTestBase {
    @Test
    public void test() throws IOException, ScriptException {
	OutputStream out = new ByteArrayOutputStream();
	Writer w = new OutputStreamWriter(out);
	e.getContext().setWriter(w);
	e.getContext().setErrorWriter(w);

	Object res = e.eval(
	        "<?php if(java_is_true(java_context()->call(java_closure()))) print('test okay'); exit(9); ?>");

	assertEquals(9, ((Number) res).intValue());
	assertTrue("test okay".equals(String.valueOf(out)));
    }

}
