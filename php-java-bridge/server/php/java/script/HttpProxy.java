/*-*- mode: Java; tab-width:8 -*-*/

package php.java.script;

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

import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents the script continuation.
 * This class can be used to allocate php scripts on a HTTP server.
 * Although this class accidentally inherits from <code>CGIRunner</code> it doesn't necessarily run CGI binaries.
 * If you pass a URLReader, it calls its read method which opens a URLConnection to the remote server
 * and holds the allocated remote script instance hostage until release is called.
 * @author jostb
 *
 */
public class HttpProxy extends CGIRunner {
    /**
     * Create a HTTP proxy which can be used to allocate a php script from a HTTP server
     * @param reader - The reader, for example a URLReader
     * @param env - The environment, must contain values for X_JAVABRIDGE_CONTEXT. It may contain X_JAVABRIDGE_OVERRIDE_HOSTS.
     * @param out - The OutputStream
     */
    public HttpProxy(Reader reader, Map env, OutputStream out) {
	super("HttpProxy", reader, env, out);
    }
    
    /**
     * Create a HTTP proxy which can be used to allocate a php script from a HTTP server
     * @param reader - The reader, for example a URLReader
     * @param ctx - The context
     */
    public HttpProxy(Reader reader, String ctx, OutputStream out) {
	this(reader, (new HashMap() {
		private static final long serialVersionUID = 3257005462371971380L;
		public HashMap init(String ctx) {put("X_JAVABRIDGE_CONTEXT", ctx); return this;}}
	).init(ctx), out);
    }

    protected void doRun() throws IOException {
    	if(reader instanceof URLReader) {
	    ((URLReader)reader).read(env, out);
     	} else {
	    super.doRun();
     	}
    }
    
}