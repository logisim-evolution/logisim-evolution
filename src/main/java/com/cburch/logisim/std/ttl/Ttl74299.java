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
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;

/**
 * TTL 74x299: 8-bit universal shift/storage register with 3-state outputs
 * Model based on <a href="https://www.ti.com/lit/ds/symlink/sn74f299.pdf">74F299 datasheet</a>.
 */
public class Ttl74299 extends AbstractTtlGate {
  enum Mode {
    HOLD,
    SHIFT_RIGHT,
    SHIFT_LEFT,
    LOAD
  }

  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   * Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74299";

  public static final int DELAY = 1;

  // IC pin indices as specified in the datasheet

  // Inputs
  public static final byte S0 = 1;
  public static final byte S1 = 19;

  public static final byte SR = 11;
  public static final byte SL = 18;

  public static final byte nOE1 = 2;
  public static final byte nOE2 = 3;

  public static final byte CLK = 12;
  public static final byte nCLR = 9;

  //Outputs
  public static final byte QA = 8;
  public static final byte QH = 17;

  // Bidirectional
  public static final byte IOA = 7;
  public static final byte IOB = 13;
  public static final byte IOC = 6;
  public static final byte IOD = 14;
  public static final byte IOE = 5;
  public static final byte IOF = 15;
  public static final byte IOG = 4;
  public static final byte IOH = 16;

  // Power supply
  public static final byte GND = 10;
  public static final byte VCC = 20;

  private static final byte[] DATA = new byte[] { IOH, IOG, IOF, IOE, IOD, IOC, IOB, IOA };

  private InstanceState _state;

  public Ttl74299() {
    super(
            _ID,
            (byte) 20,
            new byte[] { QA, QH, IOA, IOB, IOC, IOD, IOE, IOF, IOG, IOH },
            new String[] {
              "S0", "nOE1", "nOE2", "IOG", "IOE", "IOC", "IOA", "QA", "nCLR",
              "SR", "CLK", "IOB", "IOD", "IOF", "IOH", "QH", "SL", "S1"
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
  private ShiftRegisterData getData() {
    var data = (ShiftRegisterData) _state.getData();

    if (data == null) {
      data = new ShiftRegisterData(BitWidth.ONE, 8);
      _state.setData(data);
    }

    return data;
  }

  /**
   * Predicate which is true when the clock was triggered
   *
   * @return true when the clock was triggered
   */
  private boolean isTriggered() {
    return getData().updateClock(getPort(CLK), StdAttr.TRIG_RISING);
  }

  /**
   * Decodes the mode inputs
   *
   * @return the detected mode
   */
  private Mode getMode() {
    final var mode = ((getPort(S1) == Value.TRUE) ? 2 : 0) + ((getPort(S0) == Value.TRUE) ? 1 : 0);

    return Mode.values()[mode];
  }

  /**
   * Predicate which is true when the output buffers are enabled
   *
   * @return true when the output buffers are enabled
   */
  private boolean isOutputEnabled() {
    return (getPort(nOE1) == Value.FALSE)
        && (getPort(nOE2) == Value.FALSE)
        && ((getPort(S0) == Value.FALSE) || (getPort(S1) == Value.FALSE));
  }

  /**
   * Update the state of the internal register
   */
  private void propagateRegister() {
    if (getPort(nCLR) == Value.FALSE) { // CLR is active low and clear is async
      getData().clear();
    } else if (isTriggered()) {
      switch (getMode()) {
        case LOAD:
          for (var i = 0; i < 8; i++) {
            getData().set(i, getPort(DATA[i]));
          }
          break;
        case SHIFT_LEFT:
          getData().pushUp(getPort(SL));
          break;
        case SHIFT_RIGHT:
          getData().pushDown(getPort(SR));
          break;
        case HOLD:
          // Nothing to do
          break;
      }
    }
  }

  /**
   * Drive the output buffers
   */
  private void propagateOutputs() {
    setPort(QA, getData().get(7));  // Most significant bit
    setPort(QH, getData().get(0));  // Least significant bit

    for (var i = 0; i < 8; i++) {
      setPort(DATA[i], isOutputEnabled() ? getData().get(i) : Value.UNKNOWN);
    }
  }

  @Override
  public void propagateTtl(InstanceState state) {
    _state = state;

    propagateRegister();
    propagateOutputs();
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] { pinNrToPortNr(CLK) };
  }
}
