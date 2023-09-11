/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.analyze.gui;

import static com.cburch.logisim.analyze.Strings.S;

import com.cburch.logisim.analyze.file.AnalyzerTexWriter;
import com.cburch.logisim.analyze.model.AnalyzerModel;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.util.JFileChoosers;
import java.io.File;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;

public class ExportLatexButton extends JButton {
  private static final long serialVersionUID = 1L;

  private final JFrame parent;
  private final AnalyzerModel model;

  ExportLatexButton(JFrame parent, AnalyzerModel model) {
    this.parent = parent;
    this.model = model;
    addActionListener(event -> doSave());
  }

  void localeChanged() {
    setText(S.get("exportLatexButton"));
  }

  private File lastFile = null;

  private void doSave() {
    /* code taken from Kevin Walsh'e ExportTableButton and slightly modified*/
    if (lastFile == null) {
      final var c = model.getCurrentCircuit();
      if (c != null) {
        lastFile = new File(c.getName() + ".tex");
      } else {
        lastFile = new File("logisim_evolution_analyze.tex");
      }
    }
    JFileChooser chooser = JFileChoosers.createSelected(lastFile);
    chooser.setDialogTitle(S.get("saveButton"));
    chooser.addChoosableFileFilter(chooser.getAcceptAllFileFilter());
    chooser.addChoosableFileFilter(AnalyzerTexWriter.FILE_FILTER);
    chooser.setFileFilter(AnalyzerTexWriter.FILE_FILTER);
    int choice = chooser.showSaveDialog(parent);
    if (choice == JFileChooser.APPROVE_OPTION) {
      final var file = chooser.getSelectedFile();
      if (file.isDirectory()) {
        OptionPane.showMessageDialog(
            parent,
            S.get("notFileMessage", file.getName()),
            S.get("saveErrorTitle"),
            OptionPane.OK_OPTION);
        return;
      }
      if (file.exists() && !file.canWrite()) {
        OptionPane.showMessageDialog(
            parent,
            S.get("cantWriteMessage", file.getName()),
            S.get("saveErrorTitle"),
            OptionPane.OK_OPTION);
        return;
      }
      if (file.exists()) {
        final var confirm =
            OptionPane.showConfirmDialog(
                parent,
                S.get("confirmOverwriteMessage", file.getName()),
                S.get("confirmOverwriteTitle"),
                OptionPane.YES_NO_OPTION);
        if (confirm != OptionPane.YES_OPTION) return;
      }
      try {
        AnalyzerTexWriter.doSave(file, model);
        lastFile = file;
      } catch (IOException e) {
        OptionPane.showMessageDialog(
            parent, e.getMessage(), S.get("saveErrorTitle"), OptionPane.ERROR_MESSAGE);
      }
    }
  }
}
