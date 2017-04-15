package php.java.test;

import static org.junit.Assert.fail;

import org.junit.Test;

public class TestDiscovery extends ScriptEngineTestBase {

    @Test
    public void testDiscovery() {
	try {
	    StringBuffer s = new StringBuffer();
	    e.put("hello", new StringBuffer("hello world"));
	    e.put("s", s);
	    e.eval("<?php " + "$s = java_context()->getAttribute('s');"
	            + "$s->append(java_values(java_context()->getAttribute('hello')));"
	            + "/*echo java_values($s);*/"
	            + "java_context()->setAttribute('hello', '!', 100);"
	            + "?>");
	    s.append(e.get("hello"));
	    if (!(s.toString().equals("hello world!"))) {
		fail("ERROR");
	    }
	} catch (Exception e) {
	    fail(String.valueOf(e));
	}
    }
}
