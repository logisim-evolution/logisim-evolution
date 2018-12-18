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

package com.cburch.logisim.circuit.appear;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.cburch.draw.model.CanvasModelEvent;
import com.cburch.draw.model.CanvasModelListener;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.model.Drawing;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitAttributes;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.util.EventSourceWeakSupport;

public class CircuitAppearance extends Drawing {
	public static final int PINLENGTH = 10;
	
	private class MyListener implements CanvasModelListener {
		public void modelChanged(CanvasModelEvent event) {
			if (!suppressRecompute) {
				setDefaultAppearance(false);
				fireCircuitAppearanceChanged(CircuitAppearanceEvent.ALL_TYPES);
			}
		}
	}

	private Circuit circuit;
	private EventSourceWeakSupport<CircuitAppearanceListener> listeners;
	private PortManager portManager;
	private CircuitPins circuitPins;
	private MyListener myListener;
	private boolean isDefault;
	private boolean suppressRecompute;

	public CircuitAppearance(Circuit circuit) {
		this.circuit = circuit;
		listeners = new EventSourceWeakSupport<CircuitAppearanceListener>();
		portManager = new PortManager(this);
		circuitPins = new CircuitPins(portManager);
		myListener = new MyListener();
		suppressRecompute = false;
		addCanvasModelListener(myListener);
		setDefaultAppearance(true);
	}
	
	public String getName() {
		return circuit.getName();
	}
	
	public Graphics GetGraphics() {
		return circuit.GetGraphics();
	}
	
	public CircuitPins GetCircuitPin() {
		return circuitPins;
	}

	public void addCircuitAppearanceListener(CircuitAppearanceListener l) {
		listeners.add(l);
	}
	
	public void sortPinsList(List<Instance> pins, Direction facing) {
		DefaultAppearance.sortPinList(pins, facing);
	}

	@Override
	public void addObjects(int index, Collection<? extends CanvasObject> shapes) {
		super.addObjects(index, shapes);
		checkToFirePortsChanged(shapes);
	}

	@Override
	public void addObjects(Map<? extends CanvasObject, Integer> shapes) {
		super.addObjects(shapes);
		checkToFirePortsChanged(shapes.keySet());
	}

	private boolean affectsPorts(Collection<? extends CanvasObject> shapes) {
		for (CanvasObject o : shapes) {
			if (o instanceof AppearanceElement) {
				return true;
			}
		}
		return false;
	}

	private void checkToFirePortsChanged(
			Collection<? extends CanvasObject> shapes) {
		if (affectsPorts(shapes)) {
			recomputePorts();
		}
	}

	/**
	 * Code taken from Cornell's version of Logisim:
	 * http://www.cs.cornell.edu/courses/cs3410/2015sp/
	 */
	public boolean contains(Location loc) {
		Location query;
		AppearanceAnchor anchor = findAnchor();

		if (anchor == null) {
			query = loc;
		} else {
			Location anchorLoc = anchor.getLocation();
			query = loc.translate(anchorLoc.getX(), anchorLoc.getY());
		}

		for (CanvasObject o : getObjectsFromBottom()) {
			if (!(o instanceof AppearanceElement) && o.contains(query, true)) {
				return true;
			}
		}

		return false;
	}

	private AppearanceAnchor findAnchor() {
		for (CanvasObject shape : getObjectsFromBottom()) {
			if (shape instanceof AppearanceAnchor) {
				return (AppearanceAnchor) shape;
			}
		}
		return null;
	}

	private Location findAnchorLocation() {
		AppearanceAnchor anchor = findAnchor();
		if (anchor == null) {
			return Location.create(100, 100);
		} else {
			return anchor.getLocation();
		}
	}

	void fireCircuitAppearanceChanged(int affected) {
		CircuitAppearanceEvent event;
		event = new CircuitAppearanceEvent(circuit, affected);
		for (CircuitAppearanceListener listener : listeners) {
			listener.circuitAppearanceChanged(event);
		}
	}

	public Bounds getAbsoluteBounds() {
		return getBounds(false);
	}

	private Bounds getBounds(boolean relativeToAnchor) {
		Bounds ret = null;
		Location offset = null;
		for (CanvasObject o : getObjectsFromBottom()) {
			if (o instanceof AppearanceElement) {
				Location loc = ((AppearanceElement) o).getLocation();
				if (o instanceof AppearanceAnchor) {
					offset = loc;
				}
				if (ret == null) {
					ret = Bounds.create(loc);
				} else {
					ret = ret.add(loc);
				}
			} else {
				if (ret == null) {
					ret = o.getBounds();
				} else {
					ret = ret.add(o.getBounds());
				}
			}
		}
		if (ret == null) {
			return Bounds.EMPTY_BOUNDS;
		} else if (relativeToAnchor && offset != null) {
			return ret.translate(-offset.getX(), -offset.getY());
		} else {
			return ret;
		}
	}

	public CircuitPins getCircuitPins() {
		return circuitPins;
	}

	public Direction getFacing() {
		AppearanceAnchor anchor = findAnchor();
		if (anchor == null) {
			return Direction.EAST;
		} else {
			return anchor.getFacing();
		}
	}

	public Bounds getOffsetBounds() {
		return getBounds(true);
	}

