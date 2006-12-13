/*-*- mode: Java; tab-width:8 -*-*/
package php.java.bridge;

/*
 * Copyright (C) 2006 Jost Boekemeier
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


/**
 * This is the native standalone container of the PHP/Java Bridge. It starts
 * the standalone back-end, listenes for protocol requests and handles
 * CreateInstance, GetSetProp and Invoke requests. Supported protocol
 * modes are INET (listens on all interfaces), INET_LOCAL (loopback
 * only), LOCAL, SERVLET and SERVLET_LOCAL 
 * (starts the built-in servlet engine listening on all interfaces or loopback).  
 * <p> Example:<br> <code> java
 * INET_LOCAL:9676 5
 * bridge.log &<br> telnet localhost 9676<br> &lt;CreateInstance
 * value="java.lang.Long" predicate="Instance" id="0"&gt;<br> &lt;Long
 * value="6"/&gt; <br> &lt;/CreateInstance&gt;<br> &lt;Invoke
 * value="1" method="toString" predicate="Invoke" id="0"/&gt;<br>
 * </code>
 *
 */

public class StandaloneGCC extends Standalone {

    protected void javaUsage() {
	System.err.println("PHP/Java Bridge version "+Util.VERSION);
	disclaimer();
	System.err.println("Usage: java [SOCKETNAME LOGLEVEL LOGFILE]");
	System.err.println("Usage: java --convert PHP_INCLUDE_DIR [JARFILES]");
	System.err.println("SOCKETNAME is one of LOCAL, INET_LOCAL, INET, SERVLET_LOCAL, SERVLET");
	System.err.println("Example: java");
	System.err.println("Example: java LOCAL:/tmp/javabridge_native.socket 3 /var/log/php-java-bridge.log");
	System.err.println("Example: java INET:9267 3 JavaBridge.log");
	System.err.println("Example: java SERVLET:8080 3 JavaBridge.log");
	System.err.println("Example: java --convert /usr/share/pear lucene.jar ...");
    }

    /**
     * Start the PHP/Java Bridge. <br>
     * Example:<br>
     * <code>java LOCAL:@socket 5 /var/log/php-java-bridge.log</code><br>
     * Note: Do not write anything to System.out, this
     * stream is connected with a pipe which waits for the channel name.
     * @param s an array of [socketname, level, logFile]
     */
    public static void main(String s[]) {
	String args[];
	try {
	    System.loadLibrary("natcJavaBridge");
	} catch (Throwable t) {/*ignore*/}
	try {
	    int length = s.length;
	    if((length==7 || length==8) && !s[0].startsWith("--")) {
		System.setProperty("java.library.path", s[0].substring(20));
		System.setProperty("java.class.path", s[1].substring(18));
		int p=s[2].indexOf('=');
		System.setProperty(s[2].substring(2,p), s[2].substring(p+1));
		System.setProperty("php.java.bridge.base", s[3].substring(23));
		
		args = length==7 ? new String[] {s[5], s[6]} :  new String[] {s[5], s[6], s[7]};
	    } else {
		args = s;
	    }
	    (new StandaloneGCC()).init(args);
	} catch (Throwable t) {
	    t.printStackTrace();
	    System.exit(9);
	}
    }
}
