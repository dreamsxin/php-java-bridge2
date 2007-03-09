/*-*- mode: Java; tab-width:8 -*-*/

package php.java.servlet;

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
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import php.java.bridge.NotImplementedException;

/**
 * A connection pool. Example:<br><br>
 * <code>
 * ConnectionPool pool = new ConnectionPool("127.0.0.1", 8080, 20, 5000, new ConnectionPool.Factory());<br>
 * ConnectionPool.Connection conn = pool.openConnection();<br>
 * InputStream in =  conn.getInputStream();<br>
 * OutputStream out = conn.getOutputStream();<br>
 * ...<br>
 * in.close();<br>
 * out.close();<br>
 * ...<br>
 * pool.destroy();<br>
 * </code>
 * <p>Instead of using delegation (decorator pattern), it is possible to pass a factory 
 * which may create custom In- and OutputStreams. Example:<br><br>
 * <code>
 * new ConnectionPool(..., new ConnectionPool.Factory() {<br>
 * &nbsp;&nbsp;public InputStream getInputStream() {<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;return new ConnectionPool.DefaultInputStream() {<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;...<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;}<br>
 * &nbsp;&nbsp;}<br>
 * }<br>
 * </code>
 * </p>
 * @author jostb
 *
 */
public class ConnectionPool {

    private String host;
    private int port;
    private int limit;
    private int connections = 0;
    private List freeList = new LinkedList();
    private List connectionList = new LinkedList();
    private Factory factory;
    private int maxRequests;
    
    private static final class BufferedOutputStream extends java.io.BufferedOutputStream {
        public BufferedOutputStream(OutputStream out) {
            super(out);
        }
	public byte[] getBuffer() {
            return buf;
        }
    }
    /** Thrown when an IO exception occurs */
    public static class ConnectionException extends IOException {
       private static final long serialVersionUID = -5174286702617481362L;
	protected ConnectionException(Connection con, IOException ex) {
            super();
            initCause(ex);
            con.setIsClosed();
        }
    }
    /** Thrown when the server is not available anymore */
    public static class ConnectException extends IOException {
	private static final long serialVersionUID = 5242564093021250550L;
	protected ConnectException(IOException ex) {
            super();
            initCause(ex);
        }
    }
    /**
     * In-/OutputStream factory.
     * 
     * Override this class if you want to use your own streams.
     * 
     * @author jostb
     *
     */
    public static class Factory {
        /**
         * Create a new socket and connect
         * it to the given host/port
         * @param host The host, for example 127.0.0.1
         * @param port The port, for example 9667
         * @return The socket
         * @throws UnknownHostException
         * @throws ConnectionException
         */
        public Socket connect(String host, int port) throws ConnectException, SocketException {
            try {
	      return new Socket(InetAddress.getByName(host), port);
	    } catch (IOException e) {
	        throw new ConnectException(e);
	    }
        }
        /** 
         * Create a new InputStream.
         * @return The input stream. 
         */
        public InputStream createInputStream() throws ConnectionException {
           DefaultInputStream in = new DefaultInputStream();
           return in;
        }
        /**
         * Create a new OutputStream.
         * @return The output stream.
         * @throws ConnectionException
         */
        public OutputStream createOutputStream() throws ConnectionException {
            DefaultOutputStream out = new DefaultOutputStream();
            return out;
        }
    }
    /**
     * Default InputStream used by the connection pool.
     * 
     * @author jostb
     *
     */
    public static class DefaultInputStream extends InputStream {
      protected Connection connection;
      private InputStream in;

      protected void setConnection(Connection connection) throws ConnectionException {
	  this.connection = connection;	  
	  try {
	    this.in = connection.socket.getInputStream();
	  } catch (IOException e) {
	      throw new ConnectionException(connection, e);
	  }	  
      }
      public int read(byte buf[]) throws ConnectionException {
	  return read(buf, 0, buf.length);
      }
      public int read(byte buf[], int off, int buflength) throws ConnectionException {
	  try {
	      int count = in.read(buf, off, buflength);
	      if(count==-1) {
		  connection.setIsClosed();
	      }
	      return count;
	  } catch (IOException ex) {
	      throw new ConnectionException(connection, ex);
	  }
      }
      public int read() throws ConnectionException {
	throw new NotImplementedException();
      }      
      public void close() throws ConnectionException {
	  connection.state|=1;
	  if(connection.state==connection.ostate)
	    try {
	      connection.close();
	    } catch (IOException e) {
	      throw new ConnectionException(connection, e);
	    }
      }
    }
    /**
     * Default OutputStream used by the connection pool.
     * 
     * @author jostb
     *
     */
    public static class DefaultOutputStream extends OutputStream {
        private Connection connection;
        private BufferedOutputStream out;
        
