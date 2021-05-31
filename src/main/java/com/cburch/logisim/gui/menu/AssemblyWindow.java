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

package com.cburch.logisim.gui.menu;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.circuit.Simulator.Event;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.generic.LFrame;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.memory.Register;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;

public class AssemblyWindow
    implements ActionListener, WindowListener, Simulator.Listener, KeyListener {

  private static Circuit curCircuit;
  private static CircuitState curCircuitState;
  private final Preferences prefs;
  private final LFrame windows;
  private final JMenuBar winMenuBar;
  private final JCheckBoxMenuItem ontopItem;
  private final JMenuItem openFileItem;
  private final JMenuItem reloadFileItem;
  private final JMenuItem close;
  private final JButton refresh = new JButton("Get Registers");
  private final JLabel status = new JLabel();
  private final JEditorPane document = new JEditorPane();

  @SuppressWarnings("rawtypes")
  private final JComboBox combo = new JComboBox<>();

  private final HashMap<String, Component> entry = new HashMap<>();
  private final Project proj;
  private Component selReg = null;
  private File file;

  public AssemblyWindow(Project proj) {

    this.proj = proj;
    curCircuit = proj.getCurrentCircuit();
    curCircuitState = proj.getCircuitState();
    winMenuBar = new JMenuBar();
    JMenu windowMenu = new JMenu("Window");
    JMenu fileMenu = new JMenu("File");
    JPanel main = new JPanel(new BorderLayout());
    JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
    /* LinePainter painter = new LinePainter(document); */

    windowMenu.setMnemonic('W');
    fileMenu.setMnemonic('F');
    combo.addActionListener(this);
    combo.setFocusable(false);
    refresh.addActionListener(this);
    refresh.setFocusable(false);
    refresh.setToolTipText("Get register list of current displayed circuit.");

    ontopItem = new JCheckBoxMenuItem("Set on top", true);
    ontopItem.addActionListener(this);
    openFileItem = new JMenuItem("Open lss file");
    openFileItem.addActionListener(this);
    reloadFileItem = new JMenuItem("Reload lss file");
    reloadFileItem.addActionListener(this);
    close = new JMenuItem("Close");
    close.addActionListener(this);
    winMenuBar.add(fileMenu);
    winMenuBar.add(windowMenu);
    winMenuBar.setFocusable(false);
    windowMenu.add(ontopItem);
    fileMenu.add(openFileItem);
    fileMenu.add(reloadFileItem);
    fileMenu.addSeparator();
    fileMenu.add(close);

    windows = new LFrame.Dialog(null);
    windows.setTitle("Assembly: " + proj.getLogisimFile().getDisplayName());
    windows.setJMenuBar(winMenuBar);
    windows.toFront();
    windows.setAlwaysOnTop(true);
    windows.setVisible(false);
    windows.addWindowListener(this);
    windows.addKeyListener(this);

    north.add(new JLabel("Register: "));
    north.add(combo);
    north.add(refresh);

    document.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
    document.setEditable(false);
    document.setPreferredSize(
        new Dimension(document.getWidth() * 4 / 5, Math.max(200, document.getHeight() * 2 / 3)));
    document.addKeyListener(this);
    main.add(new JScrollPane(document), BorderLayout.CENTER);
    main.add(north, BorderLayout.NORTH);
    main.add(status, BorderLayout.SOUTH);
    windows.setContentPane(main);
    proj.getSimulator().addSimulatorListener(this);

    windows.pack();
    prefs = Preferences.userRoot().node(this.getClass().getName());
    windows.setLocation(prefs.getInt("X", 0), prefs.getInt("Y", 0));
    windows.setSize(
        prefs.getInt("W", windows.getSize().width), prefs.getInt("H", windows.getSize().height));
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    Object src = e.getSource();
    if (src == ontopItem) {
      e.paramString();
      windows.setAlwaysOnTop(ontopItem.getState());
    } else if (src == openFileItem) {
      final JFileChooser fileChooser = proj.createChooser();
      FileFilter ff =
          new FileFilter() {
            @Override
            public boolean accept(File f) {
              return f.isDirectory() || f.getName().toLowerCase().endsWith(".lss");
            }

            @Override
            public String getDescription() {
              return ".lss disassembly file";
            }
          };
      fileChooser.setFileFilter(ff);
      fileChooser.setAcceptAllFileFilterUsed(false);
      int result = fileChooser.showOpenDialog(windows);
      if (result == JFileChooser.APPROVE_OPTION) {
        file = fileChooser.getSelectedFile();
        try {
          if (file.getName().toLowerCase().endsWith(".lss")) {
            status.setText("");
            document.setPage(file.toURI().toURL());
          } else {
            status.setText("Wrong file selected !");
            file = null;
          }
        } catch (Exception ex) {
          status.setText("Cannot open file !");
          file = null;
        }
      }
      // Allow reload of same file
      document.getDocument().putProperty(Document.StreamDescriptionProperty, null);
      windows.invalidate();
    } else if (src == reloadFileItem) {
      if (file != null) {
        try {
          document.setPage(file.toURI().toURL());
          status.setText("File reloaded.");
        } catch (Exception ex) {
          status.setText("Cannot open file !");
          file = null;
        }
      }
      windows.invalidate();
    } else if (src == refresh) {
      curCircuit = proj.getCurrentCircuit();
      curCircuitState = proj.getCircuitState();
      fillCombo();
      updateHighlightLine();
    } else if (src == combo) {
      updateHighlightLine();
    } else if (src == close) {
      setVisible(false);
    }
  }

  @SuppressWarnings("unchecked")
  private void fillCombo() {
    Set<Component> comps = curCircuit.getNonWires();
    Iterator<Component> iter = comps.iterator();
    entry.clear();
    while (iter.hasNext()) {
      Component comp = iter.next();
      if (comp.getFactory().getName().equals("Register")) {
        if (!comp.getAttributeSet().getValue(StdAttr.LABEL).equals("")) {
          entry.put(comp.getAttributeSet().getValue(StdAttr.LABEL), comp);
        }
      }
    }

    combo.removeAllItems();

    if (entry.isEmpty()) {
      status.setText("No labeled registers found.");
      combo.setEnabled(false);
    } else {
      status.setText("");
      combo.setEnabled(true);
      Object[] objArr = entry.keySet().toArray();
      Arrays.sort(objArr);
      for (Object o : objArr) {
        combo.addItem(o);
      }
    }
  }

  public boolean isVisible() {
    if (windows != null) {
      return windows.isVisible();
    } else {
      return false;
    }
  }

  public void setVisible(boolean bool) {
    fillCombo();
    windows.setVisible(bool);
  }

  @Override
  public void keyPressed(KeyEvent ke) {
    // throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void keyReleased(KeyEvent ke) {
    int keyCode = ke.getKeyCode();
    if (keyCode == KeyEvent.VK_F2) {
      if (proj.getSimulator() != null) proj.getSimulator().tick(2);
    }
  }

  @Override
  public void keyTyped(KeyEvent ke) {
    // throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void propagationCompleted(Simulator.Event e) {
    if (e.getSource().isAutoTicking()) {
      updateHighlightLine();
    }
  }

  public void setTitle(String title) {
    windows.setTitle(title);
  }

  @Override
  public void simulatorStateChanged(Simulator.Event e) {}

  /*
   * Track the movement of the Caret by painting a background line at the
   * current caret position.
   */

  public void toFront() {
    if (windows != null) {
      windows.toFront();
    }
  }

  private void updateHighlightLine() {
    String where;
    if (combo.getSelectedItem() != null) {
      selReg = entry.get(combo.getSelectedItem().toString());
      Value val = curCircuitState.getInstanceState(selReg).getPortValue(Register.OUT);
      if (val.isFullyDefined()) {
        where = val.toHexString().replaceAll("^0*", "");
        if (where.isEmpty()) {
          where = "0";
        }
        Pattern pattern =
            Pattern.compile("^[ ]+" + where + ":", Pattern.MULTILINE + Pattern.CASE_INSENSITIVE);
        Matcher m = pattern.matcher(document.getText().replaceAll("\r", ""));
        if (m.find()) {
          document.setCaretPosition(m.start());
          status.setText("");
          try { // bug fix, highligh active line
            document.getHighlighter().removeAllHighlights();
            DefaultHighlighter.DefaultHighlightPainter highlightPainter =
                new DefaultHighlighter.DefaultHighlightPainter(Color.LIGHT_GRAY);
            document.getHighlighter().addHighlight(m.start(), m.end(), highlightPainter);
          } catch (BadLocationException ex) {
            ex.printStackTrace();
          }
        } else {
          status.setText("Line (" + where + ") not found!");
        }
      }
    }
  }

  @Override
  public void windowActivated(WindowEvent e) {}

  @Override
  public void windowClosed(WindowEvent e) {}

  @Override
  public void windowClosing(WindowEvent e) {}

  @Override
  public void windowDeactivated(WindowEvent e) {
    prefs.putInt("X", windows.getX());
    prefs.putInt("Y", windows.getY());
    prefs.putInt("W", windows.getWidth());
    prefs.putInt("H", windows.getHeight());
  }

  @Override
  public void windowDeiconified(WindowEvent e) {}

  @Override
  public void windowIconified(WindowEvent e) {}

  @Override
  public void windowOpened(WindowEvent e) {}

  @Override
  public void simulatorReset(Event e) { }
}
