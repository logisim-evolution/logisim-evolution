package com.cburch.logisim;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class LogisimVersionTest {
	
	private LogisimVersion older;
	private LogisimVersion newer;
	private LogisimVersion newerToo;
	
	@Before
	public void setUp() {
		older = LogisimVersion.get(1, 2, 3);
		newer = LogisimVersion.get(1, 2, 4);
		newerToo = LogisimVersion.get(1, 2, 4);
	}

	/**
	 * Test method for {@link com.cburch.logisim.LogisimVersion#parse(java.lang.String)}.
	 */
	@Test
	public void shouldTestParse() {
		assertNotNull(LogisimVersion.parse("1.2.3"));
		// Should return a new object
		assertNotSame(LogisimVersion.parse("1.2.3"), LogisimVersion.parse("1.2.3"));
		assertTrue(LogisimVersion.parse("1.2.3").equals(LogisimVersion.parse("1.2.3")));
		assertEquals("1.2.3", LogisimVersion.parse("1.2.3").mainVersion());
	}
	
	/**
	 * Test method for {@link com.cburch.logisim.LogisimVersion#compareTo(com.cburch.logisim.LogisimVersion)}.
	 */
	@Test
	public void testCompareTo() {
		assertTrue(older.compareTo(newer) < 0);
		assertTrue(newer.compareTo(newer) == 0);
		assertTrue(newer.compareTo(newerToo) == 0);
		assertTrue(newer.compareTo(older) > 0);
	}

	/**
	 * Test method for {@link com.cburch.logisim.LogisimVersion#equals(java.lang.Object)}.
	 */
	@Test
	public void testEqualsObject() {
		assertTrue(older.equals(older));
		assertFalse(older.equals(newer));
	}

}
