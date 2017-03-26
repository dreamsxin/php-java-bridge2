package php.java.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileWriter;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.junit.Test;

import php.java.bridge.parser.Request.AbortException;

public class TestInteractiveRequestAbort  {

    @Test
    public void test() throws Exception {
	String devNull = new File("/dev/null").exists() ? "/dev/null"
	        : "devNull";
	ScriptEngine e = ScriptEngineHelper.getPhpScriptEngine4Test();
	e.getContext().setErrorWriter(new FileWriter(new File(devNull)));
	e.getContext().setWriter(new FileWriter(new File(devNull)));

	try {
	    e.eval("function toString() {return 'hello'; }; echo java_closure(); echo new JavaException('java.lang.Exception', 'hello'); echo JavaException('foo')");
	} catch (ScriptException ex) {
	    Throwable orig = ex.getCause();
	    if (orig instanceof AbortException) {
		return;
	    }
	}

	fail("test failed");
    }

}
