/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.log;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.data.TestVector;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.util.JFileChoosers;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileWriter;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

class FilePanel extends LogPanel {
  private static final long serialVersionUID = 1L;
  private final Listener listener = new Listener();
  private final JLabel enableLabel = new JLabel();
  private final JButton enableButton = new JButton();
  private final JLabel fileLabel = new JLabel();
  private final JTextField fileField = new JTextField();
  private final JButton selectButton = new JButton();
  private final JCheckBox headerCheckBox = new JCheckBox();
  private final JFileChooser chooser = JFileChoosers.create();

  public FilePanel(LogFrame frame) {
    super(frame);

    chooser.addChoosableFileFilter(chooser.getAcceptAllFileFilter());
    chooser.addChoosableFileFilter(TestVector.FILE_FILTER);
    chooser.setFileFilter(TestVector.FILE_FILTER);

    final var filePanel = new JPanel(new GridBagLayout());
    var gbl = (GridBagLayout) filePanel.getLayout();
    var gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbl.setConstraints(fileLabel, gbc);
    filePanel.add(fileLabel);
    gbc.weightx = 1.0;
    gbl.setConstraints(fileField, gbc);
    filePanel.add(fileField);
    gbc.weightx = 0.0;
    gbl.setConstraints(selectButton, gbc);
    filePanel.add(selectButton);
    fileField.setEditable(false);
    fileField.setEnabled(false);

    setLayout(new GridBagLayout());
    gbl = (GridBagLayout) getLayout();
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.weightx = 1.0;
    gbc.gridy = GridBagConstraints.RELATIVE;
    var glue = new JPanel();
    gbc.weighty = 1.0;
    gbl.setConstraints(glue, gbc);
    add(glue);
    gbc.weighty = 0.0;
    gbl.setConstraints(enableLabel, gbc);
    add(enableLabel);
    gbl.setConstraints(enableButton, gbc);
    add(enableButton);
    glue = new JPanel();
    gbc.weighty = 1.0;
    gbl.setConstraints(glue, gbc);
    add(glue);
    gbc.weighty = 0.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbl.setConstraints(filePanel, gbc);
    add(filePanel);
    gbc.fill = GridBagConstraints.NONE;
    glue = new JPanel();
    gbc.weighty = 1.0;
    gbl.setConstraints(glue, gbc);
    add(glue);
    gbc.weighty = 0.0;
    gbl.setConstraints(headerCheckBox, gbc);
    add(headerCheckBox);
    glue = new JPanel();
    gbc.weighty = 1.0;
    gbl.setConstraints(glue, gbc);
    add(glue);
    gbc.weighty = 0.0;

    enableButton.addActionListener(listener);
    selectButton.addActionListener(listener);
    headerCheckBox.addActionListener(listener);
    modelChanged(null, getModel());
    localeChanged();
  }

  @Override
  public String getHelpText() {
    return S.get("fileHelp");
  }

  @Override
  public String getTitle() {
    return S.get("fileTab");
  }

  @Override
  public void localeChanged() {
    listener.computeEnableItems(getModel());
    fileLabel.setText(S.get("fileLabel") + " ");
    selectButton.setText(S.get("fileSelectButton"));
    headerCheckBox.setText(S.get("fileHeaderCheck"));
  }

  @Override
  public void modelChanged(Model oldModel, Model newModel) {
    if (oldModel != null) oldModel.removeModelListener(listener);
    if (newModel != null) {
      newModel.addModelListener(listener);
      listener.filePropertyChanged(null);
    }
  }

  private class Listener implements ActionListener, Model.Listener {
    @Override
    public void actionPerformed(ActionEvent event) {
      final var src = event.getSource();
      if (src == enableButton) {
        getModel().setFileEnabled(!getModel().isFileEnabled());
      } else if (src == selectButton) {
        final var result = chooser.showSaveDialog(getLogFrame());
        if (result != JFileChooser.APPROVE_OPTION) return;
        final var file = chooser.getSelectedFile();
        if (file.exists() && (!file.canWrite() || file.isDirectory())) {
          OptionPane.showMessageDialog(
              getLogFrame(),
              S.get("fileCannotWriteMessage", file.getName()),
              S.get("fileCannotWriteTitle"),
              OptionPane.OK_OPTION);
          return;
        }
        if (file.exists() && file.length() > 0) {
          final String[] options = {
            S.get("fileOverwriteOption"), S.get("fileAppendOption"), S.get("fileCancelOption"),
          };
          final var option =
              OptionPane.showOptionDialog(
                  getLogFrame(),
                  S.get("fileExistsMessage", file.getName()),
                  S.get("fileExistsTitle"),
                  0,
                  OptionPane.QUESTION_MESSAGE,
                  null,
                  options,
                  options[0]);
          if (option == 0) {
            try {
              FileWriter delete = new FileWriter(file);
              delete.close();
            } catch (IOException ignored) {
            }
          } else if (option == 1) {
            // do nothing
          } else {
            return;
          }
        }
        getModel().setFile(file);
      } else if (src == headerCheckBox) {
        getModel().setFileHeader(headerCheckBox.isSelected());
      }
    }

    private void computeEnableItems(Model model) {
      if (model.isFileEnabled()) {
        enableLabel.setText(S.get("fileEnabled"));
        enableButton.setText(S.get("fileDisableButton"));
      } else {
        enableLabel.setText(S.get("fileDisabled"));
        enableButton.setText(S.get("fileEnableButton"));
      }
    }

    @Override
    public void filePropertyChanged(Model.Event event) {
      final var model = getModel();
      computeEnableItems(model);

      final var file = model.getFile();
      fileField.setText(file == null ? "" : file.getPath());
      enableButton.setEnabled(file != null);

      headerCheckBox.setSelected(model.getFileHeader());
    }
  }
}
