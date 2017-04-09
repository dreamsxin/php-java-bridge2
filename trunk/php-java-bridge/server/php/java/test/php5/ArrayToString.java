package php.java.test.php5;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.OutputStreamWriter;

import org.junit.Test;

import php.java.test.ScriptEngineTestBase;

public class ArrayToString extends ScriptEngineTestBase {
    

    @Test
    public void test() throws Throwable {
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	e.getContext().setWriter(new OutputStreamWriter(out));
	e.eval(new FileReader(new File("arrayArray.php")));
	((Closeable)e).close();
	assertEquals("bleh", out.toString());
    }

    
    public static String arrayToString(String[] arr) {
	StringBuffer b = new StringBuffer(arr.length);
	for(int i=0; i<arr.length; i++) {
	    b.append(String.valueOf(arr[i]));
	    if(i+1<arr.length) b.append(" ");
	}
	return b.toString();
    }
    public static String arrayToString(int[] arr) {
	StringBuffer b = new StringBuffer(arr.length);
	for(int i=0; i<arr.length; i++) {
	    b.append(String.valueOf(arr[i]));
	    if(i+1<arr.length) b.append(" ");
	}
	return b.toString();
    }
    public static String arrayToString(double[] arr) {
	StringBuffer b = new StringBuffer(arr.length);
	for(int i=0; i<arr.length; i++) {
	    b.append(String.valueOf(arr[i]));
	    if(i+1<arr.length) b.append(" ");
	}
	return b.toString();
    }
    public static String arrayToString(boolean[] arr) {
	StringBuffer b = new StringBuffer(arr.length);
	for(int i=0; i<arr.length; i++) {
	    b.append(String.valueOf(arr[i]));
	    if(i+1<arr.length) b.append(" ");
	}
	return b.toString();
    }
}
