/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.memory;

import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.std.memory.Mem.MemListener;

public class RamState extends MemState implements AttributeListener {

  private Instance parent;
  private final MemListener listener;
  private ClockState[] clockState = {new ClockState(), new ClockState()};
  private long portB = -1;

  RamState(Instance parent, MemContents contents, MemListener listener) {
    super(contents);
    this.parent = parent;
    this.listener = listener;
    if (parent != null) {
      parent.getAttributeSet().addAttributeListener(this);
    }
    contents.addHexModelListener(listener);
  }

  public MemState getPortBState() {
    /* TODO: This is probably not the best way to be able
     *       to display the memory contents for the second
     *       port of the dual-ported ram component.
     */
    return this.clone();
  }
  
  @Override
  public void attributeValueChanged(AttributeEvent e) {
    AttributeSet attrs = e.getSource();
    BitWidth addrBits = attrs.getValue(Mem.ADDR_ATTR);
    BitWidth dataBits = attrs.getValue(Mem.DATA_ATTR);
    getContents().setDimensions(addrBits.getWidth(), dataBits.getWidth());
  }

  @Override
  public RamState clone() {
    RamState ret = (RamState) super.clone();
    ret.parent = null;
    ret.clockState = this.clockState.clone();
    ret.getContents().addHexModelListener(listener);
    return ret;
  }

  public boolean setClock(int clockIndex, Value newClock, Object trigger) {
    return (clockIndex < 0 || clockIndex > 1) ? false : clockState[clockIndex].updateClock(newClock, trigger);
  }
  
  public boolean setClock(Value newClock, Object trigger) {
    return setClock(0, newClock, trigger);
  }

  void setRam(Instance value) {
    if (parent == value) {
      return;
    }
    if (parent != null) {
      parent.getAttributeSet().removeAttributeListener(this);
    }
    parent = value;
    if (value != null) {
      value.getAttributeSet().addAttributeListener(this);
    }
  }
  
  long getCurrent(int index) {
    return (index == 1) ? portB : getCurrent();
  }

  void setCurrent(int index, long value) {
    if (index == 1) {
      portB = value;
    } else {
      setCurrent(value);
    }
  }
}
