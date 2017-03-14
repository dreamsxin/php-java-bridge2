/*-*- mode: Java; tab-width:8 -*-*/

package php.java.fastcgi;

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

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import php.java.bridge.Util;

public class FCGIConnectionPool {

    private int limit;
    private long timeout;
    private int connections = 0;
    private List freeList = new LinkedList();
    private List connectionList = new LinkedList();
    private FCGIConnectionFactory factory;
    private int maxRequests;
 
    /**
     * Create a new connection pool.
     * @param channelName The channel name
     * 
     * @param limit The max. number of physical connections
     * @param maxRequests 
     * @param factory A factory for creating In- and OutputStreams.
     * @throws ConnectException 
     * @see FCGIIOFactory
     */
    private FCGIConnectionPool(FCGIConnectionFactory factory, int limit, int maxRequests) throws ConnectException {
	if(Util.logLevel>3) Util.logDebug("Creating new connection pool for: " +factory);
        this.factory = factory;
        this.limit = limit;
        this.maxRequests = maxRequests;
        this.timeout = -1;
        factory.test();
    }
    /**
     * Create a new connection pool.
     * @param channelName The channel name
     * 
     * @param limit The max. number of physical connections
     * @param maxRequests 
     * @param factory A factory for creating In- and OutputStreams.
     * @param timeout The pool timeout in milliseconds.
     * @throws ConnectException 
     * @see FCGIIOFactory
     */
    public FCGIConnectionPool(FCGIConnectionFactory factory, int limit, int maxRequests, long timeout) throws ConnectException {
	this(factory, limit, maxRequests);
	this.timeout = timeout;
    }
    /* helper for openConnection() */
    private Connection createNewConnection() throws ConnectException {
        Connection connection = factory.connect();
        connectionList.add(connection);
        connections++;
        return connection;
    }
    /**
     * Opens a connection to the back end.
     * @return The connection
     * @throws InterruptedException
     * @throws ConnectException 
     */
    public synchronized Connection openConnection() throws InterruptedException, ConnectException {
        Connection connection;
      	if(freeList.isEmpty() && connections<limit) {
      	    connection = createNewConnection();
      	} else {
      	    while(freeList.isEmpty()) {
      		if (timeout > 0) {
      		    long t1 = System.currentTimeMillis();
      		    wait(timeout);
      		    long t2 = System.currentTimeMillis();
      		    long t = t2 - t1;
      		    if (t >= timeout) throw new ConnectException(new IOException("pool timeout "+timeout+" exceeded: "+t));
      		} else {
      		    wait();
      		}
      	    }
      	    connection = (Connection) freeList.remove(0);
      	    connection.reset();
      	}
      	return reopen(connection);
    }
    private Connection reopen(Connection connection) throws ConnectException {
	if(connection.isClosed()) connection = factory.connect();
	return connection;
    }
    synchronized void closeConnection(Connection connection) {
        freeList.add(connection);
        notify();
    }
    /**
     * Destroy the connection pool. 
     * 
     * It releases all physical connections.
     *
     */
    public synchronized void destroy() {
        for(Iterator ii = connectionList.iterator(); ii.hasNext();) {
            Connection connection = (Connection) ii.next();
            connection.close();
        }
        
    	if(factory!=null) 
    	    factory.destroy();
    }
}
