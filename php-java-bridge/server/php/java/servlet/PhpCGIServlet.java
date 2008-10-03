/*-*- mode: Java; tab-width:8 -*-*/

package php.java.servlet;

/*
 * Copyright (C) 2003-2007 Jost Boekemeier
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
 * THE COPYRIGHT HOLDER(S) OR AUTHOR(S) BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

import javax.script.ScriptException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import php.java.bridge.Util;
import php.java.bridge.Util.Process;
import php.java.bridge.http.IContextFactory;
import php.java.script.PhpScriptException;
import php.java.script.servlet.PhpScriptTemporarilyOutOfResourcesException;
import php.java.servlet.fastcgi.FastCGIServlet;

/**
 * Handles requests from internet clients.  <p> This servlet can handle GET/POST
 * requests directly. These requests invoke the php-cgi machinery from
 * the CGI or FastCGI servlet.  Although the servlet to php-cgi back
 * to servlet path is quite slow (compared with the http front end/j2ee back end
 * setup) and consumes two servlet instances
 * instead of only one, it can be useful as a replacement for a system php
 * installation, see the README in the <code>WEB-INF/cgi</code>
 * folder.  It is currently used for our J2EE test/demo.  </p>
 * @see php.java.bridge.JavaBridge
 *  */
public class PhpCGIServlet extends FastCGIServlet {

    public static final boolean USE_SH_WRAPPER = new File("/bin/sh").exists();
    private static final long serialVersionUID = 38983388211187962L;

    /**
     * A local port which will be used by the SocketContextServer for high-speed local communication.<br>
     * The SocketContextServer may use ports [9567,...[9667 (bound to
     * the local interface), if named pipes are not available (Windows
     * only). Use the system property
     * <code>php.java.bridge.no_socket_server=true</code> to switch it
     * off (not recommended).
     * @see php.java.bridge.http.SocketContextServer
     * @see php.java.bridge.http.PipeContextServer
     */
    public static final int CGI_CHANNEL = 9567;

    /**
     * A local port which will be used instead of the current SSL port. Requires that the J2EE server or
     * servlet engine listens on this local port.<br>
     * If SSL is used, the CGI servlet passes this number instead of the current port number to PHP.
     * Example setting for tomcat conf/server.xml (add the line marked with a <code>+</code>):<blockquote><code>
     *&lt;Service name="Catalina"&gt;<br>
     *[...]<br>
     *+  &lt;Connector port="9157" address="127.0.0.1"  /&gt;<br>
     *[...]<br>
     *&lt;/Service&gt;<br>
     *</code></blockquote><br>
     * To use a custom port#, switch off <code>override_hosts</code> in the <code>WEB-INF/web.xml</code> and add the following lines to your <code>php.ini</code> file:<blockquote><code>
     * java.hosts=127.0.0.1:&lt;CUSTOM_NON_SSL_PORT&gt;<br>
     * java.servlet=On<br>
     * </code></blockquote><br>
     */
    public static final int CGI_SSL_CHANNEL = 9157;
    
    /**
     * The max. number of concurrent CGI requests. 
     * <p>The value should be less than 1/2 of the servlet engine's thread pool size as this 
     * servlet also consumes an instance of PhpJavaServlet.</p>
     */
    private static final int CGI_MAX_REQUESTS = Integer.parseInt(Util.THREAD_POOL_MAX_SIZE)-1;
    private static int servletPoolSize = CGI_MAX_REQUESTS;
    private final CGIRunnerFactory defaultCgiRunnerFactory = new CGIRunnerFactory();
    
    private String DOCUMENT_ROOT;
    private String SERVER_SIGNATURE;
    /**@inheritDoc*/
    public void init(ServletConfig config) throws ServletException {
    	super.init(config);

	DOCUMENT_ROOT = getRealPath(context, "");
	SERVER_SIGNATURE = context.getServerInfo();
    }
    
    private static final Object lockObject = new Object();   
    private static boolean servletPoolSizeDetermined = false;
    /** Return the servlet pool size */
    public static int getServletPoolSize () {
	synchronized (lockObject) {
	    if (servletPoolSizeDetermined) return servletPoolSize;
	    servletPoolSizeDetermined = true;

	    int size = Util.getMBeanProperty("*:type=ThreadPool,name=http*", "maxThreads");
	    if (size > 2) servletPoolSize = size;
	
	    return servletPoolSize;
	}
    }
    
