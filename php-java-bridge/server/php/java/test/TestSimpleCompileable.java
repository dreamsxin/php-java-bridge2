package php.java.test;

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
	  script.getEngine().put("hello", "world!");
	  script.eval();
	  script.getEngine().put("hello", String.valueOf(this));
	  script.eval();
    }
}
