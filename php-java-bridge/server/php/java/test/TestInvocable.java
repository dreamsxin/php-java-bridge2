package php.java.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.script.Invocable;

import org.junit.Test;

public class TestInvocable  extends ScriptEngineTestBase {


    @Test
    public void test() throws Exception {


	Object result = e
	        .eval("<?php class f {function a($p) {return java_values($p)+1;}}\n"
	                + "java_context()->setAttribute('f', java_closure(new f()), 100); exit(4294967295); ?>");

	Invocable i = (Invocable) e;
	Object f = e.getContext().getAttribute("f", 100);
	assertTrue(2 == ((Integer) i.invokeMethod(f, "a",
	        new Object[] { new Integer(1) })).intValue());

	assertEquals(0xffffffff, ((Number) result).intValue());
    }

}
