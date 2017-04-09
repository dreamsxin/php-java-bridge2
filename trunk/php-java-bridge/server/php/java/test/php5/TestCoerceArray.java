package php.java.test.php5;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.OutputStreamWriter;

import org.junit.Test;

import php.java.test.ScriptEngineTestBase;

public class TestCoerceArray extends ScriptEngineTestBase {
    @Test
    public void testClosure() throws Throwable {
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	e.getContext().setWriter(new OutputStreamWriter(out));
	e.eval(new FileReader(new File("testCoerceArray.php")));
	((Closeable)e).close();
	assertEquals("bleh", out.toString());
    }
    public static String f(TestCoerceArray[] ar) {
	StringBuffer buf = new StringBuffer();
	for(int i=0; i<ar.length; i++) {
	    buf.append(ar[i].toString());
	}
	return buf.toString();
    }
}