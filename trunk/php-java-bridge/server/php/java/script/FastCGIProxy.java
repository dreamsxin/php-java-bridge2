/*-*- mode: Java; tab-width:8 -*-*/

package php.java.script;

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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import php.java.bridge.ILogger;
import php.java.bridge.Util;
import php.java.bridge.Util.HeaderParser;
import php.java.bridge.http.Channel;
import php.java.bridge.http.ChannelFactory;
import php.java.bridge.http.ConnectException;
import php.java.bridge.http.ConnectionPool;
import php.java.bridge.http.FCGIUtil;
import php.java.bridge.http.FastCGIInputStream;
import php.java.bridge.http.FastCGIOutputStream;
import php.java.bridge.http.IFCGIProcess;
import php.java.bridge.http.IFCGIProcessFactory;
import php.java.bridge.http.IOFactory;

/**
 * This class can be used to run a PHP CGI binary. Used only when
 * running local php scripts.  To allocate and invoke remote scripts
 * please use a HttpProxy and a URLReader instead.
 *  
 * @author jostb
 *
 * @see php.java.bridge.http.HttpServer
 * @see php.java.script.URLReader
 * @see php.java.script.HttpProxy
 */

public class FastCGIProxy extends Continuation implements IFCGIProcessFactory {
    private static final String PROCESSES = FCGIUtil.PHP_FCGI_CONNECTION_POOL_SIZE;
    private static final String MAX_REQUESTS = FCGIUtil.PHP_FCGI_MAX_REQUESTS;
    private static final String CGI_DIR = Util.TMPDIR.getAbsolutePath();
    
    public FastCGIProxy(Reader reader, Map env, OutputStream out,
            OutputStream err, HeaderParser headerParser,
            ResultProxy resultProxy, ILogger logger) {
	super(reader, env, out, err, headerParser, resultProxy, logger);
    }
    private ChannelFactory channelName;
    private static final String CONTEXT_PATH="";
    static final HashMap PROCESS_ENVIRONMENT = getProcessEnvironment();
    private static HashMap getProcessEnvironment() {
	HashMap map = new HashMap(Util.COMMON_ENVIRONMENT);
	return map;
    }
    private final IOFactory defaultPoolFactory = new IOFactory() {
	    public InputStream createInputStream() { return new FastCGIInputStream(FastCGIProxy.this); }
	    public OutputStream createOutputStream() { return new FastCGIOutputStream(); }
	    public Channel connect(ChannelFactory name) throws ConnectException {
		return name.connect();
	    }
	};

   private ConnectionPool createConnectionPool(int children) throws ConnectException {
	channelName = ChannelFactory.createChannelFactory(this, false);
	channelName.findFreePort(false); //FIXME
	channelName.initialize(CONTEXT_PATH);
	File cgiOsDir = Util.TMPDIR;
	File javaIncFile = new File (cgiOsDir, "launcher.sh");
	if (Util.USE_SH_WRAPPER) {
	    try {
		if (!javaIncFile.exists()) {
		    Field f = Util.LAUNCHER_UNIX.getField("bytes");
		    byte[] buf = (byte[]) f.get(Util.LAUNCHER_UNIX);
		    OutputStream out = new FileOutputStream (javaIncFile);
		    out.write(buf);
		    out.close();
		}
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	}
	File javaProxyFile = new File (cgiOsDir, "launcher.exe");
	if (!Util.USE_SH_WRAPPER) {
	    try {
		if (!javaProxyFile.exists()) {
		    Field f =  Util.LAUNCHER_WINDOWS.getField("bytes");
		    Field f2 = Util.LAUNCHER_WINDOWS2.getField("bytes");
		    Field f3 = Util.LAUNCHER_WINDOWS3.getField("bytes");
		    Field f4 = Util.LAUNCHER_WINDOWS4.getField("bytes");
		    byte[] buf =  (byte[]) f.get(Util.LAUNCHER_WINDOWS);
		    byte[] buf2 = (byte[]) f2.get(Util.LAUNCHER_WINDOWS2);
		    byte[] buf3 = (byte[]) f3.get(Util.LAUNCHER_WINDOWS3);
		    byte[] buf4 = (byte[]) f4.get(Util.LAUNCHER_WINDOWS4);
		    OutputStream out = new FileOutputStream (javaProxyFile);
		    out.write(buf);
		    out.write(buf2);
		    out.write(buf3);
		    out.write(buf4);
		    out.close();
		}
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	}

	// Start the launcher.exe or launcher.sh
	Map map = (Map) PROCESS_ENVIRONMENT.clone();
	map.put("PHP_FCGI_CHILDREN", PROCESSES);
	map.put("PHP_FCGI_MAX_REQUESTS", MAX_REQUESTS);
	channelName.startServer(Util.getLogger());
		
	return new ConnectionPool(channelName, children, 
		Integer.parseInt(MAX_REQUESTS), 
		defaultPoolFactory, 
		Integer.parseInt(FCGIUtil.PHP_FCGI_CONNECTION_POOL_TIMEOUT));
    }
    private static final Object globalCtxLock = new Object();
    private static ConnectionPool fcgiConnectionPool = null;
    protected void setupFastCGIServer() throws ConnectException {
	synchronized(globalCtxLock) {
	    if(null == fcgiConnectionPool) {
		fcgiConnectionPool= createConnectionPool(Integer.parseInt(PROCESSES));
	    }
	}

    }
    

    protected void doRun() throws IOException, Util.Process.PhpException {
	byte[] buf = new byte[FCGIUtil.FCGI_BUF_SIZE];
	setupFastCGIServer();
	
	FastCGIInputStream natIn = null;
	FastCGIOutputStream natOut = null;

	ConnectionPool.Connection connection = null;
	
	try {
	    connection = fcgiConnectionPool.openConnection();
	    natOut = (FastCGIOutputStream) connection.getOutputStream();
	    natIn = (FastCGIInputStream) connection.getInputStream();

	    natOut.writeBegin();
	    natOut.writeParams(env);
	    natOut.write(FCGIUtil.FCGI_STDIN, FCGIUtil.FCGI_EMPTY_RECORD);
	    natOut.close();
	    Util.parseBody(buf, natIn, new Util.OutputStreamFactory() { public OutputStream getOutputStream() throws IOException {return out;}}, headerParser);
	    natIn.close();
	} catch (InterruptedException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    /** required by IFCGIProcessFactory */
    /** {@inheritDoc} */
    public IFCGIProcess createFCGIProcess(String[] args, File home, Map env) throws IOException {
	return new FCGIProcess(args, home, env,
		getCgiDir(), true, true);
    }


    /** {@inheritDoc} */
    public boolean canStartFCGI() {
	return true;
    }



    /** {@inheritDoc} */
    public String getCgiDir() {
	return CGI_DIR;
    }


    /** {@inheritDoc} */
    public HashMap getEnvironment() {
	return getProcessEnvironment();
    }


    /** {@inheritDoc} */
    public String getPearDir() {
	return CGI_DIR;
    }


    /** {@inheritDoc} */
    public String getPhp() {
	return null;
    }


    /** {@inheritDoc} */
    public String getPhpConnectionPoolSize() {
	return PROCESSES;
    }


    /** {@inheritDoc} */
    public boolean getPhpIncludeJava() {
	// FIXME
	return false;
    }


    /** {@inheritDoc} */
    public String getPhpMaxRequests() {
	return MAX_REQUESTS;
    }


    /** {@inheritDoc} */
    public String getWebInfDir() {
	return CGI_DIR;
    }


    /** {@inheritDoc} */
    public void log(String msg) {
	Util.logMessage(msg);
    }
}
