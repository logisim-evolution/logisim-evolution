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

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Color;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.DefaultCaret;

import com.cburch.logisim.analyze.model.AnalyzerModel;
import com.cburch.logisim.gui.generic.OptionPane;

public class MinimizeButton  extends JButton {

  private final JFrame parent;
  private final AnalyzerModel model;
  private final int format;

  public MinimizeButton(JFrame parent, AnalyzerModel model, int format) {
    this.parent = parent;
    this.model = model;
    this.format = format;
    addActionListener(event -> doOptimize());
  }

  void doOptimize() {
    final var choice = OptionPane.showConfirmDialog(
          parent,
          S.get("OptimizeLongTimeWarning"),
          S.get("minimizeFunctionTitle"),
          OptionPane.YES_NO_OPTION);
    if (choice != OptionPane.YES_OPTION) return;
    final var info = new JTextArea(20, 80);
    info.setEditable(false);
    info.setFont(new Font("monospaced", Font.PLAIN, 12));
    info.setForeground(Color.WHITE);
    info.setBackground(Color.BLACK);
    final var caret = (DefaultCaret) info.getCaret();
    caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
    final var pane = new JScrollPane(info);
    pane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    pane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    final var doneButton = new JButton(S.get("minimizeDone"));
    final var infoPanel = new JDialog(
          parent,
          S.get("minimizeFunctionTitle"),
          true);
    infoPanel.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
    infoPanel.add(pane, BorderLayout.CENTER);
    infoPanel.add(doneButton, BorderLayout.SOUTH);
    doneButton.setVisible(false);
    infoPanel.setLocationRelativeTo(parent);
    infoPanel.pack();
    final var dialogThread = new Thread(
          new Runnable() {
              void done() {
                infoPanel.dispose();
              }

              public void run() {
                doneButton.addActionListener(Event -> done());
                infoPanel.setVisible(true);
              }
          }
    );
    dialogThread.start();
    final var optimizeThread = new Thread(
        new Runnable() {
            public void run() {
                model.getOutputExpressions().forcedOptimize(info, format);
                doneButton.setVisible(true);
            }
        }
    );
    optimizeThread.start();
  }
}
