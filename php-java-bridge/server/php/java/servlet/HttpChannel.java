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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import php.java.bridge.http.AbstractChannel;

public class HttpChannel extends AbstractChannel {
    
    private HttpServletRequest req;
    private HttpServletResponse res;

    public HttpChannel (HttpServletRequest req, HttpServletResponse res) {
	this.req = req;
	this.res = res;
    }

    public InputStream getInputStream() throws IOException {
	return req.getInputStream();
    }

    public String getName() {
	// TODO Auto-generated method stub
	return null;
    }

    public OutputStream getOuptutStream() throws IOException {
	return res.getOutputStream();
    }

    public void shutdown() {
	try {
	    req.getInputStream().close();
        } catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
        }
	try {
	    res.getOutputStream().close();
        } catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
        }

    }

}
