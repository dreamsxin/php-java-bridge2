/*-*- mode: Java; tab-width:8 -*-*/

package php.java.fastcgi;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.UnknownHostException;
import java.util.Map;

import php.java.bridge.Util;
import php.java.bridge.Util.Process;
import php.java.bridge.http.FCGIUtil;
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

public class PipeConnectionFactory extends FCGIConnectionFactory {
    /**
     * The named pipe prefix
     */
    public static final String PREFIX="\\\\.\\pipe\\";
    
    private String raPath;
    private String testRaPath;

    private FCGIConnectionPool fcgiConnectionPool;

    private int maxRequests;
    
    /**
     * Create a new factory using a given processFactory
     * @param processFactory the FCGIProcessFactory
     */
    public PipeConnectionFactory(FCGIConnectionPool fcgiConnectionPool, int maxRequests, IFCGIProcessFactory processFactory) {
	super(processFactory);
	this.fcgiConnectionPool = fcgiConnectionPool;
	this.maxRequests = maxRequests;
    }
    /**
     * Tests if the channel will be available.
     * @throws ConnectException
     */
    public void test() throws ConnectException {
	if(!new File(raPath).canWrite()){
	    String reason = "File " + raPath + " not writable";
	    if (lastException != null) {
		throw new ConnectException(reason, lastException);
	    }
	    throw new ConnectException(new IOException(reason));
	}
    }
    private PipeConnection doConnect() throws ConnectException {
	try {
	    return new PipeConnection(fcgiConnectionPool, maxRequests,new RandomAccessFile( raPath, "rw"));
	} catch (IOException e) {
	    throw new ConnectException(e);
	}
    }
    /**
     * Create a FCGIConnection
     * @throws ConnectException
     */
    public Connection connect() throws ConnectException {
	return doConnect();
    }

    protected Process doBind(Map env, String php, boolean includeJava, boolean includeDebugger) throws IOException {
        if(proc!=null) return null;
	if(raPath==null) throw new IOException("No pipe name available.");
	// Set override hosts so that php does not try to start a VM.
	// The value itself doesn't matter, we'll pass the real value
	// via the (HTTP_)X_JAVABRIDGE_OVERRIDE_HOSTS header field
	// later.
	File home = null;
	if(php!=null) try { home = ((new File(php)).getParentFile()); } catch (Exception e) {Util.printStackTrace(e);}
	proc = processFactory.createFCGIProcess(new String[]{php, raPath}, includeJava, includeDebugger, home, env);
	proc.start();
	return (Process)proc;
    }
    protected void waitForDaemon() throws UnknownHostException, InterruptedException {
	Thread.sleep(5000);
    }
    
    /**
     * Return the OS command used to start the FCGI process
     * @return the fcgi start command.
     */
    public String getFcgiStartCommand(String base, String php_fcgi_max_requests) {
	String msg =
	    "cd \"" + base + File.separator + Util.osArch + "-" + Util.osName+ "\"\n" + 
	    "set %REDIRECT_STATUS%=200\n"+ 
	    "set %X_JAVABRIDGE_OVERRIDE_HOSTS%=/\n"+ 
	    "set %PHP_JAVA_BRIDGE_FCGI_CHILDREN%=5\n"+ 
	    "set %PHP_FCGI_MAX_REQUESTS%=\""+php_fcgi_max_requests+"\"\n"+
	    "\"c:\\Program Files\\PHP\\php-cgi.exe\" -v\n"+
	    ".\\launcher.exe \"c:\\Program Files\\PHP\\php-cgi.exe\" \"" + getPath() +"\"\n\n";
        return msg;
    }
    /**
     * Find a free socket port. After that {@link #setDynamicPort()} should be called
     * @param select wether or not some hard-coded path should be used
     */
    public void findFreePort(boolean select) {
	try {
	    if(select) {
		File testRafile = File.createTempFile("JavaBridge", ".socket");
		testRaPath = PREFIX+testRafile.getCanonicalPath();
		testRafile.delete();
	    } else {
		testRaPath  = FCGIUtil.FCGI_PIPE;
	    }
	} catch (IOException e) {
	    Util.printStackTrace(e);
	}
    }
    /**
     * Set the a default port, overriding the port selected by {@link #findFreePort(boolean)}
     */
    public void setDefaultPort() {
	raPath=FCGIUtil.FCGI_PIPE;
    }
    /**
     * Set the dynamic port selected by {@link #findFreePort(boolean)}
     */
    public void setDynamicPort() {
	raPath=testRaPath;
    }
    /**
     * Return the path selected by {@link #setDefaultPort()} or {@link #setDynamicPort()}
     * @return the path
     */
    public String getPath() {
	return raPath;
    }
    /** 
     * Return the channel name 
     * @return the channel name
     * 
     */
    public String toString() {
	return "ChannelName@" + (getPath()==null ? "<not initialized>" : getPath());
    }
}
