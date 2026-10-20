/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.gui;

import static com.cburch.logisim.fpga.Strings.S;

import com.cburch.logisim.fpga.data.FpgaIoInformationContainer;
import com.cburch.logisim.fpga.data.MapComponent;
import com.cburch.logisim.fpga.data.MapListModel;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.LocaleListener;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class PartialMapDialog extends JDialog implements LocaleListener, ActionListener {
  private static final long serialVersionUID = 1L;

  private final MapListModel.MapInfo mapInfo;
  private final FpgaIoInformationContainer ioComp;
  private ArrayList<Integer> InputMapSet;
  private ArrayList<Integer> OutputMapSet;
  private ArrayList<Integer> IOMapSet;
  private JComboBox<String> InputSingleMultiple;
  private JComboBox<String> OutputSingleMultiple;
  private JComboBox<String> IOSingleMultiple;
  private JComboBox<String> InputMultipleSingle;
  private JComboBox<String> OutputMultipleSingle;
  private JComboBox<String> IOMultipleSingle;
  private ArrayList<JComboBox<String>> InputMultipleMultiple;
  private ArrayList<JComboBox<String>> OutputMultipleMultiple;
  private ArrayList<JComboBox<String>> IOMultipleMultiple;
  private final ArrayList<JLabel> MapTo;
  private final JButton OkButton;
  private final JButton CancelButton;
  private final String actionOk = "Ok";
  private final String actionCancel = "Cancel";

  public PartialMapDialog(MapListModel.MapInfo map, FpgaIoInformationContainer io, JPanel parent) {
    super();
    mapInfo = map;
    ioComp = io;
    setLayout(new BorderLayout());
    setModal(true);
    setResizable(true);
    setLocationRelativeTo(parent);
    setAlwaysOnTop(true);
    MapTo = new ArrayList<>();
    final var content = new JPanel();
    content.setLayout(new BorderLayout());
    var pane = createInputPane();
    if (pane != null) {
      content.add(pane, BorderLayout.NORTH);
    }
    pane = createOutputPane();
    if (pane != null) {
      content.add(pane, BorderLayout.CENTER);
    }
    pane = createIOPane();
    if (pane != null) {
      content.add(pane, BorderLayout.SOUTH);
    }
    final var scroll = new JScrollPane(content);
    scroll.setPreferredSize(new Dimension(AppPreferences.getScaled(450), AppPreferences.getScaled(250)));
    add(scroll, BorderLayout.CENTER);
    final var buttonBar = new JPanel();
    buttonBar.setLayout(new BorderLayout());
    OkButton = new JButton();
    OkButton.setActionCommand(actionOk);
    OkButton.addActionListener(this);
    buttonBar.add(OkButton, BorderLayout.CENTER);
    CancelButton = new JButton();
    CancelButton.setActionCommand(actionCancel);
    CancelButton.addActionListener(this);
    buttonBar.add(CancelButton, BorderLayout.WEST);
    add(buttonBar, BorderLayout.SOUTH);
  }

  public boolean doit() {
    localeChanged();
    setVisible(true);
    dispose();
    return true;
  }

  private JPanel createInputPane() {
    final var map = mapInfo.getMap();
    if (!map.hasInputs()) return null;
    if (mapInfo.getPin() >= 0 && !map.isInput(mapInfo.getPin())) return null;
    if (InputMapSet == null)
      InputMapSet = new ArrayList<>();
    if (ioComp.hasInputs()) InputMapSet.addAll(ioComp.getInputs());
    if (ioComp.hasIoPins()) InputMapSet.addAll(ioComp.getIos());
    if (InputMapSet.isEmpty()) return null;
    final var panel = new JPanel();
    panel.setBorder(BorderFactory.createTitledBorder(S.get("FpgaInputsMap")));
    panel.setLayout(new GridBagLayout());
    final var gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridx = 0;
    gbc.gridy = -1;
    if (map.nrInputs() == 1 || mapInfo.getPin() >= 0) {
      /* single input, multiple maps */
      InputSingleMultiple = new JComboBox<>();
      InputSingleMultiple.addItem(S.get("FpgaNotMapped"));
      for (var integer : InputMapSet) {
        InputSingleMultiple.addItem(ioComp.getDisplayString() + "/" + ioComp.getPinName(integer));
      }
      InputSingleMultiple.addActionListener(this);
      gbc.gridy++;
      panel.add(new JLabel(map.getDisplayString(mapInfo.getPin())), gbc);
      gbc.gridx++;
      final var mapToAdd = new JLabel();
      MapTo.add(mapToAdd);
      panel.add(mapToAdd, gbc);
      gbc.gridx++;
      panel.add(InputSingleMultiple, gbc);
    } else if (InputMapSet.size() == 1) {
      /* single map, multiple inputs */
      InputMultipleSingle = new JComboBox<>();
      InputMultipleSingle.addItem(S.get("FpgaNotMapped"));
      for (var i = 0; i < map.nrInputs(); i++) {
        InputMultipleSingle.addItem(map.getDisplayString(i));
      }
      InputMultipleSingle.addActionListener(this);
      gbc.gridy++;
      panel.add(InputMultipleSingle, gbc);
      gbc.gridx++;
      final var mapToAdd = new JLabel();
      MapTo.add(mapToAdd);
      panel.add(mapToAdd, gbc);
      gbc.gridx++;
      panel.add(new JLabel(ioComp.getDisplayString() + "/" + ioComp.getPinName(InputMapSet.get(0))), gbc);
    } else {
      /* multiple on multiple */
      InputMultipleMultiple = new ArrayList<>();
      for (var i = 0; i < map.nrInputs(); i++) {
        final var sels = new JComboBox<String>();
        sels.addItem(S.get("FpgaNotMapped"));
        for (var integer : InputMapSet)
          sels.addItem(ioComp.getDisplayString() + "/" + ioComp.getPinName(integer));
        sels.addActionListener(this);
        InputMultipleMultiple.add(sels);
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel(map.getDisplayString(i)), gbc);
        gbc.gridx++;
        final var mapToAdd = new JLabel();
        MapTo.add(mapToAdd);
        panel.add(mapToAdd, gbc);
        gbc.gridx++;
        panel.add(sels, gbc);
      }
    }
    return panel;
  }

  private JPanel createOutputPane() {
    final var map = mapInfo.getMap();
    if (!map.hasOutputs()) return null;
    if (mapInfo.getPin() >= 0 && !map.isOutput(mapInfo.getPin())) return null;
    if (OutputMapSet == null)
      OutputMapSet = new ArrayList<>();
    if (ioComp.hasOutputs()) OutputMapSet.addAll(ioComp.getOutputs());
    if (ioComp.hasIoPins()) OutputMapSet.addAll(ioComp.getIos());
    if (OutputMapSet.isEmpty()) return null;
    final var panel = new JPanel();
    panel.setBorder(BorderFactory.createTitledBorder(S.get("FpgaOutputsMap")));
    panel.setLayout(new GridBagLayout());
    final var gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridx = 0;
    gbc.gridy = -1;
    if (map.nrOutputs() == 1 || mapInfo.getPin() >= 0) {
      /* single input, multiple maps */
      OutputSingleMultiple = new JComboBox<>();
      OutputSingleMultiple.addItem(S.get("FpgaNotMapped"));
      for (var integer : OutputMapSet) {
        OutputSingleMultiple.addItem(ioComp.getDisplayString() + "/" + ioComp.getPinName(integer));
      }
      OutputSingleMultiple.addActionListener(this);
      gbc.gridy++;
      panel.add(new JLabel(map.getDisplayString(mapInfo.getPin())), gbc);
      gbc.gridx++;
      final var mapToAdd = new JLabel();
      MapTo.add(mapToAdd);
      panel.add(mapToAdd, gbc);
      gbc.gridx++;
      panel.add(OutputSingleMultiple, gbc);
    } else if (OutputMapSet.size() == 1) {
      /* single map, multiple inputs */
      OutputMultipleSingle = new JComboBox<>();
      OutputMultipleSingle.addItem(S.get("FpgaNotMapped"));
      for (var i = 0; i < map.nrOutputs(); i++) {
        OutputMultipleSingle.addItem(map.getDisplayString(map.nrInputs() + i));
      }
      OutputMultipleSingle.addActionListener(this);
      gbc.gridy++;
      panel.add(OutputMultipleSingle, gbc);
      gbc.gridx++;
      final var mapToAdd = new JLabel();
      MapTo.add(mapToAdd);
      panel.add(mapToAdd, gbc);
      gbc.gridx++;
      panel.add(new JLabel(ioComp.getDisplayString() + "/" + ioComp.getPinName(OutputMapSet.get(0))), gbc);
    } else {
      /* multiple on multiple */
      OutputMultipleMultiple = new ArrayList<>();
      for (var i = 0; i < map.nrOutputs(); i++) {
        final var sels = new JComboBox<String>();
        sels.addItem(S.get("FpgaNotMapped"));
        for (var integer : OutputMapSet)
          sels.addItem(ioComp.getDisplayString() + "/" + ioComp.getPinName(integer));
        sels.addActionListener(this);
        OutputMultipleMultiple.add(sels);
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel(map.getDisplayString(map.nrInputs() + i)), gbc);
        gbc.gridx++;
        final var mapToAdd = new JLabel();
        MapTo.add(mapToAdd);
        panel.add(mapToAdd, gbc);
        gbc.gridx++;
        panel.add(sels, gbc);
      }
    }
    return panel;
  }

  private JPanel createIOPane() {
    final var map = mapInfo.getMap();
    if (!map.hasIos()) return null;
    if (mapInfo.getPin() >= 0 && !map.isIo(mapInfo.getPin())) return null;
    if (IOMapSet == null)
      IOMapSet = new ArrayList<>();
    if (ioComp.hasIoPins()) IOMapSet.addAll(ioComp.getIos());
    if (IOMapSet.isEmpty()) return null;
    final var panel = new JPanel();
    panel.setBorder(BorderFactory.createTitledBorder(S.get("FpgaIOsMap")));
    panel.setLayout(new GridBagLayout());
    final var gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridx = 0;
    gbc.gridy = -1;
    if (map.nrIOs() == 1 || mapInfo.getPin() >= 0) {
      /* single input, multiple maps */
      IOSingleMultiple = new JComboBox<>();
      IOSingleMultiple.addItem(S.get("FpgaNotMapped"));
      for (var integer : IOMapSet) {
        IOSingleMultiple.addItem(ioComp.getDisplayString() + "/" + ioComp.getPinName(integer));
      }
      IOSingleMultiple.addActionListener(this);
      gbc.gridy++;
      panel.add(new JLabel(map.getDisplayString(mapInfo.getPin())), gbc);
      gbc.gridx++;
      final var mapToAdd = new JLabel();
      MapTo.add(mapToAdd);
      panel.add(mapToAdd, gbc);
      gbc.gridx++;
      panel.add(IOSingleMultiple, gbc);
    } else if (IOMapSet.size() == 1) {
      /* single map, multiple inputs */
      IOMultipleSingle = new JComboBox<>();
      IOMultipleSingle.addItem(S.get("FpgaNotMapped"));
      for (var i = 0; i < map.nrIOs(); i++) {
        IOMultipleSingle.addItem(map.getDisplayString(map.nrInputs() + map.nrOutputs() + i));
      }
      IOMultipleSingle.addActionListener(this);
      gbc.gridy++;
      panel.add(IOMultipleSingle, gbc);
      gbc.gridx++;
      final var mapToAdd = new JLabel();
      MapTo.add(mapToAdd);
      panel.add(mapToAdd, gbc);
      gbc.gridx++;
      panel.add(new JLabel(ioComp.getDisplayString() + "/" + ioComp.getPinName(IOMapSet.get(0))), gbc);
    } else {
      /* multiple on multiple */
      IOMultipleMultiple = new ArrayList<>();
      for (var i = 0; i < map.nrIOs(); i++) {
        final var sels = new JComboBox<String>();
        sels.addItem(S.get("FpgaNotMapped"));
        for (var integer : IOMapSet)
          sels.addItem(ioComp.getDisplayString() + "/" + ioComp.getPinName(integer));
        sels.addActionListener(this);
        IOMultipleMultiple.add(sels);
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel(map.getDisplayString(map.nrInputs() + map.nrOutputs() + i)), gbc);
        gbc.gridx++;
        final var mapToAdd = new JLabel();
        MapTo.add(mapToAdd);
        panel.add(mapToAdd, gbc);
        gbc.gridx++;
        panel.add(sels, gbc);
      }
    }
    return panel;
  }

  private void update(JComboBox<String> source) {
    if (source.getSelectedIndex() == 0) return;
    /* pass 1 find the pin */
    var pinid = -1;
    if (source.equals(InputSingleMultiple)) pinid = InputMapSet.get(source.getSelectedIndex() - 1);
    for (var i = 0;
        InputMultipleMultiple != null && pinid < 0 && i < InputMultipleMultiple.size();
        i++) {
      if (source.equals(InputMultipleMultiple.get(i)))
        pinid = InputMapSet.get(InputMultipleMultiple.get(i).getSelectedIndex() - 1);
    }
    if (source.equals(OutputSingleMultiple))
      pinid = OutputMapSet.get(source.getSelectedIndex() - 1);
    for (var i = 0;
        OutputMultipleMultiple != null && pinid < 0 && i < OutputMultipleMultiple.size();
        i++) {
      if (source.equals(OutputMultipleMultiple.get(i)))
        pinid = OutputMapSet.get(OutputMultipleMultiple.get(i).getSelectedIndex() - 1);
    }
    if (source.equals(IOSingleMultiple)) pinid = IOMapSet.get(source.getSelectedIndex() - 1);
    for (var i = 0; IOMultipleMultiple != null && pinid < 0 && i < IOMultipleMultiple.size(); i++) {
      if (source.equals(IOMultipleMultiple.get(i)))
        pinid = IOMapSet.get(IOMultipleMultiple.get(i).getSelectedIndex() - 1);
    }
    /* pass 2 disable the selection for all others */
    if (InputSingleMultiple != null
        && !source.equals(InputSingleMultiple)
        && InputSingleMultiple.getSelectedIndex() != 0) {
      final var selId = InputMapSet.get(InputSingleMultiple.getSelectedIndex() - 1);
      if (pinid == selId) InputSingleMultiple.setSelectedIndex(0);
    }
    for (var i = 0; InputMultipleMultiple != null && i < InputMultipleMultiple.size(); i++) {
      if (source.equals(InputMultipleMultiple.get(i))
          || InputMultipleMultiple.get(i).getSelectedIndex() == 0) continue;
      final var selId = InputMapSet.get(InputMultipleMultiple.get(i).getSelectedIndex() - 1);
      if (pinid == selId) InputMultipleMultiple.get(i).setSelectedIndex(0);
    }
    if (OutputSingleMultiple != null
        && !source.equals(OutputSingleMultiple)
        && OutputSingleMultiple.getSelectedIndex() != 0) {
      final var selId = OutputMapSet.get(OutputSingleMultiple.getSelectedIndex() - 1);
      if (pinid == selId) OutputSingleMultiple.setSelectedIndex(0);
    }
    for (var i = 0; OutputMultipleMultiple != null && i < OutputMultipleMultiple.size(); i++) {
      if (source.equals(OutputMultipleMultiple.get(i))
          || OutputMultipleMultiple.get(i).getSelectedIndex() == 0) continue;
      final var selId = OutputMapSet.get(OutputMultipleMultiple.get(i).getSelectedIndex() - 1);
      if (pinid == selId) OutputMultipleMultiple.get(i).setSelectedIndex(0);
    }
    if (IOSingleMultiple != null
        && !source.equals(IOSingleMultiple)
        && IOSingleMultiple.getSelectedIndex() != 0) {
      final var selId = IOMapSet.get(IOSingleMultiple.getSelectedIndex() - 1);
      if (pinid == selId) IOSingleMultiple.setSelectedIndex(0);
    }
    for (var i = 0; IOMultipleMultiple != null && i < IOMultipleMultiple.size(); i++) {
      if (source.equals(IOMultipleMultiple.get(i))
          || IOMultipleMultiple.get(i).getSelectedIndex() == 0) continue;
      final var selId = IOMapSet.get(IOMultipleMultiple.get(i).getSelectedIndex() - 1);
      if (pinid == selId) IOMultipleMultiple.get(i).setSelectedIndex(0);
    }
  }

  @Override
  public void localeChanged() {
    for (var jLabel : MapTo)
      jLabel.setText(S.get("FpgaMapTo"));
    OkButton.setText(S.get("FpgaBoardDone"));
    CancelButton.setText(S.get("FpgaBoardCancel"));
    pack();
  }

  @SuppressWarnings("unchecked")
  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource().equals(InputMultipleSingle)) {
      if (InputMultipleSingle.getSelectedIndex() != 0) {
        if (OutputMultipleSingle != null) OutputMultipleSingle.setSelectedIndex(0);
        if (IOMultipleSingle != null) IOMultipleSingle.setSelectedIndex(0);
      }
      return;
    }
    if (e.getSource().equals(OutputMultipleSingle)) {
      if (OutputMultipleSingle.getSelectedIndex() != 0) {
        if (InputMultipleSingle != null) InputMultipleSingle.setSelectedIndex(0);
        if (IOMultipleSingle != null) IOMultipleSingle.setSelectedIndex(0);
      }
      return;
    }
    if (e.getSource().equals(IOMultipleSingle)) {
      if (IOMultipleSingle.getSelectedIndex() != 0) {
        if (InputMultipleSingle != null) InputMultipleSingle.setSelectedIndex(0);
        if (OutputMultipleSingle != null) OutputMultipleSingle.setSelectedIndex(0);
      }
      return;
    }
    if (e.getSource() instanceof JComboBox box) {
      update(box);
      return;
    }
    if (e.getActionCommand().equals(actionOk)) {
      MapComponent map = mapInfo.getMap();
      if (InputSingleMultiple != null && InputSingleMultiple.getSelectedIndex() != 0) {
        final var pin = Math.max(mapInfo.getPin(), 0);
        map.unmap(pin);
        map.tryMap(pin, ioComp, InputMapSet.get(InputSingleMultiple.getSelectedIndex() - 1));
      }
      if (InputMultipleSingle != null && InputMultipleSingle.getSelectedIndex() != 0) {
        map.unmap(InputMultipleSingle.getSelectedIndex() - 1);
        map.tryMap(InputMultipleSingle.getSelectedIndex() - 1, ioComp, InputMapSet.get(0));
      }
      for (var i = 0; InputMultipleMultiple != null && i < InputMultipleMultiple.size(); i++) {
        if (InputMultipleMultiple.get(i).getSelectedIndex() == 0) continue;
        final var pinId = InputMapSet.get(InputMultipleMultiple.get(i).getSelectedIndex() - 1);
        map.unmap(i);
        map.tryMap(i, ioComp, pinId);
      }
      if (OutputSingleMultiple != null && OutputSingleMultiple.getSelectedIndex() != 0) {
        final var pin = mapInfo.getPin() < 0 ? map.nrInputs() : mapInfo.getPin();
        map.unmap(pin);
        map.tryMap(pin, ioComp, OutputMapSet.get(OutputSingleMultiple.getSelectedIndex() - 1));
      }
      if (OutputMultipleSingle != null && OutputMultipleSingle.getSelectedIndex() != 0) {
        final var pin = map.nrInputs() + OutputMultipleSingle.getSelectedIndex() - 1;
        map.unmap(pin);
        map.tryMap(pin, ioComp, OutputMapSet.get(0));
      }
      for (var i = 0; OutputMultipleMultiple != null && i < OutputMultipleMultiple.size(); i++) {
        if (OutputMultipleMultiple.get(i).getSelectedIndex() == 0) continue;
        final var pinId = OutputMapSet.get(OutputMultipleMultiple.get(i).getSelectedIndex() - 1);
        map.unmap(map.nrInputs() + i);
        map.tryMap(map.nrInputs() + i, ioComp, pinId);
      }
      if (IOSingleMultiple != null && IOSingleMultiple.getSelectedIndex() != 0) {
        final var pin = mapInfo.getPin() < 0 ? map.nrInputs() + map.nrOutputs() : mapInfo.getPin();
        map.unmap(pin);
        map.tryMap(pin, ioComp, IOMapSet.get(IOSingleMultiple.getSelectedIndex() - 1));
      }
      if (IOMultipleSingle != null && IOMultipleSingle.getSelectedIndex() != 0) {
        final var pin = map.nrInputs() + map.nrOutputs() + IOMultipleSingle.getSelectedIndex() - 1;
        map.unmap(pin);
        map.tryMap(pin, ioComp, IOMapSet.get(0));
      }
      for (var i = 0; IOMultipleMultiple != null && i < IOMultipleMultiple.size(); i++) {
        if (IOMultipleMultiple.get(i).getSelectedIndex() == 0) continue;
        final var pinId = IOMapSet.get(IOMultipleMultiple.get(i).getSelectedIndex() - 1);
        final var pin = map.nrInputs() + map.nrOutputs() + i;
        map.unmap(pin);
        map.tryMap(pin, ioComp, pinId);
      }
    }
    setVisible(false);
  }
}
