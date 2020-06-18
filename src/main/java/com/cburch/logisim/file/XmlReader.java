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
import com.cburch.logisim.LogisimVersion;
import com.cburch.logisim.Main;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitMapInfo;
import com.cburch.logisim.circuit.appear.AppearanceSvgReader;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeDefaultProvider;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.fpga.data.BoardRectangle;
import com.cburch.logisim.fpga.data.MapComponent;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.wiring.Pin;
import com.cburch.logisim.std.wiring.ProbeAttributes;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.util.InputEventUtil;
import com.cburch.logisim.util.StringUtil;
import com.cburch.logisim.vhdl.base.VhdlContent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

class XmlReader {

  static class CircuitData {
    Element circuitElement;
    Circuit circuit;
    Map<Element, Component> knownComponents;
    List<AbstractCanvasObject> appearance;

    public CircuitData(Element circuitElement, Circuit circuit) {
      this.circuitElement = circuitElement;
      this.circuit = circuit;
    }
  }

  class ReadContext {
    LogisimFile file;
    LogisimVersion sourceVersion;
    HashMap<String, Library> libs = new HashMap<String, Library>();
    private ArrayList<String> messages;

    ReadContext(LogisimFile file) {
      this.file = file;
      this.messages = new ArrayList<String>();
    }

    void addError(String message, String context) {
      messages.add(message + " [" + context + "]");
    }

    void addErrors(XmlReaderException exception, String context) {
      for (String msg : exception.getMessages()) {
        messages.add(msg + " [" + context + "]");
      }
    }

    Library findLibrary(String lib_name) throws XmlReaderException {
      if (lib_name == null || lib_name.equals("")) {
        return file;
      }

      Library ret = libs.get(lib_name);
      if (ret == null) {
        throw new XmlReaderException(StringUtil.format(S.get("libMissingError"), lib_name));
      } else {
        return ret;
      }
    }

    void initAttributeSet(
        Element parentElt,
        AttributeSet attrs,
        AttributeDefaultProvider defaults,
        boolean IsHolyCross,
        boolean IsEvolution)
        throws XmlReaderException {
      ArrayList<String> messages = null;

      HashMap<String, String> attrsDefined = new HashMap<String, String>();
      for (Element attrElt : XmlIterator.forChildElements(parentElt, "a")) {
        if (!attrElt.hasAttribute("name")) {
          if (messages == null) messages = new ArrayList<String>();
          messages.add(S.get("attrNameMissingError"));
        } else {
          String attrName = attrElt.getAttribute("name");
          String attrVal;
          if (attrElt.hasAttribute("val")) {
            attrVal = attrElt.getAttribute("val");
            if (attrName.equals("filePath")) {
              /* De-relativize the path */
              String dirPath = "";
              if (srcFilePath != null)
                dirPath = srcFilePath.substring(0, srcFilePath.lastIndexOf(File.separator));
              Path tmp = Paths.get(dirPath, attrVal);
              attrVal = tmp.toString();
            }
          } else {
            attrVal = attrElt.getTextContent();
          }
          attrsDefined.put(attrName, attrVal);
        }
      }

      if (attrs == null) return;

      LogisimVersion ver = sourceVersion;
      boolean setDefaults = defaults != null && !defaults.isAllDefaultValues(attrs, ver);
      // We need to process this in order, and we have to refetch the
      // attribute list each time because it may change as we iterate
      // (as it will for a splitter).
      for (int i = 0; true; i++) {
        List<Attribute<?>> attrList = attrs.getAttributes();
        if (i >= attrList.size()) break;
        @SuppressWarnings("unchecked")
        Attribute<Object> attr = (Attribute<Object>) attrList.get(i);
        String attrName = attr.getName();
        String attrVal = attrsDefined.get(attrName);
        if (attrVal == null) {
          if (attr.equals(ProbeAttributes.PROBEAPPEARANCE)) {
            attrs.setValue(ProbeAttributes.PROBEAPPEARANCE, StdAttr.APPEAR_CLASSIC);
          } else if (attr.equals(StdAttr.APPEARANCE)) {
            if (IsHolyCross) attrs.setValue(StdAttr.APPEARANCE, StdAttr.APPEAR_CLASSIC);
            else if (IsEvolution) attrs.setValue(StdAttr.APPEARANCE, StdAttr.APPEAR_EVOLUTION);
            else {
              Object val = defaults.getDefaultAttributeValue(attr, ver);
              if (val != null) {
                attrs.setValue(attr, val);
              }
            }
          } else if (setDefaults) {
            Object val = defaults.getDefaultAttributeValue(attr, ver);
            if (val != null) {
              attrs.setValue(attr, val);
            }
          }
        } else {
          try {
            Object val = attr.parse(attrVal);
            attrs.setValue(attr, val);
          } catch (NumberFormatException e) {
            if (messages == null) messages = new ArrayList<String>();
            messages.add(StringUtil.format(S.get("attrValueInvalidError"), attrVal, attrName));
          }
        }
      }
      if (messages != null) {
        throw new XmlReaderException(messages);
      }
    }