    public void destroy() {
      super.destroy();
    }
    
    /**
     * Adjust the standard tomcat CGI env. CGI only.
     */
    public class CGIEnvironment extends FastCGIServlet.CGIEnvironment {
    	protected SimpleServletContextFactory sessionFactory;
	public HttpServletRequest req;
    	
	protected CGIEnvironment(HttpServletRequest req, HttpServletResponse res, ServletContext context) {
	    super(req, res, context);
	    this.req = req;
	}

	/** PATH_INFO and PATH_TRANSLATED not needed for PHP, SCRIPT_FILENAME is enough */
        protected void setPathInfo(HttpServletRequest req, HashMap envp, String sCGIFullName) {
            envp.put("SCRIPT_FILENAME", nullsToBlanks(getRealPath(context, servletPath)));          
        }
	protected boolean setCGIEnvironment(HttpServletRequest req, HttpServletResponse res) {
	    boolean ret = super.setCGIEnvironment(req, res);
	    if(ret) {
	    	/* Inform the client that we are a cgi servlet and send the re-direct port */
	      String override;
	      if(override_hosts) { 
		    StringBuffer buf = new StringBuffer();
		    if(!req.isSecure())
			buf.append("h:");
		    else
			buf.append("s:");
		    buf.append("127.0.0.1");
		    buf.append(":");
		    buf.append(this.environment.get("SERVER_PORT")); 
		    buf.append('/');
		    buf.append(req.getRequestURI());
		    buf.append("javabridge");
		    override = buf.toString();
	        }
		else 
		    override = "";

	        this.environment.put("X_JAVABRIDGE_OVERRIDE_HOSTS", override);
	        // same for fastcgi, which already contains X_JAVABRIDGE_OVERRIDE_HOSTS=/ in its environment
	        this.environment.put("X_JAVABRIDGE_OVERRIDE_HOSTS_REDIRECT", override); 
	        this.environment.put("REDIRECT_STATUS", "200");
	        this.environment.put("SERVER_SOFTWARE", Util.EXTENSION_NAME);
	        this.environment.put("HTTP_HOST", this.environment.get("SERVER_NAME")+":"+this.environment.get("SERVER_PORT"));
	        String remotePort = null;
	        try {
	            remotePort = String.valueOf(req.getRemotePort());
	        } catch (Throwable t) {
	            remotePort = String.valueOf(t);
	        }
	        this.environment.put("REMOTE_PORT", remotePort);
	        String query = req.getQueryString();
	        if(query!=null)
	            this.environment.put("REQUEST_URI", nullsToBlanks(req.getRequestURI() + "?" + query));
	        else
	            this.environment.put("REQUEST_URI", nullsToBlanks(req.getRequestURI()));	          
	        
	        this.environment.put("SERVER_ADDR", req.getServerName());
	        this.environment.put("SERVER_SIGNATURE", SERVER_SIGNATURE);
	        this.environment.put("DOCUMENT_ROOT", DOCUMENT_ROOT);
	        if(req.isSecure()) this.environment.put("HTTPS", "On");
	        
	        
		/* send the session context now, otherwise the client has to 
		 * call handleRedirectConnection */
	    	String id = req.getHeader("X_JAVABRIDGE_CONTEXT");
	    	if(id==null) 
	    	    id = (ctx=ServletContextFactory.addNew(PhpCGIServlet.this, PhpCGIServlet.this.getServletContext(), req, req, res)).getId();
	    	this.environment.put("X_JAVABRIDGE_CONTEXT", id);
	    }
	    return ret;
	        	
	}
	protected String[] findCGI(String pathInfo, String webAppRootDir,
				   String contextPath, String servletPath,
				   String cgiPathPrefix) {
	    String[] retval;
	    /*
	     * Now that FCGI is started (or failed to start), connect to the FCGI server 
	     */
	    if((retval=super.findCGI(pathInfo, webAppRootDir, contextPath, servletPath, cgiPathPrefix))!=null) return retval;
	    cgiRunnerFactory = defaultCgiRunnerFactory;
	
	    // Needed by CGIServlet
	    return new String[] {
		php, // sCGIFullPath, the full path of the PHP executable: used by getCommand(), X_TOMCAT_SCRIPT_PATH and getWorkingDirectory()
		contextPath+servletPath,  		// sCGIScriptName: the php file relative to webappRootDir, e.g.: /index.php 
		empty_string,       	// sCGIFullName: not used (used in setPathInfo, which we don't use)
		empty_string};      	// sCGIName: not used anywhere
	}
    }
    
