package reveng;

import org.junit.*;
import static org.junit.Assert.*;

public class TestRevEngUtils {

	@Test
	public void testFormatClassName() {
		assertEquals("Object", RevEngAPI.formatClassName("java.lang", "java.lang.Object"));
		assertEquals("ActionEvent", RevEngAPI.formatClassName("java.awt.event", "java.awt.event.ActionEvent"));
		assertEquals("Inner", RevEngAPI.formatClassName("java.lang", "java.lang.Byte$Inner"));
	}

	@Test
	public void testdefaultValue() {
		assertEquals("0", RevEngAPI.defaultValue(int.class));
		assertEquals("0", RevEngAPI.defaultValue(double.class));
		assertEquals("null", RevEngAPI.defaultValue(String.class));
		assertEquals("false", RevEngAPI.defaultValue(boolean.class));
	}
}
