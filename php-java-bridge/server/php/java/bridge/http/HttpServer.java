/*-*- mode: Java; tab-width:8 -*-*/

package php.java.bridge.http;

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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;

import php.java.bridge.ISocketFactory;
import php.java.bridge.ThreadPool;
import php.java.bridge.Util;


/**
 * This class can be used to create a simple HTTP server. It is used
 * when running local scripts like <code>e.eval(new
 * StringReader('&lt;?php phpinfo(); ?&gt;'));</code>. For remote
 * scripts use a HttpProxy and URLReader instead.
 * 
 * @author jostb
 * 
 * @see php.java.bridge.http.HttpRequest
 * @see php.java.bridge.http.HttpResponse
 * 
 * @see php.java.script.CGIRunner
 * @see php.java.script.URLReader
 * @see php.java.script.HttpProxy
 *
 */
public abstract class HttpServer implements Runnable {
    /** Request method GET */
    public static final String PUT = "PUT";
    /** Request method PUT */
    public static final String GET = "GET";
    /** Request method POST */
    public static final String POST = "POST";
    
    protected ISocketFactory socket;
    protected Thread httpServer;
    private ThreadPool pool;

    /**
     * Create a server socket.
     * @param addr The host address, either INET:port or INET_LOCAL:port
     * @return The server socket.
     */
    public abstract ISocketFactory bind(String addr) throws IOException;

    /**
     * Create a new HTTP Server.
     * @throws IOException 
     * @see HttpServer#destroy()
     */
    public HttpServer() throws IOException {
	socket = bind(Util.JAVABRIDGE_PROMISCUOUS ? "INET:0" : "INET_LOCAL:0");
	try {
		pool = createThreadPool(Util.EXTENSION_NAME+"HttpServerThreadPool");
	} catch (SecurityException e) {/*ignore*/}
	httpServer = new Util.Thread(this, Util.EXTENSION_NAME+"HttpServer");
        httpServer.start();
    }

    /**
     * Same as Util.createThreadPool, but does not log anything at this stage.
     * @param name The name of the pool
     * @return The thread pool instance.
     */
    private static ThreadPool createThreadPool(String name) {
        ThreadPool pool = null;
        int maxSize = 20;
        try { maxSize = Integer.parseInt(Util.THREAD_POOL_MAX_SIZE); } catch (Throwable t) {/*ignore*/}
        if(maxSize>0) pool = new ThreadPool(name, maxSize);
        return pool;
    }

    /**
     * Parse the header. After that <code>req</code> contains the body.
     * @param req The HttpRequest
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    protected boolean parseHeader(HttpRequest req) throws UnsupportedEncodingException, IOException {
	byte buf[] = new byte[Util.BUF_SIZE];
		
	InputStream natIn = req.getInputStream();
	int i=0, n, s=0;
	boolean eoh=false;
	// the header and content
	while((n = natIn.read(buf, i, buf.length-i)) !=-1 ) {
	    int N = i + n;
	    // header
	    while(!eoh && i<N) {
		switch(buf[i++]) {
    			
		case '\n':
		    if(s+2==i && buf[s]=='\r') {
			eoh=true;
		    } else {
			req.addHeader(new String(buf, s, i-s-2, Util.ASCII));
			s=i;
		    }
		}
	    }
	    // body
	    if(eoh) {
		req.pushBack(buf, i, N-i);
		break;
	    }
	}
	return i!=0;
    }
		
    private class Runner implements Runnable {
        private Socket sock;
	private HttpRequest req;
        private HttpResponse res;
        public Runner(Socket sock) throws IOException {
            this.sock = sock;
            this.req = new HttpRequest(sock.getInputStream());
            this.res = new HttpResponse(sock.getOutputStream());
         }
        public void run() {
            try {
        	if(parseHeader(req)) service(req, res);
            } catch (IOException e) {
        	Util.printStackTrace(e);
 	    } finally {
  		try {this.sock.close();} catch (IOException e) {/*ignore*/}
 	    }
        }
    }
    /**
     * accept, create a HTTP request and response, parse the header and body
     * @throws IOException
     */
    protected void doRun() throws IOException {
	while(true) {
	    Socket sock;
	    try {sock = socket.accept();} catch (IOException e) {return;} // socket closed
	    Util.logDebug("Socket connection accepted");
	    if(pool==null) {
		Util.logDebug("Starting new HTTP server thread");
        	(new Util.Thread(new Runner(sock), Util.EXTENSION_NAME+"HttpServerRunner")).start();
	    } else { 
                Util.logDebug("Starting HTTP server thread from thread pool");
		pool.start(new Runner(sock));
	    }
	}
    }

    protected static final byte[] ERROR = Util.toBytes(
	    "<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">"+
	    "<html><head>" +
	    "<title>404 Not Found</title>" +
	    "</head><body>" +
	    "<h1>Not Found</h1>" +
	    "<p>The requested URL as not found on this server.</p>" +
	    "<hr>"+
    "</body></html>");
    protected void doPost(HttpRequest req, HttpResponse res) throws IOException {
	res.setContentLength(ERROR.length);
	OutputStream out = res.getOutputStream();
	out.write(ERROR);
    }
    protected void doGet(HttpRequest req, HttpResponse res) throws IOException { 
	doPost(req, res); 
    }
    protected void doPut(HttpRequest req, HttpResponse res) throws IOException { 
	doPost(req, res); 
    }
    
    /**
     * Sets the content length but leaves the rest of the body untouched.
     */
    protected void service(HttpRequest req, HttpResponse res) throws IOException {
        String contentLength = req.getHeader("Content-Length");
        if(contentLength==null) req.setContentLength(0);
        else req.setContentLength(Integer.parseInt(contentLength));
        String method = req.getMethod();
        if(method == PUT) doPut(req, res); 
        else if(method == GET) doGet(req, res); 
        else if(method == POST) doPost(req, res);
    }

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run() {
	try {
	    doRun();
	} catch (IOException e) {
	    Util.printStackTrace(e);
	}
    }

    /**
     * Stop the HTTP server.
     *
     */
    public void destroy() {
	try {
	    socket.close();
	} catch (IOException e) {
	    Util.printStackTrace(e);
	}
    }

    /**
     * Returns the server socket.
     * @return The server socket.
     */
    public ISocketFactory getSocket() {
        return socket;
    }
}