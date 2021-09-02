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

import com.cburch.draw.model.AbstractCanvasObject;
import com.cburch.logisim.LogisimVersion;
import com.cburch.logisim.Main;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeDefaultProvider;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.data.MapComponent;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.base.Text;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.util.InputEventUtil;
import com.cburch.logisim.util.StringUtil;
import com.cburch.logisim.vhdl.base.VhdlContent;
import java.io.File;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

class XmlWriter {

  /* We sort some parts of the xml tree, to help with reproducibility and to
   * ease testing (e.g. diff a circuit file). Attribute name=value pairs seem
   * to be sorted already, so we don't worry about those. The code below sorts
   * the nodes, but only in best-effort fashion (some nodes are identical
   * except for their child contents, which seems overkill to bother sorting).
   * Parts of the tree where node order matters (top-level "project", the
   * libraries, and the toolbar, for example) are not sorted.
   */

  static String attrToString(Attr a) {
    String n = a.getName();
    String v = a.getValue().replaceAll("&", "&amp;").replaceAll("\"", "&quot;");
    return n + "=\"" + v + "\"";
  }

  static String attrsToString(NamedNodeMap a) {
    int n = a.getLength();
    if (n == 0) return "";
    else if (n == 1) return attrToString((Attr) a.item(0));
    ArrayList<String> lst = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      lst.add(attrToString((Attr) a.item(i)));
    }
    Collections.sort(lst);
    return String.join(" ", lst);
  }

  private static final int stringCompare(String stringA, String stringB) {
    if (stringA == null) return -1;
    if (stringB == null) return 1;
    return stringA.compareTo(stringB);
  }
  
  private static final Comparator<Node> nodeComparator =
      (nodeA, nodeB) -> {
        var compareResult = stringCompare(nodeA.getNodeName(), nodeB.getNodeName());
        if (compareResult != 0) return compareResult;
        compareResult = stringCompare(attrsToString(nodeA.getAttributes()), attrsToString(nodeB.getAttributes()));
        if (compareResult != 0) return compareResult;
        return stringCompare(nodeA.getNodeValue(), nodeB.getNodeValue());
      };

  static void sort(Node top) {
    final var children = top.getChildNodes();
    final var childrenCount = children.getLength();
    final var name = top.getNodeName();
    // project (contains ordered elements, do not sort)
    // - main
    // - toolbar (contains ordered elements, do not sort)
    //   - tool(s)
    //     - a(s)
    // - lib(s) (contains orderd elements, do not sort)
    //   - tool(s)
    //     - a(s)
    // - options
    //   - a(s)
    // - circuit(s)
    //   - a(s)
    //   - comp(s)
    //   - wire(s)
    if (name.equals("appear")) { 
      // the appearance section only has to sort the circuit ports, the rest is static.
      final var circuitPortIndexes = new ArrayList<Integer>(); 
      for (var nodeIndex = 0; nodeIndex < childrenCount; nodeIndex++) 
        if (children.item(nodeIndex).getNodeName().equals("circ-port")) circuitPortIndexes.add(nodeIndex);
      if (circuitPortIndexes.isEmpty()) return;
      final var numberOfPorts = circuitPortIndexes.size();
      final var nodeSet = new Node[numberOfPorts];
      for (var portIndex = 0; portIndex < numberOfPorts; portIndex++) 
        nodeSet[portIndex] = children.item(circuitPortIndexes.get(portIndex));
      Arrays.sort(nodeSet, nodeComparator);
      for (var portIndex = 0; portIndex < numberOfPorts; portIndex++) top.insertBefore(nodeSet[portIndex], null);
      return;
    }
    if (childrenCount > 1 && !name.equals("project") && !name.equals("lib") && !name.equals("toolbar")) {
      final var nodeSet = new Node[childrenCount];
      for (var nodeIndex = 0; nodeIndex < childrenCount; nodeIndex++) nodeSet[nodeIndex] = children.item(nodeIndex);
      Arrays.sort(nodeSet, nodeComparator);
      for (var nodeIndex = 0; nodeIndex < childrenCount; nodeIndex++) top.insertBefore(nodeSet[nodeIndex], null);
    }
    for (var childId = 0; childId < childrenCount; childId++) {
      sort(children.item(childId));
    }
  }

  static void write(LogisimFile file, OutputStream out, LibraryLoader loader, File destFile)
      throws ParserConfigurationException, TransformerException {

    final var docFactory = DocumentBuilderFactory.newInstance();
    final var docBuilder = docFactory.newDocumentBuilder();

    final var doc = docBuilder.newDocument();
    XmlWriter context;
    if (destFile != null) {
      var dstFilePath = destFile.getAbsolutePath();
      dstFilePath = dstFilePath.substring(0, dstFilePath.lastIndexOf(File.separator));
      context = new XmlWriter(file, doc, loader, dstFilePath);
    } else context = new XmlWriter(file, doc, loader);

    context.fromLogisimFile();

    final var tfFactory = TransformerFactory.newInstance();
    try {
      tfFactory.setAttribute("indent-number", 2);
    } catch (IllegalArgumentException ignored) {
    }
    final var tf = tfFactory.newTransformer();
    tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
    tf.setOutputProperty(OutputKeys.INDENT, "yes");
    try {
      tf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
    } catch (IllegalArgumentException ignored) {
    }

    doc.normalize();
    sort(doc);
    Source src = new DOMSource(doc);
    Result dest = new StreamResult(out);
    tf.transform(src, dest);
  }

  private final LogisimFile file;
  private final Document doc;
  /**
   * Path of the file which is being written on disk -- used to relativize components stored in it.
   */
  private final String outFilepath;

  private final LibraryLoader loader;
  private final HashMap<Library, String> libs = new HashMap<>();

  private XmlWriter(LogisimFile file, Document doc, LibraryLoader loader) {
    this(file, doc, loader, null);
  }

  private XmlWriter(LogisimFile file, Document doc, LibraryLoader loader, String outFilepath) {
    this.file = file;
    this.doc = doc;
    this.loader = loader;
    this.outFilepath = outFilepath;
  }

  void addAttributeSetContent(Element elt, AttributeSet attrs, AttributeDefaultProvider source) {
    if (attrs == null) return;
    LogisimVersion ver = Main.VERSION;
    if (source != null && source.isAllDefaultValues(attrs, ver)) return;
    for (Attribute<?> attrBase : attrs.getAttributes()) {
      @SuppressWarnings("unchecked")
      Attribute<Object> attr = (Attribute<Object>) attrBase;
      Object val = attrs.getValue(attr);
      if (attrs.isToSave(attr) && val != null) {
        Object dflt = source == null ? null : source.getDefaultAttributeValue(attr, ver);
        if (dflt == null || !dflt.equals(val) || attr.equals(StdAttr.APPEARANCE)) {
          final var a = doc.createElement("a");
          a.setAttribute("name", attr.getName());
          var value = attr.toStandardString(val);
          if (attr.getName().equals("filePath") && outFilepath != null) {
            final var outFP = Paths.get(outFilepath);
            final var attrValP = Paths.get(value);
            value = (outFP.relativize(attrValP)).toString();
            a.setAttribute("val", value);
          } else {
            if (value.contains("\n")) {
              a.appendChild(doc.createTextNode(value));
            } else {
              a.setAttribute("val", attr.toStandardString(val));
            }
          }
          elt.appendChild(a);
        }
      }
    }
  }

  Library findLibrary(ComponentFactory source) {
    if (file.contains(source)) {
      return file;
    }
    for (final var lib : file.getLibraries()) {
      if (lib.contains(source)) return lib;
    }
    return null;
  }

  Library findLibrary(Tool tool) {
    if (libraryContains(file, tool)) {
      return file;
    }
    for (final var lib : file.getLibraries()) {
      if (libraryContains(lib, tool)) return lib;
    }
    return null;
  }

  Element fromCircuit(Circuit circuit) {
    final var ret = doc.createElement("circuit");
    ret.setAttribute("name", circuit.getName());
    addAttributeSetContent(ret, circuit.getStaticAttributes(), null);
    if (!circuit.getAppearance().isDefaultAppearance()) {
      final var appear = doc.createElement("appear");
      for (Object o : circuit.getAppearance().getObjectsFromBottom()) {
        if (o instanceof AbstractCanvasObject) {
          final var elt = ((AbstractCanvasObject) o).toSvgElement(doc);
          if (elt != null) {
            appear.appendChild(elt);
          }
        }
      }
      ret.appendChild(appear);
    }
    for (final var wire : circuit.getWires()) {
      ret.appendChild(fromWire(wire));
    }
    for (final var comp : circuit.getNonWires()) {
      final var elt = fromComponent(comp);
      if (elt != null) ret.appendChild(elt);
    }
    for (final var board : circuit.getBoardMapNamestoSave()) {
      final var elt = fromMap(circuit, board);
      if (elt != null) ret.appendChild(elt);
    }
    return ret;
  }

  Element fromVhdl(VhdlContent vhdl) {
    vhdl.aboutToSave();
    final var ret = doc.createElement("vhdl");
    ret.setAttribute("name", vhdl.getName());
    ret.setTextContent(vhdl.getContent());
    return ret;
  }

  Element fromMap(Circuit circ, String boardName) {
    Element ret = doc.createElement("boardmap");
    ret.setAttribute("boardname", boardName);
    for (String key : circ.getMapInfo(boardName).keySet()) {
      final var map = doc.createElement("mc");
      final var mapInfo = circ.getMapInfo(boardName).get(key);
      if (mapInfo.isOldFormat()) {
        map.setAttribute("key", key);
        if (mapInfo.isOpen()) {
          map.setAttribute(MapComponent.OPEN_KEY, MapComponent.OPEN_KEY);
        } else if (mapInfo.isConst()) {
          map.setAttribute(MapComponent.CONSTANT_KEY, Long.toString(mapInfo.getConstValue()));
        } else {
          final var rect = mapInfo.getRectangle();
          map.setAttribute("valx", Integer.toString(rect.getXpos()));
          map.setAttribute("valy", Integer.toString(rect.getYpos()));
          map.setAttribute("valw", Integer.toString(rect.getWidth()));
          map.setAttribute("valh", Integer.toString(rect.getHeight()));
        }
      } else {
        final var nmap = mapInfo.getMap();
        if (nmap != null)
          nmap.getMapElement(map);
        else {
          map.setAttribute("key", key);
          MapComponent.getComplexMap(map, mapInfo);
        }
      }
      ret.appendChild(map);
    }
    return ret;
  }

  Element fromComponent(Component comp) {
    final var source = comp.getFactory();
    final var lib = findLibrary(source);
    String lib_name;
    if (lib == null) {
      loader.showError(source.getName() + " component not found");
      return null;
    } else if (lib == file) {
      lib_name = null;
    } else {
      lib_name = libs.get(lib);
      if (lib_name == null) {
        loader.showError("unknown library within file");
        return null;
      }
    }
    if (source.getName().equals("Text")) {
      /* check if the text element is empty, in this case we do not save */
      final var value = comp.getAttributeSet().getValue(Text.ATTR_TEXT);
      if (value.isEmpty()) return null;
    }

    final var ret = doc.createElement("comp");
    if (lib_name != null) ret.setAttribute("lib", lib_name);
    ret.setAttribute("name", source.getName());
    ret.setAttribute("loc", comp.getLocation().toString());
    addAttributeSetContent(ret, comp.getAttributeSet(), comp.getFactory());
    return ret;
  }

  Element fromLibrary(Library lib) {
    final var ret = doc.createElement("lib");
    if (libs.containsKey(lib)) return null;
    final var name = "" + libs.size();
    final var desc = loader.getDescriptor(lib);
    if (desc == null) {
      loader.showError("library location unknown: " + lib.getName());
      return null;
    }
    libs.put(lib, name);
    ret.setAttribute("name", name);
    ret.setAttribute("desc", desc);
    for (Tool t : lib.getTools()) {
      final var attrs = t.getAttributeSet();
      if (attrs != null) {
        final var toAdd = doc.createElement("tool");
        toAdd.setAttribute("name", t.getName());
        addAttributeSetContent(toAdd, attrs, t);
        if (toAdd.getChildNodes().getLength() > 0) {
          ret.appendChild(toAdd);
        }
      }
    }
    return ret;
  }

  Element fromLogisimFile() {
    final var ret = doc.createElement("project");
    doc.appendChild(ret);
    ret.appendChild(
        doc.createTextNode(
            "\nThis file is intended to be "
                + "loaded by "
                + Main.APP_NAME
                + "("
                + Main.APP_URL
                + ").\n"));
    ret.setAttribute("version", "1.0");
    ret.setAttribute("source", Main.VERSION.toString());

    for (final var lib : file.getLibraries()) {
      final var elt = fromLibrary(lib);
      if (elt != null) ret.appendChild(elt);
    }

    if (file.getMainCircuit() != null) {
      final var mainElt = doc.createElement("main");
      mainElt.setAttribute("name", file.getMainCircuit().getName());
      ret.appendChild(mainElt);
    }

    ret.appendChild(fromOptions());
    ret.appendChild(fromMouseMappings());
    ret.appendChild(fromToolbarData());

    for (final var circ : file.getCircuits()) {
      ret.appendChild(fromCircuit(circ));
    }
    for (final var vhdl : file.getVhdlContents()) {
      ret.appendChild(fromVhdl(vhdl));
    }
    return ret;
  }

  Element fromMouseMappings() {
    final var elt = doc.createElement("mappings");
    final var map = file.getOptions().getMouseMappings();
    for (final var entry : map.getMappings().entrySet()) {
      final var mods = entry.getKey();
      final var tool = entry.getValue();
      final var toolElt = fromTool(tool);
      final var mapValue = InputEventUtil.toString(mods);
      toolElt.setAttribute("map", mapValue);
      elt.appendChild(toolElt);
    }
    return elt;
  }

  Element fromOptions() {
    final var elt = doc.createElement("options");
    addAttributeSetContent(elt, file.getOptions().getAttributeSet(), null);
    return elt;
  }

  Element fromTool(Tool tool) {
    final var lib = findLibrary(tool);
    String lib_name;
    if (lib == null) {
      loader.showError(StringUtil.format("tool `%s' not found", tool.getDisplayName()));
      return null;
    } else if (lib == file) {
      lib_name = null;
    } else {
      lib_name = libs.get(lib);
      if (lib_name == null) {
        loader.showError("unknown library within file");
        return null;
      }
    }

    final var elt = doc.createElement("tool");
    if (lib_name != null) elt.setAttribute("lib", lib_name);
    elt.setAttribute("name", tool.getName());
    addAttributeSetContent(elt, tool.getAttributeSet(), tool);
    return elt;
  }

  Element fromToolbarData() {
    final var elt = doc.createElement("toolbar");
    final var toolbar = file.getOptions().getToolbarData();
    for (final var tool : toolbar.getContents()) {
      if (tool == null) {
        elt.appendChild(doc.createElement("sep"));
      } else {
        elt.appendChild(fromTool(tool));
      }
    }
    return elt;
  }

  Element fromWire(Wire w) {
    final var ret = doc.createElement("wire");
    ret.setAttribute("from", w.getEnd0().toString());
    ret.setAttribute("to", w.getEnd1().toString());
    return ret;
  }

  boolean libraryContains(Library lib, Tool query) {
    for (final var tool : lib.getTools()) {
      if (tool.sharesSource(query)) return true;
    }
    return false;
  }
}
