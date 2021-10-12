/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.tcl;

import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.util.SocketClient;
import com.cburch.logisim.util.UniquelyNamedThread;
import java.util.LinkedList;

public class TclWrapperListenerThread extends UniquelyNamedThread {

  final SocketClient socket;
  final LinkedList<String> messages;
  final Simulator sim;

  Boolean socketOpen = true;

  TclWrapperListenerThread(SocketClient socket, Simulator simulator) {
    super("TclWrapperListenerThread");
    this.socket = socket;
    this.messages = new LinkedList<>();
    this.sim = simulator;
  }

  /**
   * Get message from TCL wrapper Messages ar in the lister buffer Read is blocking, unblocks if
   * socket closes
   *
   * @return The next message
   */
  public String receive() {

    while (socketOpen && messages.size() < 1) {
      try {
        sleep(100, 0);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    return messages.isEmpty() ? null : messages.pop();
  }

  @Override
  public void run() {
    String line;

    /* Continuously receive TCL wrapper messages */
    while ((line = socket.receive()) != null) {

      /* Stock the messages in temp buffer or tick simulation if asked */
      if (line.equals("run")) {
        sim.tick(1);
      } else {
        messages.add(line);
      }
    }

    socketOpen = false;
  }
}
