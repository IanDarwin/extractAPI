package extractapi;

import junit.framework.TestCase;

public class TestExtractUtils extends TestCase {

	public void testFormatClassName() {
		assertEquals("Object", ExtractAPI.formatClassName("java.lang", "java.lang.Object"));
		assertEquals("ActionEvent", ExtractAPI.formatClassName("java.awt.evetn", "java.awt.event.ActionEvent"));
		assertEquals("Inner", ExtractAPI.formatClassName("java.lang", "java.lang.Byte$Inner"));
	}
	
	public void testdefaultValue() {
		assertEquals("0", ExtractAPI.defaultValue(int.class));
		// XXX assertEquals("0", ExtractAPI.defaultValue(Double.class));
		assertEquals("null", ExtractAPI.defaultValue(String.class));
		assertEquals("false", ExtractAPI.defaultValue(boolean.class));
	}
}
