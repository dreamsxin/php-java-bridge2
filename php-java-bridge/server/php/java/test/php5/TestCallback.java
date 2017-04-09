package php.java.test.php5;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.OutputStreamWriter;

import org.junit.Test;

import php.java.test.ScriptEngineTestBase;

public class TestCallback extends ScriptEngineTestBase {
    @Test
    public void testCallback() throws Throwable {
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	e.getContext().setWriter(new OutputStreamWriter(out));
	e.eval(new FileReader(new File("callback.php")));
	((Closeable)e).close();
	assertEquals("bleh", out.toString());
    }
    @Test
    public void testCallbackMap() throws Throwable {
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	e.getContext().setWriter(new OutputStreamWriter(out));
	e.eval(new FileReader(new File("callbackMap.php")));
	((Closeable)e).close();
	assertEquals("bleh", out.toString());
    }
    @Test
    public void testCallbackName() throws Throwable {
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	e.getContext().setWriter(new OutputStreamWriter(out));
	e.eval(new FileReader(new File("callbackName.php")));
	((Closeable)e).close();
	assertEquals("bleh", out.toString());
    }
    @Test
    public void testCallback1() throws Throwable {
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	e.getContext().setWriter(new OutputStreamWriter(out));
	e.eval(new FileReader(new File("callback1.php")));
	((Closeable)e).close();
	assertEquals("bleh", out.toString());
    }
    @Test
    public void testCallback2() throws Throwable {
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	e.getContext().setWriter(new OutputStreamWriter(out));
	e.eval(new FileReader(new File("callback2.php")));
	((Closeable)e).close();
	assertEquals("bleh", out.toString());
    }


}
