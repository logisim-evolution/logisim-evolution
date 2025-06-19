/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
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
import com.cburch.logisim.std.memory.DualPortRam;
import com.cburch.logisim.std.memory.RamAttributes;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.util.CollectionUtil;
import com.cburch.logisim.util.StringUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.w3c.dom.Element;

public class XmlCircuitReader extends CircuitTransaction {

  private static final String contextFmt = "%s.%s";

  private final XmlReader.ReadContext reader;

  private final List<XmlReader.CircuitData> circuitsData;
  private boolean isHolyCross = false;
  private boolean isEvolution = false;

  public XmlCircuitReader(
      XmlReader.ReadContext reader,
      List<XmlReader.CircuitData> circDatas,
      boolean isThisHolyCrossFile,
      boolean isThisEvolutionFile) {
    this.reader = reader;
    this.circuitsData = circDatas;
    this.isHolyCross = isThisHolyCrossFile;
    this.isEvolution = isThisEvolutionFile;
  }

  /**
   * @param elt XML element to parse
   * @param reader XML file reader
   * @return the component built from its XML description
   * @throws XmlReaderException
   */
  static Component getComponent(
      Element elt, XmlReader.ReadContext reader, boolean isHolyCross, boolean isEvolution)
      throws XmlReaderException {

    // Determine the factory that creates this element
    final var name = elt.getAttribute("name");
    if (StringUtil.isNullOrEmpty(name)) {
      throw new XmlReaderException(S.get("compNameMissingError"));
    }

    final var libName = elt.getAttribute("lib");
    final var lib = reader.findLibrary(libName);
    if (lib == null) {
      // FIXME: the "no-lib" thing may not be clear enough
      throw new XmlReaderException(S.get("compUnknownError", "no-lib"));
    }

    final var tool = lib.getTool(name);
    if (!(tool instanceof AddTool)) {
      final var msg =
          StringUtil.isNullOrEmpty(libName)
              ? S.get("compUnknownError", name)
              : S.get("compAbsentError", name, libName);
      throw new XmlReaderException(msg);
    }
    final var source = ((AddTool) tool).getFactory();

    // Determine attributes
    final var locStr = elt.getAttribute("loc");
    final var attrs = source.createAttributeSet();
    var defaults = source;
    if (isHolyCross && (source instanceof Ram || source instanceof DualPortRam)) {
      final var ramAttrs = (RamAttributes) attrs;
      ramAttrs.setValue(Mem.ENABLES_ATTR, Mem.USELINEENABLES);
      ramAttrs.updateAttributes();
      defaults = null;
    }
    reader.initAttributeSet(elt, attrs, defaults, isHolyCross, isEvolution);

    // Create component if location known
    if (StringUtil.isNullOrEmpty(locStr)) {
      throw new XmlReaderException(S.get("compLocMissingError", source.getName()));
    }
    try {
      return source.createComponent(Location.parse(locStr), attrs);
    } catch (NumberFormatException e) {
      throw new XmlReaderException(S.get("compLocInvalidError", source.getName(), locStr));
    }
  }

  void addWire(Circuit dest, CircuitMutator mutator, Element elt) throws XmlReaderException {
    Location pt0;
    try {
      final var str = elt.getAttribute("from");
      if (str == null || "".equals(str)) {
        throw new XmlReaderException(S.get("wireStartMissingError"));
      }
      pt0 = Location.parse(str);
    } catch (NumberFormatException e) {
      throw new XmlReaderException(S.get("wireStartInvalidError"));
    }

    Location pt1;
    try {
      final var str = elt.getAttribute("to");
      if (str == null || "".equals(str)) {
        throw new XmlReaderException(S.get("wireEndMissingError"));
      }
      pt1 = Location.parse(str);
    } catch (NumberFormatException e) {
      throw new XmlReaderException(S.get("wireEndInvalidError"));
    }

    if (!pt0.equals(pt1)) {
      // Avoid zero length wires
      mutator.add(dest, Wire.create(pt0, pt1));
    }
  }