    private void initMouseMappings(Element elt, boolean IsHolyCross, boolean IsEvolution) {
      MouseMappings map = file.getOptions().getMouseMappings();
      for (Element sub_elt : XmlIterator.forChildElements(elt, "tool")) {
        Tool tool;
        try {
          tool = toTool(sub_elt);
        } catch (XmlReaderException e) {
          addErrors(e, "mapping");
          continue;
        }

        String mods_str = sub_elt.getAttribute("map");
        if (mods_str == null || mods_str.equals("")) {
          loader.showError(S.get("mappingMissingError"));
          continue;
        }
        int mods;
        try {
          mods = InputEventUtil.fromString(mods_str);
        } catch (NumberFormatException e) {
          loader.showError(StringUtil.format(S.get("mappingBadError"), mods_str));
          continue;
        }

        tool = tool.cloneTool();
        try {
          initAttributeSet(sub_elt, tool.getAttributeSet(), tool, IsHolyCross, IsEvolution);
        } catch (XmlReaderException e) {
          addErrors(e, "mapping." + tool.getName());
        }

        map.setToolFor(mods, tool);
      }
    }

    private void initToolbarData(Element elt, boolean IsHolyCross, boolean IsEvolution) {
      ToolbarData toolbar = file.getOptions().getToolbarData();
      for (Element sub_elt : XmlIterator.forChildElements(elt)) {
        if (sub_elt.getTagName().equals("sep")) {
          toolbar.addSeparator();
        } else if (sub_elt.getTagName().equals("tool")) {
          Tool tool;
          try {
            tool = toTool(sub_elt);
          } catch (XmlReaderException e) {
            addErrors(e, "toolbar");
            continue;
          }
          if (tool != null) {
            tool = tool.cloneTool();
            try {
              initAttributeSet(sub_elt, tool.getAttributeSet(), tool, IsHolyCross, IsEvolution);
            } catch (XmlReaderException e) {
              addErrors(e, "toolbar." + tool.getName());
            }
            if (tool.getAttributeSet() != null) {
              if (tool.getAttributeSet().containsAttribute(ProbeAttributes.PROBEAPPEARANCE))
                tool.getAttributeSet()
                    .setValue(
                        ProbeAttributes.PROBEAPPEARANCE,
                        ProbeAttributes.GetDefaultProbeAppearance());
              if (tool.getAttributeSet().containsAttribute(StdAttr.APPEARANCE))
                tool.getAttributeSet()
                    .setValue(StdAttr.APPEARANCE, AppPreferences.getDefaultAppearance());
            }
            toolbar.addTool(tool);
          }
        }
      }
    }

    private Map<Element, Component> loadKnownComponents(
        Element elt, boolean IsHolyCross, boolean IsEvolution) {
      Map<Element, Component> known = new HashMap<Element, Component>();
      for (Element sub : XmlIterator.forChildElements(elt, "comp")) {
        try {
          Component comp = XmlCircuitReader.getComponent(sub, this, IsHolyCross, IsEvolution);
          if (comp != null) known.put(sub, comp);
        } catch (XmlReaderException e) {
        }
      }
      return known;
    }
    
    void loadMap(Element board, String boardName, Circuit circ) {
      HashMap<String,CircuitMapInfo> map = new HashMap<String,CircuitMapInfo>();
      for (Element cmap : XmlIterator.forChildElements(board, "mc")) {
        int x,y,w,h;
        String key = cmap.getAttribute("key");
        if (key == null || key.isEmpty()) continue;
        if (cmap.hasAttribute("open")) {
          map.put(key, new CircuitMapInfo());
        } else if (cmap.hasAttribute("vconst")) {
          Long v;
          try {
            v = Long.parseLong(cmap.getAttribute("vconst"));
          } catch (NumberFormatException e) {
            continue;
          }
          map.put(key, new CircuitMapInfo(v));
        } else if (cmap.hasAttribute("valx") && cmap.hasAttribute("valy") &&
              cmap.hasAttribute("valw") && cmap.hasAttribute("valh")) {
          /* Backward compatibility: */
          try {
            x = Integer.parseUnsignedInt(cmap.getAttribute("valx"));
            y = Integer.parseUnsignedInt(cmap.getAttribute("valy"));
            w = Integer.parseUnsignedInt(cmap.getAttribute("valw"));
            h = Integer.parseUnsignedInt(cmap.getAttribute("valh"));
          } catch (NumberFormatException e) {
            continue;
          }
          BoardRectangle br = new BoardRectangle(x,y,w,h);
          map.put(key, new CircuitMapInfo(br));
        } else {
          CircuitMapInfo cmapi = MapComponent.getMapInfo(cmap);
          if (cmapi != null)
            map.put(key, cmapi);
        }
      }
      if (!map.isEmpty()) circ.addLoadedMap(boardName, map);
    }

