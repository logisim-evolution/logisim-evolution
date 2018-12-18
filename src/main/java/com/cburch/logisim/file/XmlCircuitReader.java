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

package com.cburch.logisim.file;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import com.cburch.draw.model.AbstractCanvasObject;
import com.cburch.logisim.Main;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitAttributes;
import com.cburch.logisim.circuit.CircuitMutator;
import com.cburch.logisim.circuit.CircuitTransaction;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;

public class XmlCircuitReader extends CircuitTransaction {

	/**
	 * Get a circuit's component from a read XML file. If the component has a
	 * non-null "trackercomp" field, it means that it is tracked, therefore it
	 * is skipped in the non-tracked version to avoid errors.
	 * 
	 * @param elt
	 *            XML element to parse
	 * @param reader
	 *            XML file reader
	 * @return the component built from its XML description
	 * @throws XmlReaderException
	 */
	static Component getComponent(Element elt, XmlReader.ReadContext reader)
			throws XmlReaderException {
		if (elt.getAttribute("trackercomp") != "" && !Main.VERSION.hasTracker()) {
			return (null);
		}

		// Determine the factory that creates this element
		String name = elt.getAttribute("name");
		if (name == null || name.equals("")) {
			throw new XmlReaderException(Strings.get("compNameMissingError"));
		}

		String libName = elt.getAttribute("lib");
		Library lib = reader.findLibrary(libName);
		if (lib == null) {
			throw new XmlReaderException(Strings.get("compUnknownError",
					"no-lib"));
		}

		Tool tool = lib.getTool(name);
		if (tool == null || !(tool instanceof AddTool)) {
			if (libName == null || libName.equals("")) {
				throw new XmlReaderException(Strings.get("compUnknownError",
						name));
			} else {
				throw new XmlReaderException(Strings.get("compAbsentError",
						name, libName));
			}
		}
		ComponentFactory source = ((AddTool) tool).getFactory();

		// Determine attributes
		String loc_str = elt.getAttribute("loc");
		AttributeSet attrs = source.createAttributeSet();
		reader.initAttributeSet(elt, attrs, source);

		// Create component if location known
		if (loc_str == null || loc_str.equals("")) {
			throw new XmlReaderException(Strings.get("compLocMissingError",
					source.getName()));
		} else {
			try {
				Location loc = Location.parse(loc_str);
				return source.createComponent(loc, attrs);
			} catch (NumberFormatException e) {
				throw new XmlReaderException(Strings.get("compLocInvalidError",
						source.getName(), loc_str));
			}
		}
	}

	private XmlReader.ReadContext reader;

	private List<XmlReader.CircuitData> circuitsData;

	public XmlCircuitReader(XmlReader.ReadContext reader,
			List<XmlReader.CircuitData> circDatas) {
		this.reader = reader;
		this.circuitsData = circDatas;
	}

	void addWire(Circuit dest, CircuitMutator mutator, Element elt)
			throws XmlReaderException {
		Location pt0;
		try {
			String str = elt.getAttribute("from");
			if (str == null || str.equals("")) {
				throw new XmlReaderException(
						Strings.get("wireStartMissingError"));
			}
			pt0 = Location.parse(str);
		} catch (NumberFormatException e) {
			throw new XmlReaderException(Strings.get("wireStartInvalidError"));
		}

		Location pt1;
		try {
			String str = elt.getAttribute("to");
			if (str == null || str.equals("")) {
				throw new XmlReaderException(Strings.get("wireEndMissingError"));
			}
			pt1 = Location.parse(str);
		} catch (NumberFormatException e) {
			throw new XmlReaderException(Strings.get("wireEndInvalidError"));
		}

		mutator.add(dest, Wire.create(pt0, pt1));
	}

