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
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.Softwares;
import java.awt.Dimension;
import java.awt.Insets;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class VhdlContentComponent extends HdlContent {

  public static VhdlContentComponent create() {
    return new VhdlContentComponent();
  }

  private static String loadTemplate() {
    InputStream input = VhdlContentComponent.class.getResourceAsStream(RESOURCE);
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
        Logger.getLogger(VhdlContentComponent.class.getName()).log(Level.SEVERE, null, ex);
      }
    }

    return tmp.toString();
  }

  private static final String RESOURCE = "/resources/logisim/hdl/vhdl.templ";

  private static final String TEMPLATE = loadTemplate();

  protected StringBuilder content;
  protected Port[] inputs;
  protected Port[] outputs;
  protected String name;
  protected String libraries;
  protected String architecture;

  protected VhdlContentComponent() {
    this.parseContent(TEMPLATE);
  }

  @Override
  public VhdlContentComponent clone() {
    try {
      VhdlContentComponent ret = (VhdlContentComponent) super.clone();
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

  public String getArchitecture() {
    if (architecture == null) return "";

    return architecture;
  }

  @Override
  public String getContent() {
    return content.toString();
  }

  public Port[] getInputs() {
    if (inputs == null) return new Port[0];

    return inputs;
  }

  public int getInputsNumber() {
    if (inputs == null) return 0;

    return inputs.length;
  }

  public String getLibraries() {
    if (libraries == null) return "";

    return libraries;
  }

  @Override
  public String getName() {
    if (name == null) return "";

    return name;
  }

  public Port[] getOutputs() {
    if (outputs == null) return new Port[0];

    return outputs;
  }

  public int getOutputsNumber() {
    if (outputs == null) return 0;

    return outputs.length;
  }

  public Port[] getPorts() {
    if (inputs == null || outputs == null) return new Port[0];

    return concat(inputs, outputs);
  }

  public int getPortsNumber() {
    if (inputs == null || outputs == null) return 0;

    return inputs.length + outputs.length;
  }

  public boolean parseContent(String content) {
    final var parser = new VhdlParser(content);
    try {
      parser.parse();
    } catch (Exception ex) {
      OptionPane.showMessageDialog(
          null, ex.getMessage(), S.get("validationParseError"), OptionPane.ERROR_MESSAGE);
      return false;
    }

    name = parser.getName();
    libraries = parser.getLibraries();
    architecture = parser.getArchitecture();

    final var inputsDesc = parser.getInputs();
    final var outputsDesc = parser.getOutputs();
    inputs = new Port[inputsDesc.size()];
    outputs = new Port[outputsDesc.size()];

    for (var i = 0; i < inputsDesc.size(); i++) {
      final var desc = inputsDesc.get(i);
      inputs[i] =
          new Port(
              0,
              (i * VhdlEntityComponent.PORT_GAP) + VhdlEntityComponent.HEIGHT,
              desc.getType(),
              desc.getWidth());
      inputs[i].setToolTip(S.getter(desc.getName()));
    }

    for (int i = 0; i < outputsDesc.size(); i++) {
      final var desc = outputsDesc.get(i);
      outputs[i] =
          new Port(
              VhdlEntityComponent.WIDTH,
              (i * VhdlEntityComponent.PORT_GAP) + VhdlEntityComponent.HEIGHT,
              desc.getType(),
              desc.getWidth());
      outputs[i].setToolTip(S.getter(desc.getName()));
    }

    this.content = new StringBuilder(content);
    fireContentSet();

    return true;
  }

  @Override
  public boolean setContent(String content) {
    final var title = new StringBuilder();
    final var result = new StringBuilder();

    switch (Softwares.validateVhdl(content, title, result)) {
      case Softwares.ERROR:
        final var message = new JTextArea();
        message.setText(result.toString());
        message.setEditable(false);
        message.setLineWrap(false);
        message.setMargin(new Insets(5, 5, 5, 5));

        final var sp = new JScrollPane(message);
        sp.setPreferredSize(new Dimension(700, 400));

        OptionPane.showOptionDialog(
            null,
            sp,
            title.toString(),
            OptionPane.OK_OPTION,
            OptionPane.ERROR_MESSAGE,
            null,
            new String[] {S.get("validationErrorButton")},
            S.get("validationErrorButton"));
        return false;
      case Softwares.ABORT:
        OptionPane.showMessageDialog(
            null, result.toString(), title.toString(), OptionPane.INFORMATION_MESSAGE);
        return false;
      case Softwares.SUCCESS:
        return parseContent(content);
    }

    return false;
  }
}