    void loadAppearance(Element appearElt, XmlReader.CircuitData circData, String context) {
      Map<Location, Instance> pins = new HashMap<Location, Instance>();
      for (Component comp : circData.knownComponents.values()) {
        if (comp.getFactory() == Pin.FACTORY) {
          Instance instance = Instance.getInstanceFor(comp);
          pins.put(comp.getLocation(), instance);
        }
      }

      List<AbstractCanvasObject> shapes = new ArrayList<AbstractCanvasObject>();
      for (Element sub : XmlIterator.forChildElements(appearElt)) {
        // Dynamic shapes are skipped here. They are resolved later in
        // XmlCircuitReader once the full Circuit tree has been built.
        // Static shapes (e.g. pins and anchors) need to be done here.
        if (sub.getTagName().startsWith("visible-")) continue;
        try {
          AbstractCanvasObject m = AppearanceSvgReader.createShape(sub, pins, null);
          if (m == null) {
            addError(
                S.fmt("fileAppearanceNotFound", sub.getTagName()),
                context + "." + sub.getTagName());
          } else {
            shapes.add(m);
          }
        } catch (RuntimeException e) {
          addError(
              S.fmt("fileAppearanceError", sub.getTagName()), context + "." + sub.getTagName());
        }
      }
      if (!shapes.isEmpty()) {
        if (circData.appearance == null) {
          circData.appearance = shapes;
        } else {
          circData.appearance.addAll(shapes);
        }
      }
    }

    private Library toLibrary(Element elt, boolean IsHolyCross, boolean IsEvolution) {
      if (!elt.hasAttribute("name")) {
        loader.showError(S.get("libNameMissingError"));
        return null;
      }
      if (!elt.hasAttribute("desc")) {
        loader.showError(S.get("libDescMissingError"));
        return null;
      }
      String name = elt.getAttribute("name");
      String desc = elt.getAttribute("desc");
      Library ret = loader.loadLibrary(desc);
      if (ret == null) return null;
      libs.put(name, ret);
      for (Element sub_elt : XmlIterator.forChildElements(elt, "tool")) {
        if (!sub_elt.hasAttribute("name")) {
          loader.showError(S.get("toolNameMissingError"));
        } else {
          String tool_str = sub_elt.getAttribute("name");
          Tool tool = ret.getTool(tool_str);
          if (tool != null) {
            try {
              initAttributeSet(sub_elt, tool.getAttributeSet(), tool, IsHolyCross, IsEvolution);
            } catch (XmlReaderException e) {
              addErrors(e, "lib." + name + "." + tool_str);
            }
          }
        }
      }
      return ret;
    }

    private void toLogisimFile(Element elt, Project proj) {
      // determine the version producing this file
      String versionString = elt.getAttribute("source");
      boolean HolyCrossFile = false;
      boolean IsEvolutionFile = true;
      if (versionString.equals("")) {
        sourceVersion = Main.VERSION;
      } else {
        sourceVersion = LogisimVersion.parse(versionString);
        HolyCrossFile = versionString.endsWith("-HC");
      }

      // If we are opening a pre-logisim-evolution file, there might be
      // some components
      // (such as the RAM or the counters), that have changed their shape
      // and other details.
      // We have therefore to warn the user that things might be a little
      // strange in their
      // circuits...
      if (sourceVersion.compareTo(LogisimVersion.get(2, 7, 2)) < 0) {
        IsEvolutionFile = true;
        OptionPane.showMessageDialog(
            null,
            "You are opening a file created with original Logisim code.\n"
                + "You might encounter some problems in the execution, since some components evolved since then.\n"
                + "Moreover, labels will be converted to match VHDL limitations for variable names.",
            "Old file format -- compatibility mode",
            OptionPane.WARNING_MESSAGE);
      }

      // first, load the sublibraries
      for (Element o : XmlIterator.forChildElements(elt, "lib")) {
        Library lib = toLibrary(o, HolyCrossFile, IsEvolutionFile);
        if (lib != null) file.addLibrary(lib);
      }

      // second, create the circuits - empty for now - and the vhdl entities
      List<CircuitData> circuitsData = new ArrayList<CircuitData>();
      for (Element circElt : XmlIterator.forChildElements(elt)) {
        String name;
        switch (circElt.getTagName()) {
          case "vhdl":
            name = circElt.getAttribute("name");
            if (name == null || name.equals("")) {
              addError(S.get("circNameMissingError"), "C??");
            }
            String vhdl = circElt.getTextContent();
            VhdlContent contents = VhdlContent.parse(name, vhdl, file);
            if (contents != null) {
              file.addVhdlContent(contents);
            }
            break;
          case "circuit":
            name = circElt.getAttribute("name");

            if (name == null || name.equals("")) {
              addError(S.get("circNameMissingError"), "C??");
            }
            CircuitData circData = new CircuitData(circElt, new Circuit(name, file, proj));
            file.addCircuit(circData.circuit);
            circData.knownComponents = loadKnownComponents(circElt, HolyCrossFile, IsEvolutionFile);
            for (Element appearElt : XmlIterator.forChildElements(circElt, "appear")) {
              loadAppearance(appearElt, circData, name + ".appear");
            }
            for (Element boardMap :  XmlIterator.forChildElements(circElt, "boardmap")) {
              String BoardName = boardMap.getAttribute("boardname");
              if (BoardName == null || BoardName.isEmpty()) continue;
              loadMap(boardMap,BoardName,circData.circuit);
            }
            circuitsData.add(circData);
          default:
            // do nothing
        }
      }

      // third, process the other child elements
      for (Element sub_elt : XmlIterator.forChildElements(elt)) {
        String name = sub_elt.getTagName();

        switch (name) {
          case "circuit":
          case "vhdl":
          case "lib":
            // Nothing to do: Done earlier.
            break;
          case "options":
            try {
              initAttributeSet(
                  sub_elt,
                  file.getOptions().getAttributeSet(),
                  null,
                  HolyCrossFile,
                  IsEvolutionFile);
            } catch (XmlReaderException e) {
              addErrors(e, "options");
            }
            break;
          case "mappings":
            initMouseMappings(sub_elt, HolyCrossFile, IsEvolutionFile);
            break;
          case "toolbar":
            initToolbarData(sub_elt, HolyCrossFile, IsEvolutionFile);
            break;
          case "main":
            String main = sub_elt.getAttribute("name");
            Circuit circ = file.getCircuit(main);
            if (circ != null) {
              file.setMainCircuit(circ);
            }
            break;
          case "message":
            file.addMessage(sub_elt.getAttribute("value"));
            break;
          default:
            throw new IllegalArgumentException("Invalid node in logisim file: " + name);
        }
      }

      // fourth, execute a transaction that initializes all the circuits
      XmlCircuitReader builder;
      builder = new XmlCircuitReader(this, circuitsData, HolyCrossFile, IsEvolutionFile);
      builder.execute();
    }

