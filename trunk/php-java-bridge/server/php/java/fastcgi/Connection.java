package php.java.fastcgi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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

public abstract class Connection {
    protected int ostate, state; // bit0: input closed, bit1: output closed
    private boolean isClosed;
    private int maxRequests;
    private int counter;
    private CloseableConnection fcgiConnectionPool;
    
    protected void reset() {
        this.state = this.ostate = 0;
    }
    protected void init() {
        counter = maxRequests; 
        reset();
    }
    protected Connection(CloseableConnection fcgiConnectionPool, int maxRequests) {
        this.fcgiConnectionPool = fcgiConnectionPool;
	this.isClosed = true;
        this.maxRequests = maxRequests;
        init();
    }
    /** Set the closed/abort flag for this connection */
    public void setIsClosed() {
        isClosed=true;
        init();
    }
    public boolean isClosed() {
	return isClosed;
    }
    protected void closeConnection() throws ConnectException {
        // PHP child terminated: mark as closed, so that reopen() can allocate 
        // a new connection for the new PHP child
        if (maxRequests>0 && --counter==0) {
            setIsClosed();
        }
        
        this.fcgiConnectionPool.closeConnection(this);
    }
    public abstract InputStream getInputStream() throws IOException;
    
    public abstract OutputStream getOutputStream() throws IOException;
    public abstract void close();
    protected InputStream getInputStream(
            InputStream in) {
	return new FCGIInputStream(this, in);
    }
    protected OutputStream getOutputStream(
            OutputStream out) {
	return new FCGIOutputStream(this, out);
    }

}