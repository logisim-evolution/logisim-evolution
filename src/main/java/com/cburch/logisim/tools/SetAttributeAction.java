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

package com.cburch.logisim.tools;

import java.util.ArrayList;
import java.util.List;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.circuit.CircuitTransaction;
import com.cburch.logisim.circuit.CircuitTransactionResult;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.StringGetter;

public class SetAttributeAction extends Action {
	private StringGetter nameGetter;
	private Circuit circuit;
	private List<Component> comps;
	private List<Attribute<Object>> attrs;
	private List<Object> values;
	private List<Object> oldValues;
	private CircuitTransaction xnReverse;

	public SetAttributeAction(Circuit circuit, StringGetter nameGetter) {
		this.nameGetter = nameGetter;
		this.circuit = circuit;
		this.comps = new ArrayList<Component>();
		this.attrs = new ArrayList<Attribute<Object>>();
		this.values = new ArrayList<Object>();
		this.oldValues = new ArrayList<Object>();
	}

	@Override
	public void doIt(Project proj) {
		CircuitMutation xn = new CircuitMutation(circuit);
		int len = values.size();
		oldValues.clear();
		for (int i = 0; i < len; i++) {
			Component comp = comps.get(i);
			Attribute<Object> attr = attrs.get(i);
			Object value = values.get(i);
			if (circuit.contains(comp)) {
				oldValues.add(null);
				xn.set(comp, attr, value);
			} else {
				AttributeSet compAttrs = comp.getAttributeSet();
				oldValues.add(compAttrs.getValue(attr));
				compAttrs.setValue(attr, value);
			}
		}

		if (!xn.isEmpty()) {
			CircuitTransactionResult result = xn.execute();
			xnReverse = result.getReverseTransaction();
		}
	}

	@Override
	public String getName() {
		return nameGetter.toString();
	}

	public boolean isEmpty() {
		return comps.isEmpty();
	}

	public void set(Component comp, Attribute<?> attr, Object value) {
		@SuppressWarnings("unchecked")
		Attribute<Object> a = (Attribute<Object>) attr;
		comps.add(comp);
		attrs.add(a);
		values.add(value);
	}

	@Override
	public void undo(Project proj) {
		if (xnReverse != null)
			xnReverse.execute();
		for (int i = oldValues.size() - 1; i >= 0; i--) {
			Component comp = comps.get(i);
			Attribute<Object> attr = attrs.get(i);
			Object value = oldValues.get(i);
			if (value != null) {
				comp.getAttributeSet().setValue(attr, value);
			}
		}
	}
}
