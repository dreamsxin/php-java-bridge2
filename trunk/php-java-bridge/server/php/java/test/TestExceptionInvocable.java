package php.java.test;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.junit.Test;

public class TestExceptionInvocable {
    
    @Test
    public void test() throws Exception {
	ScriptEngineManager manager = new ScriptEngineManager();
	ScriptEngine e = manager.getEngineByName("php-invocable");
	OutputStream out = new ByteArrayOutputStream();
	Writer w = new OutputStreamWriter(out); 
	e.getContext().setWriter(w);
	e.getContext().setErrorWriter(w);
	e.eval("<?php function f() { throw new JavaException('java.io.IOException'); };?>");

	Invocable i = (Invocable) e;
	try {
	    i.invokeFunction("f", new Object[] {});
	} catch (ScriptException ex) {
	    ((Closeable)e).close();
	    if (out.toString().length() != 0) throw new Exception("test failed");
	    return;
	}
	((Closeable)e).close();
	fail("test failed");
    }    

}
