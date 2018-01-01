package php.java.test;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.OutputStreamWriter;
import java.io.StringReader;

import org.junit.Test;

public class TestSystemExit extends ScriptEngineTestBase {
 
    public static int exit(int arg) {
	return arg;
    }
    @Test
    public void testSystemExit() throws Throwable {
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	e.getContext().setWriter(new OutputStreamWriter(out));
	
	e.eval(new StringReader("<?php use php\\java\\test\\TestSystemExit as JSystem;\n"+
	"echo JSystem::exit(42);\n"));
	
	((Closeable)e).close();
	
	assertEquals("42", out.toString());
    }
}

    
