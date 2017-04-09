package php.java.test.php5;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.OutputStreamWriter;

import org.junit.Test;

import php.java.test.ScriptEngineTestBase;

public class TestClosureCache extends ScriptEngineTestBase {
    @Test
    public void testClosure() throws Throwable {
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	e.getContext().setWriter(new OutputStreamWriter(out));
	e.eval(new FileReader(new File("testClosure.php")));
	((Closeable)e).close();
	assertEquals("bleh", out.toString());
    }
    @Test
    public void testClosure1() throws Throwable {
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	e.getContext().setWriter(new OutputStreamWriter(out));
	e.eval(new FileReader(new File("testClosure1.php")));
	((Closeable)e).close();
	assertEquals("bleh", out.toString());
    }
    @Test
    public void testClosureCache() throws Throwable {
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	e.getContext().setWriter(new OutputStreamWriter(out));
	e.eval(new FileReader(new File("testClosureCache.php")));
	((Closeable)e).close();
	assertEquals("bleh", out.toString());
    }

    public interface IFace {}
    public interface IChild extends IFace {}
    public static String proc1(IChild o) { return "ichild::"+o;}
    public static String proc1(Object o) { return "object::"+o;}
    public static String proc1(IFace o) { return "iface::"+o;}
}
