package php.java.test;

import java.io.Closeable;

import javax.script.ScriptEngine;

import org.junit.After;
import org.junit.Before;

public class ScriptEngineTestBase {
    protected ScriptEngine e;
    @Before
    public void onCreate() throws Exception {
	    e = ScriptEngineHelper.getPhpScriptEngine4Test();
    }
    @After
    public void onShutdown() throws Exception {
	((Closeable) e).close();
    }

}
