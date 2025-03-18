/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.hdl;

import static com.cburch.logisim.vhdl.Strings.S;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.icons.ArithmeticIcon;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.vhdl.base.VhdlSimConstants;
import com.cburch.logisim.vhdl.sim.VhdlSimulatorTop;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VhdlEntityComponent extends HdlCircuitComponent<VhdlContentComponent> {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "VHDL Entity";

  static final Logger logger = LoggerFactory.getLogger(VhdlEntityComponent.class);

  public static final Attribute<VhdlContentComponent> CONTENT_ATTR = new HdlContentAttribute<>(VhdlContentComponent::create);

  public VhdlEntityComponent() {
    super(_ID, S.getter("vhdlComponent"), new VhdlHdlGeneratorFactory(), true, CONTENT_ATTR);

    this.setIcon(new ArithmeticIcon("VHDL"));
  }

  public void setSimName(AttributeSet attrs, String SName) {
    if (attrs == null) return;
    final var atrs = (VhdlEntityAttributes) attrs;
    final var label = (!attrs.getValue(StdAttr.LABEL).equals("")) ? getHDLTopName(attrs) : SName;
    if (atrs.containsAttribute(VhdlSimConstants.SIM_NAME_ATTR))
      atrs.setValue(VhdlSimConstants.SIM_NAME_ATTR, label);
  }

  public String getSimName(AttributeSet attrs) {
    if (attrs == null) return null;
    final var atrs = (VhdlEntityAttributes) attrs;
    return atrs.getValue(VhdlSimConstants.SIM_NAME_ATTR);
  }

  @Override
  public AttributeSet createAttributeSet() {
    return new VhdlEntityAttributes();
  }

  @Override
  public String getHDLName(AttributeSet attrs) {
    return attrs.getValue(CONTENT_ATTR).getName().toLowerCase();
  }

  @Override
  public String getHDLTopName(AttributeSet attrs) {
    var label = "";
    if (!attrs.getValue(StdAttr.LABEL).equals("")) {
      label = "_" + attrs.getValue(StdAttr.LABEL).toLowerCase();
    }

    return getHDLName(attrs) + label;
  }

  /**
   * Propagate signals through the VHDL component. Logisim doesn't have a VHDL simulation tool. So
   * we need to use an external tool. We send signals to Questasim/Modelsim through a socket and a
   * tcl binder. Then, a simulation step is done and the tcl server sends the output signals back to
   * Logisim. Then we can set the VHDL component output properly.
   *
   * <p>This can be done only if Logisim could connect to the tcl server (socket). This is done in
   * Simulation.java.
   */
  @Override
  public void propagate(InstanceState state) {

    if (state.getProject().getVhdlSimulator().isEnabled()
        && state.getProject().getVhdlSimulator().isRunning()) {

      VhdlSimulatorTop vhdlSimulator = state.getProject().getVhdlSimulator();

      for (final var p : state.getInstance().getPorts()) {
        final var index = state.getPortIndex(p);
        final var val = state.getPortValue(index);

        String vhdlEntityName = getSimName(state.getAttributeSet());

        String message =
            p.getType()
                + ":"
                + vhdlEntityName
                + "_"
                + p.getToolTip()
                + ":"
                + val.toBinaryString()
                + ":"
                + index;

        vhdlSimulator.send(message);
      }

      vhdlSimulator.send("sync");

      /* Get response from tcl server */
      String server_response;
      while ((server_response = vhdlSimulator.receive()) != null
          && server_response.length() > 0
          && !server_response.equals("sync")) {

        final var parameters = server_response.split(":");

        final var busValue = parameters[1];

        final var vector_values = new Value[busValue.length()];

        var k = busValue.length() - 1;
        for (final var bit : busValue.toCharArray()) {
          try {
            switch (Character.getNumericValue(bit)) {
              case 0 -> vector_values[k] = Value.FALSE;
              case 1 -> vector_values[k] = Value.TRUE;
              default -> vector_values[k] = Value.UNKNOWN;
            }
          } catch (NumberFormatException e) {
            vector_values[k] = Value.UNKNOWN;
          }
          k--;
        }

        state.setPort(Integer.parseInt(parameters[2]), Value.create(vector_values), 1);
      }

      /* VhdlSimulation stopped/disabled */
    } else {

      for (final var p : state.getInstance().getPorts()) {
        int index = state.getPortIndex(p);

        /* If it is an output */
        if (p.getType() == 2) {
          final var vector_values = new Value[p.getFixedBitWidth().getWidth()];
          for (var k = 0; k < p.getFixedBitWidth().getWidth(); k++) {
            vector_values[k] = Value.UNKNOWN;
          }

          state.setPort(index, Value.create(vector_values), 1);
        }
      }

      throw new UnsupportedOperationException(
          "VHDL component simulation is not supported. This could be because there is no Questasim/Modelsim simulation server running.");
    }
  }

  /**
   * Save the VHDL entity in a file. The file is used for VHDL components simulation by
   * QUestasim/Modelsim
   */
  public void saveFile(AttributeSet attrs) {

    PrintWriter writer;
    try {
      writer =
          new PrintWriter(VhdlSimConstants.SIM_SRC_PATH + getSimName(attrs) + ".vhdl",
              StandardCharsets.UTF_8);

      var content = attrs.getValue(CONTENT_ATTR).getContent()
              .replaceAll("(?i)" + getHDLName(attrs), getSimName(attrs));

      writer.print(content);
      writer.close();
    } catch (IOException e) {
      logger.error("Could not create vhdl file: {}", e.getMessage());
      e.printStackTrace();
    }
  }
}
