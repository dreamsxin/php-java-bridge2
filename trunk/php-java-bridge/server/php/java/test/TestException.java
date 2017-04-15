package php.java.test;

import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.script.ScriptException;

import org.junit.Ignore;
import org.junit.Test;

public class TestException  extends ScriptEngineTestBase {

    // FIXME: needs protocol change to get exception through
    @Ignore
    @Test
    public void test() throws Exception {
	OutputStream out = new ByteArrayOutputStream();
	Writer w = new OutputStreamWriter(out);
	e.getContext().setWriter(w);
	e.getContext().setErrorWriter(w);
	try {
	    e.eval("<?php throw new Exception('bleh');?>");
	} catch (ScriptException ex) {
	    if (out.toString().length() == 0)
		throw new Exception("test failed");
	    return;
	}
	fail("test failed");
    }
}
