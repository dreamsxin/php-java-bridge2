package php.java.test;

import java.io.File;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

public class ScriptEngineHelper {

    public static ScriptEngine getPhpScriptEngine4Test() {
	ScriptEngineManager manager = new ScriptEngineManager();
	ScriptEngine e = manager.getEngineByName("php");
	
	// set ARGV[0] so that first element points to our PHP executable (the directory containing the machine-specific versions, actually), otherwise php-cgi must be in the PATH
	((String[])(e.get(ScriptEngine.ARGV)))[0]=new File(new File("WebContent/WEB-INF/cgi"), "php-cgi").getAbsolutePath();
	
	return e;
    }
}
