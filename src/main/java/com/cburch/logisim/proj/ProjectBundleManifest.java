/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.proj;

import static com.cburch.logisim.proj.Strings.S;

import java.io.IOException;
import java.io.InputStream;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.prefs.AppPreferences;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;

public class ProjectBundleManifest extends JDialog implements ActionListener {

  public static final String MANIFEST_FILE_NAME = "README.txt";

  private JButton closeButton = new JButton();
  private final Frame parrent;

  public ProjectBundleManifest(Project project) {
    super(project.getFrame(), S.get("projBundleManifestWindow"));
    setPreferredSize(new Dimension(AppPreferences.getScaled(500),AppPreferences.getScaled(400)));
    setModal(true);
    setAlwaysOnTop(true);
    setVisible(false);
    closeButton.addActionListener(this);
    parrent = project.getFrame();
  }
  
  public void showManifest(InputStream file) throws IOException {
    final var lines = new StringBuilder();
    var kar = 0;
    do {
      kar = file.read();
      if (kar >= 0) lines.append((char) kar);
    } while (kar >= 0);
    final var options = new MutableDataSet();
    final var parser = Parser.builder(options).build();
    final var renderer = HtmlRenderer.builder(options).build();
    final var manifest = parser.parse(lines.toString());
    final var text = renderer.render(manifest);
    final var dialog = new JEditorPane("text/html", text);
    dialog.setEditable(false);
    dialog.setCaretPosition(0);
    final var scroller = new JScrollPane(dialog);
    setLayout(new BorderLayout());
    add(scroller, BorderLayout.CENTER);
    closeButton.setText(S.get("projCloseManifest"));
    add(closeButton, BorderLayout.SOUTH);
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    setLocationRelativeTo(parrent);
    pack();
    setVisible(true);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (closeButton.equals(e.getSource())) setVisible(false);
  }
}
