/*-*- mode: Java; tab-width:8 -*-*/
package php.java.fastcgi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import php.java.bridge.Util;
import php.java.bridge.util.Logger;

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

public abstract class FCGIFactory {
    private static final int THREAD_POOL_MAX_SIZE = Integer
            .parseInt(Util.THREAD_POOL_MAX_SIZE);
    protected boolean promiscuous;
    protected CloseableConnection fcgiConnectionPool;

    /*
     * The fast CGI Server process on this computer. Switched off per default.
     */
    protected FCGIProcess proc = null;
    private boolean fcgiStarted = false;
    private final Object fcgiStartLock = new Object();
    protected Exception lastException;

    protected Map env;
    protected String[] args;
    protected int maxRequests;

    /**
     * Create a new FCGIConnectionFactory using a FCGIProcessFactory
     * 
     * @param processFactory
     *            the FCGIProcessFactory
     */
    public FCGIFactory(String args[], Map env,
            CloseableConnection fcgiConnectionPool, int maxRequests) {
	this.args = args;
	this.env = env;
	this.fcgiConnectionPool = fcgiConnectionPool;
	this.maxRequests = maxRequests;
    }

    public void startFCGIServer() throws ConnectException {

	findFreePort(true); // FIXME
	initialize();

	File cgiOsDir = Util.TMPDIR;
	File javaIncFile = new File(cgiOsDir, "launcher.sh");
	if (Util.USE_SH_WRAPPER) {
	    try {
		if (!javaIncFile.exists()) {
		    Field f = Util.LAUNCHER_UNIX.getField("bytes");
		    byte[] buf = (byte[]) f.get(Util.LAUNCHER_UNIX);
		    OutputStream out = new FileOutputStream(javaIncFile);
		    out.write(buf);
		    out.close();
		}
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	}
	File javaProxyFile = new File(cgiOsDir, "launcher.exe");
	if (!Util.USE_SH_WRAPPER) {
	    try {
		if (!javaProxyFile.exists()) {
		    OutputStream out = new FileOutputStream(javaProxyFile);
		    for (Class c : new Class[] { Util.LAUNCHER_WINDOWS,
		            Util.LAUNCHER_WINDOWS2, Util.LAUNCHER_WINDOWS3,
		            Util.LAUNCHER_WINDOWS4, Util.LAUNCHER_WINDOWS5,
		            Util.LAUNCHER_WINDOWS6, Util.LAUNCHER_WINDOWS7 }) {
			if (c != null) {
			    Field f = c.getField("bytes");
			    byte[] buf = (byte[]) f.get(c);
			    out.write(buf);
			}
		    }
		    out.close();
		}
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	}

	startServer();
	test();

    }

    /**
     * Start the FastCGI server
     * 
     * @return false if the FastCGI server failed to start.
     */
    private final boolean startServer() {
	/*
	 * Try to start the FastCGI server,
	 */
	synchronized (fcgiStartLock) {
	    if (!fcgiStarted) {
		if (canStartFCGI())
		    try {
			bind();
		    } catch (Exception e) {
			/* ignore */}

		fcgiStarted = true; // mark as started, even if start failed
	    }
	}
	return fcgiStarted;
    }

    /**
     * Test the FastCGI server.
     * 
     * @throws ConnectException
     *             thrown if a IOException occured.
     */
    public abstract void test() throws ConnectException;

    protected abstract void waitForDaemon()
            throws UnknownHostException, InterruptedException;

    protected final void runFcgi() {
	int c;
	byte buf[] = new byte[Util.BUF_SIZE];
	try {
	    FCGIProcess proc = doBind();
	    if (proc == null || proc.getInputStream() == null)
		return;
	    /// make sure that the wrapper script launcher.sh does not output to
	    /// stdout
	    proc.getInputStream().close();
	    // proc.OutputStream should be closed in shutdown, see
	    // PhpCGIServlet.destroy()
	    InputStream in = proc.getErrorStream();
	    while ((c = in.read(buf)) != -1)
		System.err.write(buf, 0, c);
	    try {
		in.close();
	    } catch (IOException e) {
		/* ignore */}
	} catch (Exception e) {
	    lastException = e;
	    System.err.println("Could not start FCGI server: " + e);
	}
	;
    }

    protected abstract FCGIProcess doBind() throws IOException;

    protected void bind() throws InterruptedException, IOException {
	Thread t = (new Thread("JavaBridgeFastCGIRunner") {
	    public void run() {
		runFcgi();
	    }
	});
	t.start();
	waitForDaemon();
    }

    private boolean canStartFCGI() {
	return true;
    }

    public void destroy() {
	synchronized (fcgiStartLock) {
	    fcgiStarted = false;
	    if (proc == null)
		return;
	    try {
		OutputStream out = proc.getOutputStream();
		if (out != null)
		    out.close();
	    } catch (IOException e) {
		Logger.printStackTrace(e);
	    }
	    try {
		proc.waitFor();
	    } catch (InterruptedException e) {
		// ignore
	    }
	    proc.destroy();
	    proc = null;
	}
    }

    /**
     * Connect to the FastCGI server and return the connection handle.
     * 
     * @return The FastCGI Channel
     * @throws ConnectException
     *             thrown if a IOException occured.
     */
    public abstract Connection connect() throws ConnectException;

    /**
     * For backward compatibility the "JavaBridge" context uses the port 9667
     * (Linux/Unix) or <code>\\.\pipe\JavaBridge@9667</code> (Windogs).
     */
    public void initialize() {
	setDynamicPort();
    }

    protected abstract void setDynamicPort();

    protected abstract void setDefaultPort();

    /**
     * Return a command which may be useful for starting the FastCGI server as a
     * separate command.
     * 
     * @param base
     *            The context directory
     * @param php_fcgi_max_requests
     *            The number of requests, see appropriate servlet option.
     * @return A command string
     */
    public abstract String getFcgiStartCommand(String base,
            String php_fcgi_max_requests);

    /**
     * Find a free port or pipe name.
     * 
     * @param select
     *            If select is true, the default name should be used.
     */
    public abstract void findFreePort(boolean select);

    /**
     * Create a new ChannelFactory.
     * 
     * @return The concrete ChannelFactory (NP or Socket channel factory).
     */
    public static FCGIFactory createConnectionFactory(String[] args, Map env,
            CloseableConnection fcgiConnectionPool, int maxRequests,
            boolean promiscuous) {
	if (Util.USE_SH_WRAPPER)
	    return new SocketFactory(args, env, fcgiConnectionPool, maxRequests,
	            promiscuous);
	else
	    return new PipeFactory(args, env, fcgiConnectionPool, maxRequests);
    }

    /** required by IFCGIProcessFactory */
    /** {@inheritDoc} */
    protected FCGIProcess createFCGIProcess(String[] args, Map env)
            throws IOException {
	env = new HashMap(env);
	env.put("PHP_FCGI_MAX_REQUESTS", maxRequests);
	Object children = env.get("PHP_JAVA_BRIDGE_FCGI_CHILDREN");
	if (children == null) {
	    env.put("PHP_JAVA_BRIDGE_FCGI_CHILDREN",
	            FCGIUtil.PHP_FCGI_CONNECTION_POOL_SIZE);
	} else if (Integer
	        .parseInt(String.valueOf(children)) > THREAD_POOL_MAX_SIZE) {
	    env.put("PHP_JAVA_BRIDGE_FCGI_CHILDREN",
	            FCGIUtil.PHP_FCGI_CONNECTION_POOL_SIZE);
	}
	return new FCGIProcess.Builder().withArgs(args).withEnv(env).build();
    }

}
