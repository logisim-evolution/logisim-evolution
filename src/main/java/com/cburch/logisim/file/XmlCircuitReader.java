/*
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.file;

import static com.cburch.logisim.file.Strings.S;

import com.cburch.draw.model.AbstractCanvasObject;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitAttributes;
import com.cburch.logisim.circuit.CircuitMutator;
import com.cburch.logisim.circuit.CircuitTransaction;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.circuit.appear.AppearanceSvgReader;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.std.base.Text;
import com.cburch.logisim.std.memory.Mem;
import com.cburch.logisim.std.memory.Ram;
import com.cburch.logisim.std.memory.RamAttributes;
import com.cburch.logisim.tools.AddTool;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.w3c.dom.Element;

public class XmlCircuitReader extends CircuitTransaction {

  /**
   * Get a circuit's component from a read XML file. If the component has a non-null "trackercomp"
   * field, it means that it is tracked, therefore it is skipped in the non-tracked version to avoid
   * errors.
   *
   * @param elt XML element to parse
   * @param reader XML file reader
   * @return the component built from its XML description
   * @throws XmlReaderException
   */
  static Component getComponent(
      Element elt, XmlReader.ReadContext reader, boolean IsHolyCross, boolean IsEvolution)
      throws XmlReaderException {

    // Determine the factory that creates this element
    final var name = elt.getAttribute("name");
    if (name == null || name.equals("")) {
      throw new XmlReaderException(S.get("compNameMissingError"));
    }

    final var libName = elt.getAttribute("lib");
    final var lib = reader.findLibrary(libName);
    if (lib == null) {
      throw new XmlReaderException(S.get("compUnknownError", "no-lib"));
    }

    final var tool = lib.getTool(name);
    if (!(tool instanceof AddTool)) {
      if (libName == null || libName.equals("")) {
        throw new XmlReaderException(S.get("compUnknownError", name));
      } else {
        throw new XmlReaderException(S.get("compAbsentError", name, libName));
      }
    }
    final var source = ((AddTool) tool).getFactory();

    // Determine attributes
    final var locStr = elt.getAttribute("loc");
    final var attrs = source.createAttributeSet();
    if (source instanceof Ram && IsHolyCross) {
      RamAttributes rattrs = (RamAttributes) attrs;
      rattrs.setValue(Mem.ENABLES_ATTR, Mem.USELINEENABLES);
      rattrs.updateAttributes();
      reader.initAttributeSet(elt, attrs, null, IsHolyCross, IsEvolution);
    } else reader.initAttributeSet(elt, attrs, source, IsHolyCross, IsEvolution);

    // Create component if location known
    if (locStr == null || locStr.equals("")) {
      throw new XmlReaderException(S.get("compLocMissingError", source.getName()));
    } else {
      try {
        final var loc = Location.parse(locStr);
        return source.createComponent(loc, attrs);
      } catch (NumberFormatException e) {
        throw new XmlReaderException(S.get("compLocInvalidError", source.getName(), locStr));
      }
    }
  }

  private final XmlReader.ReadContext reader;

  private final List<XmlReader.CircuitData> circuitsData;
  private boolean IsHolyCross = false;
  private boolean IsEvolution = false;

  public XmlCircuitReader(
      XmlReader.ReadContext reader,
      List<XmlReader.CircuitData> circDatas,
      boolean HolyCrossFile,
      boolean EvolutionFile) {
    this.reader = reader;
    this.circuitsData = circDatas;
    this.IsHolyCross = HolyCrossFile;
    this.IsEvolution = EvolutionFile;
  }

  void addWire(Circuit dest, CircuitMutator mutator, Element elt) throws XmlReaderException {
    Location pt0;
    try {
      final var str = elt.getAttribute("from");
      if (str == null || str.equals("")) {
        throw new XmlReaderException(S.get("wireStartMissingError"));
      }
      pt0 = Location.parse(str);
    } catch (NumberFormatException e) {
      throw new XmlReaderException(S.get("wireStartInvalidError"));
    }

    Location pt1;
    try {
      final var str = elt.getAttribute("to");
      if (str == null || str.equals("")) {
        throw new XmlReaderException(S.get("wireEndMissingError"));
      }
      pt1 = Location.parse(str);
    } catch (NumberFormatException e) {
      throw new XmlReaderException(S.get("wireEndInvalidError"));
    }

    if (!pt0.equals(pt1)) mutator.add(dest, Wire.create(pt0, pt1)); // Avoid zero length wires
  }

  private void buildCircuit(XmlReader.CircuitData circData, CircuitMutator mutator) {
    final var elt = circData.circuitElement;
    final var dest = circData.circuit;
    var knownComponents = circData.knownComponents;
    if (knownComponents == null) knownComponents = Collections.emptyMap();
    try {
      /* Here we check the attribute circuitnamedbox for backwards compatibility */
      var hasNamedBox = false;
      var hasNamedBoxFixedSize = false;
      var HasAppearAttr = false;
      for (final var attrElt : XmlIterator.forChildElements(circData.circuitElement, "a")) {
        if (attrElt.hasAttribute("name")) {
          final var name = attrElt.getAttribute("name");
          if (name.equals("circuitnamedbox")) {
            hasNamedBox = true;
          }
          if (name.equals("appearance")) HasAppearAttr = true;
          if (name.equals("circuitnamedboxfixedsize")) {
            hasNamedBoxFixedSize = true;
          }
        }
      }
      reader.initAttributeSet(
          circData.circuitElement, dest.getStaticAttributes(), null, IsHolyCross, IsEvolution);
      if (circData.circuitElement.hasChildNodes()) {
        if (hasNamedBox) {
          /* This situation is clear, it is an older logisim-evolution file */
          dest.getStaticAttributes()
              .setValue(CircuitAttributes.APPEARANCE_ATTR, CircuitAttributes.APPEAR_EVOLUTION);
        } else {
          if (!HasAppearAttr) {
            /* Here we have 2 possibilities, either a Holycross file or a logisim-evolution file
             * before the introduction of the named circuit boxes. So let's ask the user.
             */
            if (IsHolyCross)
              dest.getStaticAttributes()
                  .setValue(CircuitAttributes.APPEARANCE_ATTR, CircuitAttributes.APPEAR_FPGA);
            else
              dest.getStaticAttributes()
                  .setValue(CircuitAttributes.APPEARANCE_ATTR, CircuitAttributes.APPEAR_CLASSIC);
          }
        }
        if (!hasNamedBoxFixedSize) {
          dest.getStaticAttributes()
              .setValue(CircuitAttributes.NAMED_CIRCUIT_BOX_FIXED_SIZE, false);
        }
      }
    } catch (XmlReaderException e) {
      reader.addErrors(e, circData.circuit.getName() + ".static");
    }

    final var componentsAt = new HashMap<Bounds, Component>();
    final var overlapComponents = new ArrayList<Component>();
    for (Element sub_elt : XmlIterator.forChildElements(elt)) {
      final var sub_elt_name = sub_elt.getTagName();
      if (sub_elt_name.equals("comp")) {
        try {
          var comp = knownComponents.get(sub_elt);
          if (comp == null) {
            comp = getComponent(sub_elt, reader, IsHolyCross, IsEvolution);
          }
          if (comp != null) {
            /* filter out empty text boxes */
            if (comp.getFactory() instanceof Text) {
              if (comp.getAttributeSet().getValue(Text.ATTR_TEXT).isEmpty()) {
                continue;
              }
            }
            final var bds = comp.getBounds();
            final var conflict = componentsAt.get(bds);
            if (conflict != null) {
              reader.addError(
                  S.get(
                      "fileComponentOverlapError",
                      conflict.getFactory().getName() + conflict.getLocation(),
                      comp.getFactory().getName() + conflict.getLocation()),
                  circData.circuit.getName());
              overlapComponents.add(comp);
            } else {
              mutator.add(dest, comp);
              componentsAt.put(bds, comp);
            }
          }
        } catch (XmlReaderException e) {
          reader.addErrors(e, circData.circuit.getName() + "." + toComponentString(sub_elt));
        }
      } else if (sub_elt_name.equals("wire")) {
        try {
          addWire(dest, mutator, sub_elt);
        } catch (XmlReaderException e) {
          reader.addErrors(e, circData.circuit.getName() + "." + toWireString(sub_elt));
        }
      }
    }
    for (var comp : overlapComponents) {
      final var bds = comp.getBounds();
      if (bds.getHeight() == 0 || bds.getWidth() == 0) continue; // ignore empty boxes
      int d = 0;
      do {
        d += 10;
      } while ((componentsAt.get(bds.translate(d, d))) != null && (d < 100000));
      final var loc = comp.getLocation().translate(d, d);
      final var attrs = (AttributeSet) comp.getAttributeSet().clone();
      comp = comp.getFactory().createComponent(loc, attrs);
      componentsAt.put(comp.getBounds(), comp);
      mutator.add(dest, comp);
    }
  }

  private void buildDynamicAppearance(XmlReader.CircuitData circData, CircuitMutator mutator) {
    final var dest = circData.circuit;
    final var shapes = new ArrayList<AbstractCanvasObject>();
    for (final var appearElt : XmlIterator.forChildElements(circData.circuitElement, "appear")) {
      for (final var sub : XmlIterator.forChildElements(appearElt)) {
        // Dynamic shapes are handled here. Static shapes are already done.
        if (!sub.getTagName().startsWith("visible-")) continue;
        try {
          final var m = AppearanceSvgReader.createShape(sub, null, dest);
          if (m == null) {
            reader.addError(
                S.get("fileAppearanceNotFound", sub.getTagName()),
                circData.circuit.getName() + "." + sub.getTagName());
          } else {
            shapes.add(m);
          }
        } catch (RuntimeException e) {
          reader.addError(
              S.get("fileAppearanceError", sub.getTagName()),
              circData.circuit.getName() + "." + sub.getTagName());
        }
      }
    }
    if (!shapes.isEmpty()) {
      if (circData.appearance == null) {
        circData.appearance = shapes;
      } else {
        circData.appearance.addAll(shapes);
      }
    }
    if (circData.appearance != null && !circData.appearance.isEmpty()) {
      dest.getAppearance().setObjectsForce(circData.appearance);
      dest.getAppearance().setDefaultAppearance(false);
    }
  }

  @Override
  protected Map<Circuit, Integer> getAccessedCircuits() {
    final var access = new HashMap<Circuit, Integer>();
    for (final var data : circuitsData) {
      access.put(data.circuit, READ_WRITE);
    }
    return access;
  }

  @Override
  protected void run(CircuitMutator mutator) {
    for (final var circuitData : circuitsData) {
      buildCircuit(circuitData, mutator);
    }
    for (final var circuitData : circuitsData) {
      buildDynamicAppearance(circuitData, mutator);
    }
  }

  private String toComponentString(Element elt) {
    final var name = elt.getAttribute("name");
    final var loc = elt.getAttribute("loc");
    return name + "(" + loc + ")";
  }

  private String toWireString(Element elt) {
    final var from = elt.getAttribute("from");
    final var to = elt.getAttribute("to");
    return "w" + from + "-" + to;
  }
}
