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
 * TTL 74x194: 4-bit bidirectional universal shift register
 * Model based on <a href="https://www.ti.com/lit/ds/symlink/sn74ls194a.pdf>74LS194 datasheet</a>.
 */
public class Ttl74194 extends AbstractTtlGate {
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
  public static final String _ID = "74194";

  public static final int DELAY = 1;

  // IC pin indices as specified in the datasheet

  // Inputs
  public static final byte S0 = 9;
  public static final byte S1 = 10;

  public static final byte SR = 2;
  public static final byte SL = 7;

  public static final byte CLK = 11;
  public static final byte nCLR = 1;

  public static final byte A = 3;
  public static final byte B = 4;
  public static final byte C = 5;
  public static final byte D = 6;

  // Outputs
  public static final byte QA = 15;
  public static final byte QB = 14;
  public static final byte QC = 13;
  public static final byte QD = 12;

  // Power supply
  public static final byte GND = 8;
  public static final byte VCC = 16;

  private static final byte[] DATA = new byte[] { D, C, B, A };

  private InstanceState _state;

  public Ttl74194() {
    super(
            _ID,
            (byte) 16,
            new byte[] { QA, QB, QC, QD },
            new String[] {
              "nCLR", "SR", "A", "B", "C", "D", "SL",
              "S0", "S1", "CLK", "QD", "QC", "QB", "QA"
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
      data = new ShiftRegisterData(BitWidth.ONE, 4);
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
   * Update the state of the internal register
   */
  private void propagateRegister() {
    if (getPort(nCLR) == Value.FALSE) { // CLR is active low and clear is async
      getData().clear();
    } else if (isTriggered()) {
      switch (getMode()) {
        case LOAD:
          for (var i = 0; i < 4; i++) {
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
    setPort(QA, getData().get(3));  // Most significant bit
    setPort(QB, getData().get(2));
    setPort(QC, getData().get(1));
    setPort(QD, getData().get(0));  // Least significant bit
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