    Tool toTool(Element elt) throws XmlReaderException {
      Library lib = findLibrary(elt.getAttribute("lib"));
      String name = elt.getAttribute("name");
      if (name == null || name.equals("")) {
        throw new XmlReaderException(S.get("toolNameMissing"));
      }
      Tool tool = lib.getTool(name);
      if (tool == null) {
        throw new XmlReaderException(S.get("toolNotFound"));
      }
      return tool;
    }
  }

  /**
   * Change label names in an XML tree according to a list of suggested labels
   *
   * @param root root element of the XML tree
   * @param nodeType type of nodes to consider
   * @param attrType type of attributes to consider
   * @param validLabels label set of correct label names
   */
  public static void applyValidLabels(
      Element root, String nodeType, String attrType, Map<String, String> validLabels)
      throws IllegalArgumentException {
    assert (root != null);
    assert (nodeType != null);
    assert (attrType != null);
    assert (nodeType.length() > 0);
    assert (attrType.length() > 0);
    assert (validLabels != null);

    switch (nodeType) {
      case "circuit":
        replaceCircuitNodes(root, attrType, validLabels);
        break;
      case "comp":
        replaceCompNodes(root, validLabels);
        break;
      default:
        throw new IllegalArgumentException("Invalid node type requested: " + nodeType);
    }
  }

  /**
   * Sets to the empty string any label attribute in tool nodes derived from elt.
   *
   * @param root root node
   */
  private static void cleanupToolsLabel(Element root) {
    assert (root != null);

    // Iterate on tools
    for (Element toolElt : XmlIterator.forChildElements(root, "tool")) {
      // Iterate on attribute nodes
      for (Element attrElt : XmlIterator.forChildElements(toolElt, "a")) {
        // Each attribute node should have a name field
        if (attrElt.hasAttribute("name")) {
          String aName = attrElt.getAttribute("name");
          if (aName.equals("label")) {
            // Found a label node in a tool, clean it up!
            attrElt.setAttribute("val", "");
          }
        }
      }
    }
  }

  public static Element ensureLogisimCompatibility(Element elt) {
    Map<String, String> validLabels;
    validLabels = findValidLabels(elt, "circuit", "name");
    applyValidLabels(elt, "circuit", "name", validLabels);
    validLabels = findValidLabels(elt, "circuit", "label");
    applyValidLabels(elt, "circuit", "label", validLabels);
    validLabels = findValidLabels(elt, "comp", "label");
    applyValidLabels(elt, "comp", "label", validLabels);
    // In old, buggy Logisim versions, labels where incorrectly
    // stored also in toolbar and lib components. If this is the
    // case, clean them up.
    fixInvalidToolbarLib(elt);
    return (elt);
  }

  private static void findLibraryUses(
      ArrayList<Element> dest, String label, Iterable<Element> candidates) {
    for (Element elt : candidates) {
      String lib = elt.getAttribute("lib");
      if (lib.equals(label)) {
        dest.add(elt);
      }
    }
  }

