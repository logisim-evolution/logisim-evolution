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

import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.fpgaboardeditor.BoardInformation;
import com.cburch.logisim.fpga.fpgaboardeditor.BoardRectangle;
import com.cburch.logisim.fpga.fpgaboardeditor.ZoomSlider;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.LocaleListener;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
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

public class ComponentMapDialog implements ActionListener, ListSelectionListener, WindowListener, LocaleListener {

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
      Color Yellow = new Color(Value.WIDTH_ERROR_CAPTION_BGCOLOR.getRed(),
                               Value.WIDTH_ERROR_CAPTION_BGCOLOR.getGreen(),
                               Value.WIDTH_ERROR_CAPTION_BGCOLOR.getBlue(), 180);
      Color Blue = new Color(Value.WIDTH_ERROR_CAPTION_COLOR.getRed(),
                             Value.WIDTH_ERROR_CAPTION_COLOR.getGreen(),
                             Value.WIDTH_ERROR_CAPTION_COLOR.getBlue(), 180);
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
      return AppPreferences.getScaled(image_height+barheight, scale);
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
            if (Item.PointInside(AppPreferences.getDownScaled(e.getX()), AppPreferences.getDownScaled(e.getY()))
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
      HandleSelect(e);
      if (HighlightItem != null) {
        MapOne();
      }
    }
    
    private void paintConstantButton(Graphics2D g, int xpos , int ypos,
    		     boolean constant, int value) {
      int width = AppPreferences.getScaled(barwidth-2, scale);
      int height = AppPreferences.getScaled(barheight-2, scale);
      int ydif2 = height-(height>>2);
      g.setColor(Color.BLACK);
      g.setStroke(new BasicStroke(AppPreferences.getScaled(2, scale)));
      g.drawRect(xpos,ypos,width,height);
      String val = constant ? S.get("BoardMapValue") : Integer.toString(value); 
      String txt = S.fmt("BoardMapConstant", val); 
      g.setFont(AppPreferences.getScaledFont(g.getFont().deriveFont(Font.BOLD),scale));
      g.setColor(Color.BLUE);
      g.drawString(txt, xpos+height+(height>>2), ypos+ydif2);
      g.setColor(value == 0 ? Value.FALSE_COLOR : value == 1 ? Value.TRUE_COLOR : Value.UNKNOWN_COLOR);
      g.fillOval(xpos+(height>>3), ypos+(height>>3), height-(height>>2), height-(height>>2));
      g.setColor(Color.WHITE);
      if (!constant) GraphicsUtil.drawCenteredText((Graphics)g, Integer.toString(value), xpos+(height>>1), ypos+(height>>1));
      else GraphicsUtil.drawCenteredText((Graphics)g, "C", xpos+(height>>1), ypos+(height>>1));
    }
    
    private void paintOpenButton(Graphics2D g, int xpos , int ypos) {
      int width = AppPreferences.getScaled(barwidth-2, scale);
      int height = AppPreferences.getScaled(barheight-2, scale);
      int ydif2 = height-(height>>2);
      g.setColor(Color.BLACK);
      g.setStroke(new BasicStroke(AppPreferences.getScaled(2, scale)));
      g.drawRect(xpos,ypos,width,height);
      g.setFont(AppPreferences.getScaledFont(g.getFont().deriveFont(Font.BOLD),scale));
      g.setColor(Color.BLUE);
      g.drawString(S.get("BoardMapOpen"), xpos+height+(height>>2), ypos+ydif2);
      g.setColor(Color.RED);
      g.setStroke(new BasicStroke(AppPreferences.getScaled(3, scale)));
      g.drawLine(xpos+(height>>2), ypos+(height>>2), xpos+height-(height>>2), ypos+height-(height>>2));
      g.drawLine(xpos+height-(height>>2), ypos+(height>>2), xpos+(height>>2), ypos+height-(height>>2));
    }
    
