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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.cburch.logisim.circuit.ReplacementMap;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentEvent;
import com.cburch.logisim.comp.ComponentListener;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.wiring.Pin;

public class CircuitPins {
	private class MyComponentListener implements ComponentListener,
			AttributeListener {
		public void attributeListChanged(AttributeEvent e) {
		}

		public void attributeValueChanged(AttributeEvent e) {
			Attribute<?> attr = e.getAttribute();
			if (attr == StdAttr.FACING || attr == StdAttr.LABEL
					|| attr == Pin.ATTR_TYPE) {
				appearanceManager.updatePorts();
			}
		}

		public void componentInvalidated(ComponentEvent e) {
		}

		public void LabelChanged(ComponentEvent e) {
		}


		public void endChanged(ComponentEvent e) {
			appearanceManager.updatePorts();
		}
	}

	private PortManager appearanceManager;
	private MyComponentListener myComponentListener;
	private Set<Instance> pins;

	CircuitPins(PortManager appearanceManager) {
		this.appearanceManager = appearanceManager;
		myComponentListener = new MyComponentListener();
		pins = new HashSet<Instance>();
	}

	public Collection<Instance> getPins() {
		return new ArrayList<Instance>(pins);
	}

	public void transactionCompleted(ReplacementMap repl) {
		// determine the changes
		Set<Instance> adds = new HashSet<Instance>();
		Set<Instance> removes = new HashSet<Instance>();
		Map<Instance, Instance> replaces = new HashMap<Instance, Instance>();
		for (Component comp : repl.getAdditions()) {
			if (comp.getFactory() instanceof Pin) {
				Instance in = Instance.getInstanceFor(comp);
				boolean added = pins.add(in);
				if (added) {
					comp.addComponentListener(myComponentListener);
					in.getAttributeSet().addAttributeListener(
							myComponentListener);
					adds.add(in);
				}
			}
		}
		for (Component comp : repl.getRemovals()) {
			if (comp.getFactory() instanceof Pin) {
				Instance in = Instance.getInstanceFor(comp);
				boolean removed = pins.remove(in);
				if (removed) {
					comp.removeComponentListener(myComponentListener);
					in.getAttributeSet().removeAttributeListener(
							myComponentListener);
					Collection<Component> rs = repl
							.getComponentsReplacing(comp);
					if (rs.isEmpty()) {
						removes.add(in);
					} else {
						Component r = rs.iterator().next();
						Instance rin = Instance.getInstanceFor(r);
						adds.remove(rin);
						replaces.put(in, rin);
					}
				}
			}
		}

		appearanceManager.updatePorts(adds, removes, replaces, getPins());
	}
}
