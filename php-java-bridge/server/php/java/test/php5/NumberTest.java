package php.java.test.php5;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;

import org.junit.Test;

import php.java.test.ScriptEngineTestBase;

public class NumberTest extends ScriptEngineTestBase {
    @Test
    public void test() throws Throwable {
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	e.getContext().setWriter(new OutputStreamWriter(out));
	e.eval(new FileReader(new File("numberTest.php")));
	((Closeable)e).close();
	assertEquals("bleh", out.toString());
    }

	public Integer getInteger() {
		return new Integer(-20);
	}
	
	public int getPrimitiveInt() {
		return -20;
	}

	public Float getFloat() {
		return new Float(-20);
	}

	public float getPrimitiveFloat() {
			return -20;
	}
	
	public BigDecimal getBigDecimal() {
		return new BigDecimal(-20);
	}

	public Double getDouble() {
		return new Double(-20);
	}

	public double getPrimitiveDouble() {
		return -20;
	}

}