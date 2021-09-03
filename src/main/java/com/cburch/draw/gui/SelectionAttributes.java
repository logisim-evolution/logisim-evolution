/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 * 
 * https://github.com/logisim-evolution/
 * 
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.gui;

import com.cburch.draw.canvas.Selection;
import com.cburch.draw.canvas.SelectionEvent;
import com.cburch.draw.canvas.SelectionListener;
import com.cburch.draw.model.CanvasObject;
import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class SelectionAttributes extends AbstractAttributeSet {
  private final Selection selection;
  private Listener listener;
  private Map<AttributeSet, CanvasObject> selected;
  private Attribute<?>[] selAttrs;
  private Object[] selValues;
  private List<Attribute<?>> attrsView;

  public SelectionAttributes(Selection selection) {
    this.selection = selection;
    this.listener = new Listener();
    this.selected = Collections.emptyMap();
    this.selAttrs = new Attribute<?>[0];
    this.selValues = new Object[0];
    this.attrsView = List.of(selAttrs);

    selection.addSelectionListener(listener);
    listener.selectionChanged(null);
  }

  private static Object getSelectionValue(Attribute<?> attr, Set<AttributeSet> sel) {
    Object ret = null;
    for (AttributeSet attrs : sel) {
      if (attrs.containsAttribute(attr)) {
        Object val = attrs.getValue(attr);
        if (ret == null) {
          ret = val;
        } else if (val != null && val.equals(ret)) {
          // keep on, making sure everything else matches
        } else {
          return null;
        }
      }
    }
    return ret;
  }

  //
  // AbstractAttributeSet methods
  //
  @Override
  protected void copyInto(AbstractAttributeSet dest) {
    listener = new Listener();
    selection.addSelectionListener(listener);
  }

  public Iterable<Map.Entry<AttributeSet, CanvasObject>> entries() {
    Set<Map.Entry<AttributeSet, CanvasObject>> raw = selected.entrySet();
    List<Map.Entry<AttributeSet, CanvasObject>> ret;
    ret = new ArrayList<>(raw);
    return ret;
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return attrsView;
  }

  @Override
  public <V> V getValue(Attribute<V> attr) {
    Attribute<?>[] attrs = this.selAttrs;
    Object[] values = this.selValues;
    for (int i = 0; i < attrs.length; i++) {
      if (attrs[i] == attr) {
        @SuppressWarnings("unchecked")
        V ret = (V) values[i];
        return ret;
      }
    }
    return null;
  }

  @Override
  public <V> void setValue(Attribute<V> attr, V value) {
    Attribute<?>[] attrs = this.selAttrs;
    Object[] values = this.selValues;
    for (int i = 0; i < attrs.length; i++) {
      if (attrs[i] == attr) {
        boolean same = Objects.equals(value, values[i]);
        if (!same) {
          values[i] = value;
          for (AttributeSet objAttrs : selected.keySet()) {
            objAttrs.setValue(attr, value);
          }
        }
        break;
      }
    }
  }

  private class Listener implements SelectionListener, AttributeListener {
    //
    // AttributeSet listener
    //
    @Override
    public void attributeListChanged(AttributeEvent e) {
      // show selection attributes
      computeAttributeList(selected.keySet());
    }

    @Override
    public void attributeValueChanged(AttributeEvent e) {
      if (selected.containsKey(e.getSource())) {
        @SuppressWarnings("unchecked")
        Attribute<Object> attr = (Attribute<Object>) e.getAttribute();
        Attribute<?>[] attrs = SelectionAttributes.this.selAttrs;
        Object[] values = SelectionAttributes.this.selValues;
        for (int i = 0; i < attrs.length; i++) {
          if (attrs[i] == attr) {
            values[i] = getSelectionValue(attr, selected.keySet());
          }
        }
      }
    }

    private void computeAttributeList(Set<AttributeSet> attrsSet) {
      Set<Attribute<?>> attrSet = new LinkedHashSet<>();
      Iterator<AttributeSet> sit = attrsSet.iterator();
      if (sit.hasNext()) {
        AttributeSet first = sit.next();
        attrSet.addAll(first.getAttributes());
        while (sit.hasNext()) {
          AttributeSet next = sit.next();
          attrSet.removeIf(attr -> !next.containsAttribute(attr));
        }
      }

      Attribute<?>[] attrs = new Attribute[attrSet.size()];
      Object[] values = new Object[attrs.length];
      int i = 0;
      for (Attribute<?> attr : attrSet) {
        attrs[i] = attr;
        values[i] = getSelectionValue(attr, attrsSet);
        i++;
      }
      SelectionAttributes.this.selAttrs = attrs;
      SelectionAttributes.this.selValues = values;
      SelectionAttributes.this.attrsView = List.of(attrs);
      fireAttributeListChanged();
    }

    //
    // SelectionListener
    //
    @Override
    public void selectionChanged(SelectionEvent ex) {
      Map<AttributeSet, CanvasObject> oldSel = selected;
      Map<AttributeSet, CanvasObject> newSel = new HashMap<>();
      for (CanvasObject o : selection.getSelected()) {
        if (o != null) newSel.put(o.getAttributeSet(), o);
      }
      selected = newSel;
      boolean change = false;
      for (AttributeSet attrs : oldSel.keySet()) {
        if (!newSel.containsKey(attrs)) {
          change = true;
          attrs.removeAttributeListener(this);
        }
      }
      for (AttributeSet attrs : newSel.keySet()) {
        if (!oldSel.containsKey(attrs)) {
          change = true;
          attrs.addAttributeListener(this);
        }
      }
      if (change) {
        computeAttributeList(newSel.keySet());
        fireAttributeListChanged();
      }
    }
  }
}
