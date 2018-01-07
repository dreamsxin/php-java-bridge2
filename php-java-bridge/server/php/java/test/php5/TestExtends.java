package php.java.test.php5;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.junit.Ignore;
import org.junit.Test;

import php.java.test.ScriptEngineTestBase;

public class TestExtends extends ScriptEngineTestBase {
    public static abstract class TestClass {
	    public void runTest() {
		StringBuilder b = new StringBuilder();
		int val=1;
		func(b, val);

		b = new StringBuilder();
		val=1;
		func2(b, val);
	    }
	    
	    public abstract StringBuilder func2(StringBuilder s, int val);
	    
	    public StringBuilder func(StringBuilder s, int val) {
		return s.append(val+1);
	    }
	    public StringBuilder func3(StringBuilder s, int val) {
		return s.append(val+2);
	    }
    }
    @Test
    public void testExtends() throws Exception {
	OutputStream out = new ByteArrayOutputStream();
	OutputStream err = new ByteArrayOutputStream();
	Writer ou = new OutputStreamWriter(out);
	Writer er = new OutputStreamWriter(err);
	e.getContext().setWriter(ou);
	e.getContext().setErrorWriter(er);

	String res = String.valueOf(e.eval(new FileReader(new File("server/php/java/test/php5/script","TestExtends.php"))));
	assertEquals("", res);
	assertEquals("hello Java<br>\n" + 
		"from PHP<br>\n" + 
		"", out.toString());
	if (!err.toString().isEmpty()) {
	    System.err.println(err.toString());
	    fail(err.toString());
	}
    }
    
    @Test
    public void testInvocationHandler() throws Exception {
	OutputStream out = new ByteArrayOutputStream();
	OutputStream err = new ByteArrayOutputStream();
	Writer ou = new OutputStreamWriter(out);
	Writer er = new OutputStreamWriter(err);
	e.getContext().setWriter(ou);
	e.getContext().setErrorWriter(er);

	// the following code is wrong/bad style, but works. For a better example see testJavaEvalWithCleanEnv() below
	String.valueOf(e.eval("function func2($b) {echo 'func2:'.$b;};"
		+ "function func1($a){echo 'func1:'.$a;};"
		+ "$cc = java_closure();"
		+ "$ih=$cc->getInvocationHandler($cc);"
		+ "/*echo java_inspect($ih);*/"
		+ "$ih->invoke($cc,'func1', array(5));"
		+ "echo ',';"
		+ "$ih->invoke($cc,'func2', array(6));"
		+ ""
		));
	assertEquals("func1:5,func2:6", out.toString());
	if (!err.toString().isEmpty()) {
	    System.err.println(err.toString());
	}
	assertTrue(err.toString().isEmpty());
	
    }
    
    @Test
    public void testJavaEvalWithCleanEnv() throws Exception {
	OutputStream out = new ByteArrayOutputStream();
	OutputStream err = new ByteArrayOutputStream();
	Writer ou = new OutputStreamWriter(out);
	Writer er = new OutputStreamWriter(err);
	e.getContext().setWriter(ou);
	e.getContext().setErrorWriter(er);

	String.valueOf(e.eval(""
		+ "function func2($b){echo 'func2:'.$b;};"
		+ "function func1($a){echo 'func1:'.$a;};"
		+ "$c = java_closure();"
		+ "java('java.lang.reflect.Proxy')->getInvocationHandler($c)->invoke($c,'func1', array(5));"
		+ "echo ',';"
		+ "java('java.lang.reflect.Proxy')->getInvocationHandler($c)->invoke($c,'func2', array(6));"
		+ "/*exit(0);*/"
		));
	if (!err.toString().isEmpty()) {
	    System.err.println(err.toString());
	}
	assertTrue(err.toString().isEmpty());
	assertEquals("func1:5,func2:6", out.toString());
    }
    @Ignore //FIXME
    @Test
    public void testExtendsFromCustomClass() throws Exception {
	OutputStream out = new ByteArrayOutputStream();
	OutputStream err = new ByteArrayOutputStream();
	Writer ou = new OutputStreamWriter(out);
	Writer er = new OutputStreamWriter(err);
	e.getContext().setWriter(ou);
	e.getContext().setErrorWriter(er);

	String res = String.valueOf(e.eval(new FileReader(new File("server/php/java/test/php5/script","TestExtendsFromCustomClass.php"))));
	assertEquals("0", res);
	assertEquals("hello Java<br>\n" + 
		"from PHP<br>\n" + 
		"", out.toString());
	if (!err.toString().isEmpty()) {
	    System.err.println(err.toString());
	    fail(err.toString());
	}
   }
 }
