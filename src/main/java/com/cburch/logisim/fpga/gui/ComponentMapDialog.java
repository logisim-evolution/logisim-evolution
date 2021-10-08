/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.gui;

import static com.cburch.logisim.fpga.Strings.S;

import com.cburch.contracts.BaseComponentListenerContract;
import com.cburch.contracts.BaseWindowListenerContract;
import com.cburch.logisim.fpga.data.BoardInformation;
import com.cburch.logisim.fpga.data.ComponentMapParser;
import com.cburch.logisim.fpga.data.MappableResourcesContainer;
import com.cburch.logisim.fpga.file.XmlFileFilter;
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
import java.awt.event.WindowEvent;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComponentMapDialog implements ActionListener, BaseWindowListenerContract, LocaleListener, BaseComponentListenerContract {

  static final Logger logger = LoggerFactory.getLogger(ComponentMapDialog.class);

  private final JDialog panel;
  private final JFrame parent;
  private final JButton doneButton = new JButton();
  private final JButton saveButton = new JButton();
  private final JButton cancelButton = new JButton();
  private final JButton loadButton = new JButton();
  private final JLabel unmappedText = new JLabel();
  private final JLabel mappedText = new JLabel();
  private final JLabel commandText = new JLabel();
  private final JScrollPane unmappedPane;
  private final JScrollPane mappedPane;

  private final BoardManipulator boardPicture;
  private final BoardInformation boardInfo;
  private String oldDirectory = "";

  private final MappableResourcesContainer mappableComponents;

  private final Object lock = new Object();
  private boolean canceled = true;

  public ComponentMapDialog(JFrame parentFrame, String projectPath, BoardInformation board, MappableResourcesContainer mappable) {
    oldDirectory = new File(projectPath).getParent();
    if (oldDirectory == null) oldDirectory = "";
    else if (oldDirectory.length() != 0 && !oldDirectory.endsWith(File.separator))
      oldDirectory += File.separator;

    parent = parentFrame;
    if (parent != null) parent.addWindowListener(this);

    boardInfo = board;
    mappableComponents = mappable;

    panel = new JDialog(parentFrame);
    panel.addWindowListener(this);
    panel.setResizable(false);
    panel.setAlwaysOnTop(true);
    panel.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);

    GridBagLayout thisLayout = new GridBagLayout();
    GridBagConstraints c = new GridBagConstraints();
    panel.setLayout(thisLayout);

    /* Add the board Picture */
    boardPicture = new BoardManipulator(panel, parentFrame, mappable);
    boardPicture.addComponentListener(this);
    c.gridx = 0;

    /* Add some text */
    unmappedText.setHorizontalAlignment(JLabel.CENTER);
    unmappedText.setPreferredSize(new Dimension(boardPicture.getWidth() / 3, AppPreferences.getScaled(25)));
    c.gridx = 0;
    c.gridy = 0;
    c.fill = GridBagConstraints.HORIZONTAL;
    c.gridwidth = 1;
    panel.add(unmappedText, c);
    mappedText.setHorizontalAlignment(JLabel.CENTER);
    mappedText.setPreferredSize(new Dimension(boardPicture.getWidth() / 3, AppPreferences.getScaled(25)));
    c.gridx = 1;
    panel.add(mappedText, c);
    commandText.setHorizontalAlignment(JLabel.CENTER);
    commandText.setPreferredSize(new Dimension(boardPicture.getWidth() / 3, AppPreferences.getScaled(25)));
    c.gridx = 2;
    panel.add(commandText, c);

    c.gridy = 1;
    panel.add(boardPicture.getUnmapOneButton(), c);

    /* Add the UnMapAll button */
    c.gridy = 2;
    panel.add(boardPicture.getUnmapAllButton(), c);

    /* Add the Load button */
    loadButton.setActionCommand("Load");
    loadButton.addActionListener(this);
    loadButton.setEnabled(true);
    c.gridy = 3;
    panel.add(loadButton, c);

    /* Add the Save button */
    saveButton.setActionCommand("Save");
    saveButton.addActionListener(this);
    saveButton.setEnabled(true);
    c.gridy = 4;
    panel.add(saveButton, c);

    /* Add the Cancel button */
    cancelButton.setActionCommand("Cancel");
    cancelButton.addActionListener(this);
    cancelButton.setEnabled(true);
    c.gridy = 5;
    panel.add(cancelButton, c);

    /* Add the Done button */
    doneButton.setActionCommand("Done");
    doneButton.addActionListener(this);
    c.gridy = 6;
    panel.add(doneButton, c);

    /* Add the Zoom button */
    c.gridy = 7;
    panel.add(boardPicture.getZoomSlider(), c);


    /* Add the unmapped list */
    unmappedPane = new JScrollPane(boardPicture.getUnmappedList());
    c.fill = GridBagConstraints.BOTH;
    c.gridx = 0;
    c.gridy = 1;
    c.gridheight = 7;
    panel.add(unmappedPane, c);

    /* Add the mapped list */
    mappedPane = new JScrollPane(boardPicture.getMappedList());
    c.gridx = 1;
    c.gridheight = 7;
    panel.add(mappedPane, c);

    c.gridx = 0;
    c.gridheight = 1;
    c.gridy = 8;
    c.gridwidth = 3;
    c.fill = GridBagConstraints.BOTH;
    panel.add(boardPicture, c);
    panel.setLocationRelativeTo(null);
    panel.setVisible(true);
    localeChanged();
    int ScreenWidth = (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth();
    int ScreenHeight = (int) Toolkit.getDefaultToolkit().getScreenSize().getHeight();
    int ImageWidth = boardPicture.getWidth();
    int ImageHeight = boardPicture.getHeight();
    int ImageXBorder = panel.getWidth() - ImageWidth;
    int ImageYBorder = panel.getHeight() - ImageHeight;
    ScreenWidth -= ImageXBorder;
    ScreenHeight -= (ImageYBorder + (ImageYBorder >> 2));
    int zoomX = (ScreenWidth * 100) / ImageWidth;
    int zoomY = (ScreenHeight * 100) / ImageHeight;
    boardPicture.setMaxZoom(Math.min(zoomX, zoomY));
  }

  public boolean run() {
    Thread t =
        new Thread(() -> {
          synchronized (lock) {
            try {
              lock.wait();
            } catch (InterruptedException e) {
              logger.error("Bug: unable to wait for lock");
            }
          }
        });
    t.start();
    cancelButton.setEnabled(true);
    try {
      t.join();
    } catch (InterruptedException e) {
      logger.error("Bug: unable to join");
    }
    panel.setVisible(false);
    panel.dispose();
    boardPicture.cleanup();
    return !canceled;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    switch (e.getActionCommand()) {
      case "Done":
        canceled = false;
        synchronized (lock) {
          lock.notify();
        }
        break;
      case "Save":
        save();
        break;
      case "Load":
        load();
        mappableComponents.markChanged();
        break;
      case "Cancel":
        synchronized (lock) {
          lock.notify();
        }
        break;
    }
  }

  private void load() {
    final var fc = new JFileChooser(oldDirectory);
    fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    // FIXME: hardcoded string
    fc.setDialogTitle("Choose XML board description file to use");
    fc.setFileFilter(XmlFileFilter.XML_FILTER);
    fc.setAcceptAllFileFilterUsed(false);
    panel.setVisible(false);
    final var retVal = fc.showOpenDialog(null);
    if (retVal == JFileChooser.APPROVE_OPTION) {
      final var file = fc.getSelectedFile();
      final var fileName = file.getName();
      final var absoluteFileName = file.getPath();
      oldDirectory = absoluteFileName.substring(0, absoluteFileName.length() - fileName.length());
      final var parse = new ComponentMapParser(file, mappableComponents, boardInfo);
      final var result = parse.parseFile();
      if (result == 0) {
        panel.setVisible(true);
        boardPicture.update();
      } else {
        // FIXME: hardcoded string
        OptionPane.showMessageDialog(null, parse.getError(result), "Error", OptionPane.ERROR_MESSAGE);
        panel.setVisible(true);
      }
    }
  }

  private void save() {
    panel.setVisible(false);
    mappableComponents.save();
    OptionPane.showMessageDialog(
        null, S.get("BoarMapFileSaved"), "", OptionPane.INFORMATION_MESSAGE);
    panel.setVisible(true);
  }

  @Override
  public void windowClosing(WindowEvent e) {
    synchronized (lock) {
      lock.notify();
    }
  }

  @Override
  public void localeChanged() {
    panel.setTitle(S.get("BoardMapTitle"));
    unmappedText.setText(S.get("BoardMapUnmapped"));
    unmappedText.setToolTipText(S.get("BoardMapUMTooltip"));
    mappedText.setText(S.get("BoardMapMapped"));
    commandText.setText(S.get("BoardMapActions"));
    loadButton.setText(S.get("BoardMapLoad"));
    saveButton.setText(S.get("BoardMapSave"));
    cancelButton.setText(S.get("FpgaBoardCancel"));
    doneButton.setText(S.get("FpgaBoardDone"));
    panel.pack();
  }

  @Override
  public void componentResized(ComponentEvent e) {
    unmappedText.setPreferredSize(new Dimension(boardPicture.getWidth() / 3, AppPreferences.getScaled(25)));
    mappedText.setPreferredSize(new Dimension(boardPicture.getWidth() / 3, AppPreferences.getScaled(25)));
    commandText.setPreferredSize(new Dimension(boardPicture.getWidth() / 3, AppPreferences.getScaled(25)));
    panel.pack();
  }

}
