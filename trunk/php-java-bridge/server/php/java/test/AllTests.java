package php.java.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ TestBindings.class, TestCGI.class, TestCli.class,
        TestDiscovery.class, TestException.class, TestExceptionInvocable.class,
        TestExceptionInvocable2.class, TestGetInterface.class,
        TestGetResult.class, TestInteractiveRequestAbort.class,
        TestInvocable.class, TestInvocablePhpScriptEngine.class,
        TestPhpScriptEngine.class, TestScript.class, TestSetWriter.class,
        TestSimpleCompileable.class, TestSimpleInvocation.class })
public class AllTests {

}
