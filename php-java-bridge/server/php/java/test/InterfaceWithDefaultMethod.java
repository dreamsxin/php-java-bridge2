package php.java.test;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.OutputStreamWriter;
import java.io.StringReader;

import org.junit.Test;

public class InterfaceWithDefaultMethod extends ScriptEngineTestBase {
 
    public interface IWithDefaultMethod {
	public String getString1();
	default public String getString2() {
	    return "defaultMethod";
	}
    }
    
    @Test
    public void testInterfaceWithDefaultMethod() throws Throwable {
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	e.getContext().setWriter(new OutputStreamWriter(out));
	
	e.eval(new StringReader("<?php function getString1() { return 'phpMethod'; }\n"+
	"$cc=java_closure(null, null, array(java('php.java.test.InterfaceWithDefaultMethod$IWithDefaultMethod')));\n"+
	"echo $cc->getString1();echo '\n'; echo $cc->getString2(); "));
	
	((Closeable)e).close();
	
	assertEquals("phpMethod\ndefaultMethod", out.toString());
    }
}

    
