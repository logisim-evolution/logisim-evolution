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

package com.cburch.logisim.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class XmlReaderTest {

	/**
	 * Test method for
	 * {@link com.cburch.logisim.file.XmlReader#generateValidVHDLLabel(java.lang.String)}
	 * . We use here the version with a suffix in order to have a predictable
	 * suffix.
	 */
	@Test
	public final void testGenerateValidVHDLLabel() {
		// Valid labels should be left untouched
		assertEquals("aaa", XmlReader.generateValidVHDLLabel("aaa", "A"));
		assertEquals("aa_a", XmlReader.generateValidVHDLLabel("aa_a", "A"));
		assertEquals("a1_2", XmlReader.generateValidVHDLLabel("a1_2", "A"));
		assertEquals("a1_2_3_aa",
				XmlReader.generateValidVHDLLabel("a1_2_3_aa", "A"));
		// Invalid labels should be fixed and the suffix has to be appended
		// A "L_" has to be prepended if the initial character is invalid,
		// except if it is ! or ~, in which case it becomes "NOT_"
		assertEquals("L_1a1_2_3_aa_A",
				XmlReader.generateValidVHDLLabel("1a1_2_3_aa", "A"));
		assertEquals("NOT_1a1_2_3_aa_A",
				XmlReader.generateValidVHDLLabel("!1a1_2_3_aa", "A"));
		assertEquals("NOT_1a1_2_3_aa_A",
				XmlReader.generateValidVHDLLabel("~1a1_2_3_aa", "A"));
		assertEquals("NOT_1a1_2_3_aa_A",
				XmlReader.generateValidVHDLLabel("! 1a1_2_3_aa", "A"));
		assertEquals("NOT_1a1_2_3_aa_A",
				XmlReader.generateValidVHDLLabel("~ 1a1_2_3_aa", "A"));
		assertEquals("a1_2_3_aa_A",
				XmlReader.generateValidVHDLLabel("a1_____2___3___aa", "A"));
		assertEquals("a1_2_3_aa_A",
				XmlReader.generateValidVHDLLabel("a1_____2___3___aa_", "A"));
		// ! and ~ characters have to be replaced in the middle of a
		// label too
		assertEquals("L_1a1_NOT_2_3_aa_A",
				XmlReader.generateValidVHDLLabel("1a1_~2_3_aa", "A"));
		assertEquals("L_1a1_NOT_2_3_aa_A",
				XmlReader.generateValidVHDLLabel("1a1_!2_3_aa", "A"));
		assertEquals("NOT_NOT_NOT_aa_A",
				XmlReader.generateValidVHDLLabel("!~!aa", "A"));
		assertEquals("NOT_NOT_NOT_aa_A",
				XmlReader.generateValidVHDLLabel("!~!__aa", "A"));
		// Remove useless whitespaces at the beginning-end
		assertEquals("aaa", XmlReader.generateValidVHDLLabel("aaa     ", "A"));
		assertEquals("aaa", XmlReader.generateValidVHDLLabel("     aaa", "A"));
		assertEquals("aaa",
				XmlReader.generateValidVHDLLabel("     aaa      ", "A"));
		assertEquals("L_1a1_2_3_aa_A",
				XmlReader.generateValidVHDLLabel(" 1a1_2_3_aa", "A"));
		assertEquals("L_1a1_2_3_aa_A",
				XmlReader.generateValidVHDLLabel("1a1_2_3_aa ", "A"));
		assertEquals("L_1a1_2_3_aa_A",
				XmlReader.generateValidVHDLLabel(" 1a1_2_3_aa ", "A"));
	}

	/**
	 * Test method for
	 * {@link com.cburch.logisim.file.XmlReader#labelVHDLInvalid(java.lang.String)}
	 * .
	 */
	@Test
	public final void testLabelVHDLInvalid() {
		// Invalid labels
		assertTrue(XmlReader.labelVHDLInvalid("AAAA_"));
		assertTrue(XmlReader.labelVHDLInvalid("_AAAA"));
		assertTrue(XmlReader.labelVHDLInvalid("12"));
		assertTrue(XmlReader.labelVHDLInvalid("1A"));
		assertTrue(XmlReader.labelVHDLInvalid("aaaèaa"));
		assertTrue(XmlReader.labelVHDLInvalid("1 A"));
		assertTrue(XmlReader.labelVHDLInvalid("A 1"));
		assertTrue(XmlReader.labelVHDLInvalid("AAA "));
		assertTrue(XmlReader.labelVHDLInvalid("AA A"));
		assertTrue(XmlReader.labelVHDLInvalid("aa a1"));
		assertTrue(XmlReader.labelVHDLInvalid(" aa"));
		assertTrue(XmlReader.labelVHDLInvalid("a__a"));
		assertTrue(XmlReader.labelVHDLInvalid("1 2"));
		// Valid labels
		assertFalse(XmlReader.labelVHDLInvalid("a1"));
		assertFalse(XmlReader.labelVHDLInvalid("a13566356aa"));
		assertFalse(XmlReader.labelVHDLInvalid("A13566356aA"));
		assertFalse(XmlReader.labelVHDLInvalid("a_B_c"));
	}

}
