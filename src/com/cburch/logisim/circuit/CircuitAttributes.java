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

package com.cburch.logisim.circuit;

import java.awt.Font;
import java.util.Arrays;
import java.util.List;

import javax.swing.JOptionPane;

import com.bfh.logisim.designrulecheck.CorrectLabel;
import com.cburch.logisim.circuit.appear.CircuitAppearanceEvent;
import com.cburch.logisim.circuit.appear.CircuitAppearanceListener;
import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.SyntaxChecker;

public class CircuitAttributes extends AbstractAttributeSet {
	private class MyListener implements AttributeListener,
			CircuitAppearanceListener {
		public void attributeListChanged(AttributeEvent e) {
		}

		public void attributeValueChanged(AttributeEvent e) {
			@SuppressWarnings("unchecked")
			Attribute<Object> a = (Attribute<Object>) e.getAttribute();
			fireAttributeValueChanged(a, e.getValue(),e.getOldValue());
		}

		public void circuitAppearanceChanged(CircuitAppearanceEvent e) {
			SubcircuitFactory factory;
			factory = (SubcircuitFactory) subcircInstance.getFactory();
			if (e.isConcerning(CircuitAppearanceEvent.PORTS)) {
				factory.computePorts(subcircInstance);
			}
			if (e.isConcerning(CircuitAppearanceEvent.BOUNDS)) {
				subcircInstance.recomputeBounds();
			}
			subcircInstance.fireInvalidated();
		}
	}

	private static class StaticListener implements AttributeListener {
		private Circuit source;

		private StaticListener(Circuit s) {
			source = s;
		}

		public void attributeListChanged(AttributeEvent e) {
		}

		public void attributeValueChanged(AttributeEvent e) {
			if (e.getAttribute() == NAME_ATTR) {
				String NewName = (String) e.getValue();
				String OldName = e.getOldValue() == null ? "ThisShouldNotHappen" : (String) e.getOldValue();
				if (!NewName.equals(OldName)) {
					if (NewName.isEmpty()) {
						JOptionPane.showMessageDialog(null, Strings.get("EmptyNameError"));
						e.getSource().setValue(NAME_ATTR, OldName);
						source.fireEvent(CircuitEvent.ACTION_SET_NAME, OldName);
						return;
					} else 
				    if (!SyntaxChecker.isVariableNameAcceptable(NewName,true)) {
						e.getSource().setValue(NAME_ATTR, OldName);
						source.fireEvent(CircuitEvent.ACTION_SET_NAME, OldName);
						return;
					} else
					if (CorrectLabel.IsKeyword(NewName,false)) {
						JOptionPane.showMessageDialog(null, Strings.get("KeywordNameError"));
						e.getSource().setValue(NAME_ATTR, OldName);
						source.fireEvent(CircuitEvent.ACTION_SET_NAME, OldName);
						return;
					} else {
						source.fireEvent(CircuitEvent.ACTION_CHECK_NAME, OldName);
						source.fireEvent(CircuitEvent.ACTION_SET_NAME, NewName);
					}
				}
			}
		}
	}

	static AttributeSet createBaseAttrs(Circuit source, String name) {
		AttributeSet ret = AttributeSets
				.fixedSet(STATIC_ATTRS, STATIC_DEFAULTS);
		ret.setValue(CircuitAttributes.NAME_ATTR, name);
		ret.addAttributeListener(new StaticListener(source));
		return ret;
	}

	public static final Attribute<String> NAME_ATTR = Attributes.forString(
			"circuit", Strings.getter("circuitName"));

	public static final Attribute<Direction> LABEL_LOCATION_ATTR = Attributes
			.forDirection("labelloc", Strings.getter("circuitLabelLocAttr"));

	public static final Attribute<String> CIRCUIT_LABEL_ATTR = Attributes
			.forString("clabel", Strings.getter("circuitLabelAttr"));

	public static final Attribute<Direction> CIRCUIT_LABEL_FACING_ATTR = Attributes
			.forDirection("clabelup", Strings.getter("circuitLabelDirAttr"));

	public static final Attribute<Font> CIRCUIT_LABEL_FONT_ATTR = Attributes
			.forFont("clabelfont", Strings.getter("circuitLabelFontAttr"));
	public static final Attribute<Boolean> CIRCUIT_IS_VHDL_BOX = Attributes
			.forBoolean("circuitvhdl", Strings.getter("circuitIsVhdl"));
	public static final Attribute<String> CIRCUIT_VHDL_PATH = Attributes
			.forString("circuitvhdlpath", Strings.getter("circuitVhdlPath"));
	public static final Attribute<Boolean> NAMED_CIRCUIT_BOX = Attributes
			.forBoolean("circuitnamedbox",Strings.getter("circuitNamedBox"));

