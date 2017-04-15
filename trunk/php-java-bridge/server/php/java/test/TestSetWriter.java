package php.java.test;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import javax.script.ScriptContext;

import org.junit.Test;

public class TestSetWriter extends ScriptEngineTestBase {

    
    public static class TestWriter extends Writer {

	public void close() throws IOException {
	    throw new RuntimeException("failed");
	}

	public void flush() throws IOException {

	}

	public void write(char[] cbuf, int off, int len) throws IOException {
	    assertTrue("3".equals(new String(cbuf, off, len)));
	}

    }

    public static class TestReader extends Reader {

	public void close() throws IOException {
	    throw new RuntimeException("failed");
	}

	public int read(char[] cbuf, int off, int len) throws IOException {
	    return 0;
	}

    }

    @Test
    public void test() throws Exception {
	ScriptContext c = e.getContext();
	c.setWriter(new TestWriter());
	c.setErrorWriter(new TestWriter());
	c.setReader(new TestReader());
	e.eval("<?php echo 1+2; ?>");
    }

}
