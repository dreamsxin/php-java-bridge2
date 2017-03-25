package php.java.test;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.OutputStreamWriter;

import javax.script.Invocable;
import javax.script.ScriptEngine;

import org.junit.Before;
import org.junit.Test;

public class TestGetInterface {

    private ScriptEngine scriptEngine;
    private String classA;
    private String classB;
    private String test;

    @Before
    public void setUp() {
	classA = "class A{function toString(){return '::A';} function invokeA($b){$b->invokeB();}}\n";
	classB = "class B{function toString(){return '::B';} function invokeB(){echo '::B';}}\n";
	test = "<?php " + classA + classB
	        + " $thiz=java_context()->getAttribute('thiz');\n$thiz->call(java_closure(new A()), java_closure(new B())); ?>";
    }

    @Test
    public void test() throws Exception {
	scriptEngine = ScriptEngineHelper.getPhpScriptEngine4Test();
	scriptEngine.put("thiz", this);
	ByteArrayOutputStream out;
	OutputStreamWriter writer;
	scriptEngine.getContext().setWriter(writer = new OutputStreamWriter(
	        out = new ByteArrayOutputStream()));
	scriptEngine.eval(test);
	((Closeable) scriptEngine).close();

	writer.close();
	if (!"::B".equals(out.toString())) {
	    fail("test failed");
	}
	return;
    }

    interface IA {
	public void invokeA(IB ccb);
    };

    interface IB {
	public void invokeB();
    };

    public void call(Object $cca, Object $ccb) {
	IA cca = (IA) ((Invocable) scriptEngine).getInterface($cca, IA.class);
	IB ccb = (IB) ((Invocable) scriptEngine).getInterface($ccb, IB.class);
	cca.invokeA(ccb);
    }

}
