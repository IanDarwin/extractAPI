package reveng;

import junit.framework.TestCase;

public class TestRevEngUtils extends TestCase {
	public void testFormatClassName() {
		assertEquals("Object", RevEngAPI.formatClassName("java.lang", "java.lang.Object"));
		assertEquals("ActionEvent", RevEngAPI.formatClassName("java.awt.evetn", "java.awt.event.ActionEvent"));
		assertEquals("Inner", RevEngAPI.formatClassName("java.lang", "java.lang.Byte$Inner"));
	}
	
	public void testdefaultValue() {
		assertEquals("0", RevEngAPI.defaultValue(int.class));
		// XXX assertEquals("0", RevEngAPI.defaultValue(Double.class));
		assertEquals("null", RevEngAPI.defaultValue(String.class));
		assertEquals("false", RevEngAPI.defaultValue(boolean.class));
	}
}