  private void buildCircuit(XmlReader.CircuitData circData, CircuitMutator mutator) {
    final var element = circData.circuitElement;
    final var dest = circData.circuit;
    var knownComponents = circData.knownComponents;
    if (knownComponents == null) knownComponents = Collections.emptyMap();
    try {
      /* Here we check the attribute circuitnamedbox for backwards compatibility */
      var hasNamedBox = false;
      var hasNamedBoxFixedSize = false;
      var hasAppearAttr = false;
      for (final var attrElt : XmlIterator.forChildElements(circData.circuitElement, "a")) {
        if (attrElt.hasAttribute("name")) {
          final var name = attrElt.getAttribute("name");
          hasNamedBox |= "circuitnamedbox".equals(name);
          hasAppearAttr |= "appearance".equals(name);
          hasNamedBoxFixedSize |= "circuitnamedboxfixedsize".equals(name);
        }
      }
      reader.initAttributeSet(
          circData.circuitElement, dest.getStaticAttributes(), null, isHolyCross, isEvolution);
      if (circData.circuitElement.hasChildNodes()) {
        if (hasNamedBox) {
          // This situation is clear, it is an older logisim-evolution file
          final var appear =
              CollectionUtil.isNotEmpty(circData.appearance)
                  ? CircuitAttributes.APPEAR_CUSTOM
                  : CircuitAttributes.APPEAR_EVOLUTION;
          dest.getStaticAttributes().setValue(CircuitAttributes.APPEARANCE_ATTR, appear);
        } else {
          if (!hasAppearAttr) {
            // Here we have 2 possibilities, either a Holycross file or a logisim-evolution file
            // before the introduction of the named circuit boxes. So let's ask the user.
            var appear = CircuitAttributes.APPEAR_CLASSIC;
            if (CollectionUtil.isNotEmpty(circData.appearance)) {
              appear = CircuitAttributes.APPEAR_CUSTOM;
            } else if (isHolyCross) {
              appear = CircuitAttributes.APPEAR_FPGA;
            }
            dest.getStaticAttributes().setValue(CircuitAttributes.APPEARANCE_ATTR, appear);
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
    for (final var subElement : XmlIterator.forChildElements(element)) {
      final var subEltName = subElement.getTagName();
      if ("comp".equals(subEltName)) {
        try {
          var comp = knownComponents.get(subElement);
          if (comp == null) comp = getComponent(subElement, reader, isHolyCross, isEvolution);
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
              final var msg =
                  S.get(
                      "fileComponentOverlapError",
                      conflict.getFactory().getName() + conflict.getLocation(),
                      comp.getFactory().getName() + conflict.getLocation());
              reader.addError(msg, circData.circuit.getName());
              overlapComponents.add(comp);
            } else {
              mutator.add(dest, comp);
              componentsAt.put(bds, comp);
            }
          }
        } catch (XmlReaderException e) {
          final var context =
              String.format(contextFmt, circData.circuit.getName(), toComponentString(subElement));
          reader.addErrors(e, context);
        }
      } else if ("wire".equals(subEltName)) {
        try {
          addWire(dest, mutator, subElement);
        } catch (XmlReaderException e) {
          final var context =
              String.format(contextFmt, circData.circuit.getName(), toWireString(subElement));
          reader.addErrors(e, context);
        }
      }
    }
    for (var comp : overlapComponents) {
      final var bds = comp.getBounds();
      if (bds.getHeight() == 0 || bds.getWidth() == 0) {
        // ignore empty boxes
        continue;
      }
      var d = 0;
      do {
        d += 10;
      } while ((componentsAt.get(bds.translate(d, d))) != null && (d < 100_000));
      final var loc = comp.getLocation().translate(d, d);
      final var attrs = (AttributeSet) comp.getAttributeSet().clone();
      comp = comp.getFactory().createComponent(loc, attrs);
      componentsAt.put(comp.getBounds(), comp);
      mutator.add(dest, comp);
    }
  }

  private void buildDynamicAppearance(XmlReader.CircuitData circData) {
    final var dest = circData.circuit;
    final var shapes = new ArrayList<AbstractCanvasObject>();
    final var layers = new ArrayList<Integer>();
    var layer = -1;
    for (final var appearElt : XmlIterator.forChildElements(circData.circuitElement, "appear")) {
      for (final var sub : XmlIterator.forChildElements(appearElt)) {
        layer++;
        // Dynamic shapes are handled here. Static shapes are already done.
        if (!sub.getTagName().startsWith("visible-")) continue;
        try {
          final var m = AppearanceSvgReader.createShape(sub, null, dest);
          if (m == null) {
            final var context =
                String.format(contextFmt, circData.circuit.getName(), sub.getTagName());
            reader.addError(S.get("fileAppearanceNotFound", sub.getTagName()), context);
          } else {
            shapes.add(m);
            layers.add(layer);
          }
        } catch (RuntimeException e) {
          final var context =
              String.format(contextFmt, circData.circuit.getName(), sub.getTagName());
          reader.addError(S.get("fileAppearanceError", sub.getTagName()), context);
        }
      }
    }
    if (!shapes.isEmpty()) {
      if (circData.appearance == null) {
        circData.appearance = shapes;
      } else {
        for (var shapeId = 0; shapeId < shapes.size(); shapeId++) {
          circData.appearance.add(layers.get(shapeId), shapes.get(shapeId));
        }
      }
    }
    if (CollectionUtil.isNotEmpty(circData.appearance)) {
      dest.getAppearance().setObjectsForce(circData.appearance);
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
      buildDynamicAppearance(circuitData);
    }
  }

  private String toComponentString(Element elt) {
    final var name = elt.getAttribute("name");
    final var loc = elt.getAttribute("loc");
    return String.format("%s(%s)", name, loc);
  }

  private String toWireString(Element elt) {
    final var from = elt.getAttribute("from");
    final var to = elt.getAttribute("to");
    return String.format("w%s-%s", from, to);
  }
}