  /**
   * Check an XML tree for VHDL-incompatible labels, then propose a list of valid ones. Here valid
   * means: [a-zA-Z][a-zA-Z0-9_]* This applies, in our context, to circuit's names and labels (and
   * their corresponding component's names, of course), and to comp's labels.
   *
   * @param root root element of the XML tree
   * @param nodeType type of nodes to consider
   * @param attrType type of attributes to consider
   * @return map containing the original attribute values as keys, and the corresponding valid
   *     attribute values as the values
   */
  public static Map<String, String> findValidLabels(
      Element root, String nodeType, String attrType) {
    assert (root != null);
    assert (nodeType != null);
    assert (attrType != null);
    assert (nodeType.length() > 0);
    assert (attrType.length() > 0);

    Map<String, String> validLabels = new HashMap<String, String>();

    List<String> initialLabels = getXMLLabels(root, nodeType, attrType);

    Iterator<String> iterator = initialLabels.iterator();
    while (iterator.hasNext()) {
      String label = iterator.next();
      if (!validLabels.containsKey(label)) {
        // Check if the name is invalid, in which case create
        // a valid version and put it in the map
        if (VhdlContent.labelVHDLInvalid(label)) {
          String initialLabel = label;
          label = generateValidVHDLLabel(label);
          validLabels.put(initialLabel, label);
        }
      }
    }

    return validLabels;
  }

  /**
   * In some old version of Logisim, buggy Logisim versions, labels where incorrectly stored also in
   * toolbar and lib components. If this is the case, clean them up..
   *
   * @param root root element of the XML tree
   */
  private static void fixInvalidToolbarLib(Element root) {
    assert (root != null);

    // Iterate on toolbars -- though there should be only one!
    for (Element toolbarElt : XmlIterator.forChildElements(root, "toolbar")) {
      cleanupToolsLabel(toolbarElt);
    }

    // Iterate on libs
    for (Element libsElt : XmlIterator.forChildElements(root, "lib")) {
      cleanupToolsLabel(libsElt);
    }
  }

  /**
   * Given a label, generates a valid VHDL label by removing invalid characters, putting a letter at
   * the beginning, and putting a shortened (8 characters) UUID at the end if the name has been
   * altered. Whitespaces at the beginning and at the end of the string are trimmed by default (if
   * this is the only change, then no suffix is appended).
   *
   * @param initialLabel initial (possibly invalid) label
   * @return a valid VHDL label
   */
  public static String generateValidVHDLLabel(String initialLabel) {
    return (generateValidVHDLLabel(initialLabel, UUID.randomUUID().toString().substring(0, 8)));
  }

  /**
   * Given a label, generates a valid VHDL label by removing invalid characters, putting a letter at
   * the beginning, and putting the requested suffix at the end if the name has been altered.
   * Whitespaces at the beginning and at the end of the string are trimmed by default (if this is
   * the only change, then no suffix is appended).
   *
   * @param initialLabel initial (possibly invalid) label
   * @param suffix string that has to be appended to a modified label
   * @return a valid VHDL label
   */
  public static String generateValidVHDLLabel(String initialLabel, String suffix) {
    assert (initialLabel != null);

    // As a default, trim whitespaces at the beginning and at the end
    // of a label (no risks with that potentially, therefore avoid
    // to append the suffix if that was the only change)
    initialLabel = initialLabel.trim();

    String label = initialLabel;

    if (label.isEmpty()) {
      logger.warn("Empty label is not a valid VHDL label");
      label = "L_";
    }

    // If the string has a ! or ~ symbol, then replace it with "NOT"
    label = label.replaceAll("[\\!~]", "NOT_");

    // Force string to start with a letter
    if (!label.matches("^[A-Za-z].*$")) label = "L_" + label;

    // Force the rest to be either letters, or numbers, or underscores
    label = label.replaceAll("[^A-Za-z0-9_]", "_");
    // Suppress multiple successive underscores and an underscore at the end
    label = label.replaceAll("_+", "_");
    if (label.endsWith("_")) label = label.substring(0, label.length() - 1);

    if (!label.equals(initialLabel)) {
      // Concatenate a unique ID if the string has been altered
      label = label + "_" + suffix;
      // Replace the "-" characters in the UUID with underscores
      label = label.replaceAll("-", "_");
    }

    return (label);
  }

  /**
   * Traverses an XML tree and gets a list of attribute values for the given attribute and node
   * types.
   *
   * @param root root element of the XML tree
   * @param nodeType type of nodes to consider
   * @param attrType type of attributes to consider
   * @return list of names for the considered node/attribute pairs
   */
  public static List<String> getXMLLabels(Element root, String nodeType, String attrType)
      throws IllegalArgumentException {
    assert (root != null);
    assert (nodeType != null);
    assert (attrType != null);
    assert (nodeType.length() > 0);
    assert (attrType.length() > 0);

    List<String> attrValuesList = new ArrayList<String>();

    switch (nodeType) {
      case "circuit":
        inspectCircuitNodes(root, attrType, attrValuesList);
        break;
      case "comp":
        inspectCompNodes(root, attrValuesList);
        break;
      default:
        throw new IllegalArgumentException("Invalid node type requested: " + nodeType);
    }
    return attrValuesList;
  }

