/*-*- mode: Java; tab-width:8 -*-*/

package php.java.fastcgi;

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

import java.io.IOException;
import java.io.InputStream;

import php.java.bridge.NotImplementedException;

public class FCGIConnectionInputStream extends InputStream {
    protected Connection connection;
    private InputStream in;

    public FCGIConnectionInputStream(Connection connection, InputStream in) {
	this.connection = connection;
	this.in = in;
    }
    /**{@inheritDoc}*/  
    public int read(byte buf[]) throws ConnectionException {
	return read(buf, 0, buf.length);
    }
    /**{@inheritDoc}*/  
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
    /**{@inheritDoc}*/  
    public int read() throws ConnectionException {
	throw new NotImplementedException();
    }      
    /**{@inheritDoc}*/  
    public void close() throws ConnectionException {
	connection.state|=1;
	if(connection.state==connection.ostate)
	    try {
		connection.closeConnection();
	    } catch (IOException e) {
		throw new ConnectionException(connection, e);
	    }
    }
}