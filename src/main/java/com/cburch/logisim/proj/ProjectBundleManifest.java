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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import com.cburch.logisim.file.Loader;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.prefs.AppPreferences;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;

public class ProjectBundleManifest extends JDialog implements ActionListener {

  public static final String MANIFEST_FILE_NAME = "README.md";

  private JButton closeButton = new JButton();
  private JButton writeButton = new JButton();
  private JTextField projectName = new JTextField(20);
  private JTextField projectAuthor = new JTextField(20);
  private JTextField projectKeywords = new JTextField(20);
  private JEditorPane projectDescription = new JEditorPane();
  private final Frame parrent;
  private ZipOutputStream zipfile;
  private boolean manifestWritten;

  public ProjectBundleManifest(Project project, String projName) {
    super(project.getFrame(), S.get("projBundleManifestWindow"));
    setModal(true);
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    setAlwaysOnTop(true);
    setVisible(false);
    setResizable(false);
    closeButton.addActionListener(this);
    writeButton.addActionListener(this);
    projectName.setText(projName);
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
    pack();
    setLocationRelativeTo(parrent);
    setVisible(true);
  }

  public boolean writeManifest(ZipOutputStream zipfile) {
    this.zipfile = zipfile;
    manifestWritten = false;
    setLayout(new GridBagLayout());
    final var gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    add(new JLabel(S.get("projName")), gbc);
    gbc.gridx = 1;
    add(projectName, gbc);
    gbc.gridx = 0;
    gbc.gridy++;
    add(new JLabel(S.get("projAuthor")), gbc);
    gbc.gridx = 1;
    add(projectAuthor, gbc);
    gbc.gridx = 0;
    gbc.gridy++;
    add(new JLabel(S.get("projKeywords")), gbc);
    gbc.gridx = 1;
    add(projectKeywords, gbc);
    gbc.gridx = 0;
    gbc.gridy++;
    gbc.gridwidth = 2;
    add(new JLabel(S.get("projDescription"), SwingConstants.CENTER), gbc);
    gbc.gridy++;
    final var scroller = new JScrollPane(projectDescription); 
    scroller.setPreferredSize(new Dimension(AppPreferences.getScaled(500), AppPreferences.getScaled(300)));
    add(scroller, gbc);
    gbc.gridx = 0;
    gbc.gridy++;
    gbc.gridwidth = 1;
    closeButton.setText(S.get("projCancel"));
    add(closeButton, gbc);
    writeButton.setText(S.get("projWriteManifest"));
    gbc.gridx = 1;
    add(writeButton, gbc);
    pack();
    setLocationRelativeTo(parrent);
    setVisible(true);
    return manifestWritten;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (closeButton.equals(e.getSource())) {
      setVisible(false);
      dispose();
    } else if (writeButton.equals(e.getSource())) {
      ceateManifest();
      setVisible(false);
      dispose();
    }
  }

  private void ceateManifest() {
    if (zipfile == null) return;
    try {
      final var seperator = "\n---\n";
      var wroteheader1 = false;
      zipfile.putNextEntry(new ZipEntry(MANIFEST_FILE_NAME));
      final var projName = projectName.getText();
      zipfile.write(S.get("projHeader").getBytes());
      if ((projName != null) && !projName.isEmpty()) {
        zipfile.write(S.get("projHeader1").getBytes());
        wroteheader1 = true;
        zipfile.write(S.fmt("projIntro", projName).getBytes());
      }
      final var projAuthor = projectAuthor.getText();
      if ((projAuthor != null) && !projAuthor.isEmpty()) {
        final var authors = projAuthor.split(",");
        if (authors.length > 0) {
          if (!wroteheader1) {
            zipfile.write(S.get("projHeader1").getBytes());
            wroteheader1 = true;
          }
          zipfile.write(S.get("projAuthor").getBytes());
          for (var authorid = 0; authorid < authors.length; authorid++) {
            if (authorid > 0) {
              var authSep = authorid == (authors.length - 1) ? authors.length == 2 ? " and " : ", and " : ", ";
              zipfile.write(authSep.getBytes());
            }
            zipfile.write(String.format("`%s`", authors[authorid]).getBytes());
          }
          zipfile.write("\n".getBytes());
        }
      }
    } catch (IOException e) {
      manifestWritten = false;
    }
    manifestWritten = true;
  }
}
