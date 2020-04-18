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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.cburch.logisim.fpga.data.IOComponentsInformation;
import com.cburch.logisim.fpga.data.IOComponentsListener;
import com.cburch.logisim.fpga.data.BoardInformation;
import com.cburch.logisim.fpga.data.BoardManipulatorListener;
import com.cburch.logisim.fpga.data.BoardRectangle;
import com.cburch.logisim.fpga.data.FPGAIOInformationContainer;
import com.cburch.logisim.fpga.data.IOComponentTypes;
import com.cburch.logisim.fpga.data.SimpleRectangle;
import com.cburch.logisim.fpga.file.PNGFileFilter;
import com.cburch.logisim.fpga.fpgagui.IOComponentSelector;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.prefs.AppPreferences;

public class BoardManipulator extends JPanel implements MouseListener, 
     MouseMotionListener, ChangeListener , PropertyChangeListener , IOComponentsListener {
  private static final long serialVersionUID = 1L;

  public static final int IMAGE_WIDTH = 740;
  public static final int IMAGE_HEIGHT = 400;
  
  public static final int TRANSPARENT_ID = 0;
  public static final int DEFINE_COLOR_ID = 1;
  public static final int HIGHLIGHT_COLOR_ID = 2;
  public static final int MOVE_COLOR_ID = 3;
  public static final int RESIZE_COLOR_ID = 4;

  public static Color DEFINE_COLOR = new Color(AppPreferences.FPGA_DEFINE_COLOR.get());
  public static Color HIGHLIGHT_COLOR = new Color(AppPreferences.FPGA_DEFINE_HIGHLIGHT_COLOR.get());
  public static Color MOVE_COLOR = new Color(AppPreferences.FPGA_DEFINE_MOVE_COLOR.get());
  public static Color RESIZE_COLOR = new Color(AppPreferences.FPGA_DEFINE_RESIZE_COLOR.get());
  public static final Color TRANSPARENT_COLOR = new Color(0x00FFFFFF);
  
  private ZoomSlider zoom;
  private int MaxZoom;
  private float scale;
  private BufferedImage image;
  private boolean mapMode;
  private String BoardName;
  private SimpleRectangle defineRectangle; /* note this one is in real coordinates */
  private ArrayList<BoardManipulatorListener> listeners;
  private IOComponentsInformation IOcomps;
  
  public BoardManipulator(Frame parrentFrame) {
    zoom = new ZoomSlider();
    zoom.addChangeListener(this);
    MaxZoom = zoom.getMaxZoom();
    scale = (float)1.0;
    image = null;
    mapMode = false;
    setPreferredSize(new Dimension(getWidth(),getHeight()));
    addMouseListener(this);
    addMouseMotionListener(this);
    defineRectangle = null;
    IOcomps = new IOComponentsInformation(parrentFrame);
    IOcomps.addListener(this);
    AppPreferences.FPGA_DEFINE_COLOR.addPropertyChangeListener(this);
    AppPreferences.FPGA_DEFINE_HIGHLIGHT_COLOR.addPropertyChangeListener(this);
    AppPreferences.FPGA_DEFINE_MOVE_COLOR.addPropertyChangeListener(this);
    AppPreferences.FPGA_DEFINE_RESIZE_COLOR.addPropertyChangeListener(this);
  }
  
  public ZoomSlider getZoomSlider() { return zoom; }
  public int getWidth() { return AppPreferences.getScaled(IMAGE_WIDTH, scale); }
  public int getHeight() { return AppPreferences.getScaled(IMAGE_HEIGHT, scale); }
  public boolean hasIOComponents() { return IOcomps.hasComponents(); }
  
  public static Color getColor(int id) {
    switch (id) {
      case DEFINE_COLOR_ID    : return DEFINE_COLOR;
      case HIGHLIGHT_COLOR_ID : return HIGHLIGHT_COLOR;
      case MOVE_COLOR_ID      : return MOVE_COLOR;
      case RESIZE_COLOR_ID    : return RESIZE_COLOR;
      default                 : return TRANSPARENT_COLOR;
    }
  }

  public Image getImage() {
    if (image.getWidth() >= IMAGE_WIDTH && image.getWidth() <= 3*IMAGE_WIDTH &&
        image.getHeight() >= IMAGE_HEIGHT && image.getHeight() <= 3*IMAGE_HEIGHT)
      return image;
    int width = image.getWidth() < IMAGE_WIDTH ? IMAGE_WIDTH : 3*IMAGE_WIDTH;
    int height = image.getHeight() < IMAGE_HEIGHT ? IMAGE_HEIGHT : 3*IMAGE_HEIGHT;
    return image.getScaledInstance(width, height, 4);
  }
  
  public ArrayList<FPGAIOInformationContainer> getIOComponents() {
    return IOcomps.getComponents();
  }

  public void addBoardManipulatorListener(BoardManipulatorListener l) {
    if (listeners == null) {
      listeners = new ArrayList<BoardManipulatorListener>();
      listeners.add(l);
    } else if (!listeners.contains(l)) listeners.add(l);
  }
  
  public void setBoard(BoardInformation board) {
    clear();
    image = board.GetImage();
    BoardName = board.getBoardName();
    for (FPGAIOInformationContainer l : board.GetAllComponents())
      IOcomps.addComponent(l, scale);
    for (BoardManipulatorListener l : listeners) 
        l.boardNameChanged(BoardName);
  }
  
  public void clear() {
    image = null;
    IOcomps.clear();
    defineRectangle = null;
  }
  
  public void removeBoardManipulatorListener(BoardManipulatorListener l) {
    if (listeners != null && listeners.contains(l))
      listeners.remove(l);
  }
  
  public void setMaxZoom(int value) {
    if (value < zoom.getMinZoom()) {
      MaxZoom = zoom.getMinZoom();
    } else if (value > zoom.getMaxZoom()) {
      MaxZoom = zoom.getMaxZoom();
    } else {
      MaxZoom = value;
    }
  }
  
  private void defineIOComponent() {
    BoardRectangle rect = defineRectangle.getBoardRectangle(scale);
    FPGAIOInformationContainer comp = defineRectangle.getIoInfo();
    /*
     * Before doing anything we have to check that this region does not
     * overlap with an already defined region. If we detect an overlap we
     * abort the action.
     */
    if (IOcomps.hasOverlap(rect)) {
      DialogNotification.showDialogNotification(IOcomps.getParrentFram(), 
        "Error", S.get("FpgaBoardOverlap"));
      if (comp != null) IOcomps.addComponent(comp,scale);
      return;
    }
    if (comp == null) {
      String result = (new IOComponentSelector(IOcomps.getParrentFram())).run();
      if (result == null) return;
      comp = new FPGAIOInformationContainer(IOComponentTypes.valueOf(result), rect, IOcomps);
    } else
      comp.GetRectangle().updateRectangle(rect);
    if (comp.IsKnownComponent()) {
      IOcomps.addComponent(comp,scale);
      for (BoardManipulatorListener l : listeners)
        l.componentsChanged(IOcomps);
    }
  }
  

  @Override
  public void paint(Graphics g) {
    super.paint(g);
    Graphics2D g2 = (Graphics2D) g; 
    if (!mapMode && image == null) BoardPainter.newBoardpainter(this, g2);
    else if (image == null) BoardPainter.errorBoardPainter(this, g2);
    else {
      g2.drawImage(image.getScaledInstance(getWidth(), getHeight(), 4), 0, 0, null);
      IOcomps.paint(g2, scale);
      if (!mapMode && defineRectangle != null) defineRectangle.paint(g2);
    }
  }
  
  @Override
  public void stateChanged(ChangeEvent e) {
    JSlider source = (JSlider) e.getSource();
    if (!source.getValueIsAdjusting()) {
      int value = (int) source.getValue();
      if (value > MaxZoom) {
        source.setValue(MaxZoom);
        value = MaxZoom;
      }
      scale = (float) value / (float) 100.0;
      Dimension mySize = new Dimension(getWidth(),getHeight());
      setPreferredSize(mySize);
      setSize(mySize);
    }
  }
  
  @Override
  public void mouseDragged(MouseEvent e) {
    if (defineRectangle != null) {
      repaint(defineRectangle.resizeAndGetUpdate(e));
    } else if (IOcomps.hasHighlighted()) {
   	  /* resize or move the current highlighted component */
      FPGAIOInformationContainer edit = IOcomps.getHighligted();
      IOcomps.removeComponent(edit, scale);
      defineRectangle = new SimpleRectangle(e,edit,scale);
      repaint(defineRectangle.resizeAndGetUpdate(e));
    }
  }

  @Override
  public void mouseMoved(MouseEvent e) {
    IOcomps.mouseMoved(e, scale);
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    if (!mapMode && image == null) {
      JFileChooser fc = new JFileChooser();
      fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
      fc.setDialogTitle(S.get("BoardManipLoadPng"));
      fc.setFileFilter(PNGFileFilter.PNG_FILTER);
      fc.setAcceptAllFileFilterUsed(false);
      int retval = fc.showOpenDialog(this);
      if (retval == JFileChooser.APPROVE_OPTION) {
        File file = fc.getSelectedFile();
        try {
          image = ImageIO.read(file);
          repaint();
          BoardName = file.getName().toUpperCase().replaceAll(".PNG", "")
                        .replaceAll(".XML", "");
          for (BoardManipulatorListener l : listeners) 
            l.boardNameChanged(BoardName);
        } catch (IOException ex) {
          image = null;
          OptionPane.showMessageDialog(this, S.fmt("BoardManipLoadError", file.getName()), 
        		  S.get("BoardManipLoad"), OptionPane.ERROR_MESSAGE);
        }
      }
    }
  }

  @Override
  public void mousePressed(MouseEvent e) {
     if (!mapMode && image != null) {
       if (IOcomps.hasHighlighted()) {
    	 /* Edit the current highligted component */
         if (e.getClickCount() > 1) {
           try {
             FPGAIOInformationContainer clone = (FPGAIOInformationContainer) IOcomps.getHighligted().clone();
             clone.edit(true, IOcomps);
             if (clone.isToBeDeleted()) 
               IOcomps.removeComponent(IOcomps.getHighligted(), scale);
             else if (clone.IsKnownComponent())
               IOcomps.replaceComponent(IOcomps.getHighligted(), clone, e, scale);
           } catch (CloneNotSupportedException err) {
             OptionPane.showMessageDialog(IOcomps.getParrentFram(), "INTERNAL BUG: Unable to clone!", "FATAL!", OptionPane.ERROR_MESSAGE);
           }
         }
       } else {
    	 /* define a new component */
         defineRectangle = new SimpleRectangle(e);
         repaint(e.getX(),e.getY(),1,1);
       }
     }
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    if (defineRectangle != null) {
      Rectangle toBeRepainted = defineRectangle.resizeRemoveAndgetUpdate(e); 
      defineIOComponent();
      defineRectangle = null;
      repaint(toBeRepainted);
    }
  }

  @Override
  public void mouseEntered(MouseEvent e) { }

  @Override
  public void mouseExited(MouseEvent e) { IOcomps.mouseExited(scale); }

  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    DEFINE_COLOR = new Color(AppPreferences.FPGA_DEFINE_COLOR.get());
    HIGHLIGHT_COLOR = new Color(AppPreferences.FPGA_DEFINE_HIGHLIGHT_COLOR.get());
    MOVE_COLOR = new Color(AppPreferences.FPGA_DEFINE_MOVE_COLOR.get());
    RESIZE_COLOR = new Color(AppPreferences.FPGA_DEFINE_RESIZE_COLOR.get());
    this.repaint();
  }

  @Override
  public void repaintRequest(Rectangle rect) {
    repaint(rect);
  }
}
