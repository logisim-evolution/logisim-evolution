/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.util.UnmodifiableList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;
import lombok.val;

class SelectionAttributes extends AbstractAttributeSet {

  private static final Attribute<?>[] EMPTY_ATTRIBUTES = new Attribute<?>[0];
  private static final Object[] EMPTY_VALUES = new Object[0];
  private final Canvas canvas;
  @Getter
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

  private static LinkedHashMap<Attribute<Object>, Object> computeAttributes(Collection<Component> newSel) {
    val attrMap = new LinkedHashMap<Attribute<Object>, Object>();
    val sit = newSel.iterator();
    if (sit.hasNext()) {
      val first = sit.next().getAttributeSet();
      for (val attr : first.getAttributes()) {
        @SuppressWarnings("unchecked")
        Attribute<Object> attrObj = (Attribute<Object>) attr;
        attrMap.put(attrObj, first.getValue(attr));
      }
      while (sit.hasNext()) {
        val next = sit.next().getAttributeSet();
        val ait = attrMap.keySet().iterator();
        while (ait.hasNext()) {
          val attr = ait.next();
          if (next.containsAttribute(attr)) {
            val v = attrMap.get(attr);
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
    for (val comp : sel) {
      val attrs = comp.getAttributeSet();
      if (attrs.isReadOnly(attr)) {
        return true;
      }
    }
    return false;
  }

  private static Set<Component> createSet(Collection<Component> comps) {
    var includeWires = true;
    for (val comp : comps) {
      if (!(comp instanceof Wire)) {
        includeWires = false;
        break;
      }
    }

    if (includeWires) {
      return new HashSet<>(comps);
    } else {
      val ret = new HashSet<Component>();
      for (val comp : comps) {
        if (!(comp instanceof Wire)) {
          ret.add(comp);
        }
      }
      return ret;
    }
  }

  private static boolean haveSameElements(Collection<Component> a, Collection<Component> b) {
    if (a == null) {
      return b == null || b.isEmpty();
    } else if (b == null) {
      return a.isEmpty();
    } else if (a.size() != b.size()) {
      return false;
    } else {
      for (val item : a) {
        if (!b.contains(item)) {
          return false;
        }
      }
      return true;
    }
  }

  private static boolean isSame(LinkedHashMap<Attribute<Object>, Object> attrMap, Attribute<?>[] oldAttrs, Object[] oldValues) {
    if (oldAttrs.length != attrMap.size()) {
      return false;
    } else {
      var j = -1;
      for (val entry : attrMap.entrySet()) {
        j++;

        val a = entry.getKey();
        if (!oldAttrs[j].equals(a) || j >= oldValues.length) {
          return false;
        }
        val ov = oldValues[j];
        val nv = entry.getValue();
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
    val as = attrs;
    for (var i = 0; i < as.length; i++) {
      if (attr.equals(as[i])) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    val circ = canvas.getCircuit();
    return (selected.isEmpty() && circ != null)
        ? circ.getStaticAttributes().getAttributes()
        : attrsView;
  }

  @Override
  public <V> V getValue(Attribute<V> attr) {
    val circ = canvas.getCircuit();
    if (selected.isEmpty() && circ != null) {
      return circ.getStaticAttributes().getValue(attr);
    } else {
      val i = findIndex(attr);
      val vs = values;
      @SuppressWarnings("unchecked")
      V ret = (V) (i >= 0 && i < vs.length ? vs[i] : null);
      return ret;
    }
  }

  @Override
  public boolean isReadOnly(Attribute<?> attr) {
    val proj = canvas.getProject();
    val circ = canvas.getCircuit();
    if (!proj.getLogisimFile().contains(circ)) {
      return true;
    } else if (selected.isEmpty() && circ != null) {
      return circ.getStaticAttributes().isReadOnly(attr);
    } else {
      val i = findIndex(attr);
      val ro = readOnly;
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
    val circ = canvas.getCircuit();
    if (selected.isEmpty() && circ != null) {
      circ.getStaticAttributes().setValue(attr, value);
    } else {
      int i = findIndex(attr);
      val vs = values;
      if (i >= 0 && i < vs.length) {
        vs[i] = value;
        for (val comp : selected) {
          comp.getAttributeSet().setValue(attr, value);
        }
      }
    }
  }

  private void updateList(boolean ignoreIfSelectionSame) {
    val sel = selection;
    val oldSel = selected;
    Set<Component> newSel = (sel == null) ? Collections.emptySet() : createSet(sel.getComponents());
    if (haveSameElements(newSel, oldSel)) {
      if (ignoreIfSelectionSame) {
        return;
      }
      newSel = oldSel;
    } else {
      for (val o : oldSel) {
        if (!newSel.contains(o)) {
          o.getAttributeSet().removeAttributeListener(listener);
        }
      }
      for (val o : newSel) {
        if (!oldSel.contains(o)) {
          o.getAttributeSet().addAttributeListener(listener);
        }
      }
    }

    val attrMap = computeAttributes(newSel);
    val same = isSame(attrMap, this.attrs, this.values);

    if (same) {
      if (newSel != oldSel) {
        this.selected = newSel;
      }
    } else {
      val oldAttrs = this.attrs;
      val oldValues = this.values;
      Attribute<?>[] newAttrs = new Attribute[attrMap.size()];
      val newValues = new Object[newAttrs.length];
      val newReadOnly = new boolean[newAttrs.length];
      int i = -1;
      for (val entry : attrMap.entrySet()) {
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
          val oldVal = oldValues[i];
          val newVal = newValues[i];
          val sameVals = Objects.equals(oldVal, newVal);
          if (!sameVals) {
            @SuppressWarnings("unchecked")
            Attribute<Object> attr = (Attribute<Object>) oldAttrs[i];
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
