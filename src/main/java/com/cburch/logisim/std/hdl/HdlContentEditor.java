/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.hdl;

import static com.cburch.logisim.vhdl.Strings.S;

import com.cburch.contracts.BaseDocumentListenerContract;
import com.cburch.hdl.HdlFile;
import com.cburch.hdl.HdlModel;
import com.cburch.hdl.HdlModelListener;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.FileUtil;
import com.cburch.logisim.util.JFileChoosers;
import com.cburch.logisim.util.JInputDialog;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

public class HdlContentEditor extends JDialog implements JInputDialog {

  private class EditorListener implements BaseDocumentListenerContract {

    @Override
    public void insertUpdate(DocumentEvent de) {
      validate.setEnabled(!editor.getText().equals(model.getContent()));
    }

    @Override
    public void removeUpdate(DocumentEvent de) {
      validate.setEnabled(!editor.getText().equals(model.getContent()));
    }
  }

  private class FrameListener extends WindowAdapter implements ActionListener, LocaleListener {

    @Override
    public void actionPerformed(ActionEvent event) {
      Object source = event.getSource();

      if (source == open) {
        if (!editor.getText().equals(model.getContent()))
          if (!confirmImport(HdlContentEditor.this)) return;

        final var chooser = JFileChoosers.createAt(getDefaultImportFile(null));
        chooser.setDialogTitle(S.get("openButton"));
        int choice = chooser.showOpenDialog(HdlContentEditor.this);
        if (choice == JFileChooser.APPROVE_OPTION) {
          final var f = chooser.getSelectedFile();
          try {
            HdlFile.open(f, HdlContentEditor.this);
          } catch (IOException e) {
            OptionPane.showMessageDialog(
                HdlContentEditor.this,
                e.getMessage(),
                S.get("hexOpenErrorTitle"),
                OptionPane.ERROR_MESSAGE);
          }
        }
      }
      if (source == save) {
        final var chooser = JFileChoosers.createSelected(getDefaultExportFile(null));
        chooser.setDialogTitle(S.get("saveButton"));
        int choice = chooser.showSaveDialog(HdlContentEditor.this);
        if (choice == JFileChooser.APPROVE_OPTION) {
          final var f = chooser.getSelectedFile();
          try {
            HdlFile.save(f, HdlContentEditor.this);
          } catch (IOException e) {
            OptionPane.showMessageDialog(
                HdlContentEditor.this,
                e.getMessage(),
                S.get("hexSaveErrorTitle"),
                OptionPane.ERROR_MESSAGE);
          }
        }
      }
      if (source == validate) {
        model.setContent(editor.getText());
      }
      if (source == close) {
        close();
      }
    }

    @Override
    public void localeChanged() {
      setTitle(S.get("hdlFrameTitle"));
      open.setText(S.get("openButton"));
      save.setText(S.get("saveButton"));
      validate.setText(S.get("validateButton"));
      close.setText(S.get("closeButton"));
    }

    @Override
    public void windowClosing(WindowEvent e) {
      close();
    }
  }

  private class ModelListener implements HdlModelListener {

    @Override
    public void contentSet(HdlModel source) {
      validate.setEnabled(false);
    }
  }

