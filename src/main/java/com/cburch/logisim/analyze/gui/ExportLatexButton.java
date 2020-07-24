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

package com.cburch.logisim.analyze.gui;

import static com.cburch.logisim.analyze.Strings.S;

import com.cburch.logisim.analyze.file.AnalyzerTexWriter;
import com.cburch.logisim.analyze.model.AnalyzerModel;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.util.JFileChoosers;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;

public class ExportLatexButton extends JButton {

  /** */
  private static final long serialVersionUID = 1L;

  private JFrame parent;
  private AnalyzerModel model;

  ExportLatexButton(JFrame parent, AnalyzerModel model) {
    this.parent = parent;
    this.model = model;
    addActionListener(
        new ActionListener() {
          public void actionPerformed(ActionEvent event) {
            doSave();
          }
        });
  }

  void localeChanged() {
    setText(S.get("exportLatexButton"));
  }

  private File lastFile = null;

  private void doSave() {
    /* code taken from Kevin Walsh'e ExportTableButton and slightly modified*/
    if (lastFile == null) {
      Circuit c = model.getCurrentCircuit();
      if (c != null) lastFile = new File(c.getName() + ".tex");
      else lastFile = new File("logisim_evolution_analyze.tex");
    }
    JFileChooser chooser = JFileChoosers.createSelected(lastFile);
    chooser.setDialogTitle(S.get("saveButton"));
    chooser.addChoosableFileFilter(chooser.getAcceptAllFileFilter());
    chooser.addChoosableFileFilter(AnalyzerTexWriter.FILE_FILTER);
    chooser.setFileFilter(AnalyzerTexWriter.FILE_FILTER);
    int choice = chooser.showSaveDialog(parent);
    if (choice == JFileChooser.APPROVE_OPTION) {
      File file = chooser.getSelectedFile();
      if (file.isDirectory()) {
        OptionPane.showMessageDialog(
            parent,
            S.fmt("notFileMessage", file.getName()),
            S.get("saveErrorTitle"),
            OptionPane.OK_OPTION);
        return;
      }
      if (file.exists() && !file.canWrite()) {
        OptionPane.showMessageDialog(
            parent,
            S.fmt("cantWriteMessage", file.getName()),
            S.get("saveErrorTitle"),
            OptionPane.OK_OPTION);
        return;
      }
      if (file.exists()) {
        int confirm =
            OptionPane.showConfirmDialog(
                parent,
                S.fmt("confirmOverwriteMessage", file.getName()),
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
