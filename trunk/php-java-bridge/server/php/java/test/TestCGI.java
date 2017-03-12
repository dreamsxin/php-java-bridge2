package php.java.test;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import php.java.bridge.Util;
import php.java.bridge.http.HeaderParser;
import php.java.bridge.http.IFCGIProcess;
import php.java.script.CGIRunner;
import php.java.script.FastCGIProxy;
import php.java.script.HttpProxy;
import php.java.script.IPhpScriptEngine;
import php.java.script.ResultProxy;

public class TestCGI {

    private String[] args;
    private HashMap env;
    @Before
    public void setup() throws IOException {
	args = new String[]{new File("server/WEB-INF/cgi/x86-windows/php-cgi.exe").getAbsolutePath()};
	env = new HashMap();
	env.put("PHP_JAVA_BRIDGE_FCGI_CHILDREN", "5");
	env.put("REDIRECT_STATUS","200");
	env.put("PHP_FCGI_MAX_REQUESTS", "10");
	File scriptFile = File.createTempFile("tmp", "php").getAbsoluteFile();
	FileOutputStream fos = new FileOutputStream(scriptFile);
	fos.write(CODE.getBytes());
	fos.close();
	env.put("SCRIPT_FILENAME", scriptFile.getAbsolutePath());

    }
    private static final String CODE = "<?php echo 'hello world'; exit(9);?>";
    @Test
    public void testFastCGIRunner() throws Exception {
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	
	IPhpScriptEngine engine = Mockito.mock(IPhpScriptEngine.class);
	ResultProxy result = new ResultProxy(engine);
	
	// TODO: reader is ignored
	FastCGIProxy fastCGIProxy = new FastCGIProxy(null, env, 
		out,  System.err, HeaderParser.DEFAULT_HEADER_PARSER, 
		result, Util.getLogger()) {
	    public String getPhp() {
		return args[0];
	    }
	};
	
	new Thread(fastCGIProxy).start();
	Object script = fastCGIProxy.getPhpScript();
	assertEquals("hello world", out.toString());
	assertEquals(9, result.getResult());

    }
    @Test
    public void testCGIRunner() throws Exception {
	ByteArrayOutputStream out = new ByteArrayOutputStream();

	IPhpScriptEngine engine = Mockito.mock(IPhpScriptEngine.class);
	ResultProxy result = new ResultProxy(engine);
	CGIRunner runner = new HttpProxy(new StringReader(CODE), env, out, System.err, HeaderParser.DEFAULT_HEADER_PARSER, result, Util.getLogger()) {
	    public String getPhp() {
		return args[0];
	    }
	};
	
	new Thread(runner).start();
	Object script = runner.getPhpScript();
	assertEquals("hello world", out.toString());
	assertEquals(9, result.getResult());

    }

}
