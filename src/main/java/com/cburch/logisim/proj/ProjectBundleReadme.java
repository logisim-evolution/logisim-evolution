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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import com.cburch.logisim.generated.BuildInfo;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.StringUtil;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;

public class ProjectBundleReadme extends JDialog implements ActionListener {

  public static final String README_FILE_NAME = "README.md";

  private JButton closeButton = new JButton();
  private JButton writeButton = new JButton();
  private JTextField projectName = new JTextField(20);
  private JTextField projectAuthor = new JTextField(20);
  private JTextField projectKeywords = new JTextField(20);
  private JEditorPane projectDescription = new JEditorPane();
  private ReadmeInfo projectReadmeInfo;
  private final Frame parrent;
  
  public class ReadmeInfo {
    private String projectName;
    private String projectAuthor;
    private String projectKeywords;
    private String projectDescription;
    
    public ReadmeInfo(String name, String author, String keyword, String description) {
      projectName = name;
      projectAuthor = author;
      projectKeywords = keyword;
      projectDescription = description;
    }
  }

  public ProjectBundleReadme(Project project, String projName) {
    super(project.getFrame(), S.get("projBundleReadmeWindow"));
    setModal(true);
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    setAlwaysOnTop(true);
    setVisible(false);
    closeButton.addActionListener(this);
    writeButton.addActionListener(this);
    projectName.setText(projName);
    parrent = project.getFrame();
  }
  
  public void showReadme(InputStream file) throws IOException {
    final var lines = new StringBuilder();
    var kar = 0;
    do {
      kar = file.read();
      if (kar >= 0) lines.append((char) kar);
    } while (kar >= 0);
    final var options = new MutableDataSet();
    final var parser = Parser.builder(options).build();
    final var renderer = HtmlRenderer.builder(options).build();
    final var readme = parser.parse(lines.toString());
    final var text = renderer.render(readme);
    final var dialog = new JEditorPane("text/html", text);
    dialog.setEditable(false);
    dialog.setCaretPosition(0);
    final var scroller = new JScrollPane(dialog);
    setLayout(new BorderLayout());
    add(scroller, BorderLayout.CENTER);
    closeButton.setText(S.get("projCloseReadme"));
    add(closeButton, BorderLayout.SOUTH);
    pack();
    setLocationRelativeTo(parrent);
    setVisible(true);
  }

  public ReadmeInfo getReadmeInfo() {
    setLayout(new GridBagLayout());
    setResizable(false);
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
    gbc.gridx = 1;
    add(closeButton, gbc);
    writeButton.setText(S.get("projWriteReadme"));
    gbc.gridx = 0;
    add(writeButton, gbc);
    pack();
    setLocationRelativeTo(parrent);
    setVisible(true);
    return projectReadmeInfo;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (closeButton.equals(e.getSource())) {
      setVisible(false);
      dispose();
    } else if (writeButton.equals(e.getSource())) {
      projectReadmeInfo = new ReadmeInfo(projectName.getText(), projectAuthor.getText(), 
          projectKeywords.getText(), projectDescription.getText());
      setVisible(false);
      dispose();
    }
  }

  public static void writeReadmeFile(ZipOutputStream zipfile, ReadmeInfo info) {
    if ((zipfile == null) || (StringUtil.isNullOrEmpty(info.projectAuthor) 
        && StringUtil.isNullOrEmpty(info.projectDescription) && StringUtil.isNullOrEmpty(info.projectKeywords))) return;
    try {
      final var seperator = "---\n\n";
      var wroteheader1 = false;
      zipfile.putNextEntry(new ZipEntry(README_FILE_NAME));
      zipfile.write(S.get("projHeader").concat("\n\n").getBytes());
      final var projName = info.projectName;
      if (StringUtil.isNotEmpty(projName)) {
        zipfile.write(S.get("projHeader1").concat("\n\n").getBytes());
        wroteheader1 = true;
        zipfile.write(S.fmt("projIntro", projName).concat("\n\n").getBytes());
      }
      final var projAuthor = info.projectAuthor;
      if (StringUtil.isNotEmpty(projAuthor)) {
        final var authors = projAuthor.split(",");
        if (authors.length > 0) {
          if (!wroteheader1) {
            zipfile.write(S.get("projHeader1").concat("\n\n").getBytes());
            wroteheader1 = true;
          }
          zipfile.write(S.get("projAuthor").getBytes());
          for (var authorId = 0; authorId < authors.length; authorId++) {
            if (authorId > 0) {
              var authSep = authorId == (authors.length - 1) ? authors.length == 2 ? " and " : ", and " : ", ";
              zipfile.write(authSep.getBytes());
            }
            zipfile.write(String.format("`%s`", authors[authorId]).getBytes());
          }
          zipfile.write("\n\n".getBytes());
        }
      }
      final var projKeywords = info.projectKeywords;
      if (StringUtil.isNotEmpty(projKeywords)) {
        final var keywords = projKeywords.split(",");
        if (keywords.length > 0) {
          if (!wroteheader1) {
            zipfile.write(S.get("projHeader1").concat("\n\n").getBytes());
            wroteheader1 = true;
          } else {
            zipfile.write(seperator.getBytes());
          }
          for (var keywordId = 0; keywordId < keywords.length; keywordId++) {
            if (keywordId > 0) zipfile.write(", ".getBytes());
            zipfile.write(String.format("`%s`", keywords[keywordId]).getBytes());
          }
          zipfile.write("\n\n".getBytes());
        }
      }
      final var projDescription = info.projectDescription;
      if (StringUtil.isNotEmpty(projDescription)) {
        zipfile.write(S.get("projHeader2").concat("\n\n").getBytes());
        zipfile.write(projDescription.getBytes());
        zipfile.write("\n".getBytes());
      }
      final var dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");  
      final var now = LocalDateTime.now();  
      zipfile.write(S.get("projHeader3").concat("\n\n").getBytes());
      zipfile.write(S.fmt("projGenerateInfo", BuildInfo.displayName, BuildInfo.url, dtf.format(now)).concat("\n\n").getBytes());
    } catch (IOException e) {
      System.err.println(e.getMessage());
    }
  }
}
