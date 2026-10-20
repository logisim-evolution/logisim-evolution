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

  /** Maximum number of independent access ports supported by the RAM and ROM components. */
  public static final int MAX_PORTS = 8;

  private Instance parent;
  private final MemListener listener;
  private ClockState[] clockState = new ClockState[MAX_PORTS];
  /**
   * Last address presented to each port. Port 0 keeps using the address tracked by {@link MemState}
   * so that the memory display keeps scrolling to the first port's address; ports 1..3 store their
   * own last address here (initialized to -1, matching the historical Dual RAM behavior).
   */
  private long[] portCurrent = new long[MAX_PORTS];

  {
    for (var i = 0; i < MAX_PORTS; i++) {
      clockState[i] = new ClockState();
      portCurrent[i] = -1;
    }
  }

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
    ret.clockState = new ClockState[this.clockState.length];
    for (var i = 0; i < this.clockState.length; i++) {
      ret.clockState[i] = this.clockState[i] == null ? new ClockState() : this.clockState[i].clone();
    }
    ret.portCurrent = this.portCurrent.clone();
    ret.getContents().addHexModelListener(listener);
    return ret;
  }

  public boolean setClock(int clockIndex, Value newClock, Object trigger) {
    if (clockIndex < 0 || clockIndex >= clockState.length) {
      return false;
    }
    return clockState[clockIndex].updateClock(newClock, trigger);
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
    listener.setInstance(value);
  }

  long getCurrent(int index) {
    if (index <= 0) {
      return getCurrent();
    }
    return index < portCurrent.length ? portCurrent[index] : -1;
  }

  void setCurrent(int index, long value) {
    if (index <= 0) {
      setCurrent(value);
      return;
    }
    if (index < portCurrent.length) {
      portCurrent[index] = value;
    }
  }
}
