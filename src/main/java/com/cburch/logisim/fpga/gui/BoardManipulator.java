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

import com.cburch.contracts.BaseMouseListenerContract;
import com.cburch.contracts.BaseMouseMotionListenerContract;
import com.cburch.contracts.BaseWindowListenerContract;
import com.cburch.logisim.fpga.data.BoardInformation;
import com.cburch.logisim.fpga.data.BoardManipulatorListener;
import com.cburch.logisim.fpga.data.ConstantButton;
import com.cburch.logisim.fpga.data.FpgaIoInformationContainer;
import com.cburch.logisim.fpga.data.IoComponentTypes;
import com.cburch.logisim.fpga.data.IoComponentsInformation;
import com.cburch.logisim.fpga.data.IoComponentsListener;
import com.cburch.logisim.fpga.data.MapListModel;
import com.cburch.logisim.fpga.data.MappableResourcesContainer;
import com.cburch.logisim.fpga.data.SimpleRectangle;
import com.cburch.logisim.fpga.file.PngFileFilter;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.LocaleListener;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class BoardManipulator extends JPanel implements BaseMouseListenerContract,
                                                        BaseMouseMotionListenerContract,
                                                        ChangeListener,
                                                        PropertyChangeListener,
                                                        IoComponentsListener,
                                                        ListSelectionListener,
                                                        BaseWindowListenerContract,
                                                        LocaleListener,
                                                        ActionListener {
  private static final long serialVersionUID = 1L;

  public static final int IMAGE_WIDTH = 740;
  public static final int IMAGE_HEIGHT = 400;
  public static final int CONSTANT_BAR_HEIGHT = 30;
  public static final int CONSTANT_BUTTON_WIDTH = IMAGE_WIDTH >> 2;

  public static final int TRANSPARENT_ID = 0;
  public static final int DEFINE_COLOR_ID = 1;
  public static final int HIGHLIGHT_COLOR_ID = 2;
  public static final int MOVE_COLOR_ID = 3;
  public static final int RESIZE_COLOR_ID = 4;
  public static final int MAPPED_COLOR_ID = 5;
  public static final int SELECTED_MAPPED_COLOR_ID = 6;
  public static final int SELECTABLE_MAPPED_COLOR_ID = 7;
  public static final int SELECTABLE_COLOR_ID = 8;

  public static Color defineColor = new Color(AppPreferences.FPGA_DEFINE_COLOR.get());
  public static Color highlightColor = new Color(AppPreferences.FPGA_DEFINE_HIGHLIGHT_COLOR.get());
  public static Color moveColor = new Color(AppPreferences.FPGA_DEFINE_MOVE_COLOR.get());
  public static Color resizeColor = new Color(AppPreferences.FPGA_DEFINE_RESIZE_COLOR.get());
  public static Color mappedColor = new Color(AppPreferences.FPGA_MAPPED_COLOR.get());
  public static Color selectedMapColor = new Color(AppPreferences.FPGA_SELECTED_MAPPED_COLOR.get());
  public static Color selectableMapColor = new Color(AppPreferences.FPGA_SELECTABLE_MAPPED_COLOR.get());
  public static Color selectableColor = new Color(AppPreferences.FPGA_SELECT_COLOR.get());

  private ZoomSlider zoom;
  private int maxZoom;
  private float scale;
  private BufferedImage image;
  private final boolean mapMode;
  private String boardName;
  private SimpleRectangle defineRectangle; /* note this one is in real coordinates */
  private ArrayList<BoardManipulatorListener> listeners;
  private final IoComponentsInformation ioComps;
  private MappableResourcesContainer mapInfo;
  private JList<MapListModel.MapInfo> unmappedList;
  private JList<MapListModel.MapInfo> mappedList;
  private JButton unmapButton;
  private JButton unmapAllButton;


  public BoardManipulator(Frame parentFrame) {
    mapMode = false;
    ioComps = new IoComponentsInformation(parentFrame, false);
    ioComps.addListener(this);
    setup(false);
  }

  public BoardManipulator(JDialog manip, Frame parentFrame, MappableResourcesContainer mapInfo) {
    mapMode = true;
    setup(true);
    ioComps = mapInfo.getIoComponentInformation();
    ioComps.addListener(this);
    ioComps.addComponent(ConstantButton.ONE_BUTTON, 1);
    ioComps.addComponent(ConstantButton.OPEN_BUTTON, 1);
    ioComps.addComponent(ConstantButton.VALUE_BUTTON, 1);
    ioComps.addComponent(ConstantButton.ZERO_BUTTON, 1);
    image = mapInfo.getBoardInformation().getImage();
    ioComps.setParentFrame(parentFrame);
    this.mapInfo = mapInfo;
    manip.addWindowListener(this);
  }

  private void setup(boolean MapMode) {
    zoom = new ZoomSlider();
    zoom.addChangeListener(this);
    maxZoom = zoom.getMaxZoom();
    scale = (float) 1.0;
    image = null;
    setPreferredSize(new Dimension(getWidth(), getHeight()));
    addMouseListener(this);
    addMouseMotionListener(this);
    defineRectangle = null;
    AppPreferences.FPGA_DEFINE_COLOR.addPropertyChangeListener(this);
    AppPreferences.FPGA_DEFINE_HIGHLIGHT_COLOR.addPropertyChangeListener(this);
    AppPreferences.FPGA_DEFINE_MOVE_COLOR.addPropertyChangeListener(this);
    AppPreferences.FPGA_DEFINE_RESIZE_COLOR.addPropertyChangeListener(this);
    AppPreferences.FPGA_MAPPED_COLOR.addPropertyChangeListener(this);
    AppPreferences.FPGA_SELECTED_MAPPED_COLOR.addPropertyChangeListener(this);
    AppPreferences.FPGA_SELECTABLE_MAPPED_COLOR.addPropertyChangeListener(this);
    AppPreferences.FPGA_SELECT_COLOR.addPropertyChangeListener(this);
  }

  public JList<MapListModel.MapInfo> getUnmappedList() {
    if (mapInfo == null) return null;
    unmappedList = new JList<>();
    unmappedList.setModel(new MapListModel(false, mapInfo.getMappableResources()));
    unmappedList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    unmappedList.addListSelectionListener(this);
    return unmappedList;
  }

  public JList<MapListModel.MapInfo> getMappedList() {
    if (mapInfo == null) return null;
    mappedList = new JList<>();
    mappedList.setModel(new MapListModel(true, mapInfo.getMappableResources()));
    mappedList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    mappedList.addListSelectionListener(this);
    return mappedList;
  }

  public void cleanup() {
    if (mapInfo != null) {
      mapInfo.destroyIOComponentInformation();
      mapInfo = null;
    }
    if (unmappedList != null) unmappedList = null;
    if (mappedList != null) mappedList = null;
    if (unmapButton != null) unmapButton = null;
    if (unmapAllButton != null) unmapAllButton = null;
  }

  public ZoomSlider getZoomSlider() {
    return zoom;
  }

  @Override
  public int getWidth() {
    return AppPreferences.getScaled(IMAGE_WIDTH, scale);
  }

  public void update() {
    if (unmappedList != null) {
      ((MapListModel) unmappedList.getModel()).rebuild();
      unmappedList.clearSelection();
    }
    if (mappedList != null) {
      ((MapListModel) mappedList.getModel()).rebuild();
      mappedList.clearSelection();
    }
    if (unmapButton != null) unmapButton.setEnabled(false);
    if (unmapAllButton != null) unmapAllButton.setEnabled(false);
  }

  @Override
  public int getHeight() {
    return AppPreferences.getScaled(IMAGE_HEIGHT + (mapMode ? CONSTANT_BAR_HEIGHT : 0), scale);
  }

  public JButton getUnmapOneButton() {
    if (unmapButton == null) unmapButton = new JButton();
    unmapButton.setEnabled(false);
    unmapButton.setActionCommand("unmapone");
    unmapButton.addActionListener(this);
    localeChanged();
    return unmapButton;
  }

  public JButton getUnmapAllButton() {
    if (unmapAllButton == null) unmapAllButton = new JButton();
    unmapAllButton.setActionCommand("unmapall");
    unmapAllButton.addActionListener(this);
    unmapAllButton.setEnabled(false);
    localeChanged();
    return unmapAllButton;
  }

  private int getPictureHeight() {
    return AppPreferences.getScaled(IMAGE_HEIGHT, scale);
  }

  public boolean hasIOComponents() {
    return ioComps.hasComponents();
  }

  public static Color getColor(int id) {
    return switch (id) {
      case DEFINE_COLOR_ID -> defineColor;
      case HIGHLIGHT_COLOR_ID -> highlightColor;
      case MOVE_COLOR_ID -> moveColor;
      case RESIZE_COLOR_ID -> resizeColor;
      case MAPPED_COLOR_ID -> mappedColor;
      case SELECTED_MAPPED_COLOR_ID -> selectedMapColor;
      case SELECTABLE_MAPPED_COLOR_ID -> selectableMapColor;
      case SELECTABLE_COLOR_ID -> selectableColor;
      default -> null;
    };
  }

  public Image getImage() {
    if (image.getWidth() >= IMAGE_WIDTH
        && image.getWidth() <= 3 * IMAGE_WIDTH
        && image.getHeight() >= IMAGE_HEIGHT
        && image.getHeight() <= 3 * IMAGE_HEIGHT) return image;
    int width = image.getWidth() < IMAGE_WIDTH ? IMAGE_WIDTH : 3 * IMAGE_WIDTH;
    int height = image.getHeight() < IMAGE_HEIGHT ? IMAGE_HEIGHT : 3 * IMAGE_HEIGHT;
    return image.getScaledInstance(width, height, 4);
  }

  public List<FpgaIoInformationContainer> getIoComponents() {
    return ioComps.getComponents();
  }

  public void addBoardManipulatorListener(BoardManipulatorListener l) {
    if (listeners == null) {
      listeners = new ArrayList<>();
      listeners.add(l);
    } else if (!listeners.contains(l)) listeners.add(l);
  }

  public void setBoard(BoardInformation board) {
    clear();
    image = board.getImage();
    boardName = board.getBoardName();
    for (final var comp : board.getAllComponents()) {
      ioComps.addComponent(comp, scale);
    }
    for (final var listener : listeners) {
      listener.boardNameChanged(boardName);
    }
  }

  public void clear() {
    image = null;
    ioComps.clear();
    defineRectangle = null;
  }

  public void removeBoardManipulatorListener(BoardManipulatorListener l) {
    if (listeners != null) listeners.remove(l);
  }

  public void setMaxZoom(int value) {
    maxZoom = (value < zoom.getMinZoom())
              ? zoom.getMinZoom()
              : Math.min(value, zoom.getMaxZoom());
  }

  private void defineIOComponent() {
    final var rect = defineRectangle.getBoardRectangle(scale);
    var comp = defineRectangle.getIoInfo();
    /*
     * Before doing anything we have to check that this region does not
     * overlap with an already defined region. If we detect an overlap we
     * abort the action.
     */
    if (ioComps.hasOverlap(rect)) {
      DialogNotification.showDialogNotification(ioComps.getParentFrame(), "Error", S.get("FpgaBoardOverlap"));
      if (comp != null) ioComps.addComponent(comp, scale);
      return;
    }
    if (comp == null) {
      final var result = (new IoComponentSelector(ioComps.getParentFrame())).run();
      if (result == null) return;
      comp = new FpgaIoInformationContainer(IoComponentTypes.valueOf(result), rect, ioComps);
    } else
      comp.getRectangle().updateRectangle(rect);
    if (comp.isKnownComponent()) {
      ioComps.addComponent(comp, scale);
      for (final var listener : listeners) {
        listener.componentsChanged(ioComps);
      }
    }
  }


  @Override
  public void paint(Graphics g) {
    super.paint(g);
    final var g2 = (Graphics2D) g;
    if (!mapMode && image == null) BoardPainter.newBoardpainter(this, g2);
    else if (image == null) BoardPainter.errorBoardPainter(this, g2);
    else {
      g2.drawImage(image.getScaledInstance(getWidth(), getPictureHeight(), 4), 0, 0, null);
      if (mapMode) BoardPainter.paintConstantOpenBar(g2, scale);
      ioComps.paint(g2, scale);
      if (!mapMode && defineRectangle != null) defineRectangle.paint(g2);
    }
  }

  @Override
  public void stateChanged(ChangeEvent event) {
    final var source = (JSlider) event.getSource();
    if (!source.getValueIsAdjusting()) {
      int value = source.getValue();
      if (value > maxZoom) {
        source.setValue(maxZoom);
        value = maxZoom;
      }
      scale = (float) value / (float) 100.0;
      final var mySize = new Dimension(getWidth(), getHeight());
      setPreferredSize(mySize);
      setSize(mySize);
    }
  }

  @Override
  public void mouseDragged(MouseEvent e) {
    if (mapMode) return;
    if (defineRectangle != null) {
      repaint(defineRectangle.resizeAndGetUpdate(e));
    } else if (ioComps.hasHighlighted()) {
      /* resize or move the current highlighted component */
      final var edit = ioComps.getHighligted();
      ioComps.removeComponent(edit, scale);
      defineRectangle = new SimpleRectangle(e, edit, scale);
      repaint(defineRectangle.resizeAndGetUpdate(e));
    }
  }

  @Override
  public void mouseMoved(MouseEvent e) {
    ioComps.mouseMoved(e, scale);
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    if (!mapMode && image == null) {
      final var fc = new JFileChooser();
      fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
      fc.setDialogTitle(S.get("BoardManipLoadPng"));
      fc.setFileFilter(PngFileFilter.PNG_FILTER);
      fc.setAcceptAllFileFilterUsed(false);
      final var retVal = fc.showOpenDialog(this);
      if (retVal == JFileChooser.APPROVE_OPTION) {
        final var file = fc.getSelectedFile();
        try {
          image = ImageIO.read(file);
          repaint();
          boardName = file.getName().toUpperCase().replaceAll(".PNG", "")
                          .replaceAll(".XML", "");
          for (BoardManipulatorListener l : listeners)
            l.boardNameChanged(boardName);
        } catch (IOException ex) {
          image = null;
          OptionPane.showMessageDialog(
              this,
              S.get("BoardManipLoadError", file.getName()),
              S.get("BoardManipLoad"),
              OptionPane.ERROR_MESSAGE);
        }
      }
    } else if (mapMode && ioComps.tryMap(this)) {
      this.repaint();
      var sel = unmappedList.getSelectedIndex();
      update();
      while (sel > unmappedList.getModel().getSize()) sel--;
      if (sel >= 0) unmappedList.setSelectedIndex(sel);
      mapInfo.markChanged();
    }
  }

  @Override
  public void mousePressed(MouseEvent e) {
    if (mapMode) return;
    if (image != null) {
      if (ioComps.hasHighlighted()) {
        /* Edit the current highligted component */
        if (e.getClickCount() > 1) {
          try {
            final var clone = (FpgaIoInformationContainer) ioComps.getHighligted().clone();
            clone.edit(true, ioComps);
            if (clone.isToBeDeleted()) ioComps.removeComponent(ioComps.getHighligted(), scale);
            else if (clone.isKnownComponent())
              ioComps.replaceComponent(ioComps.getHighligted(), clone, e, scale);
          } catch (CloneNotSupportedException err) {
            OptionPane.showMessageDialog(
                ioComps.getParentFrame(),
                "INTERNAL BUG: Unable to clone!",
                "FATAL!",
                OptionPane.ERROR_MESSAGE);
          }
        }
      } else {
        /* define a new component */
        defineRectangle = new SimpleRectangle(e);
        repaint(e.getX(), e.getY(), 1, 1);
      }
    }
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    if (defineRectangle != null && !mapMode) {
      final var toBeRepainted = defineRectangle.resizeRemoveAndgetUpdate(e);
      defineIOComponent();
      defineRectangle = null;
      repaint(toBeRepainted);
    }
  }

  @Override
  public void mouseExited(MouseEvent e) {
    ioComps.mouseExited(scale);
  }

  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    defineColor = new Color(AppPreferences.FPGA_DEFINE_COLOR.get());
    highlightColor = new Color(AppPreferences.FPGA_DEFINE_HIGHLIGHT_COLOR.get());
    moveColor = new Color(AppPreferences.FPGA_DEFINE_MOVE_COLOR.get());
    resizeColor = new Color(AppPreferences.FPGA_DEFINE_RESIZE_COLOR.get());
    mappedColor = new Color(AppPreferences.FPGA_MAPPED_COLOR.get());
    selectedMapColor = new Color(AppPreferences.FPGA_SELECTED_MAPPED_COLOR.get());
    selectableMapColor = new Color(AppPreferences.FPGA_SELECTABLE_MAPPED_COLOR.get());
    selectableColor = new Color(AppPreferences.FPGA_SELECT_COLOR.get());
    this.repaint();
  }

  @Override
  public void repaintRequest(Rectangle rect) {
    repaint(rect);
  }

  @Override
  public void valueChanged(ListSelectionEvent event) {
    if (event.getSource().equals(unmappedList)) {
      if (unmappedList.getSelectedIndex() >= 0) {
        mappedList.clearSelection();
        if (unmapButton != null) unmapButton.setEnabled(false);
        ioComps.setSelectable(unmappedList.getSelectedValue(), scale);
      } else ioComps.removeSelectable(scale);
    } else if (event.getSource().equals(mappedList)) {
      if (mappedList.getSelectedIndex() >= 0) {
        unmappedList.clearSelection();
        if (unmapButton != null) unmapButton.setEnabled(true);
        ioComps.setSelectable(mappedList.getSelectedValue(), scale);
      } else ioComps.removeSelectable(scale);
      if (mappedList.getModel().getSize() > 0) {
        if (unmapAllButton != null) unmapAllButton.setEnabled(true);
      } else {
        if (unmapAllButton != null) unmapAllButton.setEnabled(false);
      }
    }
  }

  @Override
  public void windowDeactivated(WindowEvent event) {
    ioComps.removeSelectable(scale);
    if (unmapButton != null) unmapButton.setEnabled(false);
  }

  @Override
  public void localeChanged() {
    if (unmapButton != null) unmapButton.setText(S.get("BoardMapRelease"));
    if (unmapAllButton != null) unmapAllButton.setText(S.get("BoardMapRelAll"));
  }

  @Override
  public void actionPerformed(ActionEvent event) {
    if (event.getActionCommand().equals("unmapone")) {
      if (mappedList.getSelectedIndex() >= 0) {
        final var map = mappedList.getSelectedValue();
        if (map.getPin() < 0) map.getMap().unmap();
        else map.getMap().unmap(map.getPin());
        ioComps.removeSelectable(scale);
        update();
        mapInfo.markChanged();
      }
    } else if (event.getActionCommand().contentEquals("unmapall")) {
      ioComps.removeSelectable(scale);
      mapInfo.unMapAll();
      update();
      repaint();
      mapInfo.markChanged();
    }
  }
}