    /**
     * Create a cgi environment. Used by cgi only.
     * @param req The request
     * @param servletContext The servlet context
     * @return The new cgi environment.
     */
    protected CGIServlet.CGIEnvironment createCGIEnvironment(HttpServletRequest req, HttpServletResponse res, ServletContext servletContext) {
	CGIEnvironment env = new CGIEnvironment(req, res, servletContext);
	env.init(req, res);
	return env;
    }

    protected class CGIRunnerFactory extends CGIServlet.CGIRunnerFactory {
        protected CGIServlet.CGIRunner createCGIRunner(CGIServlet.CGIEnvironment cgiEnv) {
            return new CGIRunner(cgiEnv);
	}
    }

    protected static class HeaderParser extends Util.HeaderParser {
    	private CGIRunner runner;
	protected HeaderParser(CGIRunner runner) {
	    this.runner = runner;
    	}
    	public void parseHeader(String header) {
	    runner.addHeader(header);
    	}
    }
    protected class CGIRunner extends CGIServlet.CGIRunner {
	protected IContextFactory ctx;
	
	protected CGIRunner(CGIServlet.CGIEnvironment env) {
	    super(env);
	    ctx = ((CGIEnvironment)env).ctx;
	}
        protected void execute() throws IOException, ServletException {
	    Process proc = null;
	    
	    InputStream natIn = null;
	    OutputStream natOut = null;
	    ByteArrayOutputStream natErr = new ByteArrayOutputStream();
	    
	    InputStream in = null;
	    OutputStream out = null;

	    try {
        	proc = Util.ProcessWithErrorHandler.start(Util.getPhpArgs(new String[]{php}), wd, env, phpTryOtherLocations, preferSystemPhp, natErr);

        	byte[] buf = new byte[BUF_SIZE];// headers cannot be larger than this value!

        	// the post variables
        	in = stdin;
    		natOut = proc.getOutputStream();
        	if(in!=null) {
		    int n;
    		    while((n=in.read(buf))!=-1) {
    			natOut.write(buf, 0, n);
    		    }
    		}
        	natOut.flush();
        	
        	// header and body
         	natIn = proc.getInputStream();
    		out = response.getOutputStream();

    		Util.parseBody(buf, natIn, out, new Util.HeaderParser() {public void parseHeader(String header) {addHeader(header);}});

    		try {
     		    proc.waitFor();
     		} catch (InterruptedException e) {
    		    /*ignore*/
    		}
    	    } finally {
    		if(in!=null) try {in.close();} catch (IOException e) {/*ignore*/}
    		if(natIn!=null) try {natIn.close();} catch (IOException e) {/*ignore*/}
    		if(natOut!=null) try {natOut.close();} catch (IOException e) {/*ignore*/}
    		if(proc!=null) try {proc.destroy();} catch (Exception e) {/*ignore*/}
    		
    		if (ctx!=null) ctx.release();
    		ctx = null;
    	    }
    	    
    	    if (proc!=null)
    	    {
    	        if(natErr.size()>0) Util.logMessage(natErr.toString());
    	        try {proc.checkError(); } catch (Util.Process.PhpException e) {throw new ServletException(e);}
    	    }
    	    
        }
    } //class CGIRunner
    
    private static short count = 0;

