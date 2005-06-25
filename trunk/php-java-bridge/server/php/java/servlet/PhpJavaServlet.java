/*-*- mode: Java; tab-width:8 -*-*/
package php.java.servlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import php.java.bridge.JavaBridge;
import php.java.bridge.JavaBridgeClassLoader;
import php.java.bridge.Request;
import php.java.bridge.Session;
import php.java.bridge.Util;


public class PhpJavaServlet extends HttpServlet {
    public static class Logger extends Util.Logger {
	private ServletContext ctx;
	public Logger(ServletContext ctx) {
	    this.ctx = ctx;
	}
	public void log(String s) { ctx.log(s); }
	public String now() { return ""; }
	public void printStackTrace(Throwable t) {
	    ctx.log(Util.EXTENSION_NAME + " Exception: ", t);
	}
    }

    public void init(ServletConfig config) throws ServletException {
	Util.logLevel=Util.DEFAULT_LOG_LEVEL; /* java.log_level in php.ini overrides */
	Util.logger=new Logger(config.getServletContext());
    }

    public void doPut (HttpServletRequest req, HttpServletResponse res)
	throws ServletException, IOException {
	InputStream in; ByteArrayOutputStream out;

	HttpSession session = req.getSession();
	JavaBridge bridge = (JavaBridge) session.getAttribute("bridge");
	in = req.getInputStream();
	out = new ByteArrayOutputStream();
	if(bridge==null) {
	    bridge = new JavaBridge(in, out);
	    session.setAttribute("bridge", bridge);
	}
	if(req.getContentLength()==0) {
	    if(req.getHeader("Connection").equals("Close")) {
		session.invalidate();
		bridge.logDebug("session closed.");
	    }
	    return;
	}

	Request r = new Request(bridge);
	try {
	    if(r.initOptions(in, out)) {
		r.handleRequests();
	    }
	} catch (Throwable e) {
	    Util.printStackTrace(e);
	}
	Session.expire(bridge);
	if(session.isNew())
	    bridge.logDebug("first request terminated (session is new).");
	else
	    bridge.logDebug("request terminated (cont. session).");
        res.setContentLength(out.size());
        out.writeTo(res.getOutputStream());
    }
}
