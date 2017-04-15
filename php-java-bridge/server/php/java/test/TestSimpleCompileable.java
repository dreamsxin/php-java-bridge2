package php.java.test;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.List;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;

import org.junit.Before;
import org.junit.Test;

public class TestSimpleCompileable {

    private CompiledScript script;

    @Before
    public void setUp() throws Exception {

	script = ((Compilable) (ScriptEngineHelper.getPhpScriptEngine4Test()))
	        .compile(
	                "<?php $v = (string)java_context()->get('v'); echo $v; exit((int)$v);");
    }

    @Test
    public void test() throws Exception {
	ByteArrayOutputStream out = new ByteArrayOutputStream();

	script.getEngine().getContext().setWriter(new OutputStreamWriter(out));
	script.getEngine().put("v", "5");
	String res = String.valueOf(script.eval());
	assertEquals("5", res);
	script.getEngine().put("v", "6");
	res = String.valueOf(script.eval());
	assertEquals("6", res);
	script.getEngine().put("v", "7");
	Object o = script.eval();
	((Closeable) script.getEngine()).close();
	assertEquals("567", out.toString());
	assertEquals(7, ((Number) o).intValue());
    }

    @Test
    public void testInvocable() throws Exception {
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	CompiledScript script = ((Compilable) (ScriptEngineHelper.getPhpScriptEngine4Test()))
	        .compile("");

	script.getEngine().getContext().setWriter(new OutputStreamWriter(out));
	script.eval();
	
	((Invocable)(script.getEngine())).invokeFunction("phpinfo");
	((Closeable) script.getEngine()).close();
	String res = out.toString();
	if (res.length()<=20) {
	    System.out.println(res);
	    throw new Exception("testConnectionPoolSmoke failed");
	}
	String tail = res.substring(res.length()-20);
	assertEquals("</div></body></html>", tail);
    }
    
    @Test
    public void testEval() throws Exception {
	final ScriptEngine e = ScriptEngineHelper.getPhpScriptEngine4Test();
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	final Writer writer = new OutputStreamWriter(out);
	e.getContext().setWriter(writer);
	e.eval("<?php echo 1+2;");
	e.eval("<?php echo 3+4;");
	((Closeable)e).close();
	assertEquals("37", out.toString());
	
	CompiledScript script = ((Compilable) e).compile("echo java_context()->get('param');");
	final Bindings b = e.createBindings();
	b.put("param", "4");
	script.eval(b);
	ScriptContext ctx = new ScriptContext() {
	    
	    @Override
	    public void setWriter(Writer arg0) {}
	    
	    @Override
	    public void setReader(Reader arg0) {}
	    
	    @Override
	    public void setErrorWriter(Writer arg0) {}
	    
	    @Override
	    public void setBindings(Bindings arg0, int arg1) {}
	    
	    @Override
	    public void setAttribute(String arg0, Object arg1, int arg2) {}
	    
	    @Override
	    public Object removeAttribute(String arg0, int arg1) { return null; }
	    
	    @Override
	    public Writer getWriter() {return writer;}
	    
	    @Override
	    public List<Integer> getScopes() {return null;}
	    
	    @Override
	    public Reader getReader() {return null;}
	    
	    @Override
	    public Writer getErrorWriter() {return writer;}
	    
	    @Override
	    public Bindings getBindings(int arg0) {return b;}
	    
	    @Override
	    public int getAttributesScope(String arg0) {return 0;}
	    
	    @Override
	    public Object getAttribute(String arg0, int arg1) {return null;}
	    
	    @Override
	    public Object getAttribute(String arg0) {return null;}
	};
	b.put("param", "8");
	script.eval(ctx);
	
	((Closeable) e).close();
	
	assertEquals("3748", out.toString());
	
	
    }
}
