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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class DrawingOverlaps {
	private Map<CanvasObject, List<CanvasObject>> map;
	private Set<CanvasObject> untested;

	public DrawingOverlaps() {
		map = new HashMap<CanvasObject, List<CanvasObject>>();
		untested = new HashSet<CanvasObject>();
	}

	private void addOverlap(CanvasObject a, CanvasObject b) {
		List<CanvasObject> alist = map.get(a);
		if (alist == null) {
			alist = new ArrayList<CanvasObject>();
			map.put(a, alist);
		}
		if (!alist.contains(b)) {
			alist.add(b);
		}
	}

	public void addShape(CanvasObject shape) {
		untested.add(shape);
	}

	private void ensureUpdated() {
		for (CanvasObject o : untested) {
			List<CanvasObject> over = new ArrayList<CanvasObject>();
			for (CanvasObject o2 : map.keySet()) {
				if (o != o2 && o.overlaps(o2)) {
					over.add(o2);
					addOverlap(o2, o);
				}
			}
			map.put(o, over);
		}
		untested.clear();
	}

	public Collection<CanvasObject> getObjectsOverlapping(CanvasObject o) {
		ensureUpdated();

		List<CanvasObject> ret = map.get(o);
		if (ret == null || ret.isEmpty()) {
			return Collections.emptyList();
		} else {
			return Collections.unmodifiableList(ret);
		}
	}

	public void invalidateShape(CanvasObject shape) {
		removeShape(shape);
		untested.add(shape);
	}

	public void invalidateShapes(Collection<? extends CanvasObject> shapes) {
		for (CanvasObject o : shapes) {
			invalidateShape(o);
		}
	}

	public void removeShape(CanvasObject shape) {
		untested.remove(shape);
		List<CanvasObject> mapped = map.remove(shape);
		if (mapped != null) {
			for (CanvasObject o : mapped) {
				List<CanvasObject> reverse = map.get(o);
				if (reverse != null) {
					reverse.remove(shape);
				}
			}
		}
	}
}
