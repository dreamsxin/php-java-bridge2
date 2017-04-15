package php.java.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import javax.script.Invocable;
import javax.script.ScriptException;

import org.junit.Test;

public class TestCli extends ScriptEngineTestBase {

    @Test
    public void testInvokeFunction() throws NoSuchMethodException, ScriptException, IOException {
	    Invocable i = (Invocable)e;
	    Object res = i.invokeFunction("java_eval", "<?php exit(1+2); ?>");
	    assertEquals("3", res.toString());
	    res = i.invokeFunction("java_eval", "<?php exit(3+5); ?>");
	    assertEquals("8", res.toString());
	    res = i.invokeFunction("java_eval", "return 5+8;");
	    assertEquals("13", res.toString());
	    
	    res = i.invokeFunction("java_eval", "$GLOBALS['a']=12;");
	    res = i.invokeFunction("java_eval", "return $GLOBALS['a']+1;");
	    assertEquals("13", res.toString());
	    res = i.invokeFunction("java_eval", "function foo() {return 1;}");
	    res = i.invokeFunction("java_eval", "function bar() {return 2;}");
	    res = i.invokeFunction("java_eval", "return foo()+bar();");
	    assertEquals("3", res.toString());
    }
    @Test
    public void testSimple() {
	try {
	    ByteArrayOutputStream errOut = new ByteArrayOutputStream();
	    Writer err = new OutputStreamWriter(errOut);

	    e.getContext().setErrorWriter(err);
	    Invocable i = (Invocable)e;
	    i.invokeFunction("java_eval", "$GLOBALS['a']=new java('java.util.Vector');");
	    i.invokeFunction("java_eval", "$GLOBALS['a']->add(1);");
	    i.invokeFunction("java_eval", "$GLOBALS['a']->add(2);");
	    i.invokeFunction("java_eval", "$GLOBALS['a']->add(3);");
	    i.invokeFunction("java_eval", "class C{function toString() {return 'foo';}}");
	    i.invokeFunction("java_eval", "$GLOBALS['a']->add(java_closure(new C()));");
	    i.invokeFunction("java_eval", "$GLOBALS['b']=new java('java.util.Vector');");
	    i.invokeFunction("java_eval", "$GLOBALS['b']->add(1);");
	    i.invokeFunction("java_eval", "$GLOBALS['b']->add(2);");
	    i.invokeFunction("java_eval", "function f() {return 3;}");
	    i.invokeFunction("java_eval", "$GLOBALS['b']->add(f());");
	    Object res = i.invokeFunction("java_eval", "return $GLOBALS['b'];");
	    assertTrue("[1, 2, 3]".equals(String.valueOf(res)));
	    res = i.invokeFunction("java_eval", "return $GLOBALS[a];");
	    assertTrue("[1, 2, 3, foo]".equals(String.valueOf(res)));
	} catch (Exception e) {
	    fail(String.valueOf(e));
	}
    }

}
