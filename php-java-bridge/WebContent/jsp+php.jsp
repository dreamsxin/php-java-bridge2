<%@page import="javax.script.*" %>
<%@page import="php.java.script.servlet.PhpServletScriptContext" %>

<%!
private static CompiledScript scriptCached;
private static synchronized CompiledScript getScript(Servlet servlet, ServletContext application, HttpServletRequest request, HttpServletResponse response) throws ScriptException {
	if (scriptCached!=null) return scriptCached;
	
	// get a script engine  
	ScriptEngine engine = new ScriptEngineManager().getEngineByName("php");
	
	// set the servlet environment. This is important, otherwise we would not attach to the servlet context.
	engine.setContext(new PhpServletScriptContext(engine.getContext(),servlet,application,request,response));
	
	// compile and return a handle to it
	return ((Compilable)engine).compile("<?php echo 'Hello '.java_context()->get('hello').'!<br>\n'; function f($v){return (string)$v+1;};?>");
}
%>

<%
  CompiledScript script = null;
  try {
	  // create a custom ScriptContext to connect the engine to the ContextLoaderListener's FastCGI runner 
	script = getScript(this, application, request, response);
	
	// display hello world
	script.getEngine().put("hello", "eval1: " + Thread.currentThread());
	script.eval();
	out.println(((Invocable)script.getEngine()).invokeFunction("f", new Object[]{1})+"<br>\n");
	script.getEngine().put("hello", "eval2: " + Thread.currentThread());
	script.eval();
	out.println(((Invocable)script.getEngine()).invokeFunction("f", new Object[]{2})+"<br>\n");
	script.getEngine().put("hello", "eval3: " + Thread.currentThread());
	script.eval();
	out.println(((Invocable)script.getEngine()).invokeFunction("f", new Object[]{3})+"<br>\n");
	script.getEngine().put("hello", "eval4: " + Thread.currentThread());
	script.eval();
	out.println(((Invocable)script.getEngine()).invokeFunction("f", new Object[]{4})+"<br>\n");
	out.println("thread ended: " + Thread.currentThread());
  } catch (Exception ex) {
	out.println("Could not evaluate script: "+ex);
  } finally {
	if (script!=null) ((java.io.Closeable)script.getEngine()).close();
  }
%>
