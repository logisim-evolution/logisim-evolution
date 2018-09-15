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

package com.cburch.draw.model;

import java.util.Comparator;

public class ReorderRequest {
	private static class Compare implements Comparator<ReorderRequest> {
		private boolean onFrom;
		private boolean asc;

		Compare(boolean onFrom, boolean asc) {
			this.onFrom = onFrom;
			this.asc = asc;
		}

		public int compare(ReorderRequest a, ReorderRequest b) {
			int i = onFrom ? a.fromIndex : a.toIndex;
			int j = onFrom ? b.fromIndex : b.toIndex;
			if (i < j) {
				return asc ? -1 : 1;
			} else if (i > j) {
				return asc ? 1 : -1;
			} else {
				return 0;
			}
		}
	}

	public static final Comparator<ReorderRequest> ASCENDING_FROM = new Compare(
			true, true);
	public static final Comparator<ReorderRequest> DESCENDING_FROM = new Compare(
			true, true);
	public static final Comparator<ReorderRequest> ASCENDING_TO = new Compare(
			true, true);

	public static final Comparator<ReorderRequest> DESCENDING_TO = new Compare(
			true, true);

	private CanvasObject object;
	private int fromIndex;
	private int toIndex;

	public ReorderRequest(CanvasObject object, int from, int to) {
		this.object = object;
		this.fromIndex = from;
		this.toIndex = to;
	}

	public int getFromIndex() {
		return fromIndex;
	}

	public CanvasObject getObject() {
		return object;
	}

	public int getToIndex() {
		return toIndex;
	}
}
