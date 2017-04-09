package php.java.test;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.OutputStreamWriter;

import javax.script.Compilable;
import javax.script.CompiledScript;

import org.junit.Before;
import org.junit.Test;

public class TestSimpleCompileable {

    private CompiledScript script;

    @Before
    public void setUp() throws Exception {
	script =((Compilable)(ScriptEngineHelper.getPhpScriptEngine4Test())).compile(
        "<?php echo 'Hello '.java_context()->get('hello').'!'; ?>");
    }

    @Test
    public void test() throws Exception {
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	script.getEngine().getContext().setWriter(new OutputStreamWriter(out));
	  script.getEngine().put("hello", "world!");
	  script.eval();
	  script.getEngine().put("hello", String.valueOf(this));
	  script.eval();
	  
	  ((Closeable)(script.getEngine())).close();
	  assertEquals("Hello world!!", out.toString());
    }
}
