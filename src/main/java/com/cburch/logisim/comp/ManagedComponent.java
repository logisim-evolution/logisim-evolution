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

package com.cburch.logisim.comp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.EventSourceWeakSupport;

public abstract class ManagedComponent extends AbstractComponent {
	private EventSourceWeakSupport<ComponentListener> listeners = new EventSourceWeakSupport<ComponentListener>();
	private Location loc;
	private AttributeSet attrs;
	private ArrayList<EndData> ends;
	private List<EndData> endsView;
	private Bounds bounds = null;

	public ManagedComponent(Location loc, AttributeSet attrs, int num_ends) {
		this.loc = loc;
		this.attrs = attrs;
		this.ends = new ArrayList<EndData>(num_ends);
		this.endsView = Collections.unmodifiableList(ends);
	}

	public void addComponentListener(ComponentListener l) {
		listeners.add(l);
	}

	//
	// methods for altering data
	//
	public void clearManager() {
		for (EndData end : ends) {
			fireEndChanged(new ComponentEvent(this, end, null));
		}
		ends.clear();
		bounds = null;
	}

	//
	// user interface methods
	//
	public void expose(ComponentDrawContext context) {
		Bounds bounds = getBounds();
		java.awt.Component dest = context.getDestination();
		if (bounds != null) {
			dest.repaint(bounds.getX() - 5, bounds.getY() - 5,
					bounds.getWidth() + 10, bounds.getHeight() + 10);
		}
	}

	protected void fireComponentInvalidated(ComponentEvent e) {
		for (ComponentListener l : listeners) {
			l.componentInvalidated(e);
		}
	}

	protected void fireEndChanged(ComponentEvent e) {
		ComponentEvent copy = null;
		for (ComponentListener l : listeners) {
			if (copy == null) {
				copy = new ComponentEvent(e.getSource(),
						Collections.singletonList(e.getOldData()),
						Collections.singletonList(e.getData()));
			}
			l.endChanged(copy);
		}
	}

	protected void fireEndsChanged(List<EndData> oldEnds, List<EndData> newEnds) {
		ComponentEvent e = null;
		for (ComponentListener l : listeners) {
			if (e == null)
				e = new ComponentEvent(this, oldEnds, newEnds);
			l.endChanged(e);
		}
	}

	public AttributeSet getAttributeSet() {
		return attrs;
	}

	@Override
	public Bounds getBounds() {
		if (bounds == null) {
			Location loc = getLocation();
			Bounds offBounds = getFactory().getOffsetBounds(getAttributeSet());
			bounds = offBounds.translate(loc.getX(), loc.getY());
		}
		return bounds;
	}

	public int getEndCount() {
		return ends.size();
	}

	public Location getEndLocation(int i) {
		return getEnd(i).getLocation();
	}

	@Override
	public List<EndData> getEnds() {
		return endsView;
	}

	//
	// abstract AbstractComponent methods
	//
	@Override
	public abstract ComponentFactory getFactory();

	public Object getFeature(Object key) {
		return null;
	}

	@Override
	public Location getLocation() {
		return loc;
	}

	@Override
	public abstract void propagate(CircuitState state);

	protected void recomputeBounds() {
		bounds = null;
	}

	public void removeComponentListener(ComponentListener l) {
		listeners.remove(l);
	}

	public void removeEnd(int index) {
		ends.remove(index);
	}

	public void setAttributeSet(AttributeSet value) {
		attrs = value;
	}

	public void setBounds(Bounds bounds) {
		this.bounds = bounds;
	}

	public void setEnd(int i, EndData data) {
		if (i == ends.size()) {
			ends.add(data);
			fireEndChanged(new ComponentEvent(this, null, data));
		} else {
			EndData old = ends.get(i);
			if (old == null || !old.equals(data)) {
				ends.set(i, data);
				fireEndChanged(new ComponentEvent(this, old, data));
			}
		}
	}

	public void setEnd(int i, Location end, BitWidth width, int type) {
		setEnd(i, new EndData(end, width, type));
	}

	public void setEnd(int i, Location end, BitWidth width, int type,
			boolean exclusive) {
		setEnd(i, new EndData(end, width, type, exclusive));
	}

	public void setEnds(EndData[] newEnds) {
		List<EndData> oldEnds = ends;
		int minLen = Math.min(oldEnds.size(), newEnds.length);
		ArrayList<EndData> changesOld = new ArrayList<EndData>();
		ArrayList<EndData> changesNew = new ArrayList<EndData>();
		for (int i = 0; i < minLen; i++) {
			EndData old = oldEnds.get(i);
			if (newEnds[i] != null && !newEnds[i].equals(old)) {
				changesOld.add(old);
				changesNew.add(newEnds[i]);
				oldEnds.set(i, newEnds[i]);
			}
		}
		for (int i = oldEnds.size() - 1; i >= minLen; i--) {
			changesOld.add(oldEnds.remove(i));
			changesNew.add(null);
		}
		for (int i = minLen; i < newEnds.length; i++) {
			oldEnds.add(newEnds[i]);
			changesOld.add(null);
			changesNew.add(newEnds[i]);
		}
		fireEndsChanged(changesOld, changesNew);
	}
}
