package php.java.test.php5;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.util.*;

import org.junit.Test;

import php.java.test.ScriptEngineTestBase;

public class TestArrayMapCollection extends ScriptEngineTestBase {
    @Test
    public void test() throws Throwable {
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	e.getContext().setWriter(new OutputStreamWriter(out));
	e.eval(new FileReader(new File("testArrayMapCollection.php")));
	((Closeable)e).close();
	assertEquals("bleh", out.toString());
    }
   public TestArrayMapCollection[] array(TestArrayMapCollection[] x) {
	return x;
    }
    public Map map(Map m) {
	return m;
    }
    public List list(List m) {
	return m;
    }
    public Collection collection(Collection m) {
	return m;
    }
}
