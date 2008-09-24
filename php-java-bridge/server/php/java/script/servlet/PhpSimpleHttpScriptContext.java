/*-*- mode: Java; tab-width:8 -*-*/

package php.java.script.servlet;

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
import java.io.Reader;
import java.io.Writer;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import php.java.bridge.http.IContext;
import php.java.script.AbstractPhpScriptContext;
import php.java.script.IPhpScriptContext;
import php.java.script.PhpScriptLogWriter;
import php.java.script.PhpScriptWriter;


/**
 * A simple ScriptContext which can be used in servlet environments.
 * 
 * @author jostb
 *
 */
public class PhpSimpleHttpScriptContext extends AbstractPhpScriptContext implements IContext, IPhpScriptContext {

    protected HttpServletRequest request;
    protected HttpServletResponse response;
    protected ServletContext context;
    protected Servlet servlet;
    
    /**
     * Initialize the context.
     * @param ctx The ServletContext
     * @param req The HttpServletRequest
     * @param res The HttpServletResponse
     * @param writer The PhpScriptWriter
     * @throws ServletException
     */
    public void initialize(Servlet servlet,
		    	   ServletContext ctx,
			   HttpServletRequest req,
			   HttpServletResponse res) {
	this.context = ctx;
	this.request = req;
	this.response = res;
	this.servlet = servlet;
    }

    public Object getAttribute(String key, int scope){
	if(scope == REQUEST_SCOPE){
	    return request.getAttribute(key);
	}else if(scope == SESSION_SCOPE){
	    return request.getSession().getAttribute(key);
	}else if(scope == APPLICATION_SCOPE){
	    return context.getAttribute(key);	                        
	}else{
	    return super.getAttribute(key, scope);
	}
    }
    public Object getAttribute(String name) throws IllegalArgumentException{
	Object result;
	if (name == null) {
	    throw new IllegalArgumentException("name cannot be null");
	}
	          
	if ((engineScope!=null) && (result=engineScope.get(name)) != null) {
	    return result;
	} else if ((globalScope!=null) && (result=globalScope.get(name)) != null) {
	    return result;
	} else if ((result=request.getAttribute(name)) != null)  {
	    return result;
	} else if ((result=request.getSession().getAttribute(name)) != null)  {
	    return result;
	} else if ((result=context.getAttribute(name)) != null) {
	    return result;
	}
	return null;
    }

    public void setAttribute(String key, Object value, int scope)
	throws IllegalArgumentException {    	
	if(scope == REQUEST_SCOPE){
	    request.setAttribute(key, value);
	}else if(scope == SESSION_SCOPE){
	    request.getSession().setAttribute(key, value);
	}else if(scope == APPLICATION_SCOPE){
	    context.setAttribute(key, value);
	}else{
	    super.setAttribute(key, value, scope);    
	}
    }
		
    /**
     * Get the servlet response
     * @return The HttpServletResponse
     */
    public HttpServletResponse getResponse() {
	return response;
    }
    
    /**
     * Get the HttpServletRequest
     * @return The HttpServletRequest
     */
    public HttpServletRequest getRequest() {
        return request;
    }
    
    /**
     * Get the ServletContext
     * @return The current ServletContext
     */
    public ServletContext getContext() {
        return context;
    }

    public String getContextString() {
	StringBuffer buf = new StringBuffer();
	if(!request.isSecure())
		buf.append("h:");
	else
		buf.append("s:");
	buf.append("127.0.0.1");
	buf.append(":");
	buf.append(getSocketName()); 
	buf.append('/');
	buf.append(request.getRequestURI());
	buf.append("javabridge");
	return buf.toString();
    }

    public String getSocketName() {
	return String.valueOf(php.java.servlet.CGIServlet.getLocalPort(request));
    }
    private Writer _writer;
    /** {@inheritDoc} */
    public Writer getWriter() {
 	if(_writer == null)
 		try {
 			_writer = writer =  response.getWriter(); 
 		} catch (IllegalStateException x) {
 			/*ignore*/
 		} catch (IOException e) {
 			/*ignore*/
 		}
 	
 	if(_writer == null)
 		try { 
 			_writer = writer = new PhpScriptWriter (response.getOutputStream());
 		} catch (IOException ex) { 
 			throw new RuntimeException(ex); 
 		}

 	if(! (writer instanceof PhpScriptWriter)) setWriter(writer);
 	return writer;
    }

    private Writer _errorWriter;
    /** {@inheritDoc} */
    public Writer getErrorWriter() {
 	if(_errorWriter == null)
 		_errorWriter = errorWriter = PhpScriptLogWriter.getWriter(new php.java.servlet.Logger(context));

 	if(! (errorWriter instanceof PhpScriptWriter)) setErrorWriter(errorWriter);
 	return errorWriter;	
    }

    private Reader _reader;
    public Reader getReader() {
        if (_reader == null)
	        try {
	                _reader = reader = request.getReader();
                } catch (IOException e) {
                	throw new RuntimeException(e);
                }
	return reader;
    }
}
