/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.tcl;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.InstanceStateImpl;
import com.cburch.logisim.util.SocketClient;
import lombok.Getter;

/**
 * The TCL components needs some activity for each instance of component. Here we extend the
 * InstanceComponent for the Tcl components to create those activities
 */
public class TclComponentData implements InstanceData {

  /**
   * Retrieves the state associated with this Tcl console in the circuit state, generating the state
   * if necessary.
   */
  public static TclComponentData get(InstanceState state) {
    TclComponentData ret = (TclComponentData) state.getData();
    if (ret == null) {
      // If it doesn't yet exist, then we'll set it up with our default
      // values and put it into the circuit state so it can be retrieved
      // in future propagations.
      ret = new TclComponentData(state);
      state.setData(ret);
    }
    return ret;
  }

  @Getter private final SocketClient tclClient;
  private TclWrapperListenerThread tclWrapperListenerThread;
  @Getter private final TclWrapper tclWrapper;

  /**
   * Instance state
   */
  @Getter private final InstanceState state;

  private Value prevClockValue = Value.UNKNOWN;

  TclComponentData(InstanceState state) {

    this.state = state;

    tclClient = new SocketClient();
    tclWrapper = new TclWrapper(this);
  }

  @Override
  public Object clone() {
    return null;
  }

  public boolean isConnected() {
    return tclClient.isConnected();
  }

  public boolean isNewTick() {
    var newTick = false;
    var found = false;

    for (final var p : state.getInstance().getPorts()) {
      if (p.getToolTip().equals("sysclk_i")) {
        final var val = state.getPortValue(state.getPortIndex(p));
        newTick = (val != prevClockValue);
        if (newTick) {
          prevClockValue = val;
        }
        found = true;
        break;
      }
    }

    if (!found) {
      throw new UnsupportedOperationException("Could not find the 'sysclock' in the TCL component");
    }
    return newTick;
  }

  public String receive() {
    return tclWrapperListenerThread.receive();
  }

  public void send(String message) {
    tclClient.send(message);
  }

  public void tclWrapperStartCallback() {
    tclClient.start();
    tclWrapperListenerThread =
        new TclWrapperListenerThread(
            tclClient,
            ((InstanceStateImpl) state).getCircuitState().getProject().getSimulator());
    tclWrapperListenerThread.start();
  }
}
