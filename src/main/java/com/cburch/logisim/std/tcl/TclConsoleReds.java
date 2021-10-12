/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.tcl;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.instance.Port;
import java.util.ArrayList;
import java.util.List;

/**
 * This is a static TCL component. It onlyy defines the interface as all other things are defined by
 * the parent class.
 *
 * <p>You can use this as an example to create other static TCL components.
 *
 * <p>You may notice that this class is dynamically loaded. You have to define it int Tcl.java
 * library. If you change the name, older circuits will not be able to load the rightful component,
 * so please don't. But if you need to change the display name, you can do this in the resource
 * files (std.properties).
 */
public class TclConsoleReds extends TclComponent {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "TclConsoleReds";

  public TclConsoleReds() {
    super(_ID, S.getter("tclConsoleReds"));

    List<PortDescription> inputsDesc = new ArrayList<>();
    List<PortDescription> outputsDesc = new ArrayList<>();

    outputsDesc.add(new PortDescription("S0_sti", "output", 1));
    outputsDesc.add(new PortDescription("S1_sti", "output", 1));
    outputsDesc.add(new PortDescription("S2_sti", "output", 1));
    outputsDesc.add(new PortDescription("S3_sti", "output", 1));
    outputsDesc.add(new PortDescription("S4_sti", "output", 1));
    outputsDesc.add(new PortDescription("S5_sti", "output", 1));
    outputsDesc.add(new PortDescription("S6_sti", "output", 1));
    outputsDesc.add(new PortDescription("S7_sti", "output", 1));
    outputsDesc.add(new PortDescription("S8_sti", "output", 1));
    outputsDesc.add(new PortDescription("S9_sti", "output", 1));
    outputsDesc.add(new PortDescription("S10_sti", "output", 1));
    outputsDesc.add(new PortDescription("S11_sti", "output", 1));
    outputsDesc.add(new PortDescription("S12_sti", "output", 1));
    outputsDesc.add(new PortDescription("S13_sti", "output", 1));
    outputsDesc.add(new PortDescription("S14_sti", "output", 1));
    outputsDesc.add(new PortDescription("S15_sti", "output", 1));
    outputsDesc.add(new PortDescription("Val_A_sti", "output", 16));
    outputsDesc.add(new PortDescription("Val_B_sti", "output", 16));
    outputsDesc.add(new PortDescription("rst_o", "output", 1));

    inputsDesc.add(new PortDescription("Hex0_obs", "input", 4));
    inputsDesc.add(new PortDescription("Hex1_obs", "input", 4));
    inputsDesc.add(new PortDescription("L0_obs", "input", 1));
    inputsDesc.add(new PortDescription("L1_obs", "input", 1));
    inputsDesc.add(new PortDescription("L2_obs", "input", 1));
    inputsDesc.add(new PortDescription("L3_obs", "input", 1));
    inputsDesc.add(new PortDescription("L4_obs", "input", 1));
    inputsDesc.add(new PortDescription("L5_obs", "input", 1));
    inputsDesc.add(new PortDescription("L6_obs", "input", 1));
    inputsDesc.add(new PortDescription("L7_obs", "input", 1));
    inputsDesc.add(new PortDescription("L8_obs", "input", 1));
    inputsDesc.add(new PortDescription("L9_obs", "input", 1));
    inputsDesc.add(new PortDescription("L10_obs", "input", 1));
    inputsDesc.add(new PortDescription("L11_obs", "input", 1));
    inputsDesc.add(new PortDescription("L12_obs", "input", 1));
    inputsDesc.add(new PortDescription("L13_obs", "input", 1));
    inputsDesc.add(new PortDescription("L14_obs", "input", 1));
    inputsDesc.add(new PortDescription("L15_obs", "input", 1));
    inputsDesc.add(new PortDescription("Result_A_obs", "input", 16));
    inputsDesc.add(new PortDescription("Result_B_obs", "input", 16));
    inputsDesc.add(new PortDescription("seg7_obs", "input", 8));

    inputsDesc.add(new PortDescription("sysclk_i", "input", 1));
    inputsDesc.add(new PortDescription("rst_in", "input", 1));

    final var inputs = new Port[inputsDesc.size()];
    final var outputs = new Port[outputsDesc.size()];

    for (var i = 0; i < inputsDesc.size(); i++) {
      final var desc = inputsDesc.get(i);
      inputs[i] = new Port(0, (i * PORT_GAP) + HEIGHT, desc.getType(), desc.getWidth());
      inputs[i].setToolTip(S.getter(desc.getName()));
    }

    for (var i = 0; i < outputsDesc.size(); i++) {
      final var desc = outputsDesc.get(i);
      outputs[i] = new Port(WIDTH, (i * PORT_GAP) + HEIGHT, desc.getType(), desc.getWidth());
      outputs[i].setToolTip(S.getter(desc.getName()));
    }

    setPorts(inputs, outputs);
  }

  @Override
  public String getDisplayName() {
    return S.get("tclConsoleReds");
  }
}
