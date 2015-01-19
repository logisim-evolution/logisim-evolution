/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/

package com.cburch.logisim;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

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
	 * Test method for
	 * {@link com.cburch.logisim.LogisimVersion#parse(java.lang.String)}.
	 */
	@Test
	public void shouldTestParse() {
		assertNotNull(LogisimVersion.parse("1.2.3"));
		// Should return a new object
		assertNotSame(LogisimVersion.parse("1.2.3"),
				LogisimVersion.parse("1.2.3"));
		assertTrue(LogisimVersion.parse("1.2.3").equals(
				LogisimVersion.parse("1.2.3")));
		assertEquals("1.2.3", LogisimVersion.parse("1.2.3").mainVersion());
	}

	/**
	 * Test method for
	 * {@link com.cburch.logisim.LogisimVersion#compareTo(com.cburch.logisim.LogisimVersion)}
	 * .
	 */
	@Test
	public void testCompareTo() {
		assertTrue(older.compareTo(newer) < 0);
		assertTrue(newer.compareTo(newer) == 0);
		assertTrue(newer.compareTo(newerToo) == 0);
		assertTrue(newer.compareTo(older) > 0);
	}

	/**
	 * Test method for
	 * {@link com.cburch.logisim.LogisimVersion#equals(java.lang.Object)}.
	 */
	@Test
	public void testEqualsObject() {
		assertTrue(older.equals(older));
		assertFalse(older.equals(newer));
	}

}