/*
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

package com.cburch.logisim.vhdl.gui;

import static com.cburch.logisim.vhdl.Strings.S;

import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.FileUtil;
import com.cburch.logisim.util.JFileChoosers;
import com.cburch.logisim.util.JInputDialog;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;
import com.cburch.logisim.vhdl.base.HdlContent;
import com.cburch.logisim.vhdl.base.HdlModel;
import com.cburch.logisim.vhdl.base.HdlModelListener;
import com.cburch.logisim.vhdl.file.HdlFile;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.WeakHashMap;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.fife.ui.rtextarea.RTextScrollPane;

public class HdlContentEditor extends JDialog implements JInputDialog {

  private class EditorListener implements DocumentListener {

    @Override
    public void changedUpdate(DocumentEvent de) {}

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
        String vhdl = project.getLogisimFile().getLoader().vhdlImportChooser(HdlContentEditor.this);
        if (vhdl != null) setText(vhdl);
      }
      if (source == save) {
        JFileChooser chooser = JFileChoosers.createSelected(getDefaultExportFile(null));
        chooser.setDialogTitle(S.get("hdlSaveButton"));
        int choice = chooser.showSaveDialog(HdlContentEditor.this);
        if (choice == JFileChooser.APPROVE_OPTION) {
          File f = chooser.getSelectedFile();
          try {
            HdlFile.save(f, getText());
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
      open.setText(S.get("hdlOpenButton"));
      save.setText(S.get("hdlSaveButton"));
      validate.setText(S.get("validateAndSaveButton"));
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

    @Override
    public void aboutToSave(HdlModel source) {}

    @Override
    public void displayChanged(HdlModel source) {}

    @Override
    public void appearanceChanged(HdlModel source) {}
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
    setLocationRelativeTo(parent);
  }

  public HdlContentEditor(Frame parent, Project proj, HdlModel model) {
    super(parent, S.get("hdlFrameTitle"), true);
    configure(proj, model);
    setLocationRelativeTo(parent);
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
    int n =
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
      case OptionPane.YES_OPTION:
        dispose();
        break;
      case OptionPane.CANCEL_OPTION:
        save.doClick();
        dispose();
        break;
    }
  }

  private void configure(Project proj, HdlModel model) {
    this.project = proj;
    this.model = model;
    this.model.addHdlModelListener(modelListener);
    this.addWindowListener(frameListener);

    JPanel buttonsPanel = new JPanel();
    buttonsPanel.add(open);
    buttonsPanel.add(save);
    buttonsPanel.add(validate);
    buttonsPanel.add(close);
    open.addActionListener(frameListener);
    save.addActionListener(frameListener);
    close.addActionListener(frameListener);
    validate.addActionListener(frameListener);

    AbstractTokenMakerFactory atmf = (AbstractTokenMakerFactory)TokenMakerFactory.getDefaultInstance();
    atmf.putMapping("text/vhdl", "com.cburch.logisim.vhdl.syntax.VhdlSyntax");
    editor = new RSyntaxTextArea(ROWS, COLUMNS);
    editor.setSyntaxEditingStyle("text/vhdl");
    editor.setCodeFoldingEnabled(true);
    editor.setAntiAliasingEnabled(true);
    editor.getDocument().addDocumentListener(editorListener);

    RTextScrollPane sp = new RTextScrollPane(editor);
    sp.setFoldIndicatorEnabled(true);

    add(sp, BorderLayout.CENTER);
    add(buttonsPanel, BorderLayout.SOUTH);

    LocaleManager.addLocaleListener(frameListener);
    frameListener.localeChanged();
    pack();

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

  private static final WeakHashMap<HdlContent, HdlContentEditor> windowRegistry =
      new WeakHashMap<>();

  public static HdlContentEditor getContentEditor(Window source, HdlContent value, Project proj) {
    synchronized (windowRegistry) {
      HdlContentEditor ret = windowRegistry.get(value);
      if (ret == null) {
        if (source instanceof Frame) ret = new HdlContentEditor((Frame) source, proj, value);
        else ret = new HdlContentEditor((Dialog) source, proj, value);
        windowRegistry.put(value, ret);
      }
      return ret;
    }
  }
}
