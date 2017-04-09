package php.java.test.php5;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.OutputStreamWriter;

import org.junit.Test;

import php.java.test.ScriptEngineTestBase;

public class HashSetFactory extends ScriptEngineTestBase {
    @Test
    public void testIterator() throws Throwable {
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	e.getContext().setWriter(new OutputStreamWriter(out));
	e.eval(new FileReader(new File("iterator.php")));
	((Closeable)e).close();
	assertEquals("bleh", out.toString());
    }
    @Test
    public void test() throws Throwable {
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	e.getContext().setWriter(new OutputStreamWriter(out));
	e.eval(new FileReader(new File("iteratorInterface.php")));
	((Closeable)e).close();
	assertEquals("bleh", out.toString());
    }

    public static java.util.HashSet getSet() {
	return new java.util.HashSet();
    }
}
