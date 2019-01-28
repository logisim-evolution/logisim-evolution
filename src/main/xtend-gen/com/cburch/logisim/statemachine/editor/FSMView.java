package com.cburch.logisim.statemachine.editor;

import com.cburch.logisim.statemachine.editor.FSMEditorController;
import com.cburch.logisim.statemachine.editor.FSMPopupMenu;
import com.cburch.logisim.statemachine.editor.view.FSMSelectionZone;
import com.cburch.logisim.statemachine.fSMDSL.FSM;
import com.cburch.logisim.statemachine.fSMDSL.LayoutInfo;
import com.cburch.logisim.std.fsm.IFSMEditor;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import javax.swing.JPanel;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.InputOutput;

@SuppressWarnings("all")
public class FSMView extends JPanel implements MouseListener, MouseMotionListener {
  private static final long serialVersionUID = 0;
  
  private FSMEditorController controller;
  
  private final IFSMEditor editor;
  
  private double scale = 1.0;
  
  private Point unscaledPos;
  
  private Point scaledPos = new Point(0, 0);
  
  private FSMPopupMenu popupMenu;
  
  public FSMView(final IFSMEditor parent) {
    super();
    this.editor = parent;
    FSM fsm = this.editor.getContent().getFsm();
    FSMEditorController _fSMEditorController = new FSMEditorController(this, fsm);
    this.controller = _fSMEditorController;
    this.addMouseListener(this);
    this.addMouseMotionListener(this);
    Dimension _dimension = new Dimension(1000, 1000);
    this.setPreferredSize(_dimension);
    this.addMouseWheelListener(new MouseAdapter() {
      @Override
      public void mouseWheelMoved(final MouseWheelEvent e) {
        double _preciseWheelRotation = e.getPreciseWheelRotation();
        double delta = (0.05f * _preciseWheelRotation);
        double _scale = FSMView.this.scale;
        FSMView.this.scale = (_scale + delta);
        StringConcatenation _builder = new StringConcatenation();
        _builder.append("Scale=");
        _builder.append(FSMView.this.scale);
        System.out.println(_builder.toString());
        FSMView.this.revalidate();
        FSMView.this.repaint();
      }
    });
    FSMPopupMenu _fSMPopupMenu = new FSMPopupMenu(this);
    this.popupMenu = _fSMPopupMenu;
    this.setBackground(Color.white);
  }
  
  @Override
  public void repaint() {
    super.repaint();
  }
  
  @Override
  public void paint(final Graphics page) {
    try {
      LayoutInfo l = this.getController().getFSM().getLayout();
      int _max = Math.max(500, l.getWidth());
      int _max_1 = Math.max(500, l.getHeight());
      Dimension _dimension = new Dimension(_max, _max_1);
      this.setPreferredSize(_dimension);
    } catch (final Throwable _t) {
      if (_t instanceof Exception) {
        InputOutput.<String>println("layout issue");
      } else {
        throw Exceptions.sneakyThrow(_t);
      }
    }
    super.paint(page);
    Graphics2D g = ((Graphics2D) page);
    g.scale(this.scale, this.scale);
    this.controller.draw(((Graphics2D) page));
  }
  
  private void showMouseCursor(final Graphics page, final Graphics2D g) {
    int x = ((int) this.scaledPos.x);
    int y = ((int) this.scaledPos.y);
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("[");
    _builder.append(x);
    _builder.append(",");
    _builder.append(y);
    _builder.append("]");
    String label = _builder.toString();
    int sw = page.getFontMetrics().stringWidth(label);
    g.setColor(Color.blue);
    g.drawString(label, (x - (sw / 2)), y);
    page.drawOval((x - 10), (y - 10), 20, 20);
    g.setColor(Color.black);
  }
  
  public void showContextMenu(final FSMSelectionZone.AreaType type) {
    this.popupMenu.showPopupMenu(this.unscaledPos, type);
  }
  
  private void updatePosition(final MouseEvent e) {
    int _x = e.getX();
    double _divide = (_x / this.scale);
    int _y = e.getY();
    double _divide_1 = (_y / this.scale);
    Point _point = new Point(((int) _divide), ((int) _divide_1));
    this.scaledPos = _point;
    int _x_1 = e.getX();
    int _y_1 = e.getY();
    Point _point_1 = new Point(_x_1, _y_1);
    this.unscaledPos = _point_1;
  }
  
  @Override
  public void mouseClicked(final MouseEvent e) {
    this.updatePosition(e);
    FSMEditorController ctrl = this.getController();
    int _button = e.getButton();
    boolean _tripleEquals = (_button == MouseEvent.BUTTON1);
    if (_tripleEquals) {
      int _clickCount = e.getClickCount();
      boolean _tripleEquals_1 = (_clickCount == 2);
      if (_tripleEquals_1) {
        ctrl.executeDoubleClick(this.scaledPos);
      } else {
        ctrl.executeLeftClick(this.scaledPos);
      }
    } else {
      ctrl.executeRightClick();
    }
  }
  
  @Override
  public void mousePressed(final MouseEvent event) {
    this.updatePosition(event);
    this.getController().executePress(this.scaledPos);
    this.repaint();
  }
  
  @Override
  public void mouseDragged(final MouseEvent event) {
    this.updatePosition(event);
    this.getController().executeDragged(this.scaledPos);
    this.repaint();
  }
  
  @Override
  public void mouseReleased(final MouseEvent event) {
    this.updatePosition(event);
    this.getController().executeRelease(this.scaledPos);
    this.repaint();
  }
  
  @Override
  public void mouseEntered(final MouseEvent event) {
  }
  
  @Override
  public void mouseExited(final MouseEvent event) {
  }
  
  public Point getScaledPosition() {
    return this.scaledPos;
  }
  
  @Override
  public void mouseMoved(final MouseEvent event) {
    this.updatePosition(event);
    this.controller.executeMove(this.scaledPos);
  }
  
  public FSMEditorController getController() {
    return this.controller;
  }
  
  public void setScale(final double d) {
    this.scale = d;
  }
}
