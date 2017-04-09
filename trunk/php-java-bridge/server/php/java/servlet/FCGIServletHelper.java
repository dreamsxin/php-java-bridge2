package php.java.servlet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.HashMap;

import javax.servlet.ServletContext;

import php.java.bridge.Util;
import php.java.bridge.util.Logger;
import php.java.fastcgi.FCGIHelper;
import php.java.fastcgi.FCGIUtil;

public class FCGIServletHelper extends FCGIHelper {

    public static final String PEAR_DIR = "/WEB-INF/pear";
    public static final String CGI_DIR = "/WEB-INF/cgi";
    public static final String WEB_INF_DIR = "/WEB-INF";

    public FCGIServletHelper() {
	super();
	php = null; // force recalculation
    }

    private String getRealPath(Object context, String str) {
	return ServletUtil.getRealPath((ServletContext) context, str);
    }

    public void createPhpFiles(Object context) {

	String javaDir = getRealPath(context, "java");
	if (javaDir != null) {
	    File javaDirFile = new File(javaDir);
	    try {
		if (!javaDirFile.exists()) {
		    javaDirFile.mkdir();
		}
	    } catch (Exception e) {
		/* ignore */}

	    File javaIncFile = new File(javaDir, "Java.inc");
	    try {
		if (!javaIncFile.exists()) {
		    Field f = Util.JAVA_INC.getField("bytes");
		    byte[] buf = (byte[]) f.get(Util.JAVA_INC);
		    OutputStream out = new FileOutputStream(javaIncFile);
		    out.write(buf);
		    out.close();
		}
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	    File phpDebuggerFile = new File(javaDir, "PHPDebugger.php");
	    try {
		if (!phpDebuggerFile.exists()) {
		    Field f = Util.PHPDEBUGGER_PHP.getField("bytes");
		    byte[] buf = (byte[]) f.get(Util.PHPDEBUGGER_PHP);
		    OutputStream out = new FileOutputStream(phpDebuggerFile);
		    out.write(buf);
		    out.close();
		}
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	    File javaProxyFile = new File(javaDir, "JavaProxy.php");
	    try {
		if (!javaProxyFile.exists()) {
		    Field f = Util.JAVA_PROXY.getField("bytes");
		    byte[] buf = (byte[]) f.get(Util.JAVA_PROXY);
		    OutputStream out = new FileOutputStream(javaProxyFile);
		    out.write(buf);
		    out.close();
		}
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	}
	String pearDir = getRealPath(context, PEAR_DIR);
	if (pearDir != null) {
	    File pearDirFile = new File(pearDir);
	    try {
		if (!pearDirFile.exists()) {
		    pearDirFile.mkdir();
		}
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	}
	String cgiDir = getRealPath(context, CGI_DIR);
	File cgiOsDir = new File(cgiDir, Util.osArch + "-" + Util.osName);
	File conf = new File(cgiOsDir, "conf.d");
	File ext = new File(cgiOsDir, "ext");
	File cgiDirFile = new File(cgiDir);
	try {
	    if (!cgiDirFile.exists()) {
		cgiDirFile.mkdirs();
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}

	try {
	    if (!conf.exists()) {
		conf.mkdirs();
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}

	try {
	    if (!ext.exists()) {
		ext.mkdir();
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}

	createLauncher(cgiOsDir);

	boolean exeExists = true;
	if (Util.USE_SH_WRAPPER) {
	    try {
		File phpCgi = new File(cgiOsDir, "php-cgi");
		if (!useSystemPhp(phpCgi)) {
		    new FCGIServletHelper().updateProcessEnvironment(conf);
		    File wrapper = new File(cgiOsDir, "php-cgi.sh");
		    if (!wrapper.exists()) {
			byte[] data = ("#!/bin/sh\nchmod +x ./" + Util.osArch
			        + "-" + Util.osName + "/php-cgi\n" + "exec ./"
			        + Util.osArch + "-" + Util.osName
			        + "/php-cgi -c ./" + Util.osArch + "-"
			        + Util.osName + "/php-cgi.ini \"$@\"")
			                .getBytes();
			OutputStream out = new FileOutputStream(wrapper);
			out.write(data);
			out.close();
		    }
		    File ini = new File(cgiOsDir, "php-cgi.ini");
		    if (!ini.exists()) {
			byte[] data = (";; -*- mode: Scheme; tab-width:4 -*-\n;; A simple php.ini\n"
			        + ";; DO NOT EDIT THIS FILE!\n"
			        + ";; Add your configuration files to the "
			        + conf + " instead.\n"
			        + ";; PHP extensions go to " + ext
			        + ". Please see phpinfo() for ABI version details.\n"
			        + "extension_dir=\"" + ext + "\"\n"
			        + "include_path=\"" + pearDir
			        + ":/usr/share/pear:.\"\n").getBytes();
			OutputStream out = new FileOutputStream(ini);
			out.write(data);
			out.close();
		    }
		} else {
		    exeExists = false;
		    File readme = new File(cgiOsDir,
		            "php-cgi.MISSING.README.txt");
		    if (!readme.exists()) {
			byte[] data = ("You can add \"php-cgi\" to this directory and re-deploy your web application.\n")
			        .getBytes();
			OutputStream out = new FileOutputStream(readme);
			out.write(data);
			out.close();
		    }
		}
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	} else {
	    try {
		File phpCgi = new File(cgiOsDir, "php-cgi.exe");
		if (!useSystemPhp(phpCgi)) {
		    updateProcessEnvironment(conf);
		    File ini = new File(cgiOsDir, "php.ini");
		    if (!ini.exists()) {
			byte[] data = (";; -*- mode: Scheme; tab-width:4 -*-\r\n;; A simple php.ini\r\n"
			        + ";; DO NOT EDIT THIS FILE!\r\n"
			        + ";; Add your configuration files to the "
			        + conf + " instead.\r\n"
			        + ";; PHP extensions go to " + ext
			        + ". Please see phpinfo() for ABI version details.\r\n"
			        + "extension_dir=\"" + ext + "\"\r\n"
			        + "include_path=\"" + pearDir + ";.\"\r\n")
			                .getBytes();
			OutputStream out = new FileOutputStream(ini);
			out.write(data);
			out.close();
		    }
		} else {
		    exeExists = false;
		    File readme = new File(cgiOsDir,
		            "php-cgi.exe.MISSING.README.txt");
		    if (!readme.exists()) {
			byte[] data = ("You can add \"php-cgi.exe\" to this directory and re-deploy your web application.\r\n")
			        .getBytes();
			OutputStream out = new FileOutputStream(readme);
			out.write(data);
			out.close();
		    }
		}
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	}

	File tmpl = new File(conf, "mysql.ini");
	if (exeExists && !tmpl.exists()) {
	    String str;
	    if (Util.USE_SH_WRAPPER) {
		str = ";; -*- mode: Scheme; tab-width:4 -*-\n"
		        + ";; Example extension.ini file: mysql.ini.\n"
		        + ";; Copy the correct version (see phpinfo()) of the PHP extension \"mysql.so\" to the ./../ext directory and uncomment the following line\n"
		        + "; extension = mysql.so\n";
	    } else {
		str = ";; -*- mode: Scheme; tab-width:4 -*-\r\n"
		        + ";; Example extension.ini file: mysql.ini.\r\n"
		        + ";; Copy the correct version (see phpinfo()) of the PHP extension \"php_mysql.dll\" to the .\\..\\ext directory and uncomment the following line\r\n"
		        + "; extension = php_mysql.dll\r\n";
	    }
	    byte[] data = str.getBytes();
	    try {
		OutputStream out = new FileOutputStream(tmpl);
		out.write(data);
		out.close();
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	}
    }

    public void updateProcessEnvironment(File conf) {
	try {
	    PROCESS_ENVIRONMENT.put("PHP_INI_SCAN_DIR",
	            conf.getCanonicalPath());
	} catch (IOException e) {
	    e.printStackTrace();
	    PROCESS_ENVIRONMENT.put("PHP_INI_SCAN_DIR", conf.getAbsolutePath());
	}
    }

    private boolean useSystemPhp(File f) {

	// path hard coded in web.xml
	if (!phpTryOtherLocations)
	    return true;

	// no local php exists
	if (!f.exists())
	    return true;

	// local exists
	if (!preferSystemPhp)
	    return false;

	// check default locations for preferred system php
	for (int i = 0; i < Util.DEFAULT_CGI_LOCATIONS.length; i++) {
	    File location = new File(Util.DEFAULT_CGI_LOCATIONS[i]);
	    if (location.exists())
		return true;
	}

	return false;
    }

    private static HashMap getProcessEnvironment() {
	HashMap map = new HashMap(Util.COMMON_ENVIRONMENT);
	return map;
    }

    static final HashMap PROCESS_ENVIRONMENT = getProcessEnvironment();

    private void checkCgiBinary(ServletContext context) {
	String value;
	if (php == null) {
	    try {
		value = context.getInitParameter("php_exec");
		if (value == null || value.trim().length() == 0) {
		    value = "php-cgi";
		    phpTryOtherLocations = true;
		}
		File f = new File(value);
		if (!f.isAbsolute()) {
		    value = ServletUtil.getRealPath(context, CGI_DIR)
		            + File.separator + value;
		}
		php = value;
	    } catch (Throwable t) {
		Logger.printStackTrace(t);
	    }
	}
    }

    public int getPhpFcgiConnectionPoolSize() {
	return phpFcgiConnectionPoolSize;
    }

    public void init(ServletContext context) {
	String value;
	try {
	    value = context.getInitParameter("prefer_system_php_exec");
	    if (value == null)
		value = System.getProperty("php.java.bridge.prefer_system_php_exec");
	    if (value == null)
		value = "";
	    value = value.trim();
	    value = value.toLowerCase();
	    if (value.equals("on") || value.equals("true"))
		preferSystemPhp = true;
	} catch (Throwable t) {
	    t.printStackTrace();
	}
	String val = null;
	try {
	    val = context.getInitParameter("php_fcgi_children");
	    if (val == null)
		val = context.getInitParameter("PHP_FCGI_CHILDREN");
	    if (val == null)
		val = System.getProperty("php.java.bridge.php_fcgi_children");
	    if (val == null)
		val = context.getInitParameter("php_fcgi_connection_pool_size");
	    if (val == null)
		val = System.getProperty(
		        "php.java.bridge.php_fcgi_connection_pool_size");
	} catch (Throwable t) {
	    /* ignore */}
	if (val == null)
	    val = FCGIUtil.PHP_FCGI_CONNECTION_POOL_SIZE;
	phpFcgiConnectionPoolSize = Integer.parseInt(val);

	val = null;
	try {
	    val = context.getInitParameter("php_fcgi_external_socket_pool");
	    if (val == null)
		val = context.getInitParameter("PHP_FCGI_EXTERNAL_SOCKET_POOL");
	    if (val == null)
		val = System.getProperty("php.java.bridge.php_fcgi_external_socket_pool");
	} catch (Throwable t) {
	    /* ignore */}
	socketPort = val;

	val = null;
	try {
	    val = context.getInitParameter("php_fcgi_connection_pool_timeout");
	    if (val == null)
		val = System.getProperty(
		        "php.java.bridge.php_fcgi_connection_pool_timeout");
	    if (val != null)
		phpFcgiConnectionPoolTimeout = Integer.parseInt(val);
	} catch (Throwable t) {
	    /* ignore */}
	if (val == null)
	    val = FCGIUtil.PHP_FCGI_CONNECTION_POOL_TIMEOUT;
	phpFcgiConnectionPoolTimeout = Integer.parseInt(val);

	val = null;
	phpIncludeJava = false;
	try {
	    val = context.getInitParameter("php_include_java");
	    if (val == null)
		val = context.getInitParameter("PHP_INCLUDE_JAVA");
	    if (val == null)
		val = System.getProperty("php.java.bridge.php_include_java");
	    if (val != null && (val.equalsIgnoreCase("on")
	            || val.equalsIgnoreCase("true")))
		phpIncludeJava = true;
	} catch (Throwable t) {
	    /* ignore */}

	val = null;
	phpIncludeDebugger = true;
	try {
	    val = context.getInitParameter("php_include_debugger");
	    if (val == null)
		val = context.getInitParameter("PHP_INCLUDE_DEBUGGER");
	    if (val == null)
		val = System
		        .getProperty("php.java.bridge.php_include_debugger");
	    if (val != null && (val.equalsIgnoreCase("off")
	            || val.equalsIgnoreCase("false")))
		phpIncludeDebugger = false;
	} catch (Throwable t) {
	    /* ignore */}

	val = null;
	try {
	    val = context.getInitParameter("php_fcgi_max_requests");
	    if (val == null)
		val = System
		        .getProperty("php.java.bridge.php_fcgi_max_requests");
	} catch (Throwable t) {
	    /* ignore */}
	if (val == null) {
	    val = FCGIUtil.PHP_FCGI_MAX_REQUESTS;
	}
	phpFcgiMaxRequests = Integer.parseInt(val);

	promiscuous = true;
	value = null;
	try {
	    value = context.getInitParameter("promiscuous");
	    if (value == null)
		value = "";
	    value = value.trim();
	    value = value.toLowerCase();

	    if (value.equals("off") || value.equals("false"))
		promiscuous = false;
	} catch (Exception t) {
	    t.printStackTrace();
	}

	checkCgiBinary(context);
	createPhpFiles(context);
    }

}
