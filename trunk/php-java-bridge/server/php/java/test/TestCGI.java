package php.java.test;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import php.java.fastcgi.ConnectException;
import php.java.fastcgi.FCGIConnectionPool;
import php.java.fastcgi.FCGIHeaderParser;
import php.java.fastcgi.FCGIHelper;
import php.java.fastcgi.FCGIProxy;

public class TestCGI  {

    private String[] args;
    private HashMap env;

    @Before
    public void setup() throws IOException {
	args = new String[] {
	        new File(new File("WebContent/WEB-INF/cgi"), "php-cgi")
	                .getAbsolutePath() };
	env = new HashMap();
	env.put("REDIRECT_STATUS", "200");
	File scriptFile = File.createTempFile("tmp", "php").getAbsoluteFile();
	FileOutputStream fos = new FileOutputStream(scriptFile);
	fos.write(CODE.getBytes());
	fos.close();
	env.put("SCRIPT_FILENAME", scriptFile.getAbsolutePath());

    }

    private static final Object globalCtxLock = new Object();
    private static FCGIConnectionPool fcgiConnectionPool = null;
    protected void setupFastCGIServer(String[] args, Map env) throws ConnectException {
	synchronized(globalCtxLock) { //FIXME refactor
	    if(null == fcgiConnectionPool) {
		fcgiConnectionPool = FCGIConnectionPool.createConnectionPool(args, env, new FCGIHelper());
	    }
	}

    }

    private static final String CODE = "<?php echo 'hello world'; error_log('bleh'); exit(9);?>";

    @Test
    public void testFastCGIRunner() throws Exception {
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	ByteArrayOutputStream err = new ByteArrayOutputStream();
	setupFastCGIServer(args, env); 

	FCGIProxy fastCGIProxy = new FCGIProxy(args, env, out, err,
	        FCGIHeaderParser.DEFAULT_HEADER_PARSER, fcgiConnectionPool);

	new Thread(fastCGIProxy).start();
	fastCGIProxy.release();
	fcgiConnectionPool.destroy();
	assertEquals("hello world", out.toString());
	assertEquals("bleh", err.toString().trim());

    }
}