    private void paintBar(Graphics g) {
      Graphics2D g2 = (Graphics2D) g.create();
      int yoffset = AppPreferences.getScaled(image_height+2, scale);
      int skip = AppPreferences.getScaled(barwidth,scale);
      int xoffset = AppPreferences.getScaled(1, scale);
      g2.setColor(Color.BLACK);
      g2.setStroke(new BasicStroke(AppPreferences.getScaled(2, scale)));
      for (int i = 0 ; i < 3 ; i++)
        paintConstantButton(g2,xoffset+i*skip, yoffset, i==2 , i);
      paintOpenButton(g2,xoffset+3*skip, yoffset);
      g2.dispose();
    }

    @Override
    public void paint(Graphics g) {
      super.paint(g);
      Color Black = new Color(Value.MULTI_COLOR.getRed(), Value.MULTI_COLOR.getGreen(), Value.MULTI_COLOR.getBlue(), 150);
      Color Mapped = new Color(Value.TRUE_COLOR.getRed(), Value.TRUE_COLOR.getGreen(), Value.TRUE_COLOR.getBlue(), 100);
      Image image = BoardInfo.GetImage().getScaledInstance(
                  AppPreferences.getScaled(image_width, scale),
                  AppPreferences.getScaled(image_height, scale),
                  Image.SCALE_SMOOTH);
      if (image != null) {
        g.drawImage(image, 0, 0, null);
        paintBar(g);
      }
      ArrayList<BoardRectangle> painted = new ArrayList<BoardRectangle>();
      for (BoardRectangle rect : MappableComponents.GetMappedRectangles()) {
        if (painted.contains(rect)) continue;
        painted.add(rect);
        boolean cadre = false;
        if (MappedHighlightItem != null) {
          if (MappedHighlightItem.equals(rect)) {
            g.setColor(Mapped);
            cadre = true;
          } else {
            g.setColor(Black);
          }
        } else {
          g.setColor(Black);
        }
        if (!cadre && MappableResourcesContainer.FixedConnectButtons.contains(rect)) continue;
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
      Color selectable = new Color(Value.STROKE_COLOR.getRed(),Value.STROKE_COLOR.getGreen(),Value.STROKE_COLOR.getBlue(),100);
      for (BoardRectangle rect : SelectableItems) {
        g.setColor(selectable);
        if (MappedHighlightItem != null && MappedHighlightItem.equals(rect)) continue;
        g.fillRect(
            AppPreferences.getScaled(rect.getXpos(), scale),
            AppPreferences.getScaled(rect.getYpos(), scale),
            AppPreferences.getScaled(rect.getWidth(), scale),
            AppPreferences.getScaled(rect.getHeight(), scale));
      }
      if (HighlightItem != null && ComponentSelectionMode) {
        g.setColor(Mapped);
        g.fillRect(
            AppPreferences.getScaled(HighlightItem.getXpos(), scale),
            AppPreferences.getScaled(HighlightItem.getYpos(), scale),
            AppPreferences.getScaled(HighlightItem.getWidth(), scale),
            AppPreferences.getScaled(HighlightItem.getHeight(), scale));
      }
      if (Note != null && !MappableResourcesContainer.FixedConnectButtons.contains(Note.getRectangle())) {
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
      return S.get("BoardMapXml");
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
  private JLabel UnmappedText = new JLabel();
  private JLabel MappedText = new JLabel();
  private JLabel CommandText = new JLabel();
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
  public static final int image_width = 740;
  public static final int image_height = 400;
  public static final int barheight = 30;
  public static final int barwidth = 185;
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
    UnmappedText.setPreferredSize(new Dimension(BoardPic.getWidth() / 3, AppPreferences.getScaled(25)));
    MappedText.setPreferredSize(new Dimension(BoardPic.getWidth() / 3, AppPreferences.getScaled(25)));
    CommandText.setPreferredSize(new Dimension(BoardPic.getWidth() / 3, AppPreferences.getScaled(25)));
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
    panel.setResizable(false);
    panel.setAlwaysOnTop(true);
    panel.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);

    GridBagLayout thisLayout = new GridBagLayout();
    GridBagConstraints c = new GridBagConstraints();
    panel.setLayout(thisLayout);

    /* Add the board Picture */
    BoardPic = new SelectionWindow();
    BoardPic.setPreferredSize(new Dimension(BoardPic.getWidth(), BoardPic.getHeight()));
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

    UnMapButton.setActionCommand("UnMap");
    UnMapButton.addActionListener(this);
    UnMapButton.setEnabled(false);
    c.gridy = 1;
    panel.add(UnMapButton, c);

    /* Add the UnMapAll button */
    UnMapAllButton.setActionCommand("UnMapAll");
    UnMapAllButton.addActionListener(this);
    UnMapAllButton.setEnabled(false);
    c.gridy = 2;
    panel.add(UnMapAllButton, c);

    /* Add the Load button */
    LoadButton.setActionCommand("Load");
    LoadButton.addActionListener(this);
    LoadButton.setEnabled(true);
    c.gridy = 3;
    panel.add(LoadButton, c);

    /* Add the Save button */
    SaveButton.setActionCommand("Save");
    SaveButton.addActionListener(this);
    SaveButton.setEnabled(false);
    c.gridy = 4;
    panel.add(SaveButton, c);

    /* Add the Cancel button */
    CancelButton.setActionCommand("Cancel");
    CancelButton.addActionListener(this);
    CancelButton.setEnabled(false);
    c.gridy = 5;
    panel.add(CancelButton, c);

    /* Add the Done button */
    DoneButton.setActionCommand("Done");
    DoneButton.addActionListener(this);
    DoneButton.setEnabled(false);
    c.gridy = 6;
    panel.add(DoneButton, c);

    /* Add the Zoom button */
    c.gridy = 7;
    panel.add(ScaleButton, c);
    ScaleButton.addChangeListener(new ZoomChange(this));


    /* Add the unmapped list */
    UnmappedList = new JList();
    UnmappedList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    UnmappedList.addListSelectionListener(this);
    UnmappedList.addMouseListener(mouseListener);
    UnMappedPane = new JScrollPane(UnmappedList);
    c.fill = GridBagConstraints.BOTH;
    c.gridx = 0;
    c.gridy = 1;
    c.gridheight = 7;
    panel.add(UnMappedPane, c);
    ComponentSelectionMode = false;

    /* Add the mapped list */
    MappedList = new JList();
    MappedList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    MappedList.addListSelectionListener(this);
    MappedPane = new JScrollPane(MappedList);
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
    localeChanged();
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
    if (HighlightItem == null) return;
    String Identifier = BoardInfo.GetComponentType(HighlightItem);
    if (UnmappedList.getSelectedIndex() >= 0) {
      String key = UnmappedList.getSelectedValue().toString();
      int sel = UnmappedList.getSelectedIndex();
      MappableComponents.Map(key, HighlightItem, Identifier);
      MappableComponents.markChanged();
      RebuildSelectionLists();
      ClearSelections();
      BoardPic.paintImmediately(0, 0, BoardPic.getWidth(), BoardPic.getHeight());
      while (sel >= UnmappedList.getModel().getSize()) sel--;
      if (sel < 0) sel = 0;
      UnmappedList.setSelectedIndex(sel);
    } else if (MappedList.getSelectedIndex() >= 0) {
      String key = MappedList.getSelectedValue().toString();
      MappableComponents.Map(key, HighlightItem, Identifier);
      MappableComponents.markChanged();
      ClearSelections();
      BoardPic.paintImmediately(0, 0, BoardPic.getWidth(), BoardPic.getHeight());
    }
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
    panel.pack();
    panel.repaint();
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

  @Override
  public void localeChanged() {
    panel.setTitle(S.get("BoardMapTitle"));
    UnmappedText.setText(S.get("BoardMapUnmapped"));
    UnmappedText.setToolTipText(S.get("BoardMapUMTooltip"));
    MappedText.setText(S.get("BoardMapMapped"));
    CommandText.setText(S.get("BoardMapActions"));
    UnMapButton.setText(S.get("BoardMapRelease"));
    UnMapAllButton.setText(S.get("BoardMapRelAll"));
    LoadButton.setText(S.get("BoardMapLoad"));
    SaveButton.setText(S.get("BoardMapSave"));
    CancelButton.setText(S.get("FpgaBoardCancel"));
    DoneButton.setText(S.get("FpgaBoardDone"));
    panel.pack();
  }
}