	private void buildCircuit(XmlReader.CircuitData circData,
			CircuitMutator mutator) {
		Element elt = circData.circuitElement;
		Circuit dest = circData.circuit;
		Map<Element, Component> knownComponents = circData.knownComponents;
		if (knownComponents == null)
			knownComponents = Collections.emptyMap();
		try {
			/* Here we check the attribute circuitnamedbox for backwards compatibility */
			boolean HasNamedBox = false;
			for (Element attrElt : XmlIterator.forChildElements(circData.circuitElement, "a")) {
				if (attrElt.hasAttribute("name")) {
					String Name = attrElt.getAttribute("name");
					if (Name.equals("circuitnamedbox")) {
						HasNamedBox = true;
					}
				}
			}
			reader.initAttributeSet(circData.circuitElement,
					dest.getStaticAttributes(), null);
			if ((!HasNamedBox)&&circData.circuitElement.hasChildNodes()) {
				dest.getStaticAttributes().setValue(CircuitAttributes.NAMED_CIRCUIT_BOX, false);
			}
		} catch (XmlReaderException e) {
			reader.addErrors(e, circData.circuit.getName() + ".static");
		}
		try {
			/* Here we check the attribute circuitnamedboxFixedSize for backwards compatibility */
			boolean HasNamedBoxFixedSize = false;
			for (Element attrElt : XmlIterator.forChildElements(circData.circuitElement, "a")) {
				if (attrElt.hasAttribute("name")) {
					String Name = attrElt.getAttribute("name");
					if (Name.equals("circuitnamedboxfixedsize")) {
						HasNamedBoxFixedSize = true;
					}
				}
			}
			reader.initAttributeSet(circData.circuitElement,
					dest.getStaticAttributes(), null);
			if ((!HasNamedBoxFixedSize)&&circData.circuitElement.hasChildNodes()) {
				dest.getStaticAttributes().setValue(CircuitAttributes.NAMED_CIRCUIT_BOX_FIXED_SIZE, false);
			}
		} catch (XmlReaderException e) {
			reader.addErrors(e, circData.circuit.getName() + ".static");
		}

		for (Element sub_elt : XmlIterator.forChildElements(elt)) {
			String sub_elt_name = sub_elt.getTagName();
			if (sub_elt_name.equals("comp")) {
				try {
					Component comp = knownComponents.get(sub_elt);
					if (comp == null) {
						comp = getComponent(sub_elt, reader);
					}
					if (comp != null) {
						mutator.add(dest, comp);
					}
				} catch (XmlReaderException e) {
					reader.addErrors(e, circData.circuit.getName() + "."
							+ toComponentString(sub_elt));
				}
			} else if (sub_elt_name.equals("wire")) {
				try {
					addWire(dest, mutator, sub_elt);
				} catch (XmlReaderException e) {
					reader.addErrors(e, circData.circuit.getName() + "."
							+ toWireString(sub_elt));
				}
			}
		}

		List<AbstractCanvasObject> appearance = circData.appearance;
		if (appearance != null && !appearance.isEmpty()) {
			dest.getAppearance().setObjectsForce(appearance);
			dest.getAppearance().setDefaultAppearance(false);
		}
	}

	@Override
	protected Map<Circuit, Integer> getAccessedCircuits() {
		HashMap<Circuit, Integer> access = new HashMap<Circuit, Integer>();
		for (XmlReader.CircuitData data : circuitsData) {
			access.put(data.circuit, READ_WRITE);
		}
		return access;
	}

	@Override
	protected void run(CircuitMutator mutator) {
		for (XmlReader.CircuitData circuitData : circuitsData) {
			buildCircuit(circuitData, mutator);
		}
	}

	private String toComponentString(Element elt) {
		String name = elt.getAttribute("name");
		String loc = elt.getAttribute("loc");
		return name + "(" + loc + ")";
	}

	private String toWireString(Element elt) {
		String from = elt.getAttribute("from");
		String to = elt.getAttribute("to");
		return "w" + from + "-" + to;
	}
}