  public static boolean confirmImport(Component parent) {
    String[] options = {S.get("importOption"), S.get("cancelOption")};
    return OptionPane.showOptionDialog(
            parent,
            S.get("importMessage"),
            S.get("importTitle"),
            0,
            OptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0])
        == 0;
  }

  private static final long serialVersionUID = 1L;
  private static final int ROWS = 40;

  private static final int COLUMNS = 100;

  private static final String EXPORT_DIR = "hdl_export";

  private final FrameListener frameListener = new FrameListener();
  private final ModelListener modelListener = new ModelListener();
  private final EditorListener editorListener = new EditorListener();

  private RSyntaxTextArea editor;
  private HdlModel model;
  private Project project;

  private final JButton open = new JButton();
  private final JButton save = new JButton();
  private final JButton validate = new JButton();
  private final JButton close = new JButton();

  public HdlContentEditor(Dialog parent, Project proj, HdlModel model) {
    super(parent, S.get("hdlFrameTitle"), true);
    configure(proj, model);
  }

  public HdlContentEditor(Frame parent, Project proj, HdlModel model) {
    super(parent, S.get("hdlFrameTitle"), true);
    configure(proj, model);
  }

  private void close() {
    if (editor.getText().equals(model.getContent())) {
      dispose();
      return;
    }

    if (model.setContent(editor.getText())) {
      dispose();
      return;
    }

    Object[] options = {
      S.get("confirmCloseYes"), S.get("confirmCloseNo"), S.get("confirmCloseBackup")
    };
    final var n =
        OptionPane.showOptionDialog(
            this,
            S.get("confirmCloseMessage"),
            S.get("confirmCloseTitle"),
            OptionPane.YES_NO_CANCEL_OPTION,
            OptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]);

    switch (n) {
      case OptionPane.YES_OPTION -> dispose();
      case OptionPane.CANCEL_OPTION -> {
        save.doClick();
        dispose();
      }
    }
  }

  private void configure(Project proj, HdlModel model) {
    this.project = proj;
    this.model = model;
    this.model.addHdlModelListener(modelListener);
    this.addWindowListener(frameListener);

    final var buttonsPanel = new JPanel();
    buttonsPanel.add(open);
    buttonsPanel.add(save);
    buttonsPanel.add(validate);
    buttonsPanel.add(close);
    open.addActionListener(frameListener);
    save.addActionListener(frameListener);
    close.addActionListener(frameListener);
    validate.addActionListener(frameListener);

    editor = new RSyntaxTextArea(ROWS, COLUMNS);
    ((RSyntaxDocument) editor.getDocument()).setSyntaxStyle(SyntaxConstants.SYNTAX_STYLE_VHDL);
    editor.setCodeFoldingEnabled(true);
    editor.setAntiAliasingEnabled(true);
    editor.getDocument().addDocumentListener(editorListener);

    final var sp = new RTextScrollPane(editor);
    sp.setFoldIndicatorEnabled(true);

    add(sp, BorderLayout.CENTER);
    add(buttonsPanel, BorderLayout.SOUTH);

    LocaleManager.addLocaleListener(frameListener);
    frameListener.localeChanged();
    pack();

    final var size = getSize();
    final var screen = getToolkit().getScreenSize();
    if (size.width > screen.width || size.height > screen.height) {
      size.width = Math.min(size.width, screen.width);
      size.height = Math.min(size.height, screen.height);
      setSize(size);
    }
  }

  private File getDefaultExportFile(File defaultFile) {
    final var projectFile = project.getLogisimFile().getLoader().getMainFile();
    if (projectFile == null) {
      if (defaultFile == null) return new File(model.getName() + ".vhd");
      return defaultFile;
    }

    File compFolder;
    try {
      compFolder =
          new File(
              FileUtil.correctPath(projectFile.getParentFile().getCanonicalPath()) + EXPORT_DIR);
      if (!compFolder.exists() || (compFolder.exists() && !compFolder.isDirectory()))
        compFolder.mkdir();
      return new File(
          FileUtil.correctPath(compFolder.getCanonicalPath()) + model.getName() + ".vhd");
    } catch (IOException ex) {
      return defaultFile;
    }
  }

  private File getDefaultImportFile(File defaultFile) {
    final var projectFile = project.getLogisimFile().getLoader().getMainFile();
    if (projectFile == null) return defaultFile;

    File compFolder;
    try {
      compFolder =
          new File(
              FileUtil.correctPath(projectFile.getParentFile().getCanonicalPath()) + EXPORT_DIR);
      if (!compFolder.exists() || (compFolder.exists() && !compFolder.isDirectory()))
        compFolder.mkdir();
      return new File(FileUtil.correctPath(compFolder.getCanonicalPath()));
    } catch (IOException ex) {
      return defaultFile;
    }
  }

  public String getText() {
    return editor.getText();
  }

  @Override
  public Object getValue() {
    return model;
  }

  public void setText(String content) {
    editor.setText(content);
    editor.discardAllEdits();
  }

  @Override
  public void setValue(Object value) {
    model = (HdlModel) value;
  }

  @Override
  public void setVisible(boolean b) {
    if (b) {
      editor.setText(model.getContent());
      editor.discardAllEdits();
    }

    super.setVisible(b);
  }
}