  /**
   * Check XML's circuit nodes, and return a list of values corresponding to the desired attribute.
   *
   * @param root XML's root
   * @param attrType attribute type (either name or label)
   * @param attrValuesList empty list that will contain the values found
   */
  private static void inspectCircuitNodes(
      Element root, String attrType, List<String> attrValuesList) throws IllegalArgumentException {
    assert (root != null);
    assert (attrType != null);
    assert (attrValuesList != null);
    assert (attrValuesList.isEmpty());

    // Circuits are top-level in the XML file
    switch (attrType) {
      case "name":
        for (Element circElt : XmlIterator.forChildElements(root, "circuit")) {
          // Circuit's name is directly available as an attribute
          String name = circElt.getAttribute("name");
          attrValuesList.add(name);
        }
        break;
      case "label":
        for (Element circElt : XmlIterator.forChildElements(root, "circuit")) {
          // label is available through its a child node
          for (Element attrElt : XmlIterator.forChildElements(circElt, "a")) {
            if (attrElt.hasAttribute("name")) {
              String aName = attrElt.getAttribute("name");
              if (aName.equals("label")) {
                String label = attrElt.getAttribute("val");
                if (label.length() > 0) {
                  attrValuesList.add(label);
                }
              }
            }
          }
        }
        break;
      default:
        throw new IllegalArgumentException(
            "Invalid attribute type requested: " + attrType + " for node type: circuit");
    }
  }

  /**
   * Check XML's comp nodes, and return a list of values corresponding to the desired attribute. The
   * checked comp nodes are NOT those referring to circuits -- we can see if this is the case by
   * checking whether the lib attribute is present or not.
   *
   * @param root XML's root
   * @param attrValuesList empty list that will contain the values found
   */
  private static void inspectCompNodes(Element root, List<String> attrValuesList) {
    assert (root != null);
    assert (attrValuesList != null);
    assert (attrValuesList.isEmpty());

    for (Element circElt : XmlIterator.forChildElements(root, "circuit")) {
      // In circuits, we have to look for components, then take
      // just those components that do have a lib attribute and look at
      // their
      // a child nodes
      for (Element compElt : XmlIterator.forChildElements(circElt, "comp")) {
        if (compElt.hasAttribute("lib")) {
          for (Element attrElt : XmlIterator.forChildElements(compElt, "a")) {
            if (attrElt.hasAttribute("name")) {
              String aName = attrElt.getAttribute("name");
              if (aName.equals("label")) {
                String label = attrElt.getAttribute("val");
                if (label.length() > 0) {
                  attrValuesList.add(label);
                }
              }
            }
          }
        }
      }
    }
  }

  /**
   * Check if a given label could be a valid VHDL variable name
   *
   * @param label candidate VHDL variable name
   * @return true if the label is NOT a valid name, false otherwise
   */
  public static boolean labelVHDLInvalid(String label) {
    if (!label.matches("^[A-Za-z][A-Za-z0-9_]*") || label.endsWith("_") || label.matches(".*__.*"))
      return (true);

    return (false);
  }

  /**
   * Replace invalid labels in circuit nodes.
   *
   * @param root XML's root
   * @param attrType attribute type (either name or label)
   * @param validLabels map containing valid label values
   */
  private static void replaceCircuitNodes(
      Element root, String attrType, Map<String, String> validLabels)
      throws IllegalArgumentException {
    assert (root != null);
    assert (attrType != null);
    assert (validLabels != null);

    if (validLabels.isEmpty()) {
      // Particular case, all the labels were good!
      return;
    }

    // Circuits are top-level in the XML file
    switch (attrType) {
      case "name":
        // We have not only to replace the circuit names in each circuit,
        // but in the corresponding comps too!
        for (Element circElt : XmlIterator.forChildElements(root, "circuit")) {
          // Circuit's name is directly available as an attribute
          String name = circElt.getAttribute("name");
          if (validLabels.containsKey(name)) {
            circElt.setAttribute("name", validLabels.get(name));
            // Also, it is present as value for the "circuit" attribute
            for (Element attrElt : XmlIterator.forChildElements(circElt, "a")) {
              if (attrElt.hasAttribute("name")) {
                String aName = attrElt.getAttribute("name");
                if (aName.equals("circuit")) {
                  attrElt.setAttribute("val", validLabels.get(name));
                }
              }
            }
          }
          // Now do the comp part
          for (Element compElt : XmlIterator.forChildElements(circElt, "comp")) {
            // Circuits are components without lib
            if (!compElt.hasAttribute("lib")) {
              if (compElt.hasAttribute("name")) {
                String cName = compElt.getAttribute("name");
                if (validLabels.containsKey(cName)) {
                  compElt.setAttribute("name", validLabels.get(cName));
                }
              }
            }
          }
        }
        break;
      case "label":
        for (Element circElt : XmlIterator.forChildElements(root, "circuit")) {
          // label is available through its a child node
          for (Element attrElt : XmlIterator.forChildElements(circElt, "a")) {
            if (attrElt.hasAttribute("name")) {
              String aName = attrElt.getAttribute("name");
              if (aName.equals("label")) {
                String label = attrElt.getAttribute("val");
                if (validLabels.containsKey(label)) {
                  attrElt.setAttribute("val", validLabels.get(label));
                }
              }
            }
          }
        }
        break;
      default:
        throw new IllegalArgumentException(
            "Invalid attribute type requested: " + attrType + " for node type: circuit");
    }
  }