	private static final Attribute<?>[] STATIC_ATTRS = { NAME_ATTR,
			CIRCUIT_LABEL_ATTR, CIRCUIT_LABEL_FACING_ATTR,
			CIRCUIT_LABEL_FONT_ATTR,NAMED_CIRCUIT_BOX, 
			CIRCUIT_VHDL_PATH, };

	private static final Object[] STATIC_DEFAULTS = { "", "", Direction.EAST,
			StdAttr.DEFAULT_LABEL_FONT, 
			false, "", };

	private static final List<Attribute<?>> INSTANCE_ATTRS = Arrays
			.asList(new Attribute<?>[] { StdAttr.FACING, StdAttr.LABEL,
					LABEL_LOCATION_ATTR, StdAttr.LABEL_FONT,StdAttr.LABEL_VISIBILITY,
					CircuitAttributes.NAME_ATTR, CIRCUIT_LABEL_ATTR,
					CIRCUIT_LABEL_FACING_ATTR, CIRCUIT_LABEL_FONT_ATTR,
					NAMED_CIRCUIT_BOX, 
					CIRCUIT_VHDL_PATH, });

	private Circuit source;
	private Instance subcircInstance;
	private Direction facing;
	private String label;
	private Direction labelLocation;
	private Font labelFont;
	private Boolean LabelVisable;
	private MyListener listener;
	private Instance[] pinInstances;

	public CircuitAttributes(Circuit source) {
		this.source = source;
		subcircInstance = null;
		facing = source.getAppearance().getFacing();
		label = "";
		labelLocation = Direction.NORTH;
		labelFont = StdAttr.DEFAULT_LABEL_FONT;
		LabelVisable = true;
		pinInstances = new Instance[0];
	}

	@Override
	protected void copyInto(AbstractAttributeSet dest) {
		CircuitAttributes other = (CircuitAttributes) dest;
		other.subcircInstance = null;
		other.listener = null;
	}

	@Override
	public List<Attribute<?>> getAttributes() {
		return INSTANCE_ATTRS;
	}

	public Direction getFacing() {
		return facing;
	}

	public Instance[] getPinInstances() {
		return pinInstances;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <E> E getValue(Attribute<E> attr) {
		if (attr == StdAttr.FACING)
			return (E) facing;
		if (attr == StdAttr.LABEL)
			return (E) label;
		if (attr == StdAttr.LABEL_FONT)
			return (E) labelFont;
		if (attr == StdAttr.LABEL_VISIBILITY)
			return (E) LabelVisable;
		if (attr == LABEL_LOCATION_ATTR)
			return (E) labelLocation;
		else
			return source.getStaticAttributes().getValue(attr);
	}

	@Override
	public boolean isToSave(Attribute<?> attr) {
		Attribute<?>[] statics = STATIC_ATTRS;
		for (int i = 0; i < statics.length; i++) {
			if (statics[i] == attr)
				return false;
		}
		return true;
	}

	void setPinInstances(Instance[] value) {
		pinInstances = value;
	}

	void setSubcircuit(Instance value) {
		subcircInstance = value;
		if (subcircInstance != null && listener == null) {
			listener = new MyListener();
			source.getStaticAttributes().addAttributeListener(listener);
			source.getAppearance().addCircuitAppearanceListener(listener);
		}
	}

	@Override
	public <E> void setValue(Attribute<E> attr, E value) {
		if (attr == StdAttr.FACING) {
			Direction val = (Direction) value;
			if (facing.equals(val))
				return;
			facing = val;
			fireAttributeValueChanged(StdAttr.FACING, val,null);
			if (subcircInstance != null)
				subcircInstance.recomputeBounds();
		} else if (attr == StdAttr.LABEL) {
			String val = (String) value;
			String oldval = label;
			if (label.equals(val))
				return;
			label = val;
			fireAttributeValueChanged(StdAttr.LABEL, val, oldval);
		} else if (attr == StdAttr.LABEL_FONT) {
			Font val = (Font) value;
			if (labelFont.equals(val))
				return;
			labelFont = val;
			fireAttributeValueChanged(StdAttr.LABEL_FONT, val,null);
		} else if (attr == StdAttr.LABEL_VISIBILITY) {
			Boolean val = (Boolean) value;
			if (LabelVisable == value)
				return;
			LabelVisable = val;
			fireAttributeValueChanged(StdAttr.LABEL_VISIBILITY, val,null);
		} else if (attr == LABEL_LOCATION_ATTR) {
			Direction val = (Direction) value;
			if (labelLocation.equals(val))
				return;
			labelLocation = val;
			fireAttributeValueChanged(LABEL_LOCATION_ATTR, val,null);
		} else {
			source.getStaticAttributes().setValue(attr, value);
			if (attr == NAME_ATTR) {
				source.fireEvent(CircuitEvent.ACTION_SET_NAME, value);
			}
		}
	}
}
