<html>
<?php 
require_once ("java/Java.inc");
$Util = java("php.java.bridge.Util");
$ctx = java_context();
/* get the current instance of the JavaBridge, ServletConfig and Context */
$bridge = $ctx->getAttribute(  "php.java.bridge.JavaBridge",      100);
$config = $ctx->getAttribute ( "php.java.servlet.ServletConfig",  100);
$context = $ctx->getAttribute( "php.java.servlet.ServletContext", 100);
$CGIServlet = java("php.java.servlet.PhpCGIServlet");
$servlet = $ctx->getAttribute( "php.java.servlet.Servlet", 100);
?>
<head>
   <title>PHP/Java Bridge settings</title>
</head>
<body bgcolor="#FFFFFF">
<H1>PHP/Java Bridge settings</H1>
<p>
The PHP/Java Bridge web application contains two servlets. The <code>PhpJavaServlet</code> handles requests from remote PHP scripts running in Apache/IIS or from the command line. 
The second servlet <code>PhpCGIServlet</code> can handle requests from internet clients directly. 
<p>
The following shows the settings of the <code>PhpJavaServlet</code> and the <code>PhpCGIServlet</code>.
</p>
<H2>PhpJavaServlet</H2>
<p>
The <code>PhpJavaServlet</code> handles requests from PHP clients.
<blockquote>
<code>
Apache/IIS/console::PHP &lt;--&gt; PhpJavaServlet
</code>
</blockquote>

It listens for PHP/Java Bridge protocol requests on the local interface or on all available network interfaces and invokes Java methods or procedures. The following example accesses the bridge listening on the <strong>local</strong> interface:
<blockquote>
<code>
&lt;?php <br>
require_once("http://localhost:8080/JavaBridge/java/Java.inc");<br>
$System = java("java.lang.System");<br>
echo $System->getProperties();<br>
?&gt;
</code>
</blockquote>

</p>
<table BORDER=1 CELLSPACING=5 WIDTH="85%" >
<tr VALIGN=TOP>
<th>Option</th>
<th>Value</th>
<th WIDTH="60%">Description</th>
</tr>
<tr>
<td>servlet_log_level</td>
<td><?php echo java_values($bridge->getlogLevel());?></td>
<td>The request log level.</td>
</tr>
<tr>
<td>promiscuous</td>
<td><?php echo java_values($Util->JAVABRIDGE_PROMISCUOUS) ? "On" : "Off" ?></td>
<td>Shall the bridge accept requests from <strong>non-local</strong> PHP scripts?</td>
</tr>
</table>
</p>
<p>
<?php if (java_instanceof ($servlet, $CGIServlet)) { ?>
<H2>PhpCGIServlet</H2>
<p>
The <code>PhpCGIServlet</code> runs PHP scripts within the J2EE/Servlet engine.
</p>
<blockquote>
<code>
internet browser &lt;--&gt; PhpCGIServlet &lt;--&gt; php-cgi &lt;--&gt; PhpJavaServlet
</code>
</blockquote>
<p>
It starts a PHP FastCGI server, if possible and neccessary. Requests for PHP scripts are delegated to the FastCGI server. If the PHP code contains Java calls, the PHP/Java Bridge protocol requests are delegated back to the current VM, to an instance of the <code>PhpJavaServlet</code>.
</p>
<table BORDER=1 CELLSPACING=5 WIDTH="85%" >
<tr VALIGN=TOP>
<th>Option</th>
<th>Value</th>
<th WIDTH="60%">Description</th>
</tr>

<tr>
<td>php_exec</td>
<td><?php $val=java_values($config->getInitParameter("php_exec")); echo $val?$val:"php-cgi"?></td>
<td>The PHP FastCGI or CGI binary.</td>
</tr>

<tr>
<td>prefer_system_php_exec</td>
<td><?php $val=java_values($config->getInitParameter("prefer_system_php_exec")); echo $val?$val:"Off"?></td>
<td>May we use /usr/bin/php-cgi or c:/php/php-cgi.exe if a local WEB-INF/cgi/php-cgi-ARCH-OS executable is available?</td>
</tr>

<tr>
<td>thread pool size</td>
<td><?php $val=java_values($servlet->getServletPoolSize()); echo $val?$val:"unknown"?></td>
<td>The servlet thread pool size, taken from <code>Util.getMBeanProperty("*:type=ThreadPool,name=http*", "maxThreads")</code> or 
      <code>Util.getMBeanProperty("*:ServiceModule=*,J2EEServer=*,name=JettyWebConnector,j2eeType=*", "maxThreads");</code> or 
     from the system property <code>php.java.bridge.threads</code>.</td>
</tr>

</table>
</p>

<?php /* current sevlet is CGIServlet */ } ?>

The settings were taken from the <a href="file://<?php 
echo java_values($CGIServlet->getRealPath($context, '/WEB-INF/web.xml'))
?>">WEB-INF/web.xml</a>.
</body>
</html>