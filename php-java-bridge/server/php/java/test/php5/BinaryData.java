package php.java.test.php5;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

import org.junit.Test;

import php.java.test.ScriptEngineTestBase;

public class BinaryData extends ScriptEngineTestBase {

    @Test
    public void testBinaryData() throws Throwable {
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	e.getContext().setWriter(new OutputStreamWriter(out));
	e.eval(new FileReader(new File("binaryData.php")));
	((Closeable)e).close();
	assertEquals("bleh", out.toString());
    }
    @Test
    public void testJavaInspect() throws Throwable {
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	e.getContext().setWriter(new OutputStreamWriter(out));
	e.eval(new FileReader(new File("java_inspect.php")));
	((Closeable)e).close();
	assertEquals("bleh", out.toString());
    }

    
    public byte[] b;
    public byte[] getData(int len) {
	b = new byte[len];
	for(int i=0; i<b.length; i++) {
	    b[i]=(byte)(i%255);
	}
	return b;
    }

    public byte[] compare(byte[] b2) throws java.lang.Exception {
	if(b.length!=b2.length) throw new RuntimeException();
	for(int i=0; i<b2.length; i++)
	    if(b2[i]!=b[i]) throw new RuntimeException();
	return b2;
    }
    public String toString()  {
	try {
	    return new String(b, "ASCII");
	} catch (UnsupportedEncodingException e) {
	    e.printStackTrace();
	}
	return null;
    }
}
