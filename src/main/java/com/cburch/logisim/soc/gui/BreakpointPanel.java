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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.GutterIconInfo;
import org.fife.ui.rtextarea.RTextScrollPane;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.gui.icons.BreakpointIcon;
import com.cburch.logisim.soc.data.SocProcessorInterface;
import com.cburch.logisim.soc.file.ElfProgramHeader;
import com.cburch.logisim.soc.file.ElfSectionHeader;
import com.cburch.logisim.soc.util.AssemblerInterface;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;

public class BreakpointPanel extends JPanel implements CaretListener,LocaleListener,ActionListener,KeyListener{

  private static final long serialVersionUID = 1L;
  private RSyntaxTextArea asmWindow;
  private RTextScrollPane debugScrollPane;
  private JLabel lineIndicator;
  private int oldCaretPos;
  private HashMap<Integer,Integer> debugLines;
  private JButton addBreakPoint;
  private JButton removeBreakPoint;
  private int currentLine;
  private int maxLines;

  public BreakpointPanel(String highLiter) {
    asmWindow = new RSyntaxTextArea(20,60);
    asmWindow.setSyntaxEditingStyle(highLiter);
    asmWindow.setEditable(false);
    asmWindow.setPopupMenu(null);
    asmWindow.addCaretListener(this);
    asmWindow.addKeyListener(this);
    debugScrollPane = new RTextScrollPane(asmWindow);
    debugScrollPane.setLineNumbersEnabled(false);
    debugScrollPane.setIconRowHeaderEnabled(true);
    debugScrollPane.getGutter().setBookmarkIcon(new BreakpointIcon());
    debugScrollPane.getGutter().setBookmarkingEnabled(true);
    JPanel info = new JPanel();
    info.setLayout(new BorderLayout());
    lineIndicator = new JLabel();
    lineIndicator.setHorizontalAlignment(JLabel.CENTER);
    addBreakPoint = new JButton();
    addBreakPoint.addActionListener(this);
    removeBreakPoint = new JButton();
    removeBreakPoint.addActionListener(this);
    info.add(addBreakPoint,BorderLayout.WEST);
    info.add(lineIndicator,BorderLayout.CENTER);
    info.add(removeBreakPoint,BorderLayout.EAST);
    setLayout(new BorderLayout());
    add(info,BorderLayout.NORTH);
    add(debugScrollPane);
    oldCaretPos = -1;
    debugLines = new HashMap<Integer,Integer>();
    localeChanged();
    LocaleManager.addLocaleListener(this);
  }
  
  public void loadProgram(CircuitState state, SocProcessorInterface pIf,
		  ElfProgramHeader progInfo,ElfSectionHeader sectInfo, AssemblerInterface assembler) {
      debugLines.clear();
      debugScrollPane.getGutter().removeAllTrackingIcons();
      asmWindow.setText(assembler.getProgram(state, pIf, progInfo, sectInfo, debugLines));
      asmWindow.setCaretPosition(0);
  }
  
  public void gotoLine(int line) {
    Element root = asmWindow.getDocument().getDefaultRootElement();
    int curetPos = 0;
    while (root.getElementIndex(curetPos) != line && curetPos < root.getEndOffset()) curetPos++;
    asmWindow.setCaretPosition(curetPos);
  }
  
  public HashMap<Integer,Integer> getBreakPoints() {
    HashMap<Integer,Integer> breakPoints = new HashMap<Integer,Integer>();
    for (int i : getBreakpointLines()) breakPoints.put(debugLines.get(i),i);
    return breakPoints;
  }

  @Override
  public void caretUpdate(CaretEvent e) {
    int caretPos = e.getDot();
    if (caretPos != oldCaretPos) {
      oldCaretPos = caretPos;
      Element root = asmWindow.getDocument().getDefaultRootElement();
      currentLine = root.getElementIndex(caretPos)+1;
      maxLines = root.getElementCount();
      localeChanged();
      ArrayList<Integer> dlines = getBreakpointLines();
      if (!debugLines.containsKey(currentLine)) {
        addBreakPoint.setEnabled(false);
        removeBreakPoint.setEnabled(false);
      } else if (dlines.contains(currentLine)) {
        removeBreakPoint.setEnabled(true);
        addBreakPoint.setEnabled(false);
      } else {
        removeBreakPoint.setEnabled(false);
        addBreakPoint.setEnabled(true);
      }
    }
  }

  @Override
  public void localeChanged() {
    lineIndicator.setText(S.fmt("RV32imAsmLineIndicator", currentLine, maxLines));
    addBreakPoint.setText(S.get("RV32imSetBreakpoint"));
    removeBreakPoint.setText(S.get("RV32imRemoveBreakPoint"));
  }
  
  private ArrayList<Integer> getBreakpointLines() {
    ArrayList<Integer> lines = new ArrayList<Integer>();
    GutterIconInfo[] bookmarks = debugScrollPane.getGutter().getBookmarks();
    Element root = asmWindow.getDocument().getDefaultRootElement();
    for (int i = 0; i < bookmarks.length ; i++) {
      int pos = bookmarks[i].getMarkedOffset();
      int line = root.getElementIndex(pos)+1;
      if (debugLines.containsKey(line))
        lines.add(line);
      else
        try { 
          debugScrollPane.getGutter().toggleBookmark(line-1);
        } catch (BadLocationException e) {  }
    }
    return lines;
  }
  
  private void updateBreakpoint() {
    if (getBreakpointLines().contains(currentLine)) {
      try { 
        debugScrollPane.getGutter().toggleBookmark(currentLine-1);
		} catch (BadLocationException e) { return; }
      addBreakPoint.setEnabled(true);
      removeBreakPoint.setEnabled(false);
    } else {
      try { 
        debugScrollPane.getGutter().toggleBookmark(currentLine-1);
      } catch (BadLocationException e) { return; }
      addBreakPoint.setEnabled(false);
      removeBreakPoint.setEnabled(true);
    }
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == addBreakPoint || e.getSource() == removeBreakPoint) updateBreakpoint();
  }

  @Override
  public void keyTyped(KeyEvent e) { }

  @Override
  public void keyPressed(KeyEvent e) {
    if (e.getKeyChar() == 'b' && debugLines.containsKey(currentLine)) updateBreakpoint();
  }

  @Override
  public void keyReleased(KeyEvent e) {}

}