	protected void setConnection(Connection connection) throws ConnectionException {
	    this.connection = connection;
            try {
	      this.out = new BufferedOutputStream(connection.socket.getOutputStream());
	    } catch (IOException e) {
	      throw new ConnectionException(connection, e);
	    }
	}
	public void write(byte buf[]) throws ConnectionException {
	    write(buf, 0, buf.length);
	}
	public void write(byte buf[], int off, int buflength) throws ConnectionException {
	  try {
	      out.write(buf, off, buflength);
	  } catch (IOException ex) {
	      throw new ConnectionException(connection, ex);
	  }
	}
	public void write(int b) throws ConnectionException {
	    throw new NotImplementedException();
	}
	public void close() throws ConnectionException {
	    try { 
	        flush();
	    } finally {
	        connection.state|=2;
	        if(connection.state==connection.ostate)
		  try {
		    connection.close();
		  } catch (IOException e) {
		      throw new ConnectionException(connection, e);
		  }
	    }
	}
	public void flush() throws ConnectionException {
	    try {
	        out.flush();
	    } catch (IOException ex) {
	        throw new ConnectionException(connection, ex);
	    }
	}
    }
    /**
     * Represents the connection kept by the pool.
     * 
     * @author jostb
     *
     */
    public final class Connection {
        protected int ostate, state; // bit0: input closed, bit1: output closed
	protected Socket socket;
	private String host;
	private int port;
	private DefaultOutputStream outputStream;
	private DefaultInputStream inputStream;
	private boolean isClosed;
	private Factory factory;
	private int maxRequests;
	private int counter;
	
	protected void reset() {
            this.state = this.ostate = 0;
 	}
	protected void init() {
            inputStream = null;
            outputStream = null;
            counter = maxRequests; 
           reset();
	}
	protected Connection reopen() throws ConnectException, SocketException {
            if(isClosed) this.socket = factory.connect(host, port);
            this.isClosed = false;
            return this;
	}
	protected Connection(String host, int port, int maxRequests, Factory factory) throws ConnectException, SocketException {
            this.host = host;
            this.port = port;
            this.factory = factory;
            this.isClosed = true;
            this.maxRequests = maxRequests;
            init();
        }
	/** Set the closed/abort flag for this connection */
	public void setIsClosed() {
	    isClosed=true;
	}
	protected void close() throws ConnectException, SocketException {
	    // PHP child terminated: mark as closed, so that reopen() can allocate 
	    // a new connection for the new PHP child
	    if (maxRequests>0 && --counter==0) isClosed = true;
	    
	    if(isClosed) {
	        destroy();
	        init();
	    }
	    closeConnection(this);
        }

	private void destroy() {
	    try {
	        socket.close();
	    } catch (IOException e) {/*ignore*/}
	}
	/**
	 * Returns the OutputStream associated with this connection.
	 * @return The output stream.
	 * @throws IOException
	 */
	public OutputStream getOutputStream() throws ConnectionException {
	    if(outputStream != null) return outputStream;
	    DefaultOutputStream outputStream = (DefaultOutputStream) factory.createOutputStream();
	    outputStream.setConnection(this);
	    ostate |= 2;
	    return outputStream;
	}
	/**
	 * Returns the InputStream associated with this connection.
	 * @return The input stream.
	 * @throws IOException
	 */
	public InputStream getInputStream() throws ConnectionException {
	    if(inputStream != null) return inputStream;
	    DefaultInputStream inputStream = (DefaultInputStream) factory.createInputStream();
	    inputStream.setConnection(this);
	    ostate |= 1;
	    return inputStream;
	}
    }
    /**
     * Create a new connection pool.
     * 
     * @param host The host
     * @param port The port number
     * @param limit The max. number of physical connections
     * @param factory A factory for creating In- and OutputStreams.
     * @throws ConnectException 
      * @see ConnectionPool.Factory
     */
    public ConnectionPool(String host, int port, int limit, int maxRequests, Factory factory) throws ConnectException {
        this.host = host;
        this.port = port;
        this.limit = limit;
        this.factory = factory;
        this.maxRequests = maxRequests;
        
        Socket testSocket;
	try {
	  testSocket = new Socket(InetAddress.getByName(host), port);
	  testSocket.close();
	} catch (IOException e) {
	  throw new ConnectException(e);
	}
    }

    /* helper for openConnection() */
    private Connection createNewConnection() throws ConnectException, SocketException {
        Connection connection = new Connection(host, port, maxRequests, factory);
        connectionList.add(connection);
        connections++;
        return connection;
    }
    /**
     * Opens a connection to the back end.
     * @return The connection
     * @throws UnknownHostException
     * @throws InterruptedException
     * @throws SocketException 
     * @throws IOException 
     */
    public synchronized Connection openConnection() throws InterruptedException, ConnectException, SocketException {
        Connection connection;
      	if(freeList.isEmpty() && connections<limit) {
      	    connection = createNewConnection();
      	} else {
      	    while(freeList.isEmpty()) wait();
      	    connection = (Connection) freeList.remove(0);
      	    connection.reset();
      	}
      	return connection.reopen();
    }
    private synchronized void closeConnection(Connection connection) {
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
            connection.destroy();
        }
    }
}