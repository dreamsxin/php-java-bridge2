<%@page import="javax.script.*" %>
<%@page import="php.java.script.servlet.PhpHttpScriptContext" %>

<%!
private static final CompiledScript script;
static {
	try {
		script =((Compilable)(new ScriptEngineManager().getEngineByName("php"))).compile(
        "<?php echo 'Hello '.java_context()->get('hello').'!<br>\n'; function f($v){return (string)$v+1;};?>");
	} catch (ScriptException e) {
		throw new RuntimeException(e);
	}
}
%>

<%
  try {
	  // create a custom ScriptContext to connect the engine to the ContextLoaderListener's FastCGI runner 
	  script.getEngine().setContext(new PhpHttpScriptContext(script.getEngine().getContext(),this,application,request,response));
	
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
	  ((java.io.Closeable)script.getEngine()).close();
  }
%>
