package php.java.test;

import java.io.File;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

public class ScriptEngineHelper {

    public static ScriptEngine getPhpScriptEngine4Test() {
	ScriptEngineManager manager = new ScriptEngineManager();
	ScriptEngine e = manager.getEngineByName("php");
	String[] args = new String[] {
	        new File(new File("server/WEB-INF/cgi"), "php-cgi")
	                .getAbsolutePath() };
	e.put(ScriptEngine.ARGV, args);
	return e;
    }
}
