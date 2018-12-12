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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

public class EventSourceWeakSupport<L> implements Iterable<L> {
	private ConcurrentLinkedQueue<WeakReference<L>> listeners = new ConcurrentLinkedQueue<WeakReference<L>>();

	public EventSourceWeakSupport() {
	}

	public void add(L listener) {
		listeners.add(new WeakReference<L>(listener));
	}

	public boolean isEmpty() {
		for (Iterator<WeakReference<L>> it = listeners.iterator(); it.hasNext();) {
			L l = it.next().get();
			if (l == null) {
				it.remove();
			} else {
				return false;
			}
		}
		return true;
	}

	public Iterator<L> iterator() {
		// copy elements into another list in case any event handlers
		// want to add a listener
		ArrayList<L> ret = new ArrayList<L>(listeners.size());
		for (Iterator<WeakReference<L>> it = listeners.iterator(); it.hasNext();) {
			L l = it.next().get();
			if (l == null) {
				it.remove();
			} else {
				ret.add(l);
			}
		}
		return ret.iterator();
	}

	public void remove(L listener) {
		for (Iterator<WeakReference<L>> it = listeners.iterator(); it.hasNext();) {
			L l = it.next().get();
			if (l == null || l == listener)
				it.remove();
		}
	}
}
