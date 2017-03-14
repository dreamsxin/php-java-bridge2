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

import php.java.bridge.Util;
import php.java.bridge.http.FCGIUtil;

public class FCGIInputStream extends FCGIConnectionInputStream {
    public FCGIInputStream(Connection connection, InputStream in) {
	super(connection, in);
    }
    private StringBuffer error;
    public StringBuffer getError () {
        return error;
    }
    public String checkError() {
        return error==null?null:Util.checkError(error.toString());
    }
    public int read(byte buf[]) throws ConnectionException {
        try {
	    return doRead(buf);
        } catch (ConnectionException ex) {
	    throw ex;
        } catch (IOException e) {
            throw new ConnectionException(connection, e);
        }
    }
    private byte header[] = new byte[FCGIUtil.FCGI_HEADER_LEN];
    public int doRead(byte buf[]) throws IOException {
        int n, i;
        //assert if(buf.length!=FCGI_BUF_SIZE) throw new IOException("Invalid block size");
        for(n=0; (i=read(header, n, FCGIUtil.FCGI_HEADER_LEN-n)) > 0; )  n+=i;
        if(FCGIUtil.FCGI_HEADER_LEN != n) 
	    throw new IOException ("Protocol error");
        int type = header[1] & 0xFF;
        int contentLength = ((header[4] & 0xFF) << 8) | (header[5] & 0xFF);
        int paddingLength = header[6] & 0xFF;
        switch(type) {
        case FCGIUtil.FCGI_STDERR: 
        case FCGIUtil.FCGI_STDOUT: {
	    for(n=0; (i=read(buf, n, contentLength-n)) > 0; ) n+=i;
	    if(n!=contentLength) 
		throw new IOException("Protocol error while reading FCGI data");
	    if(type==FCGIUtil.FCGI_STDERR) { 
		String s = new String(buf, 0, n, Util.ASCII);
		//this.processFactory.log(s); //FIXME 
		contentLength = 0;

		if(error==null) error = new StringBuffer(s);
		else error.append(s);
	    }
	    if(paddingLength>0) {
		byte b[] = new byte[paddingLength];
		for(n=0; (i=read(b, n, b.length-n)) > 0; ) n+=i;
		if(n!=paddingLength) 
		    throw new IOException("Protocol error while reading FCGI padding");
	    }
	    return contentLength;
        }
        case FCGIUtil.FCGI_END_REQUEST: {
	    for(n=0; (i=read(buf, n, contentLength-n)) > 0; ) n+=i;
	    if(n!=contentLength) throw new IOException("Protocol error while reading EOF data");
	    if(paddingLength>0) {
		n = super.read(buf, 0, paddingLength);		
		if(n!=paddingLength) throw new IOException("Protocol error while reading EOF padding");
	    }
	    return -1;
        }
        }
        throw new IOException("Received unknown type");
    }
}