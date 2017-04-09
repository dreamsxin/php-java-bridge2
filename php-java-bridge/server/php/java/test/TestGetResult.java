package php.java.test;

import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.junit.Test;

public class TestGetResult extends ScriptEngineTestBase {
    @Test
    public void testDiscovery() throws Exception {
	    String result = String.valueOf(e.eval("<?php exit(2+3);"));

	    if (!result.equals("5"))
		throw new ScriptException("test failed");

	    e = ScriptEngineHelper.getPhpScriptEngine4Test();
	    OutputStream out = new ByteArrayOutputStream();
	    Writer w = new OutputStreamWriter(out);
	    e.getContext().setWriter(w);
	    e.getContext().setErrorWriter(w);
	    Object o = e.eval("<?php exit(7+9); ?>");
	    result = String.valueOf(o); // note that this releases the engine,
	                                // the next invoke will implicitly call
	                                // eval() with an empty script

	    ((Invocable) e).invokeFunction("phpinfo", new Object[] {});
	    e.eval((Reader)null); // terminate the new engine as well
	    
	    if (!result.equals("16"))
		throw new ScriptException("test failed");
	    if (out.toString().length() == 0)
		throw new ScriptException("test failed");
    }

    public void testGetResult() {
	try {
	    ScriptEngine e = ScriptEngineHelper.getPhpScriptEngine4Test();

	    String result = String
	            .valueOf(e.eval("<?php echo ('299'); exit(197);"));

	    if (!result.equals("199"))
		throw new ScriptException("test failed");
	} catch (Exception e1) {
	    fail(String.valueOf(e1));
	}
    }
}
