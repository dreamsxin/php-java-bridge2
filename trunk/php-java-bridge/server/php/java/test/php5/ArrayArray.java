package php.java.test.php5;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.OutputStreamWriter;

import org.junit.Test;

import php.java.test.ScriptEngineTestBase;

public class ArrayArray extends ScriptEngineTestBase implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    @Test
    public void testArrayArray() throws Throwable {
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	e.getContext().setWriter(new OutputStreamWriter(out));
	e.eval(new FileReader(new File("arrayArray.php")));
	((Closeable)e).close();
	assertEquals("bleh", out.toString());
    }
    
    public String[] array;

    public static ArrayArray[] create(int n) {
	ArrayArray[] a = new ArrayArray[n];

	for(int i=0; i<a.length; i++) {
	    a[i]=new ArrayArray();
	    a[i].array=new String[] {String.valueOf(i), null};
	}
	return a;
    }
}
	
	    
