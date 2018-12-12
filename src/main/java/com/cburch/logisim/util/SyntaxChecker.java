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
package com.cburch.logisim.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import com.bfh.logisim.designrulecheck.CorrectLabel;
import com.bfh.logisim.hdlgenerator.HDLGeneratorFactory;

public class SyntaxChecker {

	public static boolean isVariableNameAcceptable(String val, Boolean ShowDialog) {
		if (val.length() == 0)
			return true;
		if (val.length() > 0) {
			variableMatcher = variablePattern.matcher(val);
			forbiddenMatcher = forbiddenPattern.matcher(val);
			boolean ret = true;
			String HDL = CorrectLabel.HDLCorrectLabel(val);
			String Message ="";
			if (!variableMatcher.matches()) {
				ret = false;
				Message = Message.concat(Strings.get("variableInvalidCharacters"));
			}
			if (forbiddenMatcher.find()) {
				ret = false;
				Message = Message.concat(Strings.get("variableDoubleUnderscore"));
			}
			if (HDL!=null) {
				ret = false;
				Message = Message.concat( HDL.equals(HDLGeneratorFactory.VHDL) ? Strings.get("variableVHDLKeyword") : Strings.get("variableVerilogKeyword"));
			}
			if (!ret&&ShowDialog)
				JOptionPane.showMessageDialog(null, Message.concat("\n"+Strings.get("variableNameNotAcceptable")));
			return ret;
		}
		return false;
	}


	private static Pattern variablePattern = Pattern
			.compile("^([a-zA-Z]+\\w*)");
	private static Pattern forbiddenPattern = Pattern.compile("__");

	private static Matcher forbiddenMatcher;
	private static Matcher variableMatcher;
}
