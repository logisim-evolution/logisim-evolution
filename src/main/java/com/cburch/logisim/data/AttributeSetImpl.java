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

package com.cburch.logisim.data;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;

public class AttributeSetImpl extends AbstractAttributeSet {
	private class AttrIterator implements Iterator<Attribute<?>> {
		Node n;

		AttrIterator(Node n) {
			this.n = n;
		}

		public boolean hasNext() {
			return n != null;
		}

		public Attribute<?> next() {
			Node ret = n;
			n = n.next;
			return ret.attr;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	private class AttrList extends AbstractList<Attribute<?>> {
		@Override
		public boolean contains(Object o) {
			return indexOf(o) != -1;
		}

		@Override
		public Attribute<?> get(int i) {
			Node n = head;
			int remaining = i;
			while (remaining != 0 && n != null) {
				n = n.next;
				--remaining;
			}
			if (remaining != 0 || n == null) {
				throw new IndexOutOfBoundsException(i + " not in list " + " ["
						+ count + " elements]");
			}
			return n.attr;
		}

		@Override
		public int indexOf(Object o) {
			Node n = head;
			int ret = 0;
			while (n != null) {
				if (o.equals(n.attr))
					return ret;
				n = n.next;
				++ret;
			}
			return -1;
		}

		@Override
		public Iterator<Attribute<?>> iterator() {
			return new AttrIterator(head);
		}

		@Override
		public int size() {
			return count;
		}
	}

	private static class Node {
		Attribute<?> attr;
		Object value;
		boolean is_read_only;
		Node next;

		Node(Attribute<?> attr, Object value, boolean is_read_only, Node next) {
			this.attr = attr;
			this.value = value;
			this.is_read_only = is_read_only;
			this.next = next;
		}

		Node(Node other) {
			this.attr = other.attr;
			this.value = other.value;
			this.is_read_only = other.is_read_only;
			this.next = other.next;
		}
	}

	private AttrList list = new AttrList();
	private Node head = null;
	private Node tail = null;
	private int count = 0;

	public AttributeSetImpl() {
	}

	public AttributeSetImpl(Attribute<Object>[] attrs, Object[] values) {
		if (attrs.length != values.length) {
			throw new IllegalArgumentException("arrays must have same length");
		}

		for (int i = 0; i < attrs.length; i++) {
			addAttribute(attrs[i], values[i]);
		}
	}

	public <V> void addAttribute(Attribute<? super V> attr, V value) {
		if (attr == null) {
			throw new IllegalArgumentException("Adding null attribute");
		}
		if (findNode(attr) != null) {
			throw new IllegalArgumentException("Attribute " + attr
					+ " already created");
		}

		Node n = new Node(attr, value, false, null);
		if (head == null)
			head = n;
		else
			tail.next = n;
		tail = n;
		++count;
		fireAttributeListChanged();
	}

	@Override
	protected void copyInto(AbstractAttributeSet destObj) {
		AttributeSetImpl dest = (AttributeSetImpl) destObj;
		if (this.head != null) {
			dest.head = new Node(head);
			Node copy_prev = dest.head;
			Node cur = this.head.next;
			while (cur != null) {
				Node copy_cur = new Node(cur);
				copy_prev.next = copy_cur;
				copy_prev = copy_cur;
				cur = cur.next;
			}
			dest.tail = copy_prev;
			dest.count = this.count;
		}
	}

	//
	// private helper methods
	//
	private Node findNode(Attribute<?> attr) {
		for (Node n = head; n != null; n = n.next) {
			if (n.attr.equals(attr))
				return n;
		}
		return null;
	}

	//
	// attribute access methods
	//
	@Override
	public List<Attribute<?>> getAttributes() {
		return list;
	}

	//
	// value access methods
	//
	@Override
	public <V> V getValue(Attribute<V> attr) {
		Node n = findNode(attr);
		if (n == null) {
			throw new IllegalArgumentException("Unknown attribute " + attr);
		}
		@SuppressWarnings("unchecked")
		V ret = (V) n.value;
		return ret;
	}

	//
	// read-only methods
	//
	@Override
	public boolean isReadOnly(Attribute<?> attr) {
		Node n = findNode(attr);
		if (n == null) {
			throw new IllegalArgumentException("Unknown attribute " + attr);
		}
		return n.is_read_only;
	}

	public void removeAttribute(Attribute<?> attr) {
		Node prev = null;
		Node n = head;
		while (n != null) {
			if (n.attr.equals(attr)) {
				if (tail == n)
					tail = prev;
				if (prev == null)
					head = n.next;
				else
					prev.next = n.next;
				--count;
				fireAttributeListChanged();
				return;
			}
			prev = n;
			n = n.next;
		}
		throw new IllegalArgumentException("Attribute " + attr + " absent");
	}

	@Override
	public void setReadOnly(Attribute<?> attr, boolean value) {
		Node n = findNode(attr);
		if (n == null) {
			throw new IllegalArgumentException("Unknown attribute " + attr);
		}
		n.is_read_only = value;
	}

	@Override
	public <V> void setValue(Attribute<V> attr, V value) {
		if (value instanceof String) {
			value = attr.parse((String) value);
		}

		Node n = findNode(attr);
		if (n == null) {
			throw new IllegalArgumentException("Unknown attribute " + attr);
		}
		if (n.is_read_only) {
			throw new IllegalArgumentException("Attribute " + attr
					+ " is read-only");
		}
		if (value.equals(n.value)) {
			; // do nothing - why change what's already there?
		} else {
			@SuppressWarnings("unchecked")
			V oldvalue = (V) n.value;
			n.value = value;
			fireAttributeValueChanged(attr, value, oldvalue);
		}
	}
}
