package php.java.test;

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

}
