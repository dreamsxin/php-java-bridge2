/*-*- mode: Java; tab-width:8 -*-*/

package php.java.fastcgi;

import java.io.IOException;
import java.io.OutputStream;

import php.java.bridge.util.NotImplementedException;

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

public abstract class FCGIConnectionOutputStream extends OutputStream {
    protected Connection connection;
    private OutputStream out;
    
    public FCGIConnectionOutputStream(Connection connection, OutputStream out) {
	this.connection = connection;
	this.out = out;
    }
    /**{@inheritDoc}*/  
    public void write(byte buf[]) throws ConnectionException {
        write(buf, 0, buf.length);
    }
    /**{@inheritDoc}*/  
    public void write(byte buf[], int off, int buflength) throws ConnectionException {
	try {
	    out.write(buf, off, buflength);
	} catch (IOException ex) {
	    throw new ConnectionException(connection, ex);
	}
    }
    /**{@inheritDoc}*/  
    public void write(int b) throws ConnectionException {
        throw new NotImplementedException();
    }
    /**{@inheritDoc}*/  
    public void close() throws ConnectionException {
        try { 
            flush();
        } finally {
            connection.state|=2;
            if(connection.state==connection.ostate)
		try {
		    connection.closeConnection();
		} catch (IOException e) {
		    throw new ConnectionException(connection, e);
		}
        }
    }
    /**{@inheritDoc}*/  
    public void flush() throws ConnectionException {
        try {
            out.flush();
        } catch (IOException ex) {
            throw new ConnectionException(connection, ex);
        }
    }
}