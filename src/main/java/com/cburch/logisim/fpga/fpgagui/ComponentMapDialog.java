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

package com.cburch.logisim.fpga.fpgagui;

import static com.cburch.logisim.fpga.Strings.S;

import com.cburch.logisim.fpga.fpgaboardeditor.BoardInformation;
import com.cburch.logisim.fpga.fpgaboardeditor.BoardRectangle;
import com.cburch.logisim.fpga.fpgaboardeditor.ZoomSlider;
import com.cburch.logisim.prefs.AppPreferences;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComponentMapDialog implements ActionListener, ListSelectionListener, WindowListener {

  private class MappedComponentIdContainer {

    private String key;
    private BoardRectangle rect;

    public MappedComponentIdContainer(String key, BoardRectangle SelectedItem) {
      rect = SelectedItem;
      String label = rect.GetLabel();
      if (label != null && label.length() != 0) {
        if (key == null || key.length() == 0) key = "[" + label + "]";
        else key = "[" + label + "] " + key;
      }
      this.key = key;
    }

    public BoardRectangle getRectangle() {
      return rect;
    }

    public void Paint(Graphics g) {
      g.setFont(AppPreferences.getScaledFont(g.getFont()));
      FontMetrics metrics = g.getFontMetrics(g.getFont());
      Color Yellow = new Color(250, 250, 0, 180);
      Color Blue = new Color(0, 0, 250, 180);
      int hgt = metrics.getHeight();
      int adv = metrics.stringWidth(key);
      int real_xpos = AppPreferences.getScaled(rect.getXpos(), scale) - adv / 2 - 2;
      if (real_xpos < 0) {
        real_xpos = 0;
      } else if (real_xpos + adv + 4 > AppPreferences.getScaled(image_width, scale)) {
        real_xpos = AppPreferences.getScaled(image_width, scale) - adv - 4;
      }
      int real_ypos = AppPreferences.getScaled(rect.getYpos(), scale);
      if (real_ypos - hgt - 4 < 0) {
        real_ypos = hgt + 4;
      }
      g.setColor(Yellow);
      g.fillRect(real_xpos, real_ypos - hgt - 4, adv + 4, hgt + 4);
      g.drawRect(
          AppPreferences.getScaled(rect.getXpos(), scale) + 1,
          AppPreferences.getScaled(rect.getYpos(), scale),
          AppPreferences.getScaled(rect.getWidth(), scale) - 2,
          AppPreferences.getScaled(rect.getHeight(), scale) - 1);
      g.setColor(Blue);
      g.drawString(key, real_xpos + 2, real_ypos - 2 - metrics.getMaxDescent());
    }
  }

  @SuppressWarnings("serial")
  private class SelectionWindow extends JPanel implements MouseListener, MouseMotionListener {

    public SelectionWindow() {
      this.addMouseListener(this);
      this.addMouseMotionListener(this);
    }

    @Override
    public int getHeight() {
      return AppPreferences.getScaled(image_height, scale);
    }

    @Override
    public int getWidth() {
      return AppPreferences.getScaled(image_width, scale);
    }

    private void HandleSelect(MouseEvent e) {
      if (!SelectableItems.isEmpty()) {
        if (HighlightItem != null) {
          if (HighlightItem.PointInside(
              AppPreferences.getDownScaled(e.getX(), scale),
              AppPreferences.getDownScaled(e.getY(), scale))) {
            return;
          }
        }
        BoardRectangle NewItem = null;
        /* TODO: Optimize, SLOW! */
        for (BoardRectangle Item : SelectableItems) {
          if (Item.PointInside(
              AppPreferences.getDownScaled(e.getX(), scale),
              AppPreferences.getDownScaled(e.getY(), scale))) {
            NewItem = Item;
            break;
          }
        }
        if ((NewItem == null && HighlightItem != null) || (NewItem != HighlightItem)) {
          HighlightItem = NewItem;
          paintImmediately(0, 0, this.getWidth(), this.getHeight());
        }
      }
    }

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mouseDragged(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {
      Note = null;
      paintImmediately(0, 0, this.getWidth(), this.getHeight());
    }

    @Override
    public void mouseMoved(MouseEvent e) {
      HandleSelect(e);
      if (MappableComponents.hasMappedComponents() || !SelectableItems.isEmpty()) {
        if (Note != null) {
          if (Note.getRectangle()
              .PointInside(
                  AppPreferences.getDownScaled(e.getX(), scale),
                  AppPreferences.getDownScaled(e.getY(), scale))) {
            return;
          }
        }
        BoardRectangle NewItem = null;
        String newKey = "";
        /* TODO: This makes the things very slow optimize! */
        for (BoardRectangle ThisItem : MappableComponents.GetMappedRectangles()) {
          if (ThisItem.PointInside(
              AppPreferences.getDownScaled(e.getX(), scale),
              AppPreferences.getDownScaled(e.getY(), scale))) {
            NewItem = ThisItem;
            newKey = MappableComponents.GetDisplayName(ThisItem);
            break;
          }
        }
        if (NewItem == null) {
          for (BoardRectangle Item : SelectableItems) {
            if (Item.PointInside(
                    AppPreferences.getDownScaled(e.getX()), AppPreferences.getDownScaled(e.getY()))
                && Item.GetLabel() != null
                && Item.GetLabel().length() != 0) {
              NewItem = Item;
              break;
            }
          }
        }
        if (Note == null) {
          if (NewItem != null) {
            Note = new MappedComponentIdContainer(newKey, NewItem);
            this.paintImmediately(0, 0, this.getWidth(), this.getHeight());
          }
        } else {
          if (!Note.getRectangle().equals(NewItem)) {
            if (NewItem != null) {
              Note = new MappedComponentIdContainer(newKey, NewItem);
              this.paintImmediately(0, 0, this.getWidth(), this.getHeight());
            } else {
              Note = null;
              this.paintImmediately(0, 0, this.getWidth(), this.getHeight());
            }
          }
        }
      }
    }

    @Override
    public void mousePressed(MouseEvent e) {}

    @Override
    public void mouseReleased(MouseEvent e) {
      if (HighlightItem != null) {
        MapOne();
      }
    }

    @Override
    public void paint(Graphics g) {
      super.paint(g);
      Color Black = new Color(0, 0, 0, 150);
      Image image =
          BoardInfo.GetImage()
              .getScaledInstance(
                  AppPreferences.getScaled(image_width, scale),
                  AppPreferences.getScaled(image_height, scale),
                  Image.SCALE_SMOOTH);
      if (image != null) {
        g.drawImage(image, 0, 0, null);
      }
      for (BoardRectangle rect : MappableComponents.GetMappedRectangles()) {
        boolean cadre = false;
        if (MappedHighlightItem != null) {
          if (MappedHighlightItem.equals(rect)) {
            g.setColor(Color.RED);
            cadre = true;
          } else {
            g.setColor(Black);
          }
        } else {
          g.setColor(Black);
        }
        g.fillRect(
            AppPreferences.getScaled(rect.getXpos(), scale),
            AppPreferences.getScaled(rect.getYpos(), scale),
            AppPreferences.getScaled(rect.getWidth(), scale),
            AppPreferences.getScaled(rect.getHeight(), scale));
        if (cadre) {
          g.setColor(Black);
          g.drawRect(
              AppPreferences.getScaled(rect.getXpos(), scale),
              AppPreferences.getScaled(rect.getYpos(), scale),
              AppPreferences.getScaled(rect.getWidth(), scale),
              AppPreferences.getScaled(rect.getHeight(), scale));
          if ((rect.getWidth() >= 4) && (rect.getHeight() >= 4)) {
            g.drawRect(
                AppPreferences.getScaled(rect.getXpos(), scale) + 1,
                AppPreferences.getScaled(rect.getYpos(), scale) + 1,
                AppPreferences.getScaled(rect.getWidth(), scale) - 2,
                AppPreferences.getScaled(rect.getHeight(), scale) - 2);
          }
        }
      }
      Color test = new Color(255, 0, 0, 100);
      for (BoardRectangle rect : SelectableItems) {
        g.setColor(test);
        g.fillRect(
            AppPreferences.getScaled(rect.getXpos(), scale),
            AppPreferences.getScaled(rect.getYpos(), scale),
            AppPreferences.getScaled(rect.getWidth(), scale),
            AppPreferences.getScaled(rect.getHeight(), scale));
      }
      if (HighlightItem != null && ComponentSelectionMode) {
        g.setColor(Color.RED);
        g.fillRect(
            AppPreferences.getScaled(HighlightItem.getXpos(), scale),
            AppPreferences.getScaled(HighlightItem.getYpos(), scale),
            AppPreferences.getScaled(HighlightItem.getWidth(), scale),
            AppPreferences.getScaled(HighlightItem.getHeight(), scale));
      }
      if (Note != null) {
        Note.Paint(g);
      }
    }
  }

  private static class XMLFileFilter extends FileFilter {

    @Override
    public boolean accept(File f) {
      return f.isDirectory() || f.getName().endsWith(".xml") || f.getName().endsWith(".XML");
    }

    @Override
    public String getDescription() {
      return S.get("XMLFileFilter"); // TODO: language adaptation
    }
  }

  static final Logger logger = LoggerFactory.getLogger(ComponentMapDialog.class);

  private class ZoomChange implements ChangeListener {

    private ComponentMapDialog parent;

    public ZoomChange(ComponentMapDialog parent) {
      this.parent = parent;
    }

    @Override
    public void stateChanged(ChangeEvent e) {
      JSlider source = (JSlider) e.getSource();
      if (!source.getValueIsAdjusting()) {
        int value = source.getValue();
        if (value > MaxZoom) {
          source.setValue(MaxZoom);
          value = MaxZoom;
        }
        parent.SetScale(value / (float) 100.0);
      }
    }
  }

  private JDialog panel;
  private JButton UnMapButton = new JButton();
  private JButton UnMapAllButton = new JButton();
  private JButton DoneButton = new JButton();
  private JButton SaveButton = new JButton();
  private JButton CancelButton = new JButton();
  private JButton LoadButton = new JButton();
  private ZoomSlider ScaleButton = new ZoomSlider();
  private JLabel MessageLine = new JLabel();
  private JScrollPane UnMappedPane;
  private JScrollPane MappedPane;

  @SuppressWarnings("rawtypes")
  private JList UnmappedList;

  @SuppressWarnings("rawtypes")
  private JList MappedList;

  private SelectionWindow BoardPic;
  private boolean ComponentSelectionMode;
  private BoardRectangle HighlightItem = null;
  private BoardRectangle MappedHighlightItem = null;
  private int image_width = 740;
  private int image_height = 400;
  private float scale = 1;
  private int MaxZoom;
  private BoardInformation BoardInfo;
  private ArrayList<BoardRectangle> SelectableItems = new ArrayList<BoardRectangle>();
  private String OldDirectory = "";

  private MappedComponentIdContainer Note;

  private MappableResourcesContainer MappableComponents;

  private Object lock = new Object();
  private boolean canceled = true;

  private MouseListener mouseListener =
      new MouseListener() {
        @Override
        public void mouseClicked(MouseEvent e) {
          if ((e.getClickCount() == 2)
              && (e.getButton() == MouseEvent.BUTTON1)
              && (UnmappedList.getSelectedValue() != null)) {
            int idx = UnmappedList.getSelectedIndex();
            String item = UnmappedList.getSelectedValue().toString();
            MappableComponents.ToggleAlternateMapping(item);
            RebuildSelectionLists();
            UnmappedList.setSelectedIndex(idx);
          }
        }

        @Override
        public void mouseEntered(MouseEvent e) {}

        @Override
        public void mouseExited(MouseEvent e) {}

        @Override
        public void mousePressed(MouseEvent e) {}

        @Override
        public void mouseReleased(MouseEvent e) {}
      };

  public void SetScale(float scale) {
    this.scale = scale;
    BoardPic.setPreferredSize(new Dimension(BoardPic.getWidth(), BoardPic.getHeight()));
    BoardPic.setSize(new Dimension(BoardPic.getWidth(), BoardPic.getHeight()));
    UnMappedPane.setPreferredSize(
        new Dimension(
            BoardPic.getWidth() / 3, 6 * DoneButton.getHeight() + ScaleButton.getHeight()));
    MappedPane.setPreferredSize(
        new Dimension(
            BoardPic.getWidth() / 3, 6 * DoneButton.getHeight() + ScaleButton.getHeight()));
    panel.pack();
  }

  @SuppressWarnings("rawtypes")
  public ComponentMapDialog(JFrame parrentFrame, String projectPath) {
    OldDirectory = new File(projectPath).getParent();
    if (OldDirectory == null) OldDirectory = "";
    else if (OldDirectory.length() != 0 && !OldDirectory.endsWith(File.separator))
      OldDirectory += File.separator;

    panel = new JDialog(parrentFrame);
    panel.addWindowListener(this);
    panel.setTitle("Component to FPGA board mapping");
    panel.setResizable(false);
    panel.setAlwaysOnTop(true);
    panel.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);

    GridBagLayout thisLayout = new GridBagLayout();
    GridBagConstraints c = new GridBagConstraints();
    panel.setLayout(thisLayout);
    // PointerInfo mouseloc = MouseInfo.getPointerInfo();
    // Point mlocation = mouseloc.getLocation();
    // panel.setLocation(mlocation.x, mlocation.y);

    /* Add the board Picture */
    BoardPic = new SelectionWindow();
    BoardPic.setPreferredSize(new Dimension(BoardPic.getWidth(), BoardPic.getHeight()));
    c.gridx = 0;

    /* Add some text */
    JLabel UnmappedText = new JLabel();
    UnmappedText.setText("Unmapped Components:");
    UnmappedText.setHorizontalTextPosition(JLabel.CENTER);
    UnmappedText.setPreferredSize(
        new Dimension(BoardPic.getWidth() / 3, AppPreferences.getScaled(25)));
    UnmappedText.setToolTipText(
        "<html>Select component and place it on the board.<br>"
            + "To expand component (Port, DIP, ...) or change type (Button<->Pin),<br>"
            + "double clic on it.</html>");
    c.gridx = 0;
    c.gridy = 0;
    c.fill = GridBagConstraints.HORIZONTAL;
    c.gridwidth = 1;
    panel.add(UnmappedText, c);
    JLabel MappedText = new JLabel();
    MappedText.setText("Mapped Components:");
    MappedText.setHorizontalTextPosition(JLabel.CENTER);
    MappedText.setPreferredSize(
        new Dimension(BoardPic.getWidth() / 3, AppPreferences.getScaled(25)));
    c.gridx = 1;
    panel.add(MappedText, c);
    JLabel CommandText = new JLabel();
    CommandText.setText("Command:");
    CommandText.setHorizontalTextPosition(JLabel.CENTER);
    CommandText.setPreferredSize(
        new Dimension(BoardPic.getWidth() / 3, AppPreferences.getScaled(25)));
    c.gridx = 2;
    panel.add(CommandText, c);

    /* Add the Zoom button */
    c.gridx = 2;
    c.gridy = 8;
    panel.add(ScaleButton, c);
    ScaleButton.addChangeListener(new ZoomChange(this));

    UnMapButton.setText("Release component");
    UnMapButton.setActionCommand("UnMap");
    UnMapButton.addActionListener(this);
    UnMapButton.setEnabled(false);
    c.gridx = 2;
    c.gridy = 2;
    panel.add(UnMapButton, c);

    /* Add the UnMapAll button */
    UnMapAllButton.setText("Release all components");
    UnMapAllButton.setActionCommand("UnMapAll");
    UnMapAllButton.addActionListener(this);
    UnMapAllButton.setEnabled(false);
    c.gridy = 3;
    panel.add(UnMapAllButton, c);

    /* Add the Load button */
    LoadButton.setText("Load Map");
    LoadButton.setActionCommand("Load");
    LoadButton.addActionListener(this);
    LoadButton.setEnabled(true);
    c.gridy = 4;
    panel.add(LoadButton, c);

    /* Add the Save button */
    SaveButton.setText("Save Map");
    SaveButton.setActionCommand("Save");
    SaveButton.addActionListener(this);
    SaveButton.setEnabled(false);
    c.gridy = 5;
    panel.add(SaveButton, c);

    /* Add the Cancel button */
    CancelButton.setText("Cancel");
    CancelButton.setActionCommand("Cancel");
    CancelButton.addActionListener(this);
    CancelButton.setEnabled(false);
    c.gridy = 6;
    panel.add(CancelButton, c);

    /* Add the Done button */
    DoneButton.setText("Done");
    DoneButton.setActionCommand("Done");
    DoneButton.addActionListener(this);
    DoneButton.setEnabled(false);
    c.gridy = 7;
    panel.add(DoneButton, c);

    /* Add the unmapped list */
    UnmappedList = new JList();
    UnmappedList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    UnmappedList.addListSelectionListener(this);
    UnmappedList.addMouseListener(mouseListener);
    UnMappedPane = new JScrollPane(UnmappedList);
    c.gridx = 0;
    c.gridy = 1;
    c.gridheight = 8;
    panel.add(UnMappedPane, c);
    ComponentSelectionMode = false;

    /* Add the mapped list */
    MappedList = new JList();
    MappedList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    MappedList.addListSelectionListener(this);
    MappedPane = new JScrollPane(MappedList);
    c.gridx = 1;
    c.gridy = 1;
    c.gridheight = 8;
    panel.add(MappedPane, c);

    /* Add the message line */
    MessageLine.setForeground(Color.BLUE);
    MessageLine.setText("No messages");
    MessageLine.setEnabled(true);
    c.gridx = 0;
    c.gridy = 9;
    c.gridwidth = 3;
    c.gridheight = 1;
    panel.add(MessageLine, c);

    c.gridy = 10;
    c.gridwidth = 3;
    c.fill = GridBagConstraints.CENTER;
    panel.add(BoardPic, c);

    panel.pack();
    /*
     * panel.setLocation(Projects.getCenteredLoc(panel.getWidth(),
     * panel.getHeight()));
     */
    panel.setLocationRelativeTo(null);
    UnMappedPane.setPreferredSize(
        new Dimension(
            BoardPic.getWidth() / 3, 6 * DoneButton.getHeight() + ScaleButton.getHeight()));
    MappedPane.setPreferredSize(
        new Dimension(
            BoardPic.getWidth() / 3, 6 * DoneButton.getHeight() + ScaleButton.getHeight()));
    panel.pack();
    panel.setVisible(true);
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
    MaxZoom = (zoomY > zoomX) ? zoomX : zoomY;
    if (MaxZoom < 100) MaxZoom = 100;
  }

  public boolean run() {
    MessageLine.setForeground(Color.BLUE);
    MessageLine.setText("No messages");
    Thread t =
        new Thread() {
          public void run() {
            synchronized (lock) {
              try {
                lock.wait();
              } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
              }
            }
          }
        };
    t.run();
    CancelButton.setEnabled(true);
    try {
      t.join();
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    panel.setVisible(false);
    panel.dispose();
    return !canceled;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getActionCommand().equals("Done")) {
      canceled = false;
      synchronized (lock) {
        lock.notify();
      }
    } else if (e.getActionCommand().equals("UnMapAll")) {
      UnMapAll();
      MappableComponents.markChanged();
    } else if (e.getActionCommand().equals("UnMap")) {
      UnMapOne();
      MappableComponents.markChanged();
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

  private void ClearSelections() {
    MappedHighlightItem = null;
    HighlightItem = null;
    UnMapButton.setEnabled(false);
    MappedList.clearSelection();
    UnmappedList.clearSelection();
    ComponentSelectionMode = false;
    SelectableItems.clear();
  }

  private void Load() {
    JFileChooser fc = new JFileChooser(OldDirectory);
    fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
    fc.setDialogTitle("Choose XML board description file to use");
    FileFilter XML_FILTER = new XMLFileFilter();
    fc.setFileFilter(XML_FILTER);
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
          ClearSelections();
          RebuildSelectionLists();
          BoardPic.paintImmediately(0, 0, BoardPic.getWidth(), BoardPic.getHeight());
      } else {
    	 JOptionPane.showMessageDialog(null, parse.getError(result), "Error", JOptionPane.ERROR_MESSAGE);
      }
    }
    panel.setVisible(true);
  }

  private void MapOne() {
    if (UnmappedList.getSelectedIndex() >= 0) {
      String key = UnmappedList.getSelectedValue().toString();
      if (HighlightItem != null) {
        MappableComponents.Map(key, HighlightItem, BoardInfo.GetComponentType(HighlightItem));
        MappableComponents.markChanged();
        RebuildSelectionLists();
      }
    } else if (MappedList.getSelectedIndex() >= 0) {
      String key = MappedList.getSelectedValue().toString();
      if (HighlightItem != null) {
        MappableComponents.Map(key, HighlightItem, BoardInfo.GetComponentType(HighlightItem));
        MappableComponents.markChanged();
      }
    }
    ClearSelections();
    BoardPic.paintImmediately(0, 0, BoardPic.getWidth(), BoardPic.getHeight());
    UnmappedList.setSelectedIndex(0);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private void RebuildSelectionLists() {
    UnmappedList.clearSelection();
    MappedList.clearSelection();
    Set<String> Unmapped = MappableComponents.UnmappedList();
    Set<String> Mapped = MappableComponents.MappedList();
    JList unmapped = new JList(Unmapped.toArray());
    UnmappedList.setModel(unmapped.getModel());
    JList mapped = new JList(Mapped.toArray());
    MappedList.setModel(mapped.getModel());
    UnmappedList.paintImmediately(
        0, 0, UnmappedList.getBounds().width, UnmappedList.getBounds().height);
    MappedList.paintImmediately(0, 0, MappedList.getBounds().width, MappedList.getBounds().height);
    UnMapAllButton.setEnabled(!Mapped.isEmpty());
    CancelButton.setEnabled(true);
    DoneButton.setEnabled(Unmapped.isEmpty());
    SaveButton.setEnabled(!Mapped.isEmpty());
  }

  private void Save() {
	panel.setVisible(false);
    MappableComponents.save();
    JOptionPane.showMessageDialog(null, S.get("BoarMapFileSaved"), "", JOptionPane.INFORMATION_MESSAGE);
	panel.setVisible(true);
  }

  public void SetBoardInformation(BoardInformation Board) {
    BoardInfo = Board;
  }

  public void SetMappebleComponents(MappableResourcesContainer mappable) {
    MappableComponents = mappable;
    RebuildSelectionLists();
    ClearSelections();
  }

  private void UnMapAll() {
    ClearSelections();
    MappableComponents.UnmapAll();
    MappableComponents.rebuildMappedLists();
    BoardPic.paintImmediately(0, 0, BoardPic.getWidth(), BoardPic.getHeight());
    RebuildSelectionLists();
  }

  private void UnMapOne() {
    if (MappedList.getSelectedIndex() >= 0) {
      String key = MappedList.getSelectedValue().toString();
      MappableComponents.UnMap(key);
      ClearSelections();
      RebuildSelectionLists();
      BoardPic.paintImmediately(0, 0, BoardPic.getWidth(), BoardPic.getHeight());
    }
  }

  @Override
  public void valueChanged(ListSelectionEvent e) {
    if (e.getSource() == MappedList) {
      if (MappedList.getSelectedIndex() >= 0) {
        UnmappedList.clearSelection();
        UnMapButton.setEnabled(true);
        MappedHighlightItem = MappableComponents.GetMap(MappedList.getSelectedValue().toString());
        BoardPic.paintImmediately(0, 0, BoardPic.getWidth(), BoardPic.getHeight());
        ComponentSelectionMode = true;
        SelectableItems.clear();
        String DisplayName = MappedList.getSelectedValue().toString();
        SelectableItems = MappableComponents.GetSelectableItemsList(DisplayName, BoardInfo);
        BoardPic.paintImmediately(0, 0, BoardPic.getWidth(), BoardPic.getHeight());
      } else {
        ComponentSelectionMode = false;
        SelectableItems.clear();
        Note = null;
        MappedHighlightItem = null;
        HighlightItem = null;
      }
    } else if (e.getSource() == UnmappedList) {
      if (UnmappedList.getSelectedIndex() < 0) {
        ComponentSelectionMode = false;
        SelectableItems.clear();
        Note = null;
        MappedHighlightItem = null;
        HighlightItem = null;
      } else {
        MappedList.clearSelection();
        ComponentSelectionMode = true;
        SelectableItems.clear();
        String DisplayName = UnmappedList.getSelectedValue().toString();
        SelectableItems = MappableComponents.GetSelectableItemsList(DisplayName, BoardInfo);
      }
      MappedHighlightItem = null;
      UnMapButton.setEnabled(false);
      CancelButton.setEnabled(true);
      BoardPic.paintImmediately(0, 0, BoardPic.getWidth(), BoardPic.getHeight());
    }
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
}
