package php.java.bridge;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * Used only for release tests
 */
public class TestInstallation {
    public static void main(String[] args) throws ScriptException, IOException {
	ScriptEngine e = new ScriptEngineManager()
	        .getEngineByExtension("phtml");
	OutputStream out = new ByteArrayOutputStream();
	OutputStream err = new ByteArrayOutputStream();

	e.getContext().setWriter(new OutputStreamWriter(out));
	e.getContext().setErrorWriter(new OutputStreamWriter(err));

	e.eval("<?php echo new java('java.lang.String', 'hello php from java');");

	((Closeable)e).close();
	
	if ("hello php from java".equals(out.toString())) {
	    System.out.println("installation okay");
	} else {
	    System.err.println("err: " + err.toString());
	    System.out.println("out: " + out.toString());
	}
	
	System.exit(0);
    }
}
