package php.java.test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import php.java.test.php5.TestExtends;

@RunWith(Suite.class)
@SuiteClasses({ TestBindings.class, TestCGI.class, TestCli.class,
        TestDiscovery.class, TestException.class,
        TestExceptionInvocable2.class, TestGetInterface.class,
        TestGetResult.class, TestError.class,
        TestInvocable.class, TestInvocablePhpScriptEngine.class,
        TestPhpScriptEngine.class, TestScript.class, TestSetWriter.class,
        TestSimpleCompileable.class, TestSimpleInvocation.class,TestExtends.class ,
        TestConnectionPool.class
        })
public class AllTests {

}
