/*-*- mode: Java; tab-width:8 -*-*/

package php.java.fastcgi;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

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

public class FCGIProxy extends Continuation {
//    private static final String PROCESSES = Util.THREAD_POOL_MAX_SIZE; // PROCESSES must == Util.THREAD_POOL_MAX_SIZE
//    private static final String MAX_REQUESTS = FCGIUtil.PHP_FCGI_MAX_REQUESTS;
    
    private String[] args;
    protected Map env;
    protected OutputStream out;
    private OutputStream err;
    protected FCGIHeaderParser headerParser;
    public FCGIProxy(String[] args, Map env, OutputStream out,
            OutputStream err, FCGIHeaderParser headerParser) {
	super();
	this.args = args;
	this.env = env;
	this.out = out;
	this.err = err;
	this.headerParser = headerParser;
    }

    private static final Object globalCtxLock = new Object();
    private static FCGIConnectionPool fcgiConnectionPool = null;
    protected void setupFastCGIServer() throws ConnectException {
	synchronized(globalCtxLock) { //FIXME refactor
	    if(null == fcgiConnectionPool) {
		fcgiConnectionPool = FCGIConnectionPool.createConnectionPool(args, env);
	    }
	}

    }
 

	Connection connection = null;
    protected void doRun() throws IOException, PhpException {
	byte[] buf = new byte[FCGIUtil.FCGI_BUF_SIZE];
	setupFastCGIServer(); 
	
	FCGIInputStream natIn = null;
	FCGIOutputStream natOut = null;

	
	try {
	    connection = fcgiConnectionPool.openConnection();
	    natOut = (FCGIOutputStream) connection.getOutputStream();
	    natIn = (FCGIInputStream) connection.getInputStream();

	    natOut.writeBegin();
	    natOut.writeParams(env);
	    natOut.write(FCGIUtil.FCGI_STDIN, FCGIUtil.FCGI_EMPTY_RECORD);
	    natOut.close(); natOut = null;
	    FCGIHeaderParser.parseBody(buf, natIn, out, headerParser);
	    natIn.close(); natIn = null;
	    connection = null;
	} catch (InterruptedException e) {
	    /*ignore*/
	} catch (Throwable t) {
            Logger.printStackTrace(t);
        } finally {
	    if(connection!=null) connection.setIsClosed(); 
	    if(natIn!=null) try {natIn.close();} catch (IOException e) {}
	    if(natOut!=null) try {natOut.close();} catch (IOException e) {}
        }
    }

    /** {@inheritDoc} */
    public boolean canStartFCGI() {
	return true;
    }



    /** {@inheritDoc} */
    public void log(String msg) {
	Logger.logMessage(msg);
    }

    public void release() throws InterruptedException {
	super.release();
	synchronized(globalCtxLock) { //FIXME clean this up!
		fcgiConnectionPool.destroy();
	    fcgiConnectionPool=null;
	}

    }

}
