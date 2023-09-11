/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.util.CollectionUtil;
import com.cburch.logisim.util.UnmodifiableList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

class SelectionAttributes extends AbstractAttributeSet {

  private static final Attribute<?>[] EMPTY_ATTRIBUTES = new Attribute<?>[0];
  private static final Object[] EMPTY_VALUES = new Object[0];
  private final Canvas canvas;
  private final Selection selection;
  private final Listener listener;
  private boolean listening;
  private Set<Component> selected;
  private Attribute<?>[] attrs;
  private boolean[] readOnly;
  private Object[] values;
  private List<Attribute<?>> attrsView;

  public SelectionAttributes(Canvas canvas, Selection selection) {
    this.canvas = canvas;
    this.selection = selection;
    this.listener = new Listener();
    this.listening = true;
    this.selected = Collections.emptySet();
    this.attrs = EMPTY_ATTRIBUTES;
    this.values = EMPTY_VALUES;
    this.attrsView = Collections.emptyList();

    selection.addListener(listener);
    updateList(true);
    setListening(true);
  }

  private static LinkedHashMap<Attribute<Object>, Object> computeAttributes(
      Collection<Component> newSel) {
    final var attrMap = new LinkedHashMap<Attribute<Object>, Object>();
    final var sit = newSel.iterator();
    if (sit.hasNext()) {
      final var first = sit.next().getAttributeSet();
      for (Attribute<?> attr : first.getAttributes()) {
        @SuppressWarnings("unchecked")
        final var attrObj = (Attribute<Object>) attr;
        attrMap.put(attrObj, first.getValue(attr));
      }
      while (sit.hasNext()) {
        final var next = sit.next().getAttributeSet();
        final var ait = attrMap.keySet().iterator();
        while (ait.hasNext()) {
          final var attr = ait.next();
          if (next.containsAttribute(attr)) {
            final var v = attrMap.get(attr);
            if (v != null && !v.equals(next.getValue(attr))) {
              attrMap.put(attr, null);
            }
          } else {
            ait.remove();
          }
        }
      }
    }
    return attrMap;
  }

  private static boolean computeReadOnly(Collection<Component> sel, Attribute<?> attr) {
    for (final var comp : sel) {
      final var attrs = comp.getAttributeSet();
      if (attrs.isReadOnly(attr)) {
        return true;
      }
    }
    return false;
  }

  private static Set<Component> createSet(Collection<Component> comps) {
    var includeWires = true;
    for (final var comp : comps) {
      if (!(comp instanceof Wire)) {
        includeWires = false;
        break;
      }
    }

    if (includeWires) {
      return new HashSet<>(comps);
    } else {
      final var ret = new HashSet<Component>();
      for (final var comp : comps) {
        if (!(comp instanceof Wire)) {
          ret.add(comp);
        }
      }
      return ret;
    }
  }

  private static boolean haveSameElements(Collection<Component> a, Collection<Component> b) {
    if (a == null) {
      return CollectionUtil.isNullOrEmpty(b);
    } else if (b == null) {
      return a.isEmpty();
    } else if (a.size() != b.size()) {
      return false;
    } else {
      for (Component item : a) {
        if (!b.contains(item)) {
          return false;
        }
      }
      return true;
    }
  }

  private static boolean isSame(
      LinkedHashMap<Attribute<Object>, Object> attrMap,
      Attribute<?>[] oldAttrs,
      Object[] oldValues) {
    if (oldAttrs.length != attrMap.size()) {
      return false;
    } else {
      int j = -1;
      for (Map.Entry<Attribute<Object>, Object> entry : attrMap.entrySet()) {
        j++;

        final var a = entry.getKey();
        if (!oldAttrs[j].equals(a) || j >= oldValues.length) {
          return false;
        }
        final var ov = oldValues[j];
        final var nv = entry.getValue();
        if (!Objects.equals(ov, nv)) {
          return false;
        }
      }
      return true;
    }
  }

  @Override
  protected void copyInto(AbstractAttributeSet dest) {
    throw new UnsupportedOperationException("SelectionAttributes.copyInto");
  }

