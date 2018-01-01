package php.java.test;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.OutputStreamWriter;
import java.io.StringReader;

import org.junit.Test;

public class InterfaceWithDefaultMethod extends ScriptEngineTestBase {
 
    public interface IWithDefaultMethod {
	public String getString1(String arg);
	default public String getString2(String arg) {
	    return "default"+arg;
	}
    }
    
    @Test
    public void testInterfaceWithDefaultMethod() throws Throwable {
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	e.getContext().setWriter(new OutputStreamWriter(out));
	
	e.eval(new StringReader("<?php function getString1($arg) { return $arg; }\n"+
	"$cc=java_closure(null, null, array(java('php.java.test.InterfaceWithDefaultMethod$IWithDefaultMethod')));\n"+
	"echo $cc->getString1('phpMethod');echo '\n'; echo $cc->getString2('Method'); "));
	
	((Closeable)e).close();
	
	assertEquals("phpMethod\ndefaultMethod", out.toString());
    }
}

    
