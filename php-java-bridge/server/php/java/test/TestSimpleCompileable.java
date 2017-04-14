package php.java.test;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.OutputStreamWriter;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;

import org.junit.Before;
import org.junit.Test;

public class TestSimpleCompileable {

    private CompiledScript script;

    @Before
    public void setUp() throws Exception {

	script = ((Compilable) (ScriptEngineHelper.getPhpScriptEngine4Test()))
	        .compile(
	                "<?php $v = java_context()->get('v'); echo $v; exit((int)$v);");
    }

    @Test
    public void test() throws Exception {
	ByteArrayOutputStream out = new ByteArrayOutputStream();

	script.getEngine().getContext().setWriter(new OutputStreamWriter(out));
	script.getEngine().put("v", "5");
	String res = String.valueOf(script.eval());
	// assertEquals("5", res);
	script.getEngine().put("v", "6");
	res = String.valueOf(script.eval());
	// assertEquals("6", res);
	script.getEngine().put("v", "7");
	Object o = script.eval();
	((Closeable) script.getEngine()).close();
	assertEquals("567", out.toString());
//	assertEquals(3, ((Number) o).intValue());
    }
    @Test
    public void testEngine() throws Exception {
	ScriptEngine e = ScriptEngineHelper.getPhpScriptEngine4Test();
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	e.getContext().setWriter(new OutputStreamWriter(out));
	  e.eval("<?php echo 1+2");
	  e.eval("<?php echo 3+4");
	  
	  ((Closeable)(script.getEngine())).close();
	  assertEquals("37", out.toString());
    }
}
