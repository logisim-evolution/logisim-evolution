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

package com.cburch.logisim.soc.gui;

import static com.cburch.logisim.soc.Strings.S;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
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

public class AssemblerPanel extends JPanel implements MouseListener,LocaleListener,ActionListener,
        KeyListener,DocumentListener,CaretListener,WindowListener {

  private static final long serialVersionUID = 1L;
  
  private Assembler assembler;
  private RSyntaxTextArea asmWindow;
  private RTextScrollPane debugScrollPane;
  private JLabel Compile = new JLabel();
  private JLabel Open = new JLabel();
  private JLabel Save = new JLabel();
  private JLabel SaveAs = new JLabel();
  private JLabel Run = new JLabel();
  private JLabel Help = new JLabel();
  private JLabel NextError = new JLabel();
  private JLabel PrevError = new JLabel();
  private JLabel Line = new JLabel();
  private JMenuItem MOpen = new JMenuItem();
  private JMenuItem MSave = new JMenuItem();
  private JMenuItem MSaveAs = new JMenuItem();
  private int lineNumber = 1;
  private int numberOfLines = 1;
  private boolean documentChanged = false;
  private ListeningFrame parent;
  private File textFile;
  private SocProcessorInterface cpu;
  private CircuitState circuitState;

  public AssemblerPanel(ListeningFrame parent , String highLiter , AssemblerInterface assembler,
                        SocProcessorInterface cpu, CircuitState state) {
	parent.addWindowListener(this);
	this.parent = parent;
	this.cpu = cpu;
	circuitState = state;
	textFile = null;
    asmWindow = new RSyntaxTextArea(20,60);
    asmWindow.setSyntaxEditingStyle(highLiter);
    asmWindow.setEditable(true);
    asmWindow.addKeyListener(this);
    asmWindow.getDocument().addDocumentListener(this);
    asmWindow.addCaretListener(this);
    JPopupMenu popUp = asmWindow.getPopupMenu();
    popUp.remove(popUp.getComponentCount()-1);
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
    PrevError.setIcon(new ErrorIcon(false,true));
    PrevError.addMouseListener(this);
    info.add(Box.createHorizontalStrut(5));
    info.add(PrevError);
    NextError.setIcon(new ErrorIcon(true,false));
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
    info.setPreferredSize(new Dimension(40,AppPreferences.getScaled(20)));
    setLayout(new BorderLayout());
    info.add(Box.createHorizontalStrut(5));
    add(info,BorderLayout.NORTH);
    add(debugScrollPane);
    this.assembler = new Assembler(assembler,debugScrollPane);
    asmWindow.addParser(this.assembler);
    localeChanged();
  }
  
  private void openFile() {
    if (documentChanged) {
      int ret = OptionPane.showConfirmDialog(parent, S.get("AsmPanDocumentChangedSave"),parent.getParentTitle() , OptionPane.YES_NO_OPTION);
      if (ret == OptionPane.YES_OPTION) {
        if (textFile == null) {
        	OptionPane.showMessageDialog(parent, S.get("AsmPanSaveFirstBeforeOpen"));
        	return;
        } else saveFile(false);
      }
    }
    JFileChooser chooser = new JFileChooser();
    FileNameExtensionFilter filter = new FileNameExtensionFilter(S.get("AsmPanAmsFileExtention"),"S","asm");
    chooser.setDialogTitle(parent.getParentTitle()+": "+S.get("AsmPanReadAsmFile"));
    chooser.setFileFilter(filter);
    int ret = chooser.showOpenDialog(parent);
    if (ret != JFileChooser.APPROVE_OPTION) return;
    textFile = chooser.getSelectedFile();
    try {
      BufferedReader reader = new BufferedReader(new FileReader(textFile));
      StringBuffer s = new StringBuffer();
      String st;
      while ((st = reader.readLine()) != null) s.append(st+"\n");
      reader.close();
      asmWindow.setText(s.toString());
    } catch ( IOException e) {
      OptionPane.showMessageDialog(parent, S.fmt("AsmPanErrorReadingFile",textFile.getName()), parent.getParentTitle(), OptionPane.ERROR_MESSAGE);
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
      FileNameExtensionFilter filter = new FileNameExtensionFilter(S.get("AsmPanAmsFileExtention"),"S","asm");
      chooser.setDialogTitle(parent.getParentTitle()+": "+S.get("AsmPanSaveAsmFile"));
      chooser.setFileFilter(filter);
      int ret = chooser.showOpenDialog(parent);
      if (ret != JFileChooser.APPROVE_OPTION) return;
      textFile = chooser.getSelectedFile();
    }
    try {
      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(textFile)));
      writer.write(asmWindow.getText());
      writer.close();
    } catch (IOException e) {
      OptionPane.showMessageDialog(parent, S.fmt("AsmPanErrorCreateFile",textFile.getName()), parent.getParentTitle(), OptionPane.ERROR_MESSAGE);
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
      OptionPane.showMessageDialog(parent, S.get("AssemblerUnableToDownload"), S.get("AsmPanRun"), OptionPane.ERROR_MESSAGE);
      return;
    }
    cpu.setEntryPointandReset(circuitState, entryPoint, null, assembler.getSectionHeader());
    OptionPane.showMessageDialog(parent, S.get("AssemblerRunSuccess"), S.get("AsmPanRun"), OptionPane.INFORMATION_MESSAGE);
  }
  
  private void updateLineNumber() {
	Line.setBackground(documentChanged ? Color.YELLOW : Color.WHITE);
    Line.setText(S.fmt("RV32imAsmLineIndicator",lineNumber,numberOfLines));
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
      if (findex < 0 || findex == (errorPositions.size()-1)) asmWindow.setCaretPosition(errorPositions.get(0));
      else asmWindow.setCaretPosition(errorPositions.get(findex+1));
    } else {
      if (findex <= 0) asmWindow.setCaretPosition(errorPositions.get(errorPositions.size()-1));
      else asmWindow.setCaretPosition(errorPositions.get(findex-1));
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
    else if (!e.isAltDown() && e.isControlDown() && e.getKeyCode() == KeyEvent.VK_N) nextError(true);
    else if (!e.isAltDown() && e.isControlDown() && e.getKeyCode() == KeyEvent.VK_P) nextError(false);
  }

  @Override
  public void mousePressed(MouseEvent e) { }

  @Override
  public void mouseReleased(MouseEvent e) { }

  @Override
  public void mouseEntered(MouseEvent e) { }

  @Override
  public void mouseExited(MouseEvent e) { }

  @Override
  public void keyTyped(KeyEvent e) { }

  @Override
  public void keyReleased(KeyEvent e) { }

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
  public void changedUpdate(DocumentEvent e) { }

  @Override
  public void caretUpdate(CaretEvent e) {
    numberOfLines = asmWindow.getDocument().getDefaultRootElement().getElementCount();
    lineNumber = asmWindow.getCaretLineNumber()+1;
    updateLineNumber();
  }

  @Override
  public void windowOpened(WindowEvent e) { }

  @Override
  public void windowClosing(WindowEvent e) { 
  }

  @Override
  public void windowClosed(WindowEvent e) { 
    if (documentChanged) {
      parent.setVisible(true);
      int ret = OptionPane.showConfirmDialog(parent, S.get("AsmPanDocumentChangedSave"),parent.getParentTitle() , OptionPane.YES_NO_OPTION);
      if (ret == OptionPane.YES_OPTION) saveFile(false);
      documentChanged = false;
      parent.setVisible(false);
    }
    asmWindow.setText("");
    assembler.reset();
    documentChanged = false;
    updateLineNumber();
  }

  @Override
  public void windowIconified(WindowEvent e) { }

  @Override
  public void windowDeiconified(WindowEvent e) { }

  @Override
  public void windowActivated(WindowEvent e) { }

  @Override
  public void windowDeactivated(WindowEvent e) { }

}
