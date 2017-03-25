package php.java.test;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.junit.Test;

public class TestCli {

    @Test
    public void testSimple() {
	try {
	    ByteArrayOutputStream errOut = new ByteArrayOutputStream();
	    Writer err = new OutputStreamWriter(errOut);
	    ScriptEngine e = ScriptEngineHelper.getPhpInteractiveScriptEngine4Test();

	    e.getContext().setErrorWriter(err);
	    e.eval("$a=new java('java.util.Vector');");
	    e.eval("$a->add(1);");
	    e.eval("$a->add(2);");
	    e.eval("$a->add(3);");
	    e.eval("class C{function toString() {return 'foo';}}");
	    e.eval("$a->add(java_closure(new C()));");
	    e.eval("$b=new java('java.util.Vector');");
	    e.eval("$b->add(1);");
	    e.eval("$b->add(2);");
	    e.eval("$b->add(3);");
	    assertTrue("[1, 2, 3]".equals(e.eval("echo $b")));
	    assertTrue("[1, 2, 3, foo]".equals(e.eval("echo $a")));
	    ((Closeable) e).close();
	} catch (Exception e) {
	    fail(String.valueOf(e));
	}
    }

    public void testClosure() {
	try {
	    ByteArrayOutputStream errOut = new ByteArrayOutputStream();
	    Writer err = new OutputStreamWriter(errOut);
	    ScriptEngine eng = (new ScriptEngineManager())
	            .getEngineByName("php-interactive");
	    eng.getContext().setErrorWriter(err);
	    eng.eval("$a=new java('java.util.Vector');");
	    eng.eval("$a->add(1);");
	    eng.eval("$a->add(2);");
	    try {
		eng.eval("die();");
	    } catch (Exception e) {
		assertTrue(e.getMessage()
		        .equals("php.java.bridge.Request$AbortException"));
	    }
	    assertTrue(eng.eval("echo $a").equals("[1, 2]"));
	    ((Closeable) eng).close();
	} catch (Exception e) {
	    fail(String.valueOf(e));
	}
    }
}
