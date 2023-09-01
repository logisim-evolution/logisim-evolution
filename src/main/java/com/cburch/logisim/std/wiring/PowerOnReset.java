/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.wiring;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstancePoker;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import javax.swing.Timer;

public class PowerOnReset extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "POR";

  private static final AttributeOption OLD_FORM =
      new AttributeOption(3, S.getter("porOldSize"));
  private static final AttributeOption MEDIUM =
      new AttributeOption(1, S.getter("porMediumSize"));
  private static final AttributeOption NARROW =
      new AttributeOption(2, S.getter("porNarrowSize"));
  private static final Attribute<AttributeOption> PORSIZE =
      Attributes.forOption(
          "porsize", S.getter("PorSize"), new AttributeOption[] {OLD_FORM, MEDIUM, NARROW});
  
  private static final AttributeOption HTOL =
      new AttributeOption(1, S.getter("porHightToLow"));
  private static final AttributeOption LTOH =
      new AttributeOption(2, S.getter("porLowToHight"));
  private static final Attribute<AttributeOption> PORTRANS =
      Attributes.forOption(
          "porTransition", S.getter("porTransition"), new AttributeOption[] {HTOL,LTOH});
    
  public static final PowerOnReset FACTORY = new PowerOnReset();

  public static class Poker extends InstancePoker {
    @Override
    public void mouseReleased(InstanceState state, MouseEvent e) {
      PORState ret = (PORState) state.getData();
      ret.reset();
    }
  }
    
  public PowerOnReset() {
    super(_ID, S.getter("PowerOnResetComponent"));
    setAttributes(
        new Attribute[] {
          StdAttr.FACING,
          PORSIZE,
          PORTRANS,
          new DurationAttribute("PorHighDuration", S.getter("porHighAttr"), 1, 10, false), 
        },
        new Object[] {
          Direction.EAST,
          MEDIUM,
          HTOL,
          2,
        });
    setFacingAttribute(StdAttr.FACING);
    setIconName("por.png");
    setInstancePoker(Poker.class);
  }

  private static class PORState implements InstanceData, Cloneable, ActionListener {

    private boolean value;
    private final Timer tim;
    private final InstanceState state;
    private int tstart;
    private int tend;
    private int duration;

    public PORState(InstanceState state) {
      value = true;
      DurationAttribute attr =
          (DurationAttribute) state.getAttributeSet().getAttribute("PorHighDuration");
      duration = state.getAttributeValue(attr) * 1000;
      tim = new Timer(duration, this);
      
      if ( state.getAttributeValue(PORTRANS) == LTOH) {
        tstart = 0;
        tend = 1;
      } else {
        tstart = 1;
        tend = 0;
      }
      state.setPort(0, Value.createKnown(BitWidth.ONE,tstart) , 0); 
      tim.start();
      this.state = state;
    }

    public boolean getValue() {
      return value;
    }

    public int gettstart() {
      return tstart;
    }
    
    public int gettend() {
      return tend;
    }
    
    public void reset() {
      if (value) {
        tim.stop();
        value = false;
      }
      value = true;

      if ( state.getAttributeValue(PORTRANS) == LTOH) {
        tstart = 0;
        tend = 1;
      } else {
        tstart = 1;
        tend = 0;
      }

      DurationAttribute attr =
          (DurationAttribute) state.getAttributeSet().getAttribute("PorHighDuration");
      duration = state.getAttributeValue(attr) * 1000;

      state.setPort(0, Value.createKnown(BitWidth.ONE,tstart) , 0); 

      tim.setInitialDelay(duration);
      tim.start();
    }
    
    @Override
    public Object clone() {
      try {
        return super.clone();
      } catch (CloneNotSupportedException e) {
        return null;
      }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (e.getSource() == tim) {
        if (value) {
          value = false;
          state.getInstance().fireInvalidated();
          tim.stop();
        }
      }
    }
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    instance.setPorts(new Port[] {new Port(0, 0, Port.OUTPUT, BitWidth.ONE)});
 
 }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    Direction facing = attrs.getValue(StdAttr.FACING);

    final var psize = attrs.getValue(PORSIZE); 
    if (psize==MEDIUM) {
      return Bounds.create(0, -20, 40, 40).rotate(Direction.WEST, facing, 0, 0);
    } else if (psize==NARROW) {
      return Bounds.create(0, -10, 20, 20).rotate(Direction.WEST, facing, 0, 0);
    } else {
      return Bounds.create(0, -20, 200, 40).rotate(Direction.WEST, facing, 0, 0);
    }   
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == StdAttr.FACING || attr == PORSIZE ) {
      instance.recomputeBounds();
    }
    
  }

  @Override
  public void paintInstance(InstancePainter painter) {

    java.awt.Graphics g = painter.getGraphics();
    Bounds bds = painter.getInstance().getBounds();
    int x = bds.getX();
    int y = bds.getY();
    int width =  bds.getWidth();
    int height = bds.getHeight();
    GraphicsUtil.switchToWidth(g, 2);
    g.setColor(Color.WHITE);
    g.fillRect(x, y, width, height);
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    g.drawRect(x, y, width, height);

    final var psize = painter.getAttributeValue(PORSIZE); 

    if (psize == OLD_FORM) {
      Font old = g.getFont();
      g.setFont(old.deriveFont(16.0f).deriveFont(Font.BOLD));
      String txt = S.get("porLongName"); 
     
      FontMetrics fm = g.getFontMetrics();  
      int wide = Math.max(width, height);

      int offset = (wide - fm.stringWidth(txt)) / 2;
      Direction facing = painter.getAttributeValue(StdAttr.FACING);

      if (((facing == Direction.NORTH) || (facing == Direction.SOUTH)) && (g instanceof Graphics2D g2)) {
        int xpos = facing == Direction.NORTH ? x + 20 - fm.getDescent() : x + 20 + fm.getDescent();
        int ypos = facing == Direction.NORTH ? y + offset : y + height - offset;
        g2.translate(xpos, ypos);
        g2.rotate(facing.toRadians());
        g.drawString(txt, 0, 0);
        g2.rotate(-facing.toRadians());
        g2.translate(-xpos, -ypos);
      } else {
        g.drawString(txt, x + offset, y + fm.getDescent() + 20);
      }
    } else {
      int x1;
      int x2;
      int x3;
      int y1;
      int y2;
      int offset;
      
      Font old = g.getFont();
      if  ( psize == NARROW) {
        g.setFont(old.deriveFont(6.0f).deriveFont(Font.BOLD));
        offset = 7;
      } else {
        g.setFont(old.deriveFont(14.0f).deriveFont(Font.BOLD));
        offset = 13;
      }
      
      y1 = y + height - 4;
      y2 = y + offset;
      x1 = x + 3;
      x2 = x + width - 4;
      
      Graphics2D g2 = (Graphics2D)g;
      var oldStroke = g2.getStroke();
      g2.setStroke(new BasicStroke(1));
      g.setColor(Color.BLUE);
      g.drawLine(x1, y1, x2, y1);
      x1 = x1 + 1;
      y1 = y1 + 1;
      g.drawLine(x1, y2, x1, y1); 
      g2.setStroke(oldStroke);
      
      x1 = x + 4;
      x2 = x + width / 2;
      x3 = x + width - 4;
      y1 = y + offset + 2;
      y2 = y + height - 5;

      final var pstat = painter.getAttributeValue(PORTRANS); 
      if (pstat == LTOH) {
        y1 = y1 ^ y2 ^ ( y2 = y1 );  // swap y1 <-> y2
      }
      
      g.setColor(Color.RED);
      g.drawLine(x1, y1, x2, y1);
      g.drawLine(x2, y1, x2, y2);
      g.drawLine(x2, y2, x3, y2);

      g.setColor(Color.BLACK);
      String txt = S.get("PowerOnResetComponent");
      g.drawString(txt, x + 2, y + offset - 1);
    }

    painter.drawPorts();
  }

  @Override
  public void propagate(InstanceState state) {
    PORState ret = (PORState) state.getData();
    if (ret == null) {
      ret = new PORState(state);
      state.setData(ret);
    }

    state.setPort(0, Value.createKnown(BitWidth.ONE, ret.getValue() ? ret.gettstart() : ret.gettend()), 0);

    // TODO Auto-generated method stub

  }
}
