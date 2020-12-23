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

package com.cburch.logisim.fpga.gui;

import static com.cburch.logisim.fpga.Strings.S;

import com.cburch.logisim.fpga.data.BoardInformation;
import com.cburch.logisim.fpga.data.ComponentMapParser;
import com.cburch.logisim.fpga.data.MappableResourcesContainer;
import com.cburch.logisim.fpga.file.XMLFileFilter;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.LocaleListener;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComponentMapDialog implements ActionListener, WindowListener, 
        LocaleListener, ComponentListener {

  static final Logger logger = LoggerFactory.getLogger(ComponentMapDialog.class);

  private JDialog panel;
  private JFrame parent;
  private JButton DoneButton = new JButton();
  private JButton SaveButton = new JButton();
  private JButton CancelButton = new JButton();
  private JButton LoadButton = new JButton();
  private JLabel UnmappedText = new JLabel();
  private JLabel MappedText = new JLabel();
  private JLabel CommandText = new JLabel();
  private JScrollPane UnMappedPane;
  private JScrollPane MappedPane;

  private BoardManipulator BoardPic;
  private BoardInformation BoardInfo;
  private String OldDirectory = "";

  private MappableResourcesContainer MappableComponents;

  private Object lock = new Object();
  private boolean canceled = true;

  public ComponentMapDialog(JFrame parentFrame, String projectPath, BoardInformation Board,
                            MappableResourcesContainer mappable) {
    OldDirectory = new File(projectPath).getParent();
    if (OldDirectory == null) OldDirectory = "";
    else if (OldDirectory.length() != 0 && !OldDirectory.endsWith(File.separator))
      OldDirectory += File.separator;
    
    parent = parentFrame;
    if (parent != null) parent.addWindowListener(this);
    
    BoardInfo = Board;
    MappableComponents = mappable;

    panel = new JDialog(parentFrame);
    panel.addWindowListener(this);
    panel.setResizable(false);
    panel.setAlwaysOnTop(true);
    panel.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);

    GridBagLayout thisLayout = new GridBagLayout();
    GridBagConstraints c = new GridBagConstraints();
    panel.setLayout(thisLayout);

    /* Add the board Picture */
    BoardPic = new BoardManipulator(panel,parentFrame, mappable);
    BoardPic.addComponentListener(this);
    c.gridx = 0;

    /* Add some text */
    UnmappedText.setHorizontalAlignment(JLabel.CENTER);
    UnmappedText.setPreferredSize(new Dimension(BoardPic.getWidth() / 3, AppPreferences.getScaled(25)));
    c.gridx = 0;
    c.gridy = 0;
    c.fill = GridBagConstraints.HORIZONTAL;
    c.gridwidth = 1;
    panel.add(UnmappedText, c);
    MappedText.setHorizontalAlignment(JLabel.CENTER);
    MappedText.setPreferredSize(new Dimension(BoardPic.getWidth() / 3, AppPreferences.getScaled(25)));
    c.gridx = 1;
    panel.add(MappedText, c);
    CommandText.setHorizontalAlignment(JLabel.CENTER);
    CommandText.setPreferredSize(new Dimension(BoardPic.getWidth() / 3, AppPreferences.getScaled(25)));
    c.gridx = 2;
    panel.add(CommandText, c);

    c.gridy = 1;
    panel.add(BoardPic.getUnmapOneButton(), c);

    /* Add the UnMapAll button */
    c.gridy = 2;
    panel.add(BoardPic.getUnmapAllButton(), c);

    /* Add the Load button */
    LoadButton.setActionCommand("Load");
    LoadButton.addActionListener(this);
    LoadButton.setEnabled(true);
    c.gridy = 3;
    panel.add(LoadButton, c);

    /* Add the Save button */
    SaveButton.setActionCommand("Save");
    SaveButton.addActionListener(this);
    SaveButton.setEnabled(true);
    c.gridy = 4;
    panel.add(SaveButton, c);

    /* Add the Cancel button */
    CancelButton.setActionCommand("Cancel");
    CancelButton.addActionListener(this);
    CancelButton.setEnabled(true);
    c.gridy = 5;
    panel.add(CancelButton, c);

    /* Add the Done button */
    DoneButton.setActionCommand("Done");
    DoneButton.addActionListener(this);
    c.gridy = 6;
    panel.add(DoneButton, c);

    /* Add the Zoom button */
    c.gridy = 7;
    panel.add(BoardPic.getZoomSlider(), c);


    /* Add the unmapped list */
    UnMappedPane = new JScrollPane(BoardPic.getUnmappedList());
    c.fill = GridBagConstraints.BOTH;
    c.gridx = 0;
    c.gridy = 1;
    c.gridheight = 7;
    panel.add(UnMappedPane, c);

    /* Add the mapped list */
    MappedPane = new JScrollPane(BoardPic.getMappedList());
    c.gridx = 1;
    c.gridheight = 7;
    panel.add(MappedPane, c);

    c.gridx = 0;
    c.gridheight = 1;
    c.gridy = 8;
    c.gridwidth = 3;
    c.fill = GridBagConstraints.BOTH;
    panel.add(BoardPic, c);
    panel.setLocationRelativeTo(null);
    panel.setVisible(true);
    localeChanged();
    int ScreenWidth = (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth();
    int ScreenHeight = (int) Toolkit.getDefaultToolkit().getScreenSize().getHeight();
    int ImageWidth = BoardPic.getWidth();
    int ImageHeight = BoardPic.getHeight();
    int ImageXBorder = panel.getWidth() - ImageWidth;
    int ImageYBorder = panel.getHeight() - ImageHeight;
    ScreenWidth -= ImageXBorder;
    ScreenHeight -= (ImageYBorder + (ImageYBorder >> 2));
    int zoomX = (ScreenWidth * 100) / ImageWidth;
    int zoomY = (ScreenHeight * 100) / ImageHeight;
    BoardPic.setMaxZoom( Math.min( zoomX, zoomY ) );
  }

  public boolean run() {
    Thread t =
        new Thread() {
          public void run() {
            synchronized (lock) {
              try {
                lock.wait();
              } catch (InterruptedException e) {
                logger.error("Bug: unable to wait for lock");
              }
            }
          }
        };
    t.run();
    CancelButton.setEnabled(true);
    try {
      t.join();
    } catch (InterruptedException e) {
      logger.error("Bug: unable to join");
    }
    panel.setVisible(false);
    panel.dispose();
    BoardPic.cleanup();
    return !canceled;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getActionCommand().equals("Done")) {
      canceled = false;
      synchronized (lock) {
        lock.notify();
      }
    } else if (e.getActionCommand().equals("Save")) {
      Save();
    } else if (e.getActionCommand().equals("Load")) {
      Load();
      MappableComponents.markChanged();
    } else if (e.getActionCommand().equals("Cancel")) {
      synchronized (lock) {
        lock.notify();
      }
    }
  }

  private void Load() {
    JFileChooser fc = new JFileChooser(OldDirectory);
    fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    fc.setDialogTitle("Choose XML board description file to use");
    fc.setFileFilter(XMLFileFilter.XML_FILTER);
    fc.setAcceptAllFileFilterUsed(false);
    panel.setVisible(false);
    int retval = fc.showOpenDialog(null);
    if (retval == JFileChooser.APPROVE_OPTION) {
      File file = fc.getSelectedFile();
      String FileName = file.getName();
      String AbsoluteFileName = file.getPath();
      OldDirectory = AbsoluteFileName.substring(0, AbsoluteFileName.length() - FileName.length());
      ComponentMapParser parse = new ComponentMapParser(file,MappableComponents,BoardInfo);
      int result = parse.parseFile();
      if (result == 0) {
        panel.setVisible(true);
        BoardPic.update();
      } else {
    	 OptionPane.showMessageDialog(null, parse.getError(result), "Error", OptionPane.ERROR_MESSAGE);
         panel.setVisible(true);
      }
    }
  }

  private void Save() {
	panel.setVisible(false);
    MappableComponents.save();
    OptionPane.showMessageDialog(null, S.get("BoarMapFileSaved"), "", OptionPane.INFORMATION_MESSAGE);
	panel.setVisible(true);
  }

  @Override
  public void windowOpened(WindowEvent e) {}

  @Override
  public void windowClosing(WindowEvent e) {
    synchronized (lock) {
      lock.notify();
    }
  }

  @Override
  public void windowClosed(WindowEvent e) {}

  @Override
  public void windowIconified(WindowEvent e) {}

  @Override
  public void windowDeiconified(WindowEvent e) {}

  @Override
  public void windowActivated(WindowEvent e) {}

  @Override
  public void windowDeactivated(WindowEvent e) {}

  @Override
  public void localeChanged() {
    panel.setTitle(S.get("BoardMapTitle"));
    UnmappedText.setText(S.get("BoardMapUnmapped"));
    UnmappedText.setToolTipText(S.get("BoardMapUMTooltip"));
    MappedText.setText(S.get("BoardMapMapped"));
    CommandText.setText(S.get("BoardMapActions"));
    LoadButton.setText(S.get("BoardMapLoad"));
    SaveButton.setText(S.get("BoardMapSave"));
    CancelButton.setText(S.get("FpgaBoardCancel"));
    DoneButton.setText(S.get("FpgaBoardDone"));
    panel.pack();
  }

  @Override
  public void componentResized(ComponentEvent e) {
    UnmappedText.setPreferredSize(new Dimension(BoardPic.getWidth() / 3, AppPreferences.getScaled(25)));
    MappedText.setPreferredSize(new Dimension(BoardPic.getWidth() / 3, AppPreferences.getScaled(25)));
    CommandText.setPreferredSize(new Dimension(BoardPic.getWidth() / 3, AppPreferences.getScaled(25)));
    panel.pack();
  }

  @Override
  public void componentMoved(ComponentEvent e) { }

  @Override
  public void componentShown(ComponentEvent e) { }

  @Override
  public void componentHidden(ComponentEvent e) { }
}
