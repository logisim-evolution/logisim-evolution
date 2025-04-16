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
import com.cburch.logisim.gui.generic.OptionPane;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * BlifContentComponent connects the BLIF parser with the rest of the HDL content code.
 */
public class BlifContentComponent extends HdlContent {

  public static BlifContentComponent create() {
    return new BlifContentComponent();
  }

  private static String loadTemplate() {
    InputStream input = BlifContentComponent.class.getResourceAsStream(RESOURCE);
    BufferedReader in = new BufferedReader(new InputStreamReader(input));

    StringBuilder tmp = new StringBuilder();
    String line;

    try {
      while ((line = in.readLine()) != null) {
        tmp.append(line);
        tmp.append(System.getProperty("line.separator"));
      }
    } catch (IOException ex) {
      return "";
    } finally {
      try {
        if (input != null) {
          input.close();
        }
      } catch (IOException ex) {
        Logger.getLogger(BlifContentComponent.class.getName()).log(Level.SEVERE, null, ex);
      }
    }

    return tmp.toString();
  }

  private static final String RESOURCE = "/resources/logisim/hdl/vhdl.templ";

  private static final String TEMPLATE = loadTemplate();

  protected StringBuilder content;
  protected PortDescription[] inputs;
  protected PortDescription[] outputs;
  protected DenseLogicCircuit compiled;
  /**
   * These map the input/output bits to cells.
   * Some values may be -1 (indicating a bit does not exist for some reason).
   */
  protected int[][] compiledInputPinsX;
  protected int[][] compiledInputPinsO;
  protected int[][] compiledOutputPinsX;
  protected int[][] compiledOutputPinsO;
  protected String name;

  protected BlifContentComponent() {
    setContent(TEMPLATE);
  }

  @Override
  public BlifContentComponent clone() {
    try {
      BlifContentComponent ret = (BlifContentComponent) super.clone();
      ret.content = new StringBuilder(this.content);
      return ret;
    } catch (CloneNotSupportedException ex) {
      return this;
    }
  }

  @Override
  public boolean compare(HdlModel model) {
    return compare(model.getContent());
  }

  @Override
  public boolean compare(String value) {
    return content
        .toString()
        .replaceAll("\\r\\n|\\r|\\n", " ")
        .equals(value.replaceAll("\\r\\n|\\r|\\n", " "));
  }

  @Override
  public String getContent() {
    return content.toString();
  }

  @Override
  public PortDescription[] getInputs() {
    return inputs == null ? new PortDescription[0] : inputs;
  }

  @Override
  public String getName() {
    return name == null ? "" : name;
  }

  @Override
  public PortDescription[] getOutputs() {
    return outputs == null ? new PortDescription[0] : outputs;
  }

  @Override
  public boolean setContent(String content) {
    final var parser = new BlifParser(content);
    DenseLogicCircuit compiledCircuit;
    try {
      parser.parse();
      compiledCircuit = parser.compile();
    } catch (Exception ex) {
      // important for debugging!
      ex.printStackTrace();
      OptionPane.showMessageDialog(
          null, ex.getMessage(), S.get("validationParseError"), OptionPane.ERROR_MESSAGE);
      return false;
    }

    name = parser.getName();

    final var inputsDesc = parser.getInputs();
    final var outputsDesc = parser.getOutputs();
    inputs = inputsDesc.toArray(new PortDescription[0]);
    outputs = outputsDesc.toArray(new PortDescription[0]);
    compiled = compiledCircuit;
    compiledInputPinsX = derivePins(parser, compiled, inputs, false);
    compiledInputPinsO = derivePins(parser, compiled, inputs, true);
    compiledOutputPinsX = derivePins(parser, compiled, outputs, false);
    compiledOutputPinsO = derivePins(parser, compiled, outputs, true);

    this.content = new StringBuilder(content);
    fireContentSet();

    return true;
  }

  private int[][] derivePins(BlifParser parser, DenseLogicCircuit compiled,
      PortDescription[] ports, boolean output) {
    int[][] results = new int[ports.length][];
    for (int i = 0; i < ports.length; i++) {
      PortDescription desc = ports[i];
      int[] result = new int[desc.getWidthInt()];
      for (int j = 0; j < result.length; j++) {
        String symbol = parser.getPinDlcSymbol(desc, j, output);
        Integer symRes = compiled.symbolTable.get(symbol);
        if (symRes != null) {
          result[j] = symRes;
        } else {
          // System.out.println("suspicious: " + symbol + " missing");
          result[j] = -1;
        }
      }
      results[i] = result;
    }
    return results;
  }
}
