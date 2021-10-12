/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.comp;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.EventSourceWeakSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class ManagedComponent extends AbstractComponent {
  private final EventSourceWeakSupport<ComponentListener> listeners =
      new EventSourceWeakSupport<>();
  private final Location loc;
  private AttributeSet attrs;
  private final ArrayList<EndData> ends;
  private final List<EndData> endsView;
  private Bounds bounds = null;

  public ManagedComponent(Location loc, AttributeSet attrs, int num_ends) {
    this.loc = loc;
    this.attrs = attrs;
    this.ends = new ArrayList<>(num_ends);
    this.endsView = Collections.unmodifiableList(ends);
  }

  @Override
  public void addComponentListener(ComponentListener l) {
    listeners.add(l);
  }

  //
  // methods for altering data
  //
  public void clearManager() {
    for (final var end : ends) {
      fireEndChanged(new ComponentEvent(this, end, null));
    }
    ends.clear();
    bounds = null;
  }

  //
  // user interface methods
  //
  @Override
  public void expose(ComponentDrawContext context) {
    final var bds = getBounds();
    final var dest = context.getDestination();
    if (bds != null) {
      dest.repaint(bds.getX() - 5, bds.getY() - 5, bds.getWidth() + 10, bds.getHeight() + 10);
    }
  }

  protected void fireComponentInvalidated(ComponentEvent e) {
    for (final var l : listeners) {
      l.componentInvalidated(e);
    }
  }

  protected void fireEndChanged(ComponentEvent e) {
    ComponentEvent copy = null;
    for (final var l : listeners) {
      if (copy == null) {
        copy =
            new ComponentEvent(
                e.getSource(),
                Collections.singletonList(e.getOldData()),
                Collections.singletonList(e.getData()));
      }
      l.endChanged(copy);
    }
  }

  protected void fireEndsChanged(List<EndData> oldEnds, List<EndData> newEnds) {
    ComponentEvent e = null;
    for (final var l : listeners) {
      if (e == null) e = new ComponentEvent(this, oldEnds, newEnds);
      l.endChanged(e);
    }
  }

  @Override
  public AttributeSet getAttributeSet() {
    return attrs;
  }

  @Override
  public Bounds getBounds() {
    if (bounds == null) {
      final var loc = getLocation();
      final var offBounds = getFactory().getOffsetBounds(getAttributeSet());
      bounds = offBounds.translate(loc.getX(), loc.getY());
    }
    return bounds;
  }

  public int getEndCount() {
    return ends.size();
  }

  public Location getEndLocation(int i) {
    return getEnd(i).getLocation();
  }

  @Override
  public List<EndData> getEnds() {
    return endsView;
  }

  //
  // abstract AbstractComponent methods
  //
  @Override
  public abstract ComponentFactory getFactory();

  public Object getFeature(Object key) {
    return null;
  }

  @Override
  public Location getLocation() {
    return loc;
  }

  @Override
  public abstract void propagate(CircuitState state);

  protected void recomputeBounds() {
    bounds = null;
  }

  @Override
  public void removeComponentListener(ComponentListener l) {
    listeners.remove(l);
  }

  public void removeEnd(int index) {
    ends.remove(index);
  }

  public void setAttributeSet(AttributeSet value) {
    attrs = value;
  }

  public void setBounds(Bounds bounds) {
    this.bounds = bounds;
  }

  public void setEnd(int i, EndData data) {
    if (i == ends.size()) {
      ends.add(data);
      fireEndChanged(new ComponentEvent(this, null, data));
    } else {
      final var old = ends.get(i);
      if (old == null || !old.equals(data)) {
        ends.set(i, data);
        fireEndChanged(new ComponentEvent(this, old, data));
      }
    }
  }

  public void setEnd(int i, Location end, BitWidth width, int type) {
    setEnd(i, new EndData(end, width, type));
  }

  public void setEnd(int i, Location end, BitWidth width, int type, boolean exclusive) {
    setEnd(i, new EndData(end, width, type, exclusive));
  }

  public void setEnds(EndData[] newEnds) {
    final var oldEnds = ends;
    final var minLen = Math.min(oldEnds.size(), newEnds.length);
    final var changesOld = new ArrayList<EndData>();
    final var changesNew = new ArrayList<EndData>();
    for (var i = 0; i < minLen; i++) {
      final var old = oldEnds.get(i);
      if (newEnds[i] != null && !newEnds[i].equals(old)) {
        changesOld.add(old);
        changesNew.add(newEnds[i]);
        oldEnds.set(i, newEnds[i]);
      }
    }
    for (var i = oldEnds.size() - 1; i >= minLen; i--) {
      changesOld.add(oldEnds.remove(i));
      changesNew.add(null);
    }
    for (var i = minLen; i < newEnds.length; i++) {
      oldEnds.add(newEnds[i]);
      changesOld.add(null);
      changesNew.add(newEnds[i]);
    }
    fireEndsChanged(changesOld, changesNew);
  }
}
