package php.java.test.php5;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import php.java.test.ScriptEngineTestBase;

public class Array extends ScriptEngineTestBase {
    @Test
    public void testArray() throws Throwable {
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	e.getContext().setWriter(new OutputStreamWriter(out));
	e.eval(new FileReader(new File("array.php")));
	((Closeable)e).close();
	assertEquals("bleh", out.toString());
    }
    @Test
    public void testArrayInterface() throws Throwable {
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	e.getContext().setWriter(new OutputStreamWriter(out));
	e.eval(new FileReader(new File("arrayInterface.php")));
	((Closeable)e).close();
	assertEquals("bleh", out.toString());
    }
    @Test
    public void testByteArray() throws Throwable {
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	e.getContext().setWriter(new OutputStreamWriter(out));
	e.eval(new FileReader(new File("byteArray.php")));
	((Closeable)e).close();
	assertEquals("bleh", out.toString());
    }
    @Test
    public void testCharArray() throws Throwable {
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	e.getContext().setWriter(new OutputStreamWriter(out));
	e.eval(new FileReader(new File("charArray.php")));
	((Closeable)e).close();
	assertEquals("bleh", out.toString());
    }
    @Test
    public void testNestesArrays() throws Throwable {
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	e.getContext().setWriter(new OutputStreamWriter(out));
	e.eval(new FileReader(new File("nestedArrays.php")));
	((Closeable)e).close();
	assertEquals("bleh", out.toString());
    }
    @Test
    public void testSendArray() throws Throwable {
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	e.getContext().setWriter(new OutputStreamWriter(out));
	e.eval(new FileReader(new File("sendArray.php")));
	((Closeable)e).close();
	assertEquals("bleh", out.toString());
    }
    
    private List entries;
    private Map conversion;

    public Array() {
	entries = new ArrayList(7);
	entries.add("Jakob der Lügner, Jurek Becker 1937--1997");
	entries.add("Mutmassungen über Jakob, Uwe Johnson, 1934--1984");
	entries.add("Die Blechtrommel, Günter Grass, 1927--");
	entries.add("Die Verfolgung und Ermordung Jean Paul Marats dargestellt durch die Schauspielgruppe des Hospizes zu Charenton unter Anleitung des Herrn de Sade, Peter Weiss, 1916--1982");
	entries.add("Der Mann mit den Messern, Heinrich Böll, 1917--1985");
	entries.add("Biedermann und die Brandstifter, Max Frisch, 1911--1991");
	entries.add("Seelandschaft mit Pocahontas, Arno Schmidt, 1914--1979");
	
	conversion = new HashMap();
	conversion.put("long", "java.lang.Byte java.lang.Short java.lang.Integer");
	conversion.put("boolean", "java.lang.Boolean");
	conversion.put("double", "java.lang.Double");
	conversion.put("null", "null");
	conversion.put("object", "depends");
	conversion.put("array of longs", "int[]");
	conversion.put("array of doubles", "double[]");
	conversion.put("array of boolean", "boolean[]");
	conversion.put("mixed array", "");
    }

    public Map getConversion() {
	return conversion;
    }
    public List getEntries() {
	return entries;
    }
    public String getEntry(int index) throws IndexOutOfBoundsException {
	return (String)entries.get(index);
    }
    public int getIndex(String title) {
	return entries.indexOf(title);
    }
}

    
