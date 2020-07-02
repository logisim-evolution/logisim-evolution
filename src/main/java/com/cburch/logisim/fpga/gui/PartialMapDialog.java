/**
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.fpga.gui;

import static com.cburch.logisim.fpga.Strings.S;

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

import com.cburch.logisim.fpga.data.FPGAIOInformationContainer;
import com.cburch.logisim.fpga.data.MapComponent;
import com.cburch.logisim.fpga.data.MapListModel;
import com.cburch.logisim.util.LocaleListener;

public class PartialMapDialog extends JDialog implements LocaleListener,ActionListener {
  private static final long serialVersionUID = 1L;
  
  private MapListModel.MapInfo mapInfo;
  private FPGAIOInformationContainer ioComp;
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
  private ArrayList<JLabel> MapTo;
  private JButton OkButton;
  private JButton CancelButton;

  public PartialMapDialog(MapListModel.MapInfo map, FPGAIOInformationContainer io, JPanel parent) {
    super();
    mapInfo = map;
    ioComp = io;
    setLayout(new GridBagLayout());
    setModal(true);
    setResizable(false);
    setLocationRelativeTo(parent);
    MapTo = new ArrayList<JLabel>();
    GridBagConstraints cs = new GridBagConstraints();
    cs.gridx = 0;
    cs.gridy = -1;
    cs.gridwidth = 2;
    cs.fill = GridBagConstraints.HORIZONTAL;
    JPanel pane = createInputPane();
    if (pane != null) {
      cs.gridy++;
      add(pane,cs);
    }
    pane = createOutputPane();
    if (pane != null) {
      cs.gridy++;
      add(pane,cs);
    }
    pane = createIOPane();
    if (pane != null) {
      cs.gridy++;
      add(pane,cs);
    }
    cs.gridwidth = 1;
    cs.gridy++;
    OkButton = new JButton();
    OkButton.setActionCommand("Ok");
    OkButton.addActionListener(this);
    add(OkButton,cs);
    cs.gridx++;
    CancelButton = new JButton();
    CancelButton.setActionCommand("Cancel");
    CancelButton.addActionListener(this);
    add(CancelButton,cs);
  }
  
  public boolean doit() {
    localeChanged();
    setVisible(true);
    dispose();
    return true;
  }
  
  private JPanel createInputPane() {
    MapComponent map = mapInfo.getMap();
    if (!map.hasInputs()) return null;
    if (mapInfo.getPin() >= 0 && !map.isInput(mapInfo.getPin())) return null;
    if (InputMapSet == null)
      InputMapSet = new ArrayList<Integer>();
    if (ioComp.hasInputs()) InputMapSet.addAll(ioComp.getInputs());
    if (ioComp.hasIOs()) InputMapSet.addAll(ioComp.getIOs());
    if (InputMapSet.isEmpty()) return null;
    JPanel panel = new JPanel();
    panel.setBorder(BorderFactory.createTitledBorder(S.get("FpgaInputsMap")));
    panel.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridx = 0;
    gbc.gridy = -1;
    if (map.nrInputs() == 1 || mapInfo.getPin() >= 0) {
      /* single input, multiple maps */
      InputSingleMultiple = new JComboBox<String>();
      InputSingleMultiple.addItem(S.get("FpgaNotMapped"));
      for (int i = 0 ; i < InputMapSet.size() ; i++) {
        InputSingleMultiple.addItem(ioComp.GetDisplayString()+"/"+ioComp.getPinName(InputMapSet.get(i)));
      }
      InputSingleMultiple.addActionListener(this);
      gbc.gridy++;
      panel.add(new JLabel(map.getDisplayString(mapInfo.getPin())), gbc);
      gbc.gridx++;
      JLabel mapToAdd = new JLabel();
      MapTo.add(mapToAdd);
      panel.add(mapToAdd, gbc);
      gbc.gridx++;
      panel.add(InputSingleMultiple, gbc);
    } else if (InputMapSet.size() == 1) {
      /* single map, multiple inputs */
      InputMultipleSingle = new JComboBox<String>();
      InputMultipleSingle.addItem(S.get("FpgaNotMapped"));
      for (int i = 0 ; i < map.nrInputs() ; i++) {
        InputMultipleSingle.addItem(map.getDisplayString(i));
      }
      InputMultipleSingle.addActionListener(this);
      gbc.gridy++;
      panel.add(InputMultipleSingle, gbc);
      gbc.gridx++;
      JLabel mapToAdd = new JLabel();
      MapTo.add(mapToAdd);
      panel.add(mapToAdd, gbc);
      gbc.gridx++;
      panel.add(new JLabel(ioComp.GetDisplayString()+"/"+ioComp.getPinName(InputMapSet.get(0))), gbc);
    } else {
      /* multiple on multiple */
      InputMultipleMultiple = new ArrayList<JComboBox<String>>();
      for (int i = 0 ; i < map.nrInputs() ; i++) {
        JComboBox<String> sels = new JComboBox<String>();
        sels.addItem(S.get("FpgaNotMapped"));
        for (int j = 0 ; j < InputMapSet.size() ; j++)
          sels.addItem(ioComp.GetDisplayString()+"/"+ioComp.getPinName(InputMapSet.get(j)));
        sels.addActionListener(this);
        InputMultipleMultiple.add(sels);
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel(map.getDisplayString(i)),gbc);
        gbc.gridx++;
        JLabel mapToAdd = new JLabel();
        MapTo.add(mapToAdd);
        panel.add(mapToAdd,gbc);
        gbc.gridx++;
        panel.add(sels,gbc);
      }
    }
    return panel;
  }
  
  private JPanel createOutputPane() {
    MapComponent map = mapInfo.getMap();
    if (!map.hasOutputs()) return null;
    if (mapInfo.getPin() >= 0 && !map.isOutput(mapInfo.getPin())) return null;
    if (OutputMapSet == null)
      OutputMapSet = new ArrayList<Integer>();
    if (ioComp.hasOutputs()) OutputMapSet.addAll(ioComp.getOutputs());
    if (ioComp.hasIOs()) OutputMapSet.addAll(ioComp.getIOs());
    if (OutputMapSet.isEmpty()) return null;
    JPanel panel = new JPanel();
    panel.setBorder(BorderFactory.createTitledBorder(S.get("FpgaOutputsMap")));
    panel.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridx = 0;
    gbc.gridy = -1;
    if (map.nrOutputs() == 1 || mapInfo.getPin() >= 0) {
      /* single input, multiple maps */
      OutputSingleMultiple = new JComboBox<String>();
      OutputSingleMultiple.addItem(S.get("FpgaNotMapped"));
      for (int i = 0 ; i < OutputMapSet.size() ; i++) {
        OutputSingleMultiple.addItem(ioComp.GetDisplayString()+"/"+ioComp.getPinName(OutputMapSet.get(i)));
      }
      OutputSingleMultiple.addActionListener(this);
      gbc.gridy++;
      panel.add(new JLabel(map.getDisplayString(mapInfo.getPin())), gbc);
      gbc.gridx++;
      JLabel mapToAdd = new JLabel();
      MapTo.add(mapToAdd);
      panel.add(mapToAdd, gbc);
      gbc.gridx++;
      panel.add(OutputSingleMultiple, gbc);
    } else if (OutputMapSet.size() == 1) {
      /* single map, multiple inputs */
      OutputMultipleSingle = new JComboBox<String>();
      OutputMultipleSingle.addItem(S.get("FpgaNotMapped"));
      for (int i = 0 ; i < map.nrOutputs() ; i++) {
        OutputMultipleSingle.addItem(map.getDisplayString(map.nrInputs()+i));
      }
      OutputMultipleSingle.addActionListener(this);
      gbc.gridy++;
      panel.add(OutputMultipleSingle, gbc);
      gbc.gridx++;
      JLabel mapToAdd = new JLabel();
      MapTo.add(mapToAdd);
      panel.add(mapToAdd, gbc);
      gbc.gridx++;
      panel.add(new JLabel(ioComp.GetDisplayString()+"/"+ioComp.getPinName(OutputMapSet.get(0))), gbc);
    } else {
      /* multiple on multiple */
      OutputMultipleMultiple = new ArrayList<JComboBox<String>>();
      for (int i = 0 ; i < map.nrOutputs() ; i++) {
        JComboBox<String> sels = new JComboBox<String>();
        sels.addItem(S.get("FpgaNotMapped"));
        for (int j = 0 ; j < OutputMapSet.size() ; j++)
          sels.addItem(ioComp.GetDisplayString()+"/"+ioComp.getPinName(OutputMapSet.get(j)));
        sels.addActionListener(this);
        OutputMultipleMultiple.add(sels);
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel(map.getDisplayString(map.nrInputs()+i)),gbc);
        gbc.gridx++;
        JLabel mapToAdd = new JLabel();
        MapTo.add(mapToAdd);
        panel.add(mapToAdd,gbc);
        gbc.gridx++;
        panel.add(sels,gbc);
      }
    }
    return panel;
  }
  
  private JPanel createIOPane() {
    MapComponent map = mapInfo.getMap();
    if (!map.hasIOs()) return null;
    if (mapInfo.getPin() >= 0 && !map.isIO(mapInfo.getPin())) return null;
    if (IOMapSet == null)
      IOMapSet = new ArrayList<Integer>();
    if (ioComp.hasIOs()) IOMapSet.addAll(ioComp.getIOs());
    if (IOMapSet.isEmpty()) return null;
    JPanel panel = new JPanel();
    panel.setBorder(BorderFactory.createTitledBorder(S.get("FpgaIOsMap")));
    panel.setLayout(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridx = 0;
    gbc.gridy = -1;
    if (map.nrIOs() == 1 || mapInfo.getPin() >= 0) {
      /* single input, multiple maps */
      IOSingleMultiple = new JComboBox<String>();
      IOSingleMultiple.addItem(S.get("FpgaNotMapped"));
      for (int i = 0 ; i < IOMapSet.size() ; i++) {
        IOSingleMultiple.addItem(ioComp.GetDisplayString()+"/"+ioComp.getPinName(IOMapSet.get(i)));
      }
      IOSingleMultiple.addActionListener(this);
      gbc.gridy++;
      panel.add(new JLabel(map.getDisplayString(mapInfo.getPin())), gbc);
      gbc.gridx++;
      JLabel mapToAdd = new JLabel();
      MapTo.add(mapToAdd);
      panel.add(mapToAdd, gbc);
      gbc.gridx++;
      panel.add(IOSingleMultiple, gbc);
    } else if (IOMapSet.size() == 1) {
      /* single map, multiple inputs */
      IOMultipleSingle = new JComboBox<String>();
      IOMultipleSingle.addItem(S.get("FpgaNotMapped"));
      for (int i = 0 ; i < map.nrIOs() ; i++) {
        IOMultipleSingle.addItem(map.getDisplayString(map.nrInputs()+map.nrOutputs()+i));
      }
      IOMultipleSingle.addActionListener(this);
      gbc.gridy++;
      panel.add(IOMultipleSingle, gbc);
      gbc.gridx++;
      JLabel mapToAdd = new JLabel();
      MapTo.add(mapToAdd);
      panel.add(mapToAdd, gbc);
      gbc.gridx++;
      panel.add(new JLabel(ioComp.GetDisplayString()+"/"+ioComp.getPinName(IOMapSet.get(0))), gbc);
    } else {
      /* multiple on multiple */
      IOMultipleMultiple = new ArrayList<JComboBox<String>>();
      for (int i = 0 ; i < map.nrIOs() ; i++) {
        JComboBox<String> sels = new JComboBox<String>();
        sels.addItem(S.get("FpgaNotMapped"));
        for (int j = 0 ; j < IOMapSet.size() ; j++)
          sels.addItem(ioComp.GetDisplayString()+"/"+ioComp.getPinName(IOMapSet.get(j)));
        sels.addActionListener(this);
        IOMultipleMultiple.add(sels);
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel(map.getDisplayString(map.nrInputs()+map.nrOutputs()+i)),gbc);
        gbc.gridx++;
        JLabel mapToAdd = new JLabel();
        MapTo.add(mapToAdd);
        panel.add(mapToAdd,gbc);
        gbc.gridx++;
        panel.add(sels,gbc);
      }
    }
    return panel;
  }
  
  private void update(JComboBox<String> source) {
    if (source.getSelectedIndex() == 0) return;
    /* pass 1 find the pin */
    int pinid = -1;
    if (source.equals(InputSingleMultiple))
      pinid = InputMapSet.get(source.getSelectedIndex()-1);
    for (int i = 0 ; InputMultipleMultiple != null && pinid < 0 && i < InputMultipleMultiple.size() ; i++) {
      if (source.equals(InputMultipleMultiple.get(i)))
        pinid = InputMapSet.get(InputMultipleMultiple.get(i).getSelectedIndex()-1);
    }
    if (source.equals(OutputSingleMultiple))
      pinid = OutputMapSet.get(source.getSelectedIndex()-1);
    for (int i = 0 ; OutputMultipleMultiple != null && pinid < 0 && i < OutputMultipleMultiple.size() ; i++) {
      if (source.equals(OutputMultipleMultiple.get(i)))
        pinid = OutputMapSet.get(OutputMultipleMultiple.get(i).getSelectedIndex()-1);
    }
    if (source.equals(IOSingleMultiple))
      pinid = IOMapSet.get(source.getSelectedIndex()-1);
    for (int i = 0 ; IOMultipleMultiple != null && pinid < 0 && i < IOMultipleMultiple.size() ; i++) {
      if (source.equals(IOMultipleMultiple.get(i)))
        pinid = IOMapSet.get(IOMultipleMultiple.get(i).getSelectedIndex()-1);
    }
    /* pass 2 disable the selection for all others */
    if (InputSingleMultiple != null && !source.equals(InputSingleMultiple) && 
        InputSingleMultiple.getSelectedIndex() != 0) {
      int selId = InputMapSet.get(InputSingleMultiple.getSelectedIndex()-1);
      if (pinid == selId) InputSingleMultiple.setSelectedIndex(0);
    }
    for (int i = 0 ; InputMultipleMultiple != null && i < InputMultipleMultiple.size() ; i++) {
      if (source.equals(InputMultipleMultiple.get(i)) || 
          InputMultipleMultiple.get(i).getSelectedIndex() == 0) continue;
      int selId = InputMapSet.get(InputMultipleMultiple.get(i).getSelectedIndex()-1);
      if (pinid == selId) InputMultipleMultiple.get(i).setSelectedIndex(0);
    }
    if (OutputSingleMultiple != null && !source.equals(OutputSingleMultiple) && 
        OutputSingleMultiple.getSelectedIndex() != 0) {
      int selId = OutputMapSet.get(OutputSingleMultiple.getSelectedIndex()-1);
      if (pinid == selId) OutputSingleMultiple.setSelectedIndex(0);
    }
    for (int i = 0 ; OutputMultipleMultiple != null && i < OutputMultipleMultiple.size() ; i++) {
      if (source.equals(OutputMultipleMultiple.get(i)) || 
          OutputMultipleMultiple.get(i).getSelectedIndex() == 0) continue;
      int selId = OutputMapSet.get(OutputMultipleMultiple.get(i).getSelectedIndex()-1);
      if (pinid == selId) OutputMultipleMultiple.get(i).setSelectedIndex(0);
    }
    if (IOSingleMultiple != null && !source.equals(IOSingleMultiple) && 
        IOSingleMultiple.getSelectedIndex() != 0) {
      int selId = IOMapSet.get(IOSingleMultiple.getSelectedIndex()-1);
      if (pinid == selId) IOSingleMultiple.setSelectedIndex(0);
    }
    for (int i = 0 ; IOMultipleMultiple != null && i < IOMultipleMultiple.size() ; i++) {
      if (source.equals(IOMultipleMultiple.get(i)) || 
          IOMultipleMultiple.get(i).getSelectedIndex() == 0) continue;
      int selId = IOMapSet.get(IOMultipleMultiple.get(i).getSelectedIndex()-1);
      if (pinid == selId) IOMultipleMultiple.get(i).setSelectedIndex(0);
    }
  }

  @Override
  public void localeChanged() {
    for (int i = 0 ; i < MapTo.size() ; i++)
      MapTo.get(i).setText(S.get("FpgaMapTo"));
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
    if (e.getSource() instanceof JComboBox) {
      update((JComboBox<String>)e.getSource());
      return;
    }
    if (e.getActionCommand().equals("Ok")) {
      MapComponent map = mapInfo.getMap();
      if (InputSingleMultiple != null && InputSingleMultiple.getSelectedIndex() != 0) {
        int pin = mapInfo.getPin() < 0 ? 0 : mapInfo.getPin();
        map.tryMap(pin, ioComp, InputMapSet.get(InputSingleMultiple.getSelectedIndex()-1));
      }
      if (InputMultipleSingle != null && InputMultipleSingle.getSelectedIndex() != 0) {
      map.tryMap(InputMultipleSingle.getSelectedIndex()-1, ioComp, InputMapSet.get(0));
      }
      for (int i = 0 ; InputMultipleMultiple != null && i < InputMultipleMultiple.size() ; i++) {
        if (InputMultipleMultiple.get(i).getSelectedIndex() == 0) continue;
        int pinId = InputMapSet.get(InputMultipleMultiple.get(i).getSelectedIndex()-1);
        map.tryMap(i,ioComp,pinId);
      }
      if (OutputSingleMultiple != null && OutputSingleMultiple.getSelectedIndex() != 0) {
        int pin = mapInfo.getPin() < 0 ? map.nrInputs() : mapInfo.getPin();
        map.tryMap(pin, ioComp, OutputMapSet.get(OutputSingleMultiple.getSelectedIndex()-1));
      }
      if (OutputMultipleSingle != null && OutputMultipleSingle.getSelectedIndex() != 0) {
        map.tryMap(map.nrInputs() + OutputMultipleSingle.getSelectedIndex() - 1, ioComp, OutputMapSet.get(0));
      }
      for (int i = 0 ; OutputMultipleMultiple != null && i < OutputMultipleMultiple.size() ; i++) {
        if (OutputMultipleMultiple.get(i).getSelectedIndex() == 0) continue;
        int pinId = OutputMapSet.get(OutputMultipleMultiple.get(i).getSelectedIndex()-1);
        map.tryMap(map.nrInputs()+i,ioComp,pinId);
      }
      if (IOSingleMultiple != null && IOSingleMultiple.getSelectedIndex() != 0) {
        int pin = mapInfo.getPin() < 0 ? map.nrInputs()+map.nrOutputs() : mapInfo.getPin();
        map.tryMap(pin, ioComp, IOMapSet.get(IOSingleMultiple.getSelectedIndex()-1));
      }
      if (IOMultipleSingle != null && IOMultipleSingle.getSelectedIndex() != 0) {
        map.tryMap(map.nrInputs()+map.nrOutputs()+IOMultipleSingle.getSelectedIndex() - 1, ioComp, IOMapSet.get(0));
      }
      for (int i = 0 ; IOMultipleMultiple != null && i < IOMultipleMultiple.size() ; i++) {
        if (IOMultipleMultiple.get(i).getSelectedIndex() == 0) continue;
        int pinId = IOMapSet.get(IOMultipleMultiple.get(i).getSelectedIndex()-1);
        map.tryMap(map.nrInputs()+map.nrOutputs()+i,ioComp,pinId);
      }
    }
    setVisible(false);
  }
}
