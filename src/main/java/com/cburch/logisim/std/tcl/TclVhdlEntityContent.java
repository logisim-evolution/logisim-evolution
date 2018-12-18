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
package com.cburch.logisim.std.tcl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.cburch.logisim.std.hdl.VhdlContent;

/**
 * This is the same as the parent, just the template has to change. Code
 * duplication is due to VhdlContent strange structure. Please optimize if you
 * got the time, sorry for this debt.
 *
 * @author christian.mueller@heig-vd.ch
 *
 */
public class TclVhdlEntityContent extends VhdlContent {

	public static TclVhdlEntityContent create() {
		return new TclVhdlEntityContent();
	}

	// TODO: remove code duplication with parent class
	private static String loadTemplate() {
		InputStream input = VhdlContent.class.getResourceAsStream(RESOURCE);
		BufferedReader in = new BufferedReader(new InputStreamReader(input));

		StringBuilder tmp = new StringBuilder();
		String line;

		try {
			while ((line = in.readLine()) != null) {
				tmp.append(line);
				tmp.append(System.getProperty("line.separator"));
			}
		} catch (IOException ex) {
			return "";
		} finally {
			try {
				if (input != null)
					input.close();
			} catch (IOException ex) {
				Logger.getLogger(VhdlContent.class.getName()).log(Level.SEVERE,
						null, ex);
			}
		}

		return tmp.toString();
	}

	private static final String RESOURCE = "/resources/logisim/tcl/entity.templ";

	private static final String TEMPLATE = loadTemplate();

	protected TclVhdlEntityContent() {
		super.parseContent(TEMPLATE);
	}

}