    /**
     * This is necessary because some servlet engines only have one global servlet pool. 
     * Since the PhpCGIServlet depends on the outcome of the PhpJavaServlet, we can have only
     * <code>pool-size/2</code> PhpCGIServlet instances without running into a dead lock: PhpCGIServlet instance
     * waiting for result from PhpJavaServlet instance, Servlet engine trying to allocate a PhpJavaServlet instance,
     * waiting for the pool to release an old servlet instance. This may never happen when the pool is 
     * filled up with PhpCGIServlet instances all waiting for PhpJavaServlet instances, which the pool cannot deliver.
     * <p>
     * It is recommended to use a servlet engine which uses a thread pool per servlet or to use the Apache/IIS
     * front-end instead.
     * </p>
     * @param res The servlet response
     * @return true if the number of active PhpCGIServlet instances is less than cgi_max_requests.
     * @throws ServletException
     * @throws IOException
     */
    private boolean checkPool(HttpServletResponse res) throws ServletException, IOException {
	if(count++>=(getServletPoolSize()/2-1)) {
            res.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Out of system resources. Try again shortly or use the Apache or IIS front end instead.");
            Util.logFatal("Out of system resources. Adjust php.java.bridge.threads and the pool size in server.xml.");
            return false;
        }
        return true;
    }
    private static int engineCount = 0;
    /**
     * Since each script captures up to two servlet instances, we must check the servlet engine's thread pool.
     * Check if a script continuation is available and capture it. Otherwise throw a PhpScriptException.
     * @param how many entries should be reserved
     * @throws ScriptException
     */
    /*
     * @see PhpCGIServlet#checkPool(javax.servlet.http.HttpServletResponse)
     * @see JavaBridgeRunner#doGet(php.java.bridge.http.HttpRequest, php.java.bridge.http.HttpResponse)
     */
    public static void reserveContinuation () throws ScriptException {
	if (engineCount++ >= (PhpCGIServlet.getServletPoolSize()/3-1) || count>=(getServletPoolSize()/2-1)) 
	    throw new PhpScriptTemporarilyOutOfResourcesException ("Out of system resources. Adjust php.java.bridge.threads and the pool size in server.xml.");
    }
    /** 
     * Release a captured continuation
     */
    public static void releaseReservedContinuation () {
	--engineCount;
    }
    
    /**
     * Used when running as a cgi binary only.
     * 
     *  (non-Javadoc)
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void handle(HttpServletRequest req, HttpServletResponse res, boolean handleInput)
	throws ServletException, IOException {
    	try {
    	   if(!checkPool(res)) return;
 	    super.handle(req, res, handleInput);
    	} catch (IOException e) {
    	    try {res.reset();} catch (Exception ex) {/*ignore*/}
	    StringBuffer buf = new StringBuffer(getRealPath(getServletConfig().getServletContext(), cgiPathPrefix));
	    buf.append(File.separator);
	    buf.append("php-cgi-");
	    buf.append(Util.osArch);
	    buf.append("-");
	    buf.append(Util.osName);
	    buf.append("[.sh]|[.exe]");
    	    String wrapper = buf.toString();
 	    ServletException ex = new ServletException("An IO exception occured. " +
	    		"Probably php was not installed as \"/usr/bin/php-cgi\" or \"c:/php/php-cgi.exe\"\n or \""+wrapper+"\".\n" +
	    		"Please see \"php_exec\" in your WEB-INF/web.xml and WEB-INF/cgi/README for details.", e);
	    php=null;
	    checkCgiBinary(getServletConfig());
	    throw ex;
    	} catch (SecurityException sec) {
    	    try {res.reset();} catch (Exception ex) {/*ignore*/}
    	    String base = CGIServlet.getRealPath(context, cgiPathPrefix);
    	    
	    ServletException ex = new ServletException(
		    "A security exception occured, could not run PHP.\n" + channelName.getFcgiStartCommand(base, php_fcgi_max_requests));
	    fcgiIsAvailable=fcgiIsConfigured;
	    php=null;
	    checkCgiBinary(getServletConfig());
	    throw ex;    	    
    	} catch (ServletException e) {
    	    try {res.reset();} catch (Exception ex) {/*ignore*/}
    	    throw e;
    	}
    	catch (Throwable t) {
    	    try {res.reset();} catch (Exception ex) {/*ignore*/}
	    Util.printStackTrace(t);
    	    throw new ServletException(t);
    	} finally {
    	    count--;
    	}
   }
}
