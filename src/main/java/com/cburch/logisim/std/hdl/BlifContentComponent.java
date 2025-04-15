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
import com.cburch.logisim.std.hdl.VhdlParser.IllegalVhdlContentException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        if (input != null) input.close();
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
    if (inputs == null) return new PortDescription[0];

    return inputs;
  }

  @Override
  public String getName() {
    if (name == null) return "";

    return name;
  }

  @Override
  public PortDescription[] getOutputs() {
    if (outputs == null) return new PortDescription[0];

    return outputs;
  }

  @Override
  public boolean setContent(String content) {
    final var parser = new BlifParser(content);
    try {
      parser.parse();
    } catch (Exception ex) {
      OptionPane.showMessageDialog(
          null, ex.getMessage(), S.get("validationParseError"), OptionPane.ERROR_MESSAGE);
      return false;
    }

    name = parser.getName();

    final var inputsDesc = parser.getInputs();
    final var outputsDesc = parser.getOutputs();
    inputs = inputsDesc.toArray(new PortDescription[0]);
    outputs = outputsDesc.toArray(new PortDescription[0]);
    compiled = parser.compile();

    this.content = new StringBuilder(content);
    fireContentSet();

    return true;
  }

  public void parse() throws IllegalVhdlContentException {
  }
}
