package php.java.test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.junit.Test;

import php.java.bridge.Util;
import php.java.bridge.http.IFCGIProcess;

public class TestCGI {

    public class FCGIProcess extends Util.Process implements IFCGIProcess {
	    String realPath;
	    public FCGIProcess(String[] args, boolean includeJava, boolean includeDebugger, String cgiDir, String pearDir, String webInfDir, File homeDir, Map env, String realPath, boolean tryOtherLocations, boolean preferSystemPhp) throws IOException {
		super(args, includeJava, includeDebugger, cgiDir, pearDir, webInfDir, homeDir, env, tryOtherLocations, preferSystemPhp);
		this.realPath = realPath;
	    }
	    protected String[] getArgumentArray(String[] php, String[] args) {
	        LinkedList buf = new LinkedList();
	        if(Util.USE_SH_WRAPPER) {
		    buf.add("/bin/sh");
		    buf.add(realPath+File.separator+Util.osArch+"-"+Util.osName+File.separator+"launcher.sh");
		    buf.addAll(java.util.Arrays.asList(php));
		    for(int i=1; i<args.length; i++) {
			buf.add(args[i]);
		    }
	        } else {
		    //buf.add(realPath+File.separator+Util.osArch+"-"+Util.osName+File.separator+"launcher.exe");
		    buf.addAll(java.util.Arrays.asList(php));
		    for(int i=1; i<args.length; i++) {
			buf.add(args[i]);
		    }
		}
	        return (String[]) buf.toArray(new String[buf.size()]);
	    }
	    /* (non-Javadoc)
	     * @see php.java.servlet.fastcgi.IFCGIProcess#start()
	     */
	    public void start() throws NullPointerException, IOException {
	        super.start();
	    }
	}

    
    
    
    @Test
    public void testCGI() throws Exception {
	String[] args = new String[]{new File("server/WEB-INF/cgi/x86-windows/php-cgi.exe").getAbsolutePath()};
	boolean includeJava = false;
	boolean includeDebugger = false;
	File homeDir = new File("server/WEB-INF/cgi/x86-windows/").getAbsoluteFile();
	String webInfDir;
	String pearDir;
	String cgiDir = pearDir = webInfDir = homeDir.getAbsolutePath();
	Map env = new HashMap();
	env.put("PHP_FCGI_CHILDREN", "5");
	env.put("REDIRECT_STATUS","200");
	env.put("PHP_FCGI_MAX_REQUESTS", "10");
	String realPath = "";
	boolean tryOtherLocations = false;
	boolean preferSystemPhp = false;
	FCGIProcess proc = new FCGIProcess(args, includeJava, includeDebugger, cgiDir, pearDir, webInfDir, homeDir, env, realPath, tryOtherLocations, preferSystemPhp);
	proc.start();
	
	//proc.waitFor();

    }
}
