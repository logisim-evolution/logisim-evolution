/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.file;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XmlIterator<E extends Node> implements Iterable<E>, Iterator<E>, Cloneable {

  private final NodeList list;
  private int index;

  public XmlIterator(NodeList nodes) {
    list = nodes;
    index = 0;
  }

  public static Iterable<Element> forChildElements(Element node) {
    final var nodes = node.getChildNodes();
    final var ret = new ArrayList<Element>();
    for (int i = 0, n = nodes.getLength(); i < n; i++) {
      final var sub = nodes.item(i);
      if (sub.getNodeType() == Node.ELEMENT_NODE) {
        ret.add((Element) sub);
      }
    }
    return ret;
  }

  public static Iterable<Element> forChildElements(Element node, String tagName) {
    final var nodes = node.getChildNodes();
    final var ret = new ArrayList<Element>();
    for (int i = 0, n = nodes.getLength(); i < n; i++) {
      final var sub = nodes.item(i);
      if (sub.getNodeType() == Node.ELEMENT_NODE) {
        final var elt = (Element) sub;
        if (elt.getTagName().equals(tagName)) ret.add(elt);
      }
    }
    return ret;
  }

  public static XmlIterator<Node> forChildren(Element node) {
    return new XmlIterator<>(node.getChildNodes());
  }

  public static Iterable<Element> forDescendantElements(Element node, String tagName) {
    return new XmlIterator<>(node.getElementsByTagName(tagName));
  }

  @Override
  public XmlIterator<E> clone() {
    try {
      @SuppressWarnings("unchecked")
      final var ret = (XmlIterator<E>) super.clone();
      return ret;
    } catch (CloneNotSupportedException e) {
      return this;
    }
  }

  @Override
  public boolean hasNext() {
    return list != null && index < list.getLength();
  }

  @Override
  public Iterator<E> iterator() {
    final var ret = this.clone();
    ret.index = 0;
    return ret;
  }

  @Override
  public E next() {
    Node ret = list.item(index);
    if (ret == null) {
      throw new NoSuchElementException();
    } else {
      index++;
      @SuppressWarnings("unchecked")
      E ret2 = (E) ret;
      return ret2;
    }
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException("XmlChildIterator.remove");
  }
}