  private int findIndex(Attribute<?> attr) {
    if (attr == null) {
      return -1;
    }
    final var as = attrs;
    for (var i = 0; i < as.length; i++) {
      if (attr.equals(as[i])) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    final var circ = canvas.getCircuit();
    return (circ != null && selected.isEmpty())
        ? circ.getStaticAttributes().getAttributes()
        : attrsView;
  }

  public Selection getSelection() {
    return selection;
  }

  @Override
  public <V> V getValue(Attribute<V> attr) {
    final var circ = canvas.getCircuit();
    if (circ != null && selected.isEmpty()) {
      return circ.getStaticAttributes().getValue(attr);
    }

    final var i = findIndex(attr);
    final var vs = values;
    @SuppressWarnings("unchecked")
    V ret = (V) (i >= 0 && i < vs.length ? vs[i] : null);
    return ret;
  }

  @Override
  public boolean isReadOnly(Attribute<?> attr) {
    final var proj = canvas.getProject();
    final var circ = canvas.getCircuit();
    if (!proj.getLogisimFile().contains(circ)) {
      return true;
    } else if (circ != null && selected.isEmpty()) {
      return circ.getStaticAttributes().isReadOnly(attr);
    } else {
      int i = findIndex(attr);
      final var ro = readOnly;
      return i < 0 || i >= ro.length || ro[i];
    }
  }

  @Override
  public boolean isToSave(Attribute<?> attr) {
    return false;
  }

  private void setListening(boolean value) {
    if (listening != value) {
      listening = value;
      if (value) {
        updateList(false);
      }
    }
  }

  @Override
  public <V> void setValue(Attribute<V> attr, V value) {
    final var circ = canvas.getCircuit();
    if (selected.isEmpty() && circ != null) {
      circ.getStaticAttributes().setValue(attr, value);
    } else {
      int i = findIndex(attr);
      final var vs = values;
      if (i >= 0 && i < vs.length) {
        vs[i] = value;
        for (final var comp : selected) {
          comp.getAttributeSet().setValue(attr, value);
        }
      }
    }
  }

  private void updateList(boolean ignoreIfSelectionSame) {
    final var sel = selection;
    final var oldSel = selected;
    Set<Component> newSel = (sel == null)
        ? Collections.emptySet()
        : createSet(sel.getComponents());
    if (haveSameElements(newSel, oldSel)) {
      if (ignoreIfSelectionSame) {
        return;
      }
      newSel = oldSel;
    } else {
      for (Component o : oldSel) {
        if (!newSel.contains(o)) {
          o.getAttributeSet().removeAttributeListener(listener);
        }
      }
      for (Component o : newSel) {
        if (!oldSel.contains(o)) {
          o.getAttributeSet().addAttributeListener(listener);
        }
      }
    }

    final var attrMap = computeAttributes(newSel);
    final var same = isSame(attrMap, this.attrs, this.values);

    if (same) {
      if (newSel != oldSel) {
        this.selected = newSel;
      }
    } else {
      final Attribute<?>[] oldAttrs = this.attrs;
      final var oldValues = this.values;
      final Attribute<?>[] newAttrs = new Attribute[attrMap.size()];
      final var newValues = new Object[newAttrs.length];
      final var newReadOnly = new boolean[newAttrs.length];
      int i = -1;
      for (final var entry : attrMap.entrySet()) {
        i++;
        newAttrs[i] = entry.getKey();
        newValues[i] = entry.getValue();
        newReadOnly[i] = computeReadOnly(newSel, newAttrs[i]);
      }
      if (newSel != oldSel) {
        this.selected = newSel;
      }
      this.attrs = newAttrs;
      this.attrsView = new UnmodifiableList<>(newAttrs);
      this.values = newValues;
      this.readOnly = newReadOnly;

      var listSame = oldAttrs != null && oldAttrs.length == newAttrs.length;
      if (listSame) {
        for (i = 0; i < oldAttrs.length; i++) {
          if (!oldAttrs[i].equals(newAttrs[i])) {
            listSame = false;
            break;
          }
        }
      }
      if (listSame) {
        for (i = 0; i < oldValues.length; i++) {
          final var oldVal = oldValues[i];
          final var newVal = newValues[i];
          final var sameVals = Objects.equals(oldVal, newVal);
          if (!sameVals) {
            @SuppressWarnings("unchecked")
            final var attr = (Attribute<Object>) oldAttrs[i];
            fireAttributeValueChanged(attr, newVal, oldVal);
          }
        }
      } else {
        fireAttributeListChanged();
      }
    }
  }

  private class Listener implements Selection.Listener, AttributeListener {

    @Override
    public void attributeListChanged(AttributeEvent e) {
      if (listening) {
        updateList(false);
      }
    }

    @Override
    public void attributeValueChanged(AttributeEvent e) {
      if (listening) {
        updateList(false);
      }
    }

    @Override
    public void selectionChanged(Selection.Event e) {
      updateList(true);
    }
  }
}
