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
  private ClockState clockState;

  RamState(Instance parent, MemContents contents, MemListener listener) {
    super(contents);
    this.parent = parent;
    this.listener = listener;
    this.clockState = new ClockState();
    if (parent != null) {
      parent.getAttributeSet().addAttributeListener(this);
    }
    contents.addHexModelListener(listener);
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

  public boolean setClock(Value newClock, Object trigger) {
    return clockState.updateClock(newClock, trigger);
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
}
