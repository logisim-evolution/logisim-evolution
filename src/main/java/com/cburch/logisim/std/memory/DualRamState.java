/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 * Modified and converted to Dual Port RAM by: abdelrhman alaa
 * GitHub: https://github.com/abdelrhman1040
 * Date: February 2026
 *
 * https://github.com/logisim-evolution/
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

public class DualRamState extends MemState implements AttributeListener {

  private Instance parent;
  private final MemListener listener;
  private ClockState clockState0;
  private ClockState clockState1;

  private MemState stateB;

  DualRamState(Instance parent, MemContents contents, MemListener listener) {
    super(contents);
    this.parent = parent;
    this.listener = listener;

    this.clockState0 = new ClockState();
    this.clockState1 = new ClockState();

    this.stateB = new MemState(contents);

    if (parent != null) {
      parent.getAttributeSet().addAttributeListener(this);
    }
    contents.addHexModelListener(listener);
  }

  public MemState getPortBState() {
    return stateB;
  }

  @Override
  public void attributeValueChanged(AttributeEvent e) {
    AttributeSet attrs = e.getSource();
    BitWidth addrBits = attrs.getValue(Mem.ADDR_ATTR);
    BitWidth dataBits = attrs.getValue(Mem.DATA_ATTR);
    getContents().setDimensions(addrBits.getWidth(), dataBits.getWidth());
  }

  @Override
  public DualRamState clone() {
    DualRamState ret = (DualRamState) super.clone();
    ret.parent = null;
    ret.clockState0 = this.clockState0.clone();
    ret.clockState1 = this.clockState1.clone();
    ret.stateB = this.stateB.clone();
    return ret;
  }

  public boolean setClock(int portIndex, Value newClock, Object trigger) {
    if (portIndex == 0) {
      return clockState0.updateClock(newClock, trigger);
    } else {
      return clockState1.updateClock(newClock, trigger);
    }
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

  @Override
  public long getAddressAt(int x, int y) {
    long addrA = super.getAddressAt(x, y);
    if (addrA != -1)
      return addrA;
    return stateB.getAddressAt(x, y);
  }

  public long getCurrent(int portIndex) {
    if (portIndex == 0) {
      return super.getCurrent();
    }
    return stateB.getCurrent();
  }

  public void setCurrent(int portIndex, long val) {
    if (portIndex == 0) {
      super.setCurrent(val);
    } else {
      stateB.setCurrent(val);
    }
  }

  public void scrollToShow(int portIndex, long addr) {
    if (portIndex == 0)
      super.scrollToShow(addr);
    else
      stateB.scrollToShow(addr);
  }
}