  /**
   * Replace invalid labels in comp nodes.
   *
   * @param root XML's root
   * @param validLabels map containing valid label values
   */
  private static void replaceCompNodes(Element root, Map<String, String> validLabels) {
    assert (root != null);
    assert (validLabels != null);

    if (validLabels.isEmpty()) {
      // Particular case, all the labels were good!
      return;
    }

    for (Element circElt : XmlIterator.forChildElements(root, "circuit")) {
      // In circuits, we have to look for components, then take
      // just those components that do have a lib attribute and look at
      // their
      // a child nodes
      for (Element compElt : XmlIterator.forChildElements(circElt, "comp")) {
        if (compElt.hasAttribute("lib")) {
          for (Element attrElt : XmlIterator.forChildElements(compElt, "a")) {
            if (attrElt.hasAttribute("name")) {
              String aName = attrElt.getAttribute("name");
              if (aName.equals("label")) {
                String label = attrElt.getAttribute("val");
                if (validLabels.containsKey(label)) {
                  attrElt.setAttribute("val", validLabels.get(label));
                }
              }
            }
          }
        }
      }
    }
  }

  public static final Logger logger = LoggerFactory.getLogger(XmlReader.class);

  private LibraryLoader loader;

  /**
   * Path of the source file -- it is used to make the paths of the components stored in the file
   * absolute, to prevent the system looking for them in some strange directories.
   */
  private String srcFilePath;

  XmlReader(Loader loader, File file) {
    this.loader = loader;
    if (file != null) this.srcFilePath = file.getAbsolutePath();
    else this.srcFilePath = null;
  }

  private void addToLabelMap(
      HashMap<String, String> labelMap, String srcLabel, String dstLabel, String toolNames) {
    if (srcLabel != null && dstLabel != null) {
      for (String tool : toolNames.split(";")) {
        labelMap.put(srcLabel + ":" + tool, dstLabel);
      }
    }
  }

  private void considerRepairs(Document doc, Element root) {
    LogisimVersion version = LogisimVersion.parse(root.getAttribute("source"));
    if (version.compareTo(LogisimVersion.get(2, 3, 0)) < 0) {
      // This file was saved before an Edit tool existed. Most likely
      // we should replace the Select and Wiring tools in the toolbar
      // with the Edit tool instead.
      for (Element toolbar : XmlIterator.forChildElements(root, "toolbar")) {
        Element wiring = null;
        Element select = null;
        Element edit = null;
        for (Element elt : XmlIterator.forChildElements(toolbar, "tool")) {
          String eltName = elt.getAttribute("name");
          if (eltName != null && !eltName.equals("")) {
            if (eltName.equals("Select Tool")) select = elt;
            if (eltName.equals("Wiring Tool")) wiring = elt;
            if (eltName.equals("Edit Tool")) edit = elt;
          }
        }
        if (select != null && wiring != null && edit == null) {
          select.setAttribute("name", "Edit Tool");
          toolbar.removeChild(wiring);
        }
      }
    }
    if (version.compareTo(LogisimVersion.get(2, 6, 3)) < 0) {
      for (Element circElt : XmlIterator.forChildElements(root, "circuit")) {
        for (Element attrElt : XmlIterator.forChildElements(circElt, "a")) {
          String name = attrElt.getAttribute("name");
          if (name != null && name.startsWith("label")) {
            attrElt.setAttribute("name", "c" + name);
          }
        }
      }

      repairForWiringLibrary(doc, root);
      repairForLegacyLibrary(doc, root);
    }
  }

