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

package com.cburch.logisim.analyze.model;

import com.cburch.logisim.util.StringGetter;

public class Entry {
	public static Entry parse(String description) {
		if (ZERO.description.equals(description))
			return ZERO;
		if (ONE.description.equals(description))
			return ONE;
		if (DONT_CARE.description.equals(description))
			return DONT_CARE;
		if (BUS_ERROR.description.equals(description))
			return BUS_ERROR;
		return null;
	}

	public static final Entry ZERO = new Entry("0");
	public static final Entry ONE = new Entry("1");
	public static final Entry DONT_CARE = new Entry("-");
	public static final Entry BUS_ERROR = new Entry(Strings.getter("busError"));

	public static final Entry OSCILLATE_ERROR = new Entry(
			Strings.getter("oscillateError"));

	private String description;
	private StringGetter errorMessage;

	private Entry(String description) {
		this.description = description;
		this.errorMessage = null;
	}

	private Entry(StringGetter errorMessage) {
		this.description = "!!";
		this.errorMessage = errorMessage;
	}

	public String getDescription() {
		return description;
	}

	public String getErrorMessage() {
		return errorMessage == null ? null : errorMessage.toString();
	}

	public boolean isError() {
		return errorMessage != null;
	}

	@Override
	public String toString() {
		return "Entry[" + description + "]";
	}
}
