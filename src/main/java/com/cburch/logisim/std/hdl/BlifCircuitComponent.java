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

import com.cburch.hdl.HdlModel;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.icons.ArithmeticIcon;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceState;

/**
 * Represents a BLIF circuit.
 */
public class BlifCircuitComponent extends HdlCircuitComponent<BlifContentComponent> {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "BLIFCircuit";

  public static final Attribute<BlifContentComponent> CONTENT_ATTR = new HdlContentAttribute<>(BlifContentComponent::create);

  public BlifCircuitComponent() {
    super(_ID, S.getter("blifComponent"), null, false, CONTENT_ATTR);
    this.setIcon(new ArithmeticIcon("BLIF"));
  }

  @Override
  public AttributeSet createAttributeSet() {
    return new BlifCircuitAttributes();
  }

  @Override
  public void propagate(InstanceState state) {
    // get the circuit, make sure it matches, etc.
    final var content = state.getAttributeValue(contentAttr);
    // can't do anything if it didn't compile...
    if (content.compiled == null)
      return;
    BlifCircuitState id = (BlifCircuitState) state.getData();
    if (id == null || id.circuit != content.compiled) {
      id = new BlifCircuitState(content.compiled);
      state.setData(id);
    }
    // this can somehow get out of sync, so give it a bit
    if (state.getInstance().getPorts().size() != content.inputs.length + content.outputs.length)
      return;

    // alright, load in inputs
    loadInInputs(0, content.inputs, content.compiledInputPinsX, id, state);
    // In a rather 'at the last minute' change, outputs were made outputs and inputs were made inputs.
    // This is because the propagation model suggested by SimpleGrayCounter suggests that feedback would occur if all pins were bi-directional.
    // Still, the support is left in the design in case it is needed.
    // loadInInputs(content.inputs.length, content.outputs, content.compiledOutputPinsX, id, state);

    id.circuit.simulate(id.cells, id.auxData);

    // read out outputs
    // readOutOutputs(0, content.inputs, content.compiledInputPinsO, id, state);
    readOutOutputs(content.inputs.length, content.outputs, content.compiledOutputPinsO, id, state);
  }

  private void loadInInputs(int base, HdlModel.PortDescription[] set, int[][] pinX, BlifCircuitState id, InstanceState state) {
    for (int i = 0; i < set.length; i++) {
      Value v = state.getPortValue(base + i);
      int width = set[i].getWidthInt();
      for (int j = 0; j < width; j++) {
        int cellId = pinX[i][j];
        if (cellId == -1)
          continue;
        byte b = DenseLogicCircuit.LEV_NONE;
        Value bit = v.get(j);
        if (bit == Value.FALSE)
          b = DenseLogicCircuit.LEV_LOW;
        else if (bit == Value.TRUE)
          b = DenseLogicCircuit.LEV_HIGH;
        else if (bit == Value.ERROR)
          b = DenseLogicCircuit.LEV_ERR;
        id.circuit.setCell(cellId, b, id.cells, id.auxData);
      }
    }
  }

  private void readOutOutputs(int base, HdlModel.PortDescription[] set, int[][] pinO, BlifCircuitState id, InstanceState state) {
    for (int i = 0; i < set.length; i++) {
      Value[] translated = new Value[set[i].getWidthInt()];
      for (int j = 0; j < translated.length; j++) {
        int cellId = pinO[i][j];
        if (cellId == -1) {
          translated[j] = Value.UNKNOWN;
        } else {
          translated[j] = DenseLogicCircuit.LEV_TO_LS[id.cells[cellId]];
        }
      }
      state.setPort(base + i, Value.create(translated), 1);
    }
  }

  public class BlifCircuitState implements InstanceData {
    public final DenseLogicCircuit circuit; 
    public final byte[] cells;
    public final int[] auxData;

    public BlifCircuitState(DenseLogicCircuit circuit) {
      this(circuit, circuit.newCells(), circuit.newAuxData());
    }

    public BlifCircuitState(DenseLogicCircuit circuit, byte[] cells, int[] auxData) {
      this.circuit = circuit;
      this.cells = cells;
      this.auxData = auxData;
    }

    @Override
    public BlifCircuitState clone() {
      return new BlifCircuitState(circuit, cells.clone(), auxData.clone());
    }
  }
}
