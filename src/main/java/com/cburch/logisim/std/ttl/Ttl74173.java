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
 * TTL 74x173: 4-bit D-type registers with 3-state outputs
 * Model based on <a href="https://www.ti.com/lit/ds/symlink/sn74ls173a.pdf">74LS173 datasheet</a>.
 */
public class Ttl74173 extends AbstractTtlGate {
  enum Mode {
    CLEAR,
    HOLD,
    LOAD
  }

  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   * Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74173";

  public static final int DELAY = 1;

  // IC pin indices as specified in the datasheet

  // Inputs
  public static final byte nM = 1;
  public static final byte nN = 2;

  public static final byte nG1 = 9;
  public static final byte nG2 = 10;

  public static final byte D1 = 14;
  public static final byte D2 = 13;
  public static final byte D3 = 12;
  public static final byte D4 = 11;

  public static final byte CLK = 7;
  public static final byte CLR = 15;

  //Outputs
  public static final byte Q1 = 3;
  public static final byte Q2 = 4;
  public static final byte Q3 = 5;
  public static final byte Q4 = 6;

  // Power supply
  public static final byte GND = 8;
  public static final byte VCC = 16;

  private static final byte[] DATA = new byte[] { D4, D3, D2, D1 };
  private static final byte[] OUTPUTS = new byte[] { Q4, Q3, Q2, Q1 };

  public Ttl74173() {
    super(
        _ID,
        (byte) 16,
        OUTPUTS,
        new String[] {
            "nM", "nN", "Q1", "Q2", "Q3", "Q4", "CLK",
            "nG1", "nG2", "D4", "D3", "D2", "D1", "CLR"
        },
        null);
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    super.paintBase(painter, true, false);
    Drawgates.paintPortNames(painter, x, y, height, portNames);
  }

  private record LogicScope(InstanceState state, TtlRegisterData data) {
    LogicScope(InstanceState state) {
      this(state, getData(state));
    }

    /** IC pin indices are datasheet based (1-indexed), but ports are 0-indexed
     *
     * @param dsPinNr datasheet pin number
     * @return port number
     */
    public static byte pinNrToPortNr(byte dsPinNr) {
      return (byte) ((dsPinNr <= GND) ? dsPinNr - 1 : dsPinNr - 2);
    }

    /** Gets the current state of the specified pin
     *
     * @param dsPinNr datasheet pin number
     * @return the current state of the specified pin
     */
    private Value getPort(byte dsPinNr) {
      return state.getPortValue(pinNrToPortNr(dsPinNr));
    }

    /** Sets the specified pin to the specified value
     *
     * @param dsPinNr datasheet pin number
     * @param v the value for the pin
     */
    private void setPort(byte dsPinNr, Value v) {
      state.setPort(pinNrToPortNr(dsPinNr), v, DELAY);
    }

    /**
     * Gets the instance data
     *
     * @return the instance data
     */
    private static TtlRegisterData getData(InstanceState state) {
      var data = (TtlRegisterData) state.getData();

      if (data == null) {
        data = new TtlRegisterData(BitWidth.ONE, DATA.length);
        state.setData(data);
      }

      return data;
    }

    /**
     * Predicate which is true when the clock was triggered
     *
     * @return true when the clock was triggered
     */
    private boolean isTriggered() {
      return data.updateClock(getPort(CLK), StdAttr.TRIG_RISING);
    }

    /**
     * Decodes the mode inputs
     *
     * @return the detected mode
     */
    private Mode getMode() {
      if (getPort(CLR) == Value.TRUE) {
        return Mode.CLEAR;
      } else if ((getPort(nG1) == Value.FALSE) && (getPort(nG2) == Value.FALSE)) {
        return Mode.LOAD;
      }

      return Mode.HOLD;
    }

    /**
     * Predicate which is true when the output buffers are enabled
     *
     * @return true when the output buffers are enabled
     */
    private boolean isOutputEnabled() {
      return (getPort(nM) == Value.FALSE) && (getPort(nN) == Value.FALSE);
    }

    /**
     * Update the state of the internal register
     */
    private void propagateRegister() {
      switch (getMode()) {
        case CLEAR:
          data.clear();
          break;
        case LOAD:
          if (isTriggered()) {
            for (var i = 0; i < DATA.length; i++) {
              data.setValue(i, getPort(DATA[i]));
            }
          }
          break;
        case HOLD:
          // Nothing to do
          break;
      }
    }

    /**
     * Drive the output buffers
     */
    private void propagateOutputs() {
      for (var i = 0; i < OUTPUTS.length; i++) {
        setPort(OUTPUTS[i], isOutputEnabled() ? data.getValue(i) : Value.UNKNOWN);
      }
    }

    public void propagate() {
      propagateRegister();
      propagateOutputs();
    }
  }

  @Override
  public void propagateTtl(InstanceState state) {
    new LogicScope(state).propagate();
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] { LogicScope.pinNrToPortNr(CLK) };
  }
}