  private Document loadXmlFrom(InputStream is) throws SAXException, IOException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    try {
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    } catch (ParserConfigurationException ex) {
      // All implementations are required to support FEATURE_SECURE_PROCESSING.
    }
    DocumentBuilder builder = null;
    try {
      builder = factory.newDocumentBuilder();
    } catch (ParserConfigurationException ex) {
    }
    return builder.parse(is);
  }

  LogisimFile readLibrary(InputStream is, Project proj) throws IOException, SAXException {
    Document doc = loadXmlFrom(is);
    Element elt = doc.getDocumentElement();
    elt = ensureLogisimCompatibility(elt);

    considerRepairs(doc, elt);
    LogisimFile file = new LogisimFile((Loader) loader);
    ReadContext context = new ReadContext(file);

    context.toLogisimFile(elt, proj);

    if (file.getCircuitCount() == 0) {
      file.addCircuit(new Circuit("main", file, proj));
    }
    if (context.messages.size() > 0) {
      StringBuilder all = new StringBuilder();
      for (String msg : context.messages) {
        all.append(msg);
        all.append("\n");
      }
      loader.showError(all.substring(0, all.length() - 1));
    }
    return file;
  }

  private void relocateTools(Element src, Element dest, HashMap<String, String> labelMap) {
    if (src == null || src == dest) return;
    String srcLabel = src.getAttribute("name");
    if (srcLabel == null) return;

    ArrayList<Element> toRemove = new ArrayList<Element>();
    for (Element elt : XmlIterator.forChildElements(src, "tool")) {
      String name = elt.getAttribute("name");
      if (name != null && labelMap.containsKey(srcLabel + ":" + name)) {
        toRemove.add(elt);
      }
    }
    for (Element elt : toRemove) {
      src.removeChild(elt);
      if (dest != null) {
        dest.appendChild(elt);
      }
    }
  }

  private void repairForLegacyLibrary(Document doc, Element root) {
    Element legacyElt = null;
    String legacyLabel = null;
    for (Element libElt : XmlIterator.forChildElements(root, "lib")) {
      String desc = libElt.getAttribute("desc");
      String label = libElt.getAttribute("name");
      if (desc != null && desc.equals("#Legacy")) {
        legacyElt = libElt;
        legacyLabel = label;
      }
    }

    if (legacyElt != null) {
      root.removeChild(legacyElt);

      ArrayList<Element> toRemove = new ArrayList<Element>();
      findLibraryUses(toRemove, legacyLabel, XmlIterator.forDescendantElements(root, "comp"));
      boolean componentsRemoved = !toRemove.isEmpty();
      findLibraryUses(toRemove, legacyLabel, XmlIterator.forDescendantElements(root, "tool"));
      for (Element elt : toRemove) {
        elt.getParentNode().removeChild(elt);
      }
      if (componentsRemoved) {
        String error =
            "Some components have been deleted;" + " the Legacy library is no longer supported.";
        Element elt = doc.createElement("message");
        elt.setAttribute("value", error);
        root.appendChild(elt);
      }
    }
  }

  private void repairForWiringLibrary(Document doc, Element root) {
    Element oldBaseElt = null;
    String oldBaseLabel = null;
    Element gatesElt = null;
    String gatesLabel = null;
    int maxLabel = -1;
    Element firstLibElt = null;
    Element lastLibElt = null;
    for (Element libElt : XmlIterator.forChildElements(root, "lib")) {
      String desc = libElt.getAttribute("desc");
      String label = libElt.getAttribute("name");
      if (desc == null) {
        // skip these tests
      } else if (desc.equals("#Base")) {
        oldBaseElt = libElt;
        oldBaseLabel = label;
      } else if (desc.equals("#Wiring")) {
        // Wiring library already in file. This shouldn't happen, but if
        // somehow it does, we don't want to add it again.
        return;
      } else if (desc.equals("#Gates")) {
        gatesElt = libElt;
        gatesLabel = label;
      }

      if (firstLibElt == null) firstLibElt = libElt;
      lastLibElt = libElt;
      try {
        if (label != null) {
          int thisLabel = Integer.parseInt(label);
          if (thisLabel > maxLabel) maxLabel = thisLabel;
        }
      } catch (NumberFormatException e) {
      }
    }

    Element wiringElt;
    String wiringLabel;
    Element newBaseElt;
    String newBaseLabel;
    if (oldBaseElt != null) {
      wiringLabel = oldBaseLabel;
      wiringElt = oldBaseElt;
      wiringElt.setAttribute("desc", "#Wiring");

      newBaseLabel = "" + (maxLabel + 1);
      newBaseElt = doc.createElement("lib");
      newBaseElt.setAttribute("desc", "#Base");
      newBaseElt.setAttribute("name", newBaseLabel);
      root.insertBefore(newBaseElt, lastLibElt.getNextSibling());
    } else {
      wiringLabel = "" + (maxLabel + 1);
      wiringElt = doc.createElement("lib");
      wiringElt.setAttribute("desc", "#Wiring");
      wiringElt.setAttribute("name", wiringLabel);
      root.insertBefore(wiringElt, lastLibElt.getNextSibling());

      newBaseLabel = null;
      newBaseElt = null;
    }

    HashMap<String, String> labelMap = new HashMap<String, String>();
    addToLabelMap(
        labelMap,
        oldBaseLabel,
        newBaseLabel,
        "Poke Tool;" + "Edit Tool;Select Tool;Wiring Tool;Text Tool;Menu Tool;Text");
    addToLabelMap(
        labelMap,
        oldBaseLabel,
        wiringLabel,
        "Splitter;Pin;" + "Probe;Tunnel;Clock;Pull Resistor;Bit Extender");
    addToLabelMap(labelMap, gatesLabel, wiringLabel, "Constant");
    relocateTools(oldBaseElt, newBaseElt, labelMap);
    relocateTools(oldBaseElt, wiringElt, labelMap);
    relocateTools(gatesElt, wiringElt, labelMap);
    updateFromLabelMap(XmlIterator.forDescendantElements(root, "comp"), labelMap);
    updateFromLabelMap(XmlIterator.forDescendantElements(root, "tool"), labelMap);
  }

  private void updateFromLabelMap(Iterable<Element> elts, HashMap<String, String> labelMap) {
    for (Element elt : elts) {
      String oldLib = elt.getAttribute("lib");
      String name = elt.getAttribute("name");
      if (oldLib != null && name != null) {
        String newLib = labelMap.get(oldLib + ":" + name);
        if (newLib != null) {
          elt.setAttribute("lib", newLib);
        }
      }
    }
  }
}
