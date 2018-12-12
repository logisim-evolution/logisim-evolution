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

package com.cburch.logisim.std.memory;

import com.cburch.hex.HexModel;
import com.cburch.hex.HexModelListener;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Project;

class RomContentsListener implements HexModelListener {
	private static class Change extends Action {
		private RomContentsListener source;
		private MemContents contents;
		private long start;
		private int[] oldValues;
		private int[] newValues;
		private boolean completed = true;

		Change(RomContentsListener source, MemContents contents, long start,
				int[] oldValues, int[] newValues) {
			this.source = source;
			this.contents = contents;
			this.start = start;
			this.oldValues = oldValues;
			this.newValues = newValues;
		}

		@Override
		public Action append(Action other) {
			if (other instanceof Change) {
				Change o = (Change) other;
				long oEnd = o.start + o.newValues.length;
				long end = start + newValues.length;
				if (oEnd >= start && end >= o.start) {
					long nStart = Math.min(start, o.start);
					long nEnd = Math.max(end, oEnd);
					int[] nOld = new int[(int) (nEnd - nStart)];
					int[] nNew = new int[(int) (nEnd - nStart)];
					System.arraycopy(o.oldValues, 0, nOld,
							(int) (o.start - nStart), o.oldValues.length);
					System.arraycopy(oldValues, 0, nOld,
							(int) (start - nStart), oldValues.length);
					System.arraycopy(newValues, 0, nNew,
							(int) (start - nStart), newValues.length);
					System.arraycopy(o.newValues, 0, nNew,
							(int) (o.start - nStart), o.newValues.length);
					return new Change(source, contents, nStart, nOld, nNew);
				}
			}
			return super.append(other);
		}

		@Override
		public void doIt(Project proj) {
			if (!completed) {
				completed = true;
				try {
					source.setEnabled(false);
					contents.set(start, newValues);
				} finally {
					source.setEnabled(true);
				}
			}
		}

		@Override
		public String getName() {
			return Strings.get("romChangeAction");
		}

		@Override
		public boolean shouldAppendTo(Action other) {
			if (other instanceof Change) {
				Change o = (Change) other;
				long oEnd = o.start + o.newValues.length;
				long end = start + newValues.length;
				if (oEnd >= start && end >= o.start)
					return true;
			}
			return super.shouldAppendTo(other);
		}

		@Override
		public void undo(Project proj) {
			if (completed) {
				completed = false;
				try {
					source.setEnabled(false);
					contents.set(start, oldValues);
				} finally {
					source.setEnabled(true);
				}
			}
		}
	}

	Project proj;
	boolean enabled = true;

	RomContentsListener(Project proj) {
		this.proj = proj;
	}

	public void bytesChanged(HexModel source, long start, long numBytes,
			int[] oldValues) {
		if (enabled && proj != null && oldValues != null) {
			// this change needs to be logged in the undo log
			int[] newValues = new int[oldValues.length];
			for (int i = 0; i < newValues.length; i++) {
				newValues[i] = source.get(start + i);
			}
			proj.doAction(new Change(this, (MemContents) source, start,
					oldValues, newValues));
		}
	}

	public void metainfoChanged(HexModel source) {
		// ignore - this can only come from an already-registered
		// action
	}

	void setEnabled(boolean value) {
		enabled = value;
	}
}
