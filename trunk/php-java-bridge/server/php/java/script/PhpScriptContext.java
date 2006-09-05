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

import java.io.Writer;

import javax.script.SimpleScriptContext;

import php.java.bridge.JavaBridgeRunner;
import php.java.bridge.PhpProcedureProxy;
import php.java.bridge.Util;
import php.java.bridge.http.HttpServer;

/**
 * This class implements a simple script context for PHP. It starts a standalone 
 * <code>JavaBridgeRunner</code> which listens for requests from php instances.<p>
 * 
 * In a servlet environment please use a <code>php.java.script.PhpSimpleHttpScriptContext</code> instead.
 * @see php.java.script.PhpSimpleHttpScriptContext
 * @see php.java.bridge.JavaBridgeRunner
 * @author jostb
 *
 */
public class PhpScriptContext extends SimpleScriptContext implements IPhpScriptContext {
    static JavaBridgeRunner bridgeRunner = null;

    static {
	try {
	    bridgeRunner = JavaBridgeRunner.getInstance();
	} catch (Exception e) {
	    Util.printStackTrace(e);
	}
    }

    /**
     * Return the http server associated with this VM.
     * @return The http server.
     */
    public static HttpServer getHttpServer() {
      return bridgeRunner;
    }

    private HttpProxy kont;
    private Writer writer;
	
    /**
     * Create a standalone PHP script context.
     *
     */
    public PhpScriptContext() {
        super();
    }


    public Writer getWriter() {
	if(writer == null) return writer = new PhpScriptLogWriter();
	return writer;
    }
	
    /**
     * Set the php continuation
     * @param kont - The continuation.
     */
    public void setContinuation(HttpProxy kont) {
	this.kont = kont;
    }


    /* (non-Javadoc)
     * @see php.java.bridge.Invocable#call(php.java.bridge.PhpProcedureProxy)
     */
    /**@inheritDoc*/
    public boolean call(PhpProcedureProxy kont) throws InterruptedException {
	this.kont.call(kont);
	return true;
    }


    /**@inheritDoc*/
    public void setWriter(Writer writer) {
        this.writer = writer;
    }


    /**@inheritDoc*/
    public HttpProxy getContinuation() {
        return kont;
    }
}
