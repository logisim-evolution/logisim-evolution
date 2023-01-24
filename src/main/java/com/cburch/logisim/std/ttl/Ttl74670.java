/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;

/**
 * TTL 74x670: 4-by-4 register file with 3-state outputs
 * Model based on <a href="https://www.ti.com/lit/ds/symlink/sn74ls670.pdf">74LS670 datasheet</a>.
 */
public class Ttl74670 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   * Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74670";

  public static final int DELAY = 1;

  // IC pin indices as specified in the datasheet

  // Inputs
  public static final byte D1 = 15;
  public static final byte D2 = 1;
  public static final byte D3 = 2;
  public static final byte D4 = 3;

  public static final byte WA0 = 14;
  public static final byte WA1 = 13;

  public static final byte RA0 = 5;
  public static final byte RA1 = 4;

  public static final byte nWE = 12;
  public static final byte nOE = 11;

  // Outputs
  public static final byte Q1 = 10;
  public static final byte Q2 = 9;
  public static final byte Q3 = 7;
  public static final byte Q4 = 6;

  // Power supply
  public static final byte GND = 8;
  public static final byte VCC = 16;

  private static final byte[] DATA_OUTPUTS = new byte[] { Q1, Q2, Q3, Q4 };

  private InstanceState _state;
  private TtlRegisterData _data;

  public Ttl74670() {
    super(
            _ID,
            (byte) 16,
            DATA_OUTPUTS,
            new String[] {
              "D2", "D3", "D4", "RA1", "RA0", "Q4", "Q3",
              "Q2", "Q1", "nOE", "nWE", "WA1", "WA0", "D1"
            },
            null);
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    super.paintBase(painter, true, false);
    Drawgates.paintPortNames(painter, x, y, height, portNames);
  }

  /** IC pin indices are datasheet based (1-indexed), but ports are 0-indexed
   *
   * @param dsPinNr datasheet pin number
   * @return port number
   */
  private byte pinNrToPortNr(byte dsPinNr) {
    return (byte) ((dsPinNr <= GND) ? dsPinNr - 1 : dsPinNr - 2);
  }

  /** Gets the current state of the specified pin
   *
   * @param dsPinNr datasheet pin number
   * @return the current state of the specified pin
   */
  private Value getPort(byte dsPinNr) {
    return _state.getPortValue(pinNrToPortNr(dsPinNr));
  }

  /** Sets the specified pin to the specified value
   *
   * @param dsPinNr datasheet pin number
   * @param v the value for the pin
   */
  private void setPort(byte dsPinNr, Value v) {
    _state.setPort(pinNrToPortNr(dsPinNr), v, DELAY);
  }

  /**
   * Gets the instance data
   *
   * @return the instance data
   */
  private TtlRegisterData getData() {
    var data = (TtlRegisterData) _state.getData();

    if (data == null) {
      data = new TtlRegisterData(BitWidth.create(4), 4);
      _state.setData(data);
    }

    return data;
  }

  /**
   * Calculate the read/write address
   *
   * @param msbPinNr the pin number of the most significant bit
   * @param lsbPinNr the pin number of the least significant bit
   * @return the address
   */
  private int getAddress(byte msbPinNr, byte lsbPinNr) {
    return ((getPort(msbPinNr) == Value.TRUE) ? 2 : 0)
         + ((getPort(lsbPinNr) == Value.TRUE) ? 1 : 0);
  }

  /**
   * Calculate the read-address based on the levels on RA0 and RA1
   *
   * @return the read-address
   */
  private int getReadAddress() {
    return getAddress(RA1, RA0);
  }

  /**
   * Calculate the write-address based on the levels on WA0 and WA1
   *
   * @return the write-address
   */
  private int getWriteAddress() {
    return getAddress(WA1, WA0);
  }

  /**
   * Predicate which is true when the clock was triggered for the selected address.
   *
   * @return true when the clock was triggered
   */
  private boolean isTriggered() {
    return _data.updateClock(getPort(nWE), getWriteAddress(), StdAttr.TRIG_LOW);
  }

  /**
   * Propagate data from the data inputs to internal memory
   */
  private void propagateWritePort() {
    if (isTriggered()) {
      for (var i = 0; i < 4; i++) {
        _data.setValue(
            getWriteAddress(),
            Value.create(new Value[] { getPort(D1), getPort(D2), getPort(D3), getPort(D4) }));
      }
    }
  }

  /**
   * Propagate data from internal memory to the data outputs
   */
  private void propagateReadPort() {
    var readEnabled = getPort(nOE) == Value.FALSE;  // nOE is active low
    var readData = _data.getValue(getReadAddress());

    for (var i = 0; i < 4; i++) {
      setPort(DATA_OUTPUTS[i], readEnabled ? readData.get(i) : Value.UNKNOWN);
    }
  }

  @Override
  public void propagateTtl(InstanceState state) {
    _state = state;
    _data = getData();

    // Must write before read in order to simulate the internal latches
    propagateWritePort();
    propagateReadPort();
  }
}
