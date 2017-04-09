package php.java.test.php5;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.OutputStreamWriter;

import org.junit.Test;

import php.java.test.ScriptEngineTestBase;

public class TestClass extends ScriptEngineTestBase {
    @Test
    public void test() throws Throwable {
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	e.getContext().setWriter(new OutputStreamWriter(out));
	e.eval(new FileReader(new File("testClass2.php")));
	((Closeable)e).close();
	assertEquals("bleh", out.toString());
    }
    public String getName() {
	return "test clazz";
    }
}