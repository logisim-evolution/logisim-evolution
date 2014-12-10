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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XmlIterator<E extends Node> implements Iterable<E>, Iterator<E>,
		Cloneable {
	public static Iterable<Element> forChildElements(Element node) {
		NodeList nodes = node.getChildNodes();
		ArrayList<Element> ret = new ArrayList<Element>();
		for (int i = 0, n = nodes.getLength(); i < n; i++) {
			Node sub = nodes.item(i);
			if (sub.getNodeType() == Node.ELEMENT_NODE) {
				ret.add((Element) sub);
			}
		}
		return ret;
	}

	public static Iterable<Element> forChildElements(Element node,
			String tagName) {
		NodeList nodes = node.getChildNodes();
		ArrayList<Element> ret = new ArrayList<Element>();
		for (int i = 0, n = nodes.getLength(); i < n; i++) {
			Node sub = nodes.item(i);
			if (sub.getNodeType() == Node.ELEMENT_NODE) {
				Element elt = (Element) sub;
				if (elt.getTagName().equals(tagName))
					ret.add(elt);
			}
		}
		return ret;
	}

	public static XmlIterator<Node> forChildren(Element node) {
		return new XmlIterator<Node>(node.getChildNodes());
	}

	public static Iterable<Element> forDescendantElements(Element node,
			String tagName) {
		return new XmlIterator<Element>(node.getElementsByTagName(tagName));
	}

	private NodeList list;
	private int index;

	public XmlIterator(NodeList nodes) {
		list = nodes;
		index = 0;
	}

	@Override
	public XmlIterator<E> clone() {
		try {
			@SuppressWarnings("unchecked")
			XmlIterator<E> ret = (XmlIterator<E>) super.clone();
			return ret;
		} catch (CloneNotSupportedException e) {
			return this;
		}
	}

	public boolean hasNext() {
		return list != null && index < list.getLength();
	}

	public Iterator<E> iterator() {
		XmlIterator<E> ret = this.clone();
		ret.index = 0;
		return ret;
	}

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

	public void remove() {
		throw new UnsupportedOperationException("XmlChildIterator.remove");
	}
}
