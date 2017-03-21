package php.java.test;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;

import php.java.bridge.Util;
import php.java.fastcgi.FCGIHeaderParser;
import php.java.fastcgi.FCGIProxy;

public class TestCGI {

    private String[] args;
    private HashMap env;

    @Before
    public void setup() throws IOException {
	args = new String[] {
	        new File(new File("server/WEB-INF/cgi"), "php-cgi")
	                .getAbsolutePath() };
	env = new HashMap();
	env.put("REDIRECT_STATUS", "200");
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

	FCGIProxy fastCGIProxy = new FCGIProxy(args, env, out, System.err,
	        FCGIHeaderParser.DEFAULT_HEADER_PARSER);

	new Thread(fastCGIProxy).start();
	fastCGIProxy.release();
	assertEquals("hello world", out.toString());

    }
}
