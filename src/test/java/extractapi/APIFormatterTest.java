package extractapi;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class APIFormatterTest {

	APIFormatter af;

	@Before
	public void setUp() throws Exception {
		af = new APIFormatter() {
			@Override
			protected void doClass(Class<?> c) throws IOException {
				System.out.println("Mock doClass() called with " + c.getCanonicalName());
			}
		};
	}

	@Test
	public void testSplitClassPath() {
		List<String> x = af.splitClassPath("zooks:/home/bar:.");
		assertEquals(3, x.size());
		assertEquals(".", x.get(2));
	}

}
