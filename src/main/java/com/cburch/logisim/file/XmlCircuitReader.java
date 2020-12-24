/**
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
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.std.base.Text;
import com.cburch.logisim.std.memory.Mem;
import com.cburch.logisim.std.memory.Ram;
import com.cburch.logisim.std.memory.RamAttributes;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
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
    String name = elt.getAttribute("name");
    if (name == null || name.equals("")) {
      throw new XmlReaderException(S.get("compNameMissingError"));
    }

    String libName = elt.getAttribute("lib");
    Library lib = reader.findLibrary(libName);
    if (lib == null) {
      throw new XmlReaderException(S.fmt("compUnknownError", "no-lib"));
    }

    Tool tool = lib.getTool(name);
    if (tool == null || !(tool instanceof AddTool)) {
      if (libName == null || libName.equals("")) {
        throw new XmlReaderException(S.fmt("compUnknownError", name));
      } else {
        throw new XmlReaderException(S.fmt("compAbsentError", name, libName));
      }
    }
    ComponentFactory source = ((AddTool) tool).getFactory();

    // Determine attributes
    String loc_str = elt.getAttribute("loc");
    AttributeSet attrs = source.createAttributeSet();
    if (source instanceof Ram && IsHolyCross) {
      RamAttributes rattrs = (RamAttributes) attrs; 
      rattrs.setValue(Mem.ENABLES_ATTR, Mem.USELINEENABLES);
      rattrs.updateAttributes();
      reader.initAttributeSet(elt, attrs, null, IsHolyCross, IsEvolution);
    } else reader.initAttributeSet(elt, attrs, source, IsHolyCross, IsEvolution);

    // Create component if location known
    if (loc_str == null || loc_str.equals("")) {
      throw new XmlReaderException(S.fmt("compLocMissingError", source.getName()));
    } else {
      try {
        Location loc = Location.parse(loc_str);
        return source.createComponent(loc, attrs);
      } catch (NumberFormatException e) {
        throw new XmlReaderException(S.fmt("compLocInvalidError", source.getName(), loc_str));
      }
    }
  }

  private XmlReader.ReadContext reader;

  private List<XmlReader.CircuitData> circuitsData;
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
      String str = elt.getAttribute("from");
      if (str == null || str.equals("")) {
        throw new XmlReaderException(S.get("wireStartMissingError"));
      }
      pt0 = Location.parse(str);
    } catch (NumberFormatException e) {
      throw new XmlReaderException(S.get("wireStartInvalidError"));
    }

    Location pt1;
    try {
      String str = elt.getAttribute("to");
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
    Element elt = circData.circuitElement;
    Circuit dest = circData.circuit;
    Map<Element, Component> knownComponents = circData.knownComponents;
    if (knownComponents == null) knownComponents = Collections.emptyMap();
    try {
      /* Here we check the attribute circuitnamedbox for backwards compatibility */
      boolean HasNamedBox = false;
      boolean HasNamedBoxFixedSize = false;
      boolean HasAppearAttr = false;
      for (Element attrElt : XmlIterator.forChildElements(circData.circuitElement, "a")) {
        if (attrElt.hasAttribute("name")) {
          String Name = attrElt.getAttribute("name");
          if (Name.equals("circuitnamedbox")) {
            HasNamedBox = true;
          }
          if (Name.equals("appearance")) HasAppearAttr = true;
          if (Name.equals("circuitnamedboxfixedsize")) {
            HasNamedBoxFixedSize = true;
          }
        }
      }
      reader.initAttributeSet(
          circData.circuitElement, dest.getStaticAttributes(), null, IsHolyCross, IsEvolution);
      if (circData.circuitElement.hasChildNodes()) {
        if (HasNamedBox) {
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
        if (!HasNamedBoxFixedSize) {
          dest.getStaticAttributes()
              .setValue(CircuitAttributes.NAMED_CIRCUIT_BOX_FIXED_SIZE, false);
        }
      }
    } catch (XmlReaderException e) {
      reader.addErrors(e, circData.circuit.getName() + ".static");
    }

    HashMap<Bounds, Component> componentsAt = new HashMap<>();
    ArrayList<Component> overlapComponents = new ArrayList<>();
    for (Element sub_elt : XmlIterator.forChildElements(elt)) {
      String sub_elt_name = sub_elt.getTagName();
      if (sub_elt_name.equals("comp")) {
        try {
          Component comp = knownComponents.get(sub_elt);
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
            Bounds bds = comp.getBounds();
            Component conflict = componentsAt.get(bds);
            if (conflict != null) {
              reader.addError(S.fmt("fileComponentOverlapError", 
                      conflict.getFactory().getName()+conflict.getLocation(),
                      comp.getFactory().getName()+conflict.getLocation()),
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
    for (Component comp : overlapComponents) {
      Bounds bds = comp.getBounds();
      if (bds.getHeight() == 0 || bds.getWidth() == 0) continue; // ignore empty boxes
      int d = 0;
      do {
        d += 10;
      } while ((componentsAt.get(bds.translate(d, d))) != null && (d < 100000)) ;
      Location loc = comp.getLocation().translate(d, d);
      AttributeSet attrs = (AttributeSet) comp.getAttributeSet().clone();
      comp = comp.getFactory().createComponent(loc, attrs);
      componentsAt.put(comp.getBounds(), comp);
      mutator.add(dest, comp);
    }
  }

  private void buildDynamicAppearance(XmlReader.CircuitData circData, CircuitMutator mutator) {
    Circuit dest = circData.circuit;
    List<AbstractCanvasObject> shapes = new ArrayList<AbstractCanvasObject>();
    for (Element appearElt : XmlIterator.forChildElements(circData.circuitElement, "appear")) {
      for (Element sub : XmlIterator.forChildElements(appearElt)) {
        // Dynamic shapes are handled here. Static shapes are already done.
        if (!sub.getTagName().startsWith("visible-")) continue;
        try {
          AbstractCanvasObject m = AppearanceSvgReader.createShape(sub, null, dest);
          if (m == null) {
            reader.addError(
                S.fmt("fileAppearanceNotFound", sub.getTagName()),
                circData.circuit.getName() + "." + sub.getTagName());
          } else {
            shapes.add(m);
          }
        } catch (RuntimeException e) {
          reader.addError(
              S.fmt("fileAppearanceError", sub.getTagName()),
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
    for (XmlReader.CircuitData circuitData : circuitsData) {
      buildDynamicAppearance(circuitData, mutator);
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
