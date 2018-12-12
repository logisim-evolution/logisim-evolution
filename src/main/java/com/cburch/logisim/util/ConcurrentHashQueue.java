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

/**
 * Code taken from Cornell's version of Logisim:
 * http://www.cs.cornell.edu/courses/cs3410/2015sp/
 */

package com.cburch.logisim.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrentHashQueue<E> {

	private static final int DONE_MARKER = Integer.MIN_VALUE / 2;

	private ConcurrentHashMap<E, Boolean> members;
	private ConcurrentLinkedQueue<E> queue;
	private AtomicInteger removeCount;

	public ConcurrentHashQueue() {
		members = new ConcurrentHashMap<E, Boolean>();
		queue = new ConcurrentLinkedQueue<E>();
		removeCount = new AtomicInteger(0);
	}

	public void add(E value) {
		if (value == null) {
			throw new IllegalArgumentException(
					"Cannot add null into ConcurrentHashQueue");
		}
		if (members.putIfAbsent(value, Boolean.TRUE) == null) {
			queue.add(value);
		}
	}

	public E remove() {
		int val = removeCount.getAndIncrement();
		if (val < 0) {
			removeCount.set(DONE_MARKER);
			return null;
		} else {
			E ret = queue.remove();
			if (ret == null) {
				return null;
			} else {
				removeCount.getAndDecrement();
				members.remove(ret);
				return ret;
			}
		}
	}

	public void setDone() {
		int num = removeCount.getAndSet(DONE_MARKER);
		if (num >= 0) {
			for (int i = 0; i < num; i++) {
				queue.add(null);
			}
		}
	}

}
