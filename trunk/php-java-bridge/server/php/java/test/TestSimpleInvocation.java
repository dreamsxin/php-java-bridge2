package php.java.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.junit.Test;

public class TestSimpleInvocation {

    @Test
    public void testSimple() throws Exception {
	ScriptEngine e = ScriptEngineHelper.getPhpScriptEngine4Test();
	OutputStream out = new ByteArrayOutputStream();
	Writer w = new OutputStreamWriter(out);
	e.getContext().setWriter(w);
	e.getContext().setErrorWriter(w);
	Invocable i = (Invocable) e;
	i.invokeFunction("phpinfo", new Object[0]);
	((Closeable) e).close();
	if (out.toString().length() == 0)
	    throw new ScriptException("test failed");
    }

    // FIXME do not create a new PHP file for each request => use Compilable instead of ScriptEngine
    private void testfunc() throws Exception {
	ScriptEngine e = ScriptEngineHelper.getPhpScriptEngine4Test();
	OutputStream out = new ByteArrayOutputStream();
	Writer w = new OutputStreamWriter(out);
	e.getContext().setWriter(w);
	Invocable i = (Invocable) e;
	i.invokeFunction("phpinfo", new Object[0]);
	((Closeable) e).close();
	String res = out.toString();
	if (res.length()<=20) {
	    System.out.println(res);
	    throw new Exception("testConnectionPoolSmoke failed");
	}
	String tail = res.substring(res.length()-20);
	assertEquals("</div></body></html>", tail);
    }

    @Test
    public void testConnectionPoolSmoke() throws Exception {
	for (int i=0; i<2000; i++) {
	 try {
	    testfunc();
	 } catch (Exception e) {
	     throw new ScriptException("test failed after " + i + " iterations");
	 }
	}
    }
    
}
