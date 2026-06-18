/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.vhdl.gui;

import static com.cburch.logisim.vhdl.Strings.S;

import com.cburch.contracts.BaseDocumentListenerContract;
import com.cburch.draw.toolbar.ToolbarModel;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.FileUtil;
import com.cburch.logisim.util.JFileChoosers;
import com.cburch.logisim.vhdl.base.HdlModel;
import com.cburch.logisim.vhdl.base.HdlModelListener;
import com.cburch.logisim.vhdl.file.HdlFile;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

public class HdlContentView extends JPanel
    implements BaseDocumentListenerContract, HdlModelListener {

  private class HdlEditAction extends Action {
    final HdlModel model;
    final String original;

    HdlEditAction(HdlModel model, String original) {
      this.model = model;
      this.original = original;
    }

    @Override
    public void doIt(Project proj) {
      /* nop b/c already done */
    }

    @Override
    public String getName() {
      return "VHDL edits";
    }

    @Override
    public boolean isModification() {
      return true;
    }

    @Override
    public boolean shouldAppendTo(Action other) {
      return (other instanceof HdlEditAction) && ((HdlEditAction) other).model == model;
    }

    @Override
    public void undo(Project proj) {
      setText(original);
      model.setContentNoValidation(original);
      toolbar.setDirty(!model.isValid());
      dirty = false;
      if (HdlContentView.this.model != model) setHdlModel(model);
    }

    @Override
    public Action append(Action other) {
      return this;
    }
  }

  @Override
  public void insertUpdate(DocumentEvent de) {
    docChanged();
  }

  @Override
  public void removeUpdate(DocumentEvent de) {
    docChanged();
  }

  void docChanged() {
    if (model == null) return;
    model.setContentNoValidation(editor.getText());
    if (dirty || model == null) return;
    toolbar.setDirty(true);
    project.doAction(new HdlEditAction(model, model.getContent()));
    dirty = true;
  }

  void doExport() {
    JFileChooser chooser = JFileChoosers.createSelected(getDefaultExportFile(null));
    chooser.setDialogTitle(S.get("hdlSaveDialog"));
    int choice = chooser.showSaveDialog(HdlContentView.this);
    if (choice == JFileChooser.APPROVE_OPTION) {
      File f = chooser.getSelectedFile();
      try {
        HdlFile.save(f, editor.getText());
      } catch (IOException e) {
        OptionPane.showMessageDialog(
            HdlContentView.this,
            e.getMessage(),
            S.get("hexSaveErrorTitle"),
            OptionPane.ERROR_MESSAGE);
      }
    }
  }

  void doImport() {
    if (!editor.getText().equals(model.getContent()))
      if (!confirmImport(HdlContentView.this)) return;
    String vhdl = project.getLogisimFile().getLoader().vhdlImportChooser(HdlContentView.this);
    if (vhdl != null) setText(vhdl);
  }

  void doValidate() {
    model.setContent(editor.getText());
    dirty = false;
    toolbar.setDirty(!model.isValid());
    if (!model.isValid()) model.showErrors();
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

  private RSyntaxTextArea editor;
  private HdlModel model;
  private final Project project;

  private final HdlToolbarModel toolbar;

  public HdlContentView(Project proj) {
    super(new BorderLayout());
    this.project = proj;
    this.model = null;
    this.toolbar = new HdlToolbarModel(proj, this);
    configure("vhdl");
  }

  private void configure(String lang) {
    JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

    editor = new RSyntaxTextArea(ROWS, COLUMNS);
    if (lang.equals("vhdl")) {
      ((RSyntaxDocument) editor.getDocument()).setSyntaxStyle(SyntaxConstants.SYNTAX_STYLE_VHDL);
    } else {
      editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_DELPHI);
    }
    editor.setCodeFoldingEnabled(true);
    editor.setAntiAliasingEnabled(true);
    editor.getDocument().addDocumentListener(this);

    RTextScrollPane sp = new RTextScrollPane(editor);
    sp.setFoldIndicatorEnabled(true);

    add(sp, BorderLayout.CENTER);
    add(buttonsPanel, BorderLayout.NORTH);

    // pack();

    Dimension size = getSize();
    Dimension screen = getToolkit().getScreenSize();
    if (size.width > screen.width || size.height > screen.height) {
      size.width = Math.min(size.width, screen.width);
      size.height = Math.min(size.height, screen.height);
      setSize(size);
    }
  }

  private File getDefaultExportFile(File defaultFile) {
    File projectFile = project.getLogisimFile().getLoader().getMainFile();
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

  public HdlModel getHdlModel() {
    return model;
  }

  public ToolbarModel getToolbarModel() {
    return toolbar;
  }

  boolean dirty = false;

  public void setText(String content) {
    dirty = true;
    editor.setText(content);
    editor.discardAllEdits();
    dirty = false;
    editor.setCaretPosition(0);
  }

  public void clearHdlModel() {
    if (model == null) return;
    if (!editor.getText().equals(model.getContent())) model.setContent(editor.getText());
    model.removeHdlModelListener(toolbar);
    model.removeHdlModelListener(this);
    model = null;
    setText("");
    dirty = false;
  }

  @Override
  public void contentSet(HdlModel source) {
    if (!editor.getText().equals(model.getContent())) setText(model.getContent());
    dirty = false;
  }

  @Override
  public void aboutToSave(HdlModel source) {
    if (model != source) return;
    if (!editor.getText().equals(model.getContent())) {
      model.setContent(editor.getText());
      dirty = false;
      toolbar.setDirty(!model.isValid());
    }
  }

  public void setHdlModel(HdlModel model) {
    if (this.model == model) return;
    clearHdlModel();
    this.model = model;
    if (this.model != null) {
      this.model.addHdlModelListener(toolbar);
      this.model.addHdlModelListener(this);
      setText(model.getContent());
      toolbar.setDirty(!model.isValid());
    }
  }
}
