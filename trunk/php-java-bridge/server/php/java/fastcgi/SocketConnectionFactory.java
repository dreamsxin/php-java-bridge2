/*-*- mode: Java; tab-width:8 -*-*/

package php.java.fastcgi;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Map;

import php.java.bridge.ILogger;
import php.java.bridge.Util;
import php.java.bridge.Util.Process;
import php.java.bridge.http.FCGIUtil;
import php.java.bridge.http.IFCGIProcessFactory;

/*
 * Copyright (C) 2017 Jost B�kemeier
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

class SocketConnectionFactory extends FCGIConnectionFactory {
    public static final String LOCAL_HOST = "127.0.0.1";
    private int port;

    private  ServerSocket fcgiTestSocket = null;
    private  int fcgiTestPort;
    private FCGIConnectionPool fcgiConnectionPool;
    private int maxRequests;

    public SocketConnectionFactory (FCGIConnectionPool fcgiConnectionPool, int maxRequests,IFCGIProcessFactory processFactory, boolean promiscuous) {
	super(processFactory);
	this.promiscuous = promiscuous;
	this.fcgiConnectionPool = fcgiConnectionPool;
	this.maxRequests = maxRequests;
    }
    public void test() throws ConnectException {
        Socket testSocket;
	try {
	    testSocket = new Socket(InetAddress.getByName(getName()), port);
	    testSocket.close();
	} catch (IOException e) {
	    if (lastException != null) {
		throw new ConnectException(String.valueOf(e), lastException);
	    }
	    throw new ConnectException(e);
	}
    }
    /**
     * Create a new socket and connect
     * it to the given host/port
     * @param host The host, for example 127.0.0.1
     * @param port The port, for example 9667
     * @return The socket
     * @throws UnknownHostException
     * @throws ConnectionException
     */
    private Socket doConnect(String host, int port) throws ConnectException {
        Socket s = null;
	try {
            s = new Socket(InetAddress.getByName(host), port);
	} catch (IOException e) {
	    throw new ConnectException(e);
	}
	try {
	    s.setTcpNoDelay(true);
	} catch (SocketException e) {
	    Util.printStackTrace(e);
	}
	return s;
    }

    public Connection connect() throws ConnectException {
	Socket s = doConnect(getName(), getPort());
	return new SocketConnection(fcgiConnectionPool, maxRequests, s); 	
    }
    
    protected void waitForDaemon() throws UnknownHostException, InterruptedException {
	long T0 = System.currentTimeMillis();
	int count = 15;
	InetAddress addr = InetAddress.getByName(LOCAL_HOST);
	if(Util.logLevel>3) Util.logDebug("Waiting for PHP FastCGI daemon");
	while(count-->0) {
	    try {
		Socket s = new Socket(addr, getPort());
		s.close();
		break;
	    } catch (IOException e) {/*ignore*/}
	    if(System.currentTimeMillis()-16000>T0) break;
	    Thread.sleep(1000);
	}
	if(count==-1) Util.logError("Timeout waiting for PHP FastCGI daemon");
	if(Util.logLevel>3) Util.logDebug("done waiting for PHP FastCGI daemon");
    }
	    
    /* Start a fast CGI Server process on this computer. Switched off per default. */
    protected Process doBind(Map env, String php, boolean includeJava, boolean includeDebugger) throws IOException {
	if(proc!=null) return null;
	StringBuffer buf = new StringBuffer((Util.JAVABRIDGE_PROMISCUOUS || promiscuous) ? "" : LOCAL_HOST); // bind to all available or loopback only
	buf.append(':');
	buf.append(String.valueOf(getPort()));
	String port = buf.toString();
	        
	// Set override hosts so that php does not try to start a VM.
	// The value itself doesn't matter, we'll pass the real value
	// via the (HTTP_)X_JAVABRIDGE_OVERRIDE_HOSTS header field
	// later.
	File home = null;
	if(php!=null) try { home = ((new File(php)).getParentFile()); } catch (Exception e) {Util.printStackTrace(e);}
	proc = processFactory.createFCGIProcess(new String[]{php, "-b", port}, includeJava, includeDebugger, home, env);
	proc.start();
	return (Process)proc;
    }
    protected int getPort() {
	return port;
    }
    protected String getName() {
	return LOCAL_HOST;
    }
    public String getFcgiStartCommand(String base, String php_fcgi_max_requests) {
	String msg=
	    "cd " + base + File.separator + Util.osArch + "-" + Util.osName+ "\n" + 
	    "REDIRECT_STATUS=200 " +
	    "X_JAVABRIDGE_OVERRIDE_HOSTS=\"/\" " +
	    "PHP_JAVA_BRIDGE_FCGI_CHILDREN=\"5\" " +
	    "PHP_FCGI_MAX_REQUESTS=\""+php_fcgi_max_requests+"\" /usr/bin/php-cgi -b 127.0.0.1:" +
	    getPort()+"\n\n";
	return msg;
    }
    protected void bind(ILogger logger) throws InterruptedException, IOException {
	if(fcgiTestSocket!=null) { fcgiTestSocket.close(); fcgiTestSocket=null; }// replace the allocated socket# with the real fcgi server
	super.bind(logger);
    }
		
    public void findFreePort(boolean select) {
	fcgiTestPort=FCGIUtil.FCGI_PORT; 
	fcgiTestSocket=null;
	for(int i=FCGIUtil.FCGI_PORT+1; select && (i<FCGIUtil.FCGI_PORT+100); i++) {
	    try {
		ServerSocket s = new ServerSocket(i, Util.BACKLOG, InetAddress.getByName(LOCAL_HOST));
		fcgiTestPort = i;
		fcgiTestSocket = s;
		break;
	    } catch (IOException e) {/*ignore*/}
	}
    }
    public void setDefaultPort() {
	port = FCGIUtil.FCGI_PORT;
    }
    protected void setDynamicPort() {
	port = fcgiTestPort;
    }
    public void destroy() {
	super.destroy();
	if(fcgiTestSocket!=null) try { fcgiTestSocket.close(); fcgiTestSocket=null;} catch (Exception e) {/*ignore*/}
    }	  
    /** 
     * Return the channel name 
     * @return the channel name
     * 
     */
    public String toString() {
	return "ChannelName@127.0.0.1:" + port;
    }
}