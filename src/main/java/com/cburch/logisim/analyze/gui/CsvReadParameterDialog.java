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

import com.cburch.logisim.analyze.data.CsvInterpretor;
import com.cburch.logisim.analyze.data.CsvParameter;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.prefs.AppPreferences;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Scanner;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class CsvReadParameterDialog extends JDialog implements ActionListener {

  private final JComboBox<String> quotes;
  private final JComboBox<String> seperators;
  private final JButton okButton;
  private final JLabel[] labels;
  private final File file;

  private static final long serialVersionUID = 1L;

  private final CsvParameter param;
  private final String sepSpace;
  private boolean setVisible;

  public CsvReadParameterDialog(CsvParameter sel, File file, JFrame parentFrame) {
    super(parentFrame);
    setVisible = true;
    this.file = file;
    sepSpace = S.get("seperatorSpace");
    final String[] possibleSeperators = {",", ";", ":", sepSpace, S.get("SeperatorTab")};
    final String[] possibleQuotes = {"\"", "'"};
    okButton = new JButton(S.get("ConfirmCsvParameters"));
    okButton.addActionListener(this);
    quotes = new JComboBox<>(possibleQuotes);
    quotes.addActionListener(this);
    seperators = new JComboBox<>(possibleSeperators);
    seperators.addActionListener(this);
    setLocationRelativeTo(parentFrame);
    this.param = sel;
    GridBagLayout gb = new GridBagLayout();
    GridBagConstraints c = new GridBagConstraints();
    setLayout(gb);
    c.gridy = 0;
    c.gridx = 0;
    c.fill = GridBagConstraints.HORIZONTAL;
    c.gridwidth = 3;
    add(new JLabel(S.get("UsedQuotesInFile")), c);
    c.gridx = 3;
    c.gridwidth = 1;
    add(quotes, c);
    c.gridy = 1;
    c.gridx = 0;
    c.gridwidth = 3;
    add(new JLabel(S.get("UsedSeperatorInFile")), c);
    c.gridx = 3;
    c.gridwidth = 1;
    add(seperators, c);
    c.gridx = 0;
    c.gridy = 2;
    c.gridwidth = 4;
    c.fill = GridBagConstraints.CENTER;
    add(new JLabel(S.get("cvsFilePreview")), c);
    pack();
    var celwidth = AppPreferences.getScaled(200);
    var celHeight = AppPreferences.getScaled(25);
    c.gridwidth = 1;
    labels = new JLabel[16];
    final var border = BorderFactory.createLineBorder(Color.BLACK, AppPreferences.getScaled(1));
    final var dim = new Dimension(celwidth, celHeight);
    for (int x = 0; x < 4; x++)
      for (int y = 0; y < 4; y++) {
        final var j = new JPanel();
        labels[y * 4 + x] = new JLabel(x + "," + y);
        j.setBorder(border);
        j.setBackground(Color.WHITE);
        j.setPreferredSize(dim);
        j.add(labels[y * 4 + x]);
        c.gridx = x;
        c.gridy = 3 + y;
        add(j, c);
      }
    c.gridwidth = 4;
    c.gridy = 7;
    c.gridx = 0;
    add(okButton, c);
    pack();
    updateLabels();
    setModal(true);
    setVisible(setVisible);
  }

  private void updateLabels() {
    try {
      final var scan = new Scanner(file);
      for (int y = 0; y < 4; y++) {
        List<String> line = null;
        if (scan.hasNext())
          line = CsvInterpretor.parseCsvLine(scan.next(), param.seperator(), param.quote());
        for (int x = 0; x < 4; x++) {
          if (line == null || x >= line.size()) {
            labels[y * 4 + x].setText("");
          } else {
            labels[y * 4 + x].setText(line.get(x));
          }
        }
      }
      scan.close();
    } catch (FileNotFoundException e) {
      OptionPane.showMessageDialog(
          this,
          S.get("cantReadMessage", file.getName()),
          S.get("openButton"),
          OptionPane.ERROR_MESSAGE);
      setVisible = false;
      if (this.isVisible()) {
        setVisible(false);
        dispose();
      }
    }
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (param == null) return;
    if (e.getSource() == quotes) {
      final var sel = (String) quotes.getSelectedItem();
      param.setQuote(sel.charAt(0));
      updateLabels();
    }
    if (e.getSource() == seperators) {
      final var sel = (String) seperators.getSelectedItem();
      if (sel.length() == 1) {
        param.setSeperator(sel.charAt(0));
      } else if (sel.equals(sepSpace)) {
        param.setSeperator(' ');
      } else param.setSeperator('\t');
      updateLabels();
    }
    if (e.getSource() == okButton) {
      param.setValid();
      setVisible(false);
      dispose();
    }
  }
}
