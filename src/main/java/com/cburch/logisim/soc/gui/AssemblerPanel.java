/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 * 
 * https://github.com/logisim-evolution/
 * 
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.gui;

import static com.cburch.logisim.soc.Strings.S;

import com.cburch.contracts.BaseDocumentListenerContract;
import com.cburch.contracts.BaseKeyListenerContract;
import com.cburch.contracts.BaseMouseListenerContract;
import com.cburch.contracts.BaseWindowListenerContract;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.gui.icons.CompileIcon;
import com.cburch.logisim.gui.icons.ErrorIcon;
import com.cburch.logisim.gui.icons.InfoIcon;
import com.cburch.logisim.gui.icons.OpenSaveIcon;
import com.cburch.logisim.gui.icons.RunIcon;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.soc.data.SocProcessorInterface;
import com.cburch.logisim.soc.util.Assembler;
import com.cburch.logisim.soc.util.AssemblerInterface;
import com.cburch.logisim.util.LocaleListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import javax.swing.Box;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;

public class AssemblerPanel extends JPanel
    implements BaseMouseListenerContract,
        LocaleListener,
        ActionListener,
        BaseKeyListenerContract,
        BaseDocumentListenerContract,
        CaretListener,
        BaseWindowListenerContract {

  private static final long serialVersionUID = 1L;

  private final Assembler assembler;
  private final RSyntaxTextArea asmWindow;
  private final RTextScrollPane debugScrollPane;
  private final JLabel Compile = new JLabel();
  private final JLabel Open = new JLabel();
  private final JLabel Save = new JLabel();
  private final JLabel SaveAs = new JLabel();
  private final JLabel Run = new JLabel();
  private final JLabel Help = new JLabel();
  private final JLabel NextError = new JLabel();
  private final JLabel PrevError = new JLabel();
  private final JLabel Line = new JLabel();
  private final JMenuItem MOpen = new JMenuItem();
  private final JMenuItem MSave = new JMenuItem();
  private final JMenuItem MSaveAs = new JMenuItem();
  private final ListeningFrame parent;
  private final SocProcessorInterface cpu;
  private final CircuitState circuitState;
  private int lineNumber = 1;
  private int numberOfLines = 1;
  private boolean documentChanged = false;
  private File textFile;

  public AssemblerPanel(
      ListeningFrame parent,
      String highLiter,
      AssemblerInterface assembler,
      SocProcessorInterface cpu,
      CircuitState state) {
    parent.addWindowListener(this);
    this.parent = parent;
    this.cpu = cpu;
    circuitState = state;
    textFile = null;
    asmWindow = new RSyntaxTextArea(20, 60);
    asmWindow.setSyntaxEditingStyle(highLiter);
    asmWindow.setEditable(true);
    asmWindow.addKeyListener(this);
    asmWindow.getDocument().addDocumentListener(this);
    asmWindow.addCaretListener(this);
    JPopupMenu popUp = asmWindow.getPopupMenu();
    popUp.remove(popUp.getComponentCount() - 1);
    popUp.add(MOpen);
    MOpen.addActionListener(this);
    popUp.add(MSave);
    MSave.addActionListener(this);
    popUp.add(MSaveAs);
    MSaveAs.addActionListener(this);
    asmWindow.setPopupMenu(popUp);
    debugScrollPane = new RTextScrollPane(asmWindow);
    debugScrollPane.setLineNumbersEnabled(true);
    debugScrollPane.setIconRowHeaderEnabled(true);
    debugScrollPane.getGutter().setBookmarkingEnabled(false);
    Box info = Box.createHorizontalBox();
    Open.setIcon(new OpenSaveIcon(OpenSaveIcon.FILE_OPEN));
    Open.addMouseListener(this);
    info.add(Open);
    Save.setIcon(new OpenSaveIcon(OpenSaveIcon.FILE_SAVE));
    Save.addMouseListener(this);
    info.add(Box.createHorizontalStrut(5));
    info.add(Save);
    SaveAs.setIcon(new OpenSaveIcon(OpenSaveIcon.FILE_SAVE_AS));
    SaveAs.addMouseListener(this);
    info.add(Box.createHorizontalStrut(5));
    info.add(SaveAs);
    Compile.setIcon(new CompileIcon());
    Compile.addMouseListener(this);
    info.add(Box.createHorizontalStrut(5));
    info.add(Compile);
    PrevError.setIcon(new ErrorIcon(false, true));
    PrevError.addMouseListener(this);
    info.add(Box.createHorizontalStrut(5));
    info.add(PrevError);
    NextError.setIcon(new ErrorIcon(true, false));
    NextError.addMouseListener(this);
    info.add(Box.createHorizontalStrut(5));
    info.add(NextError);
    Run.setIcon(new RunIcon());
    Run.addMouseListener(this);
    info.add(Box.createHorizontalStrut(5));
    info.add(Run);
    Help.setIcon(new InfoIcon());
    Help.addMouseListener(this);
    info.add(Box.createHorizontalStrut(5));
    info.add(Help);
    info.add(Box.createHorizontalGlue());
    Line.setOpaque(true);
    info.add(Line);
    info.setPreferredSize(new Dimension(40, AppPreferences.getScaled(20)));
    setLayout(new BorderLayout());
    info.add(Box.createHorizontalStrut(5));
    add(info, BorderLayout.NORTH);
    add(debugScrollPane);
    this.assembler = new Assembler(assembler, debugScrollPane);
    asmWindow.addParser(this.assembler);
    localeChanged();
  }

  private void openFile() {
    if (documentChanged) {
      int ret =
          OptionPane.showConfirmDialog(
              parent,
              S.get("AsmPanDocumentChangedSave"),
              parent.getParentTitle(),
              OptionPane.YES_NO_OPTION);
      if (ret == OptionPane.YES_OPTION) {
        if (textFile == null) {
          OptionPane.showMessageDialog(parent, S.get("AsmPanSaveFirstBeforeOpen"));
          return;
        } else saveFile(false);
      }
    }
    JFileChooser chooser = new JFileChooser();
    FileNameExtensionFilter filter =
        new FileNameExtensionFilter(S.get("AsmPanAmsFileExtention"), "S", "asm");
    chooser.setDialogTitle(parent.getParentTitle() + ": " + S.get("AsmPanReadAsmFile"));
    chooser.setFileFilter(filter);
    int ret = chooser.showOpenDialog(parent);
    if (ret != JFileChooser.APPROVE_OPTION) return;
    textFile = chooser.getSelectedFile();
    try {
      BufferedReader reader = new BufferedReader(new FileReader(textFile));
      StringBuilder s = new StringBuilder();
      String st;
      while ((st = reader.readLine()) != null) s.append(st).append("\n");
      reader.close();
      asmWindow.setText(s.toString());
    } catch (IOException e) {
      OptionPane.showMessageDialog(
          parent,
          S.get("AsmPanErrorReadingFile", textFile.getName()),
          parent.getParentTitle(),
          OptionPane.ERROR_MESSAGE);
      textFile = null;
      return;
    }
    asmWindow.setCaretPosition(0);
    documentChanged = false;
    assembler.reset();
    if (!assembler.assemble()) {
      asmWindow.setCaretPosition(assembler.getErrorPositions().get(0));
    }
    updateLineNumber();
  }

  private void saveFile(boolean AskFileName) {
    if (!documentChanged) return;
    if (AskFileName || textFile == null) {
      JFileChooser chooser = new JFileChooser();
      FileNameExtensionFilter filter =
          new FileNameExtensionFilter(S.get("AsmPanAmsFileExtention"), "S", "asm");
      chooser.setDialogTitle(parent.getParentTitle() + ": " + S.get("AsmPanSaveAsmFile"));
      chooser.setFileFilter(filter);
      int ret = chooser.showOpenDialog(parent);
      if (ret != JFileChooser.APPROVE_OPTION) return;
      textFile = chooser.getSelectedFile();
    }
    try {
      BufferedWriter writer =
          new BufferedWriter(new OutputStreamWriter(new FileOutputStream(textFile)));
      writer.write(asmWindow.getText());
      writer.close();
    } catch (IOException e) {
      OptionPane.showMessageDialog(
          parent,
          S.get("AsmPanErrorCreateFile", textFile.getName()),
          parent.getParentTitle(),
          OptionPane.ERROR_MESSAGE);
    }
    documentChanged = false;
    updateLineNumber();
  }

  private boolean Assemble(boolean showWindow) {
    boolean result = assembler.assemble();
    if (!result) asmWindow.setCaretPosition(assembler.getErrorPositions().get(0));
    else if (showWindow) OptionPane.showMessageDialog(parent, S.get("AssemblerAssembleSuccess"));
    return result;
  }

  private void runProgram() {
    if (!Assemble(false)) return;
    long entryPoint = assembler.getEntryPoint();
    if (entryPoint < 0) return;
    if (!assembler.download(cpu, circuitState)) {
      OptionPane.showMessageDialog(
          parent, S.get("AssemblerUnableToDownload"), S.get("AsmPanRun"), OptionPane.ERROR_MESSAGE);
      return;
    }
    cpu.setEntryPointandReset(circuitState, entryPoint, null, assembler.getSectionHeader());
    OptionPane.showMessageDialog(
        parent, S.get("AssemblerRunSuccess"), S.get("AsmPanRun"), OptionPane.INFORMATION_MESSAGE);
  }

  private void updateLineNumber() {
    Line.setBackground(documentChanged ? Color.YELLOW : Color.WHITE);
    Line.setText(S.get("RV32imAsmLineIndicator", lineNumber, numberOfLines));
    Line.repaint();
  }

  private void nextError(boolean after) {
    int carretPos = asmWindow.getCaretPosition();
    ArrayList<Integer> errorPositions = assembler.getErrorPositions();
    if (errorPositions.isEmpty()) return;
    int findex = -1;
    int index = 0;
    while (findex < 0 && index < errorPositions.size()) {
      if (carretPos <= errorPositions.get(index)) findex = index;
      index++;
    }
    if (after) {
      if (findex < 0 || findex == (errorPositions.size() - 1))
        asmWindow.setCaretPosition(errorPositions.get(0));
      else asmWindow.setCaretPosition(errorPositions.get(findex + 1));
    } else {
      if (findex <= 0) asmWindow.setCaretPosition(errorPositions.get(errorPositions.size() - 1));
      else asmWindow.setCaretPosition(errorPositions.get(findex - 1));
    }
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    Object source = e.getSource();
    if (source == Open) openFile();
    else if (source == Save) saveFile(false);
    else if (source == SaveAs) saveFile(true);
    else if (source == Compile) Assemble(true);
    else if (source == NextError) nextError(true);
    else if (source == PrevError) nextError(false);
    else if (source == Run) runProgram();
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    Object source = e.getSource();
    if (source == MOpen) openFile();
    else if (source == MSave) saveFile(false);
    else if (source == MSaveAs) saveFile(true);
  }

  @Override
  public void keyPressed(KeyEvent e) {
    if (!e.isAltDown() && e.isControlDown() && e.getKeyCode() == KeyEvent.VK_S) saveFile(false);
    else if (!e.isAltDown() && e.isControlDown() && e.getKeyCode() == KeyEvent.VK_L) openFile();
    else if (e.isAltDown() && !e.isControlDown() && e.getKeyCode() == KeyEvent.VK_A) Assemble(true);
    else if (e.isAltDown() && !e.isControlDown() && e.getKeyCode() == KeyEvent.VK_R) runProgram();
    else if (!e.isAltDown() && e.isControlDown() && e.getKeyCode() == KeyEvent.VK_N)
      nextError(true);
    else if (!e.isAltDown() && e.isControlDown() && e.getKeyCode() == KeyEvent.VK_P)
      nextError(false);
  }

  @Override
  public void localeChanged() {
    Open.setToolTipText(S.get("AsmPanOpenFile"));
    Save.setToolTipText(S.get("AsmPanSaveFile"));
    SaveAs.setToolTipText(S.get("AsmPanSaveFileAs"));
    Compile.setToolTipText(S.get("AsmPanAssemble"));
    NextError.setToolTipText(S.get("AsmPanNextError"));
    PrevError.setToolTipText(S.get("AsmPanPreviousError"));
    Run.setToolTipText(S.get("AsmPanRun"));
    MOpen.setText(S.get("AsmPanOpenFile"));
    MSave.setText(S.get("AsmPanSaveFile"));
    MSaveAs.setText(S.get("AsmPanSaveFileAs"));
    updateLineNumber();
  }

  @Override
  public void insertUpdate(DocumentEvent e) {
    documentChanged = true;
    assembler.checkAndBuildTokens(asmWindow.getCaretLineNumber());
  }

  @Override
  public void removeUpdate(DocumentEvent e) {
    documentChanged = true;
    assembler.checkAndBuildTokens(asmWindow.getCaretLineNumber());
  }

  @Override
  public void caretUpdate(CaretEvent e) {
    numberOfLines = asmWindow.getDocument().getDefaultRootElement().getElementCount();
    lineNumber = asmWindow.getCaretLineNumber() + 1;
    updateLineNumber();
  }

  @Override
  public void windowClosed(WindowEvent e) {
    if (documentChanged) {
      parent.setVisible(true);
      int ret =
          OptionPane.showConfirmDialog(
              parent,
              S.get("AsmPanDocumentChangedSave"),
              parent.getParentTitle(),
              OptionPane.YES_NO_OPTION);
      if (ret == OptionPane.YES_OPTION) saveFile(false);
      documentChanged = false;
      parent.setVisible(false);
    }
    asmWindow.setText("");
    assembler.reset();
    documentChanged = false;
    updateLineNumber();
  }
}
