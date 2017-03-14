/*-*- mode: Java; tab-width:8 -*-*/
package php.java.fastcgi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.util.Map;

import php.java.bridge.ILogger;
import php.java.bridge.Util;
import php.java.bridge.Util.Process;
import php.java.bridge.http.IFCGIProcess;
import php.java.bridge.http.IFCGIProcessFactory;

/*
 * Copyright (C) 2017 Jost Bökemeier
 *
 * The PHP/Java Bridge ("the library") is free software; you can
 * redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either
 * version 2, or (at your option) any later version.
 *
 * The library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the PHP/Java Bridge; see the file COPYING.  If not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA.
 *
 * Linking this file statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole
 * combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent
 * modules, and to copy and distribute the resulting executable under
 * terms of your choice, provided that you also meet, for each linked
 * independent module, the terms and conditions of the license of that
 * module.  An independent module is a module which is not derived from
 * or based on this library.  If you modify this library, you may extend
 * this exception to your version of the library, but you are not
 * obligated to do so.  If you do not wish to do so, delete this
 * exception statement from your version.
 */

public abstract class FCGIConnectionFactory {
    protected boolean promiscuous;
    protected IFCGIProcessFactory processFactory;
    
    /* The fast CGI Server process on this computer. Switched off per default. */
    protected IFCGIProcess proc = null;
    private boolean fcgiStarted = false;
    private final Object fcgiStartLock = new Object();
    protected Exception lastException;
    
    /**
     * Create a new FCGIConnectionFactory using a FCGIProcessFactory
     * @param processFactory the FCGIProcessFactory
     */
    public FCGIConnectionFactory(IFCGIProcessFactory processFactory) {
	this.processFactory = processFactory;
    }
    /**
     * Start the FastCGI server
     * @return false if the FastCGI server failed to start.
     */
    public final boolean startServer(ILogger logger) {
	/*
	 * Try to start the FastCGI server,
	 */
	synchronized(fcgiStartLock) {
	    if(!fcgiStarted) {
		    if(canStartFCGI()) 
			try {
			    bind(logger);
			} catch (Exception e) {/*ignore*/}
		
		fcgiStarted = true; // mark as started, even if start failed
	    } 
	}
	return fcgiStarted;
    }
    /**
     * Test the FastCGI server.
     * @throws ConnectException thrown if a IOException occured.
     */
    public abstract void test() throws ConnectException;
    
    protected abstract void waitForDaemon() throws UnknownHostException, InterruptedException;
    protected final void runFcgi(Map env, String php, boolean includeJava, boolean includeDebugger) {
	int c;
	byte buf[] = new byte[Util.BUF_SIZE];
	try {
	    Process proc = doBind(env, php, includeJava, includeDebugger);
	    if(proc==null || proc.getInputStream() == null) return;
	    /// make sure that the wrapper script launcher.sh does not output to stdout
	    proc.getInputStream().close();
	    // proc.OutputStream should be closed in shutdown, see PhpCGIServlet.destroy()
	    InputStream in = proc.getErrorStream();
	    while((c=in.read(buf))!=-1) System.err.write(buf, 0, c);
	    try { in.close(); } catch (IOException e) {/*ignore*/}
	} catch (Exception e) {
	    lastException = e;
	    System.err.println("Could not start FCGI server: " + e);
	};
    }

    protected abstract Process doBind(Map env, String php, boolean includeJava, boolean includeDebugger) throws IOException;
    protected void bind(final ILogger logger) throws InterruptedException, IOException {
	Thread t = (new Util.Thread("JavaBridgeFastCGIRunner") {
		public void run() {
		    Map env = (Map) processFactory.getEnvironment().clone();
		    env.put("PHP_JAVA_BRIDGE_FCGI_CHILDREN", processFactory.getPhpConnectionPoolSize());
		    env.put("PHP_FCGI_MAX_REQUESTS", processFactory.getPhpMaxRequests());
		    runFcgi(env, processFactory.getPhp(), processFactory.getPhpIncludeJava(), processFactory.getPhpIncludeDebugger());
		}
	    });
	t.start();
	waitForDaemon();
    }

    private boolean canStartFCGI() {
	return processFactory.canStartFCGI();
    }
	
    public void destroy() {
	synchronized(fcgiStartLock) {
	    fcgiStarted = false;
	    if(proc==null) return;  	
	    try {
		OutputStream out = proc.getOutputStream();
		if (out != null) out.close();
	    } catch (IOException e) {
		Util.printStackTrace(e);
	    }
	    try {
		proc.waitFor();
	    } catch (InterruptedException e) {
		// ignore
	    }
	    proc.destroy();
	    proc=null;
	}
    }

    /**
     * Connect to the FastCGI server and return the connection handle.
     * @return The FastCGI Channel
     * @throws ConnectException thrown if a IOException occured.
     */
    public abstract Connection connect() throws ConnectException;

    /**
     * For backward compatibility the "JavaBridge" context uses the port 9667 (Linux/Unix) or <code>\\.\pipe\JavaBridge@9667</code> (Windogs).
     */
    public void initialize() {
	setDynamicPort();
    }
    protected abstract void setDynamicPort();
    protected abstract void setDefaultPort();

    /**
     * Return a command which may be useful for starting the FastCGI server as a separate command.
     * @param base The context directory
     * @param php_fcgi_max_requests The number of requests, see appropriate servlet option.
     * @return A command string
     */
    public abstract String getFcgiStartCommand(String base, String php_fcgi_max_requests);
	
    /**
     * Find a free port or pipe name. 
     * @param select If select is true, the default name should be used.
     */
    public abstract void findFreePort(boolean select);

    /**
     * Create a new ChannelFactory.
     * @return The concrete ChannelFactory (NP or Socket channel factory).
     */
    public static FCGIConnectionFactory createConnectionFactory(FCGIConnectionPool fcgiConnectionPool, int maxRequests,IFCGIProcessFactory processFactory, boolean promiscuous) {
	if(Util.USE_SH_WRAPPER)
	    return new SocketConnectionFactory(fcgiConnectionPool, maxRequests, processFactory, promiscuous);
	else 
	    return new PipeConnectionFactory(fcgiConnectionPool, maxRequests,processFactory);
    }
}
