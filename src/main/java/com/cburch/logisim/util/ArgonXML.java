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

package com.cburch.logisim.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class ArgonXML {

  private static FileInputStream fileInputStream(String filename) {
    try {
      return new FileInputStream(filename);
    } catch (IOException exception) {
      throw new RuntimeException(exception);
    }
  }

  private static Element rootElement(InputStream inputStream, String rootName) {
    try {
      final var builderFactory = DocumentBuilderFactory.newInstance();
      final var builder = builderFactory.newDocumentBuilder();
      final var document = builder.parse(inputStream);
      final var rootElement = document.getDocumentElement();
      if (!rootElement.getNodeName().equals(rootName))
        throw new RuntimeException("Could not find root node: " + rootName);
      return rootElement;
    } catch (IOException | SAXException | ParserConfigurationException exception) {
      throw new RuntimeException(exception);
    } finally {
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (Exception exception) {
          throw new RuntimeException(exception);
        }
      }
    }
  }

  private final String name;
  private String content;

  private final Map<String, String> nameAttributes = new HashMap<>();

  private final Map<String, ArrayList<ArgonXML>> nameChildren = new HashMap<>();

  private ArgonXML(Element element) {
    this.name = element.getNodeName();
    this.content = element.getTextContent();
    final var namedNodeMap = element.getAttributes();
    int n = namedNodeMap.getLength();
    for (var i = 0; i < n; i++) {
      final var node = namedNodeMap.item(i);
      final var name = node.getNodeName();
      addAttribute(name, node.getNodeValue());
    }
    final var nodes = element.getChildNodes();
    n = nodes.getLength();
    for (var i = 0; i < n; i++) {
      final var node = nodes.item(i);
      int type = node.getNodeType();
      if (type == Node.ELEMENT_NODE) {
        final var child = new ArgonXML((Element) node);
        addChild(node.getNodeName(), child);
      }
    }
  }

  public ArgonXML(InputStream inputStream, String rootName) {
    this(rootElement(inputStream, rootName));
  }

  public ArgonXML(String rootName) {
    this.name = rootName;
  }

  public ArgonXML(String filename, String rootName) {
    this(fileInputStream(filename), rootName);
  }

  public void addAttribute(String name, String value) {
    nameAttributes.put(name, value);
  }

  public void addChild(ArgonXML xml) {
    addChild(xml.name(), xml);
  }

  private void addChild(String name, ArgonXML child) {
    final var children = nameChildren.computeIfAbsent(name, k -> new ArrayList<>());
    children.add(child);
  }

  public void addChildren(ArgonXML... xmls) {
    for (final var xml : xmls) addChild(xml.name(), xml);
  }

  public ArgonXML child(String name) {
    final var child = optChild(name);
    if (child == null) throw new RuntimeException("Could not find child node: " + name);
    return child;
  }

  public ArrayList<ArgonXML> children(String name) {
    final var children = nameChildren.get(name);
    return children == null ? new ArrayList<>() : children;
  }

  public String content() {
    return content;
  }

  public double doubleValue(String name) {
    return Double.parseDouble(optString(name));
  }

  public int integer(String name) {
    return Integer.parseInt(string(name));
  }

  public String name() {
    return name;
  }

  public ArgonXML optChild(String name) {
    final var children = children(name);
    final var n = children.size();
    if (n > 1) throw new RuntimeException("Could not find individual child node: " + name);
    return n == 0 ? null : children.get(0);
  }

  public Double optDouble(String name) {
    final var string = optString(name);
    return string == null ? null : doubleValue(name);
  }

  public Integer optInteger(String name) {
    final var string = optString(name);
    return string == null ? null : integer(name);
  }

  public boolean option(String name) {
    return optChild(name) != null;
  }

  public String optString(String name) {
    return nameAttributes.get(name);
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String string(String name) {
    final var value = optString(name);
    if (value == null) {
      throw new RuntimeException("Could not find attribute: " + name + ", in node: " + this.name);
    }
    return value;
  }
}