	public SortedMap<Location, Instance> getPortOffsets(Direction facing) {
		Location anchor = null;
		Direction defaultFacing = Direction.EAST;
		List<AppearancePort> ports = new ArrayList<AppearancePort>();
		for (CanvasObject shape : getObjectsFromBottom()) {
			if (shape instanceof AppearancePort) {
				ports.add((AppearancePort) shape);
			} else if (shape instanceof AppearanceAnchor) {
				AppearanceAnchor o = (AppearanceAnchor) shape;
				anchor = o.getLocation();
				defaultFacing = o.getFacing();
			}
		}

		SortedMap<Location, Instance> ret = new TreeMap<Location, Instance>();
		for (AppearancePort port : ports) {
			Location loc = port.getLocation();
			if (anchor != null) {
				loc = loc.translate(-anchor.getX(), -anchor.getY());
			}
			if (facing != defaultFacing) {
				loc = loc.rotate(defaultFacing, facing, 0, 0);
			}
			ret.put(loc, port.getPin());
		}
		return ret;
	}

	public boolean isDefaultAppearance() {
		return isDefault;
	}

	public void paintSubcircuit(Graphics g, Direction facing) {
		Direction defaultFacing = getFacing();
		double rotate = 0.0;
		if (facing != defaultFacing && g instanceof Graphics2D) {
			rotate = defaultFacing.toRadians() - facing.toRadians();
			((Graphics2D) g).rotate(rotate);
		}
		Location offset = findAnchorLocation();
		g.translate(-offset.getX(), -offset.getY());
		for (CanvasObject shape : getObjectsFromBottom()) {
			if (!(shape instanceof AppearanceElement)) {
				Graphics dup = g.create();
				shape.paint(dup, null);
				dup.dispose();
			}
		}
		g.translate(offset.getX(), offset.getY());
		if (rotate != 0.0) {
			((Graphics2D) g).rotate(-rotate);
		}
	}
	
	public boolean IsNamedBoxShaped() {
		return circuit.getStaticAttributes().getValue(CircuitAttributes.NAMED_CIRCUIT_BOX);
	}

	public boolean IsNamedBoxShapedFixedSize() {
		return circuit.getStaticAttributes().getValue(CircuitAttributes.NAMED_CIRCUIT_BOX_FIXED_SIZE);
	}

	public void recomputeDefaultAppearance() {
		if (isDefault) {
			List<CanvasObject> shapes;
			shapes = DefaultAppearance.build(circuitPins.getPins(),this.IsNamedBoxShaped(),this.IsNamedBoxShapedFixedSize(),this.getName(),circuit.GetGraphics());
			setObjectsForce(shapes);
		}
	}

	void recomputePorts() {
		if (isDefault) {
			recomputeDefaultAppearance();
		} else {
			fireCircuitAppearanceChanged(CircuitAppearanceEvent.ALL_TYPES);
		}
	}

	public void removeCircuitAppearanceListener(CircuitAppearanceListener l) {
		listeners.remove(l);
	}

	@Override
	public void removeObjects(Collection<? extends CanvasObject> shapes) {
		super.removeObjects(shapes);
		checkToFirePortsChanged(shapes);
	}

	void replaceAutomatically(List<AppearancePort> removes,
			List<AppearancePort> adds) {
		// this should be called only when substituting ports via PortManager
		boolean oldSuppress = suppressRecompute;
		try {
			suppressRecompute = true;
			removeObjects(removes);
			addObjects(getObjectsFromBottom().size() - 1, adds);
			recomputeDefaultAppearance();
		} finally {
			suppressRecompute = oldSuppress;
		}
		fireCircuitAppearanceChanged(CircuitAppearanceEvent.ALL_TYPES);
	}

	public void setDefaultAppearance(boolean value) {
		if (isDefault != value) {
			isDefault = value;
			if (value) {
				recomputeDefaultAppearance();
			}
		}
	}

	public void setObjectsForce(List<? extends CanvasObject> shapesBase) {
		// This shouldn't ever be an issue, but just to make doubly sure, we'll
		// check that the anchor and all ports are in their proper places.
		List<CanvasObject> shapes = new ArrayList<CanvasObject>(shapesBase);
		int n = shapes.size();
		int ports = 0;
		for (int i = n - 1; i >= 0; i--) { // count ports, move anchor to end
			CanvasObject o = shapes.get(i);
			if (o instanceof AppearanceAnchor) {
				if (i != n - 1) {
					shapes.remove(i);
					shapes.add(o);
				}
			} else if (o instanceof AppearancePort) {
				ports++;
			}
		}
		for (int i = (n - ports - 1) - 1; i >= 0; i--) { // move ports to top
			CanvasObject o = shapes.get(i);
			if (o instanceof AppearancePort) {
				shapes.remove(i);
				shapes.add(n - ports - 1, o);
				i--;
			}
		}

		try {
			suppressRecompute = true;
			super.removeObjects(new ArrayList<CanvasObject>(
					getObjectsFromBottom()));
			super.addObjects(0, shapes);
		} finally {
			suppressRecompute = false;
		}
		fireCircuitAppearanceChanged(CircuitAppearanceEvent.ALL_TYPES);
	}

	@Override
	public void translateObjects(Collection<? extends CanvasObject> shapes,
			int dx, int dy) {
		super.translateObjects(shapes, dx, dy);
		checkToFirePortsChanged(shapes);
	}
}
