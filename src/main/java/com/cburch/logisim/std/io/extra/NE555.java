/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.io.extra;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceComponent;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstanceLogger;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.tools.key.DirectionConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.lang.ref.WeakReference;
import javax.swing.Timer;

public class NE555 extends InstanceFactory {

  public static final String _ID = "NE555";

  public static final int PIN_OUT = 0;
  public static final int PIN_TRIG = 1;

  public static final AttributeOption MODE_ASTABLE = new AttributeOption("astable",
      S.getter("ne555ModeAstable"));
  public static final AttributeOption MODE_MONOSTABLE = new AttributeOption("monostable",
      S.getter("ne555ModeMonostable"));
  public static final Attribute<AttributeOption> ATTR_MODE = Attributes.forOption(
      "mode",
      S.getter("ne555ModeAttr"),
      new AttributeOption[] { MODE_ASTABLE, MODE_MONOSTABLE });

  public static final double FIXED_RFIXED = 1000.0; // 1 kOhm, fixed pull-up from VCC

  public static final int MIN_R1_OHM = 1000;
  public static final int MAX_R1_OHM = 10_000_000;
  public static final int DEFAULT_R1_OHM = 71_660;   // ~71.66 kOhm gives T ≈ 1 s

  public static final int MIN_C1_UF = 10;
  public static final int MAX_C1_UF = 1000;
  public static final int DEFAULT_C1_UF = 10;

  public static final Attribute<Integer> ATTR_R1 = Attributes.forIntegerRange(
      "r1", S.getter("ne555ResistorR1Attr"), MIN_R1_OHM, MAX_R1_OHM);
  public static final Attribute<Integer> ATTR_C1 = Attributes.forIntegerRange(
      "c1", S.getter("ne555CapacitorC1Attr"), MIN_C1_UF, MAX_C1_UF);

  private static final double DEFAULT_VCC = 5.0;
  private static final double TIMER_INTERVAL_SEC = 0.010;

  public static class NE555State implements InstanceData, Cloneable, ActionListener {
    private WeakReference<InstanceComponent> comp;
    private WeakReference<Simulator> simRef;
    private Timer timer;

    volatile double capVoltage;
    volatile double vccVoltage;
    volatile boolean output;
    volatile boolean disch;
    volatile boolean running;
    volatile boolean waiting;
    volatile double activeElapsedSec;
    volatile long lastUpdateNanos;

    public NE555State(InstanceState state) {
      comp = new WeakReference<>(state.getInstance().getComponent());
      simRef = new WeakReference<>(state.getProject() != null ? state.getProject().getSimulator() : null);
      capVoltage = 0.0;
      vccVoltage = DEFAULT_VCC;
      output = true;
      disch = true;
      running = false;
      waiting = false;
      activeElapsedSec = 0.0;
      lastUpdateNanos = System.nanoTime();
      timer = new Timer((int) (TIMER_INTERVAL_SEC * 1000), this);
      timer.setCoalesce(true);
      timer.start();
    }

    public void updateReferences(InstanceState state) {
      if (state != null) {
        final var currentComp = state.getInstance().getComponent();
        if (comp.get() != currentComp) {
          comp = new WeakReference<>(currentComp);
        }
        final var proj = state.getProject();
        final var currentSim = proj != null ? proj.getSimulator() : null;
        if (simRef.get() != currentSim) {
          simRef = new WeakReference<>(currentSim);
        }
      }
    }

    public void stop() {
      if (timer != null && timer.isRunning()) {
        timer.stop();
      }
    }

    @Override
    public Object clone() {
      try {
        final var s = (NE555State) super.clone();
        s.comp = new WeakReference<>(comp.get());
        s.simRef = new WeakReference<>(simRef.get());
        s.timer = new Timer((int) (TIMER_INTERVAL_SEC * 1000), s);
        s.timer.setCoalesce(true);
        s.timer.start();
        return s;
      } catch (CloneNotSupportedException e) {
        return null;
      }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      final var c = comp.get();
      final var s = simRef.get();
      if (c == null || s == null) {
        stop();
        return;
      }
      if (!s.isAutoPropagating()) {
        lastUpdateNanos = System.nanoTime();
        return;
      }
      c.fireInvalidated();
      s.nudge();
    }
  }

  public static class Logger extends InstanceLogger {
    @Override
    public String getLogName(InstanceState state, Object option) {
      final var label = state.getAttributeValue(StdAttr.LABEL);
      return (label != null && !label.isEmpty()) ? label : null;
    }

    @Override
    public BitWidth getBitWidth(InstanceState state, Object option) {
      return BitWidth.ONE;
    }

    @Override
    public Value getLogValue(InstanceState state, Object option) {
      final var s = (NE555State) state.getData();
      return s == null ? Value.FALSE : (s.output ? Value.TRUE : Value.FALSE);
    }
  }

  public NE555() {
    super(_ID, S.getter("ne555Component"));
    setAttributes(
        new Attribute[] {
            StdAttr.LABEL,
            StdAttr.LABEL_LOC,
            StdAttr.LABEL_FONT,
            StdAttr.LABEL_COLOR,
            StdAttr.LABEL_VISIBILITY,
            ATTR_MODE,
            ATTR_R1,
            ATTR_C1,
        },
        new Object[] {
            "",
            Direction.NORTH,
            StdAttr.DEFAULT_LABEL_FONT,
            StdAttr.DEFAULT_LABEL_COLOR,
            true,
            MODE_ASTABLE,
            DEFAULT_R1_OHM,
            DEFAULT_C1_UF,
        });
    setIcon(new NE555Icon());
    setKeyConfigurator(new DirectionConfigurator(StdAttr.LABEL_LOC, KeyEvent.ALT_DOWN_MASK));
    setInstanceLogger(Logger.class);
  }

  private static AttributeOption getMode(AttributeSet attrs) {
    final var mode = attrs.getValue(ATTR_MODE);
    return mode != null ? mode : MODE_ASTABLE;
  }

  public static double getResistanceOhm(AttributeSet attrs) {
    final var raw = attrs.getValue(ATTR_R1);
    final var clamped = (raw == null) ? DEFAULT_R1_OHM : Math.max(MIN_R1_OHM, Math.min(MAX_R1_OHM, raw));
    return (double) clamped;
  }

  public static double getCapFarad(AttributeSet attrs) {
    final var raw = attrs.getValue(ATTR_C1);
    final var clamped = (raw == null) ? DEFAULT_C1_UF : Math.max(MIN_C1_UF, Math.min(MAX_C1_UF, raw));
    return clamped * 1e-6;
  }

  private static double getVcc() {
    return DEFAULT_VCC;
  }

  private static double getCtrlVoltage(NE555State s) {
    return s.vccVoltage * 2.0 / 3.0;
  }

  @Override
  protected Object getInstanceFeature(Instance instance, Object key) {
    if (key == com.cburch.logisim.tools.ToolTipMaker.class) {
      return (com.cburch.logisim.tools.ToolTipMaker) e -> {
        final var attrs = instance.getAttributeSet();
        final var mode = getMode(attrs);
        if (mode == MODE_ASTABLE) {
          return S.get("ne555FormulaAstableTip");
        } else {
          return S.get("ne555FormulaMonostableTip");
        }
      };
    }
    return super.getInstanceFeature(instance, key);
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    configurePorts(instance);
    instance.computeLabelTextField(Instance.AVOID_SIDES);
  }

  private void configurePorts(Instance instance) {
    final var attrs = instance.getAttributeSet();
    final var mode = getMode(attrs);
    final var edgeX = 80;

    if (mode == MODE_MONOSTABLE) {
      final var ports = new Port[2];
      ports[PIN_OUT] = new Port(edgeX, 0, Port.OUTPUT, BitWidth.ONE);
      ports[PIN_OUT].setToolTip(S.getter("ne555PortOutTip"));
      ports[PIN_TRIG] = new Port(-edgeX, 10, Port.INPUT, BitWidth.ONE);
      ports[PIN_TRIG].setToolTip(S.getter("ne555PortTrigTip"));
      instance.setPorts(ports);
    } else {
      final var ports = new Port[1];
      ports[PIN_OUT] = new Port(edgeX, 0, Port.OUTPUT, BitWidth.ONE);
      ports[PIN_OUT].setToolTip(S.getter("ne555PortOutTip"));
      instance.setPorts(ports);
    }
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    return Bounds.create(-80, -60, 160, 120);
  }

  @Override
  public boolean isHDLSupportedComponent(AttributeSet attrs) {
    return false;
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == StdAttr.LABEL_LOC) {
      instance.computeLabelTextField(Instance.AVOID_SIDES);
    } else if (attr == ATTR_MODE) {
      instance.recomputeBounds();
      configurePorts(instance);
      instance.computeLabelTextField(Instance.AVOID_SIDES);
      instance.fireInvalidated();
    } else if (attr == ATTR_R1 || attr == ATTR_C1) {
      instance.fireInvalidated();
    }
  }

  private static NE555State getOrCreateState(InstanceState state) {
    var ne555State = (NE555State) state.getData();
    if (ne555State == null) {
      ne555State = new NE555State(state);
      state.setData(ne555State);
    }
    return ne555State;
  }

  private static void applyVcc(double vcc, NE555State ne555State) {
    ne555State.vccVoltage = Math.max(0.1, vcc);
  }

  private static void updateCap(AttributeSet attrs, NE555State ne555State, double dt,
      double targetVoltage, double rCharge) {
    if (rCharge > 0) {
      final var capacitance = getCapFarad(attrs);
      final var timeConstant = rCharge * capacitance;
      if (timeConstant > 1e-18) {
        final var chargeFraction = 1.0 - Math.exp(-dt / timeConstant);
        final var clampedFraction = Math.min(chargeFraction, 1.0 - 1e-6);
        ne555State.capVoltage += (targetVoltage - ne555State.capVoltage) * clampedFraction;
      } else {
        ne555State.capVoltage = targetVoltage;
      }
    }
  }

  public static double getPeriodSeconds(AttributeSet attrs) {
    final var mode = getMode(attrs);
    final var capacitance = getCapFarad(attrs);
    if (mode == MODE_ASTABLE) {
      final var r2Fixed = FIXED_RFIXED;
      final var r1 = getResistanceOhm(attrs);
      return 0.693 * (r2Fixed + 2.0 * r1) * capacitance;
    } else {
      final var r1 = getResistanceOhm(attrs);
      return 1.1 * r1 * capacitance;
    }
  }

  public static double getFrequencyHz(AttributeSet attrs) {
    final var mode = getMode(attrs);
    if (mode != MODE_ASTABLE) return 0.0;
    final var period = getPeriodSeconds(attrs);
    return (period > 0) ? (1.0 / period) : 0.0;
  }

  public static String formatTime(double seconds) {
    if (seconds <= 0) return "0s";
    if (seconds < 1e-6) return String.format(java.util.Locale.US, "%.2f ns", seconds * 1e9);
    if (seconds < 1e-3) return String.format(java.util.Locale.US, "%.2f µs", seconds * 1e6);
    if (seconds < 1.0) return String.format(java.util.Locale.US, "%.2f ms", seconds * 1e3);
    return String.format(java.util.Locale.US, "%.2f s", seconds);
  }

  public static String formatFrequency(double hz) {
    if (hz <= 0) return "N/A";
    if (hz >= 1e6) return String.format(java.util.Locale.US, "%.2f MHz", hz / 1e6);
    if (hz >= 1e3) return String.format(java.util.Locale.US, "%.2f kHz", hz / 1e3);
    return String.format(java.util.Locale.US, "%.2f Hz", hz);
  }

  @Override
  public void propagate(InstanceState state) {
    final var ne555State = getOrCreateState(state);
    ne555State.updateReferences(state);
    final var attrs = state.getAttributeSet();
    final var mode = getMode(attrs);
    final var vcc = getVcc();
    applyVcc(vcc, ne555State);

    final var trigPortValue = (mode == MODE_MONOSTABLE) ? state.getPortValue(PIN_TRIG) : Value.UNKNOWN;
    final var trigLow = (trigPortValue == Value.FALSE);

    final var upperThreshold = getCtrlVoltage(ne555State);

    final var now = System.nanoTime();
    final var proj = state.getProject();
    final var sim = proj != null ? proj.getSimulator() : null;
    final var isSimRunning = sim != null && sim.isAutoPropagating();

    final var dt = isSimRunning ? Math.min((now - ne555State.lastUpdateNanos) / 1e9, 0.1) : 0.0;
    ne555State.lastUpdateNanos = now;

    if (mode == MODE_ASTABLE) {
      final var lowerThreshold = ne555State.vccVoltage / 3.0;
      doAstable(state, ne555State, lowerThreshold, upperThreshold, dt);
    } else {
      doMonostable(state, ne555State, trigLow, upperThreshold, dt);
    }

    setOutputs(state, ne555State, vcc);
  }

  private static void doAstable(InstanceState state, NE555State ne555State,
      double lowerThreshold, double upperThreshold, double dt) {
    final var r2Fixed = FIXED_RFIXED;
    final var r1 = getResistanceOhm(state.getAttributeSet());
    final var vcc = ne555State.vccVoltage;

    if (!ne555State.running && !ne555State.waiting) {
      ne555State.capVoltage = lowerThreshold;
      ne555State.output = true;
      ne555State.disch = false;
      ne555State.running = true;
    }

    if (ne555State.output && !ne555State.disch) {
      updateCap(state.getAttributeSet(), ne555State, dt, vcc, r2Fixed + r1);
      if (ne555State.capVoltage >= upperThreshold) {
        ne555State.output = false;
        ne555State.disch = true;
      }
    } else if (!ne555State.output && ne555State.disch) {
      updateCap(state.getAttributeSet(), ne555State, dt, 0.0, r1);
      if (ne555State.capVoltage <= lowerThreshold) {
        ne555State.output = true;
        ne555State.disch = false;
      }
    }
  }

  private static void doMonostable(InstanceState state, NE555State ne555State,
      boolean trigLow, double upperThreshold, double dt) {
    final var resistance = getResistanceOhm(state.getAttributeSet());
    final var capacitance = getCapFarad(state.getAttributeSet());

    if (!ne555State.running && !ne555State.waiting) {
      ne555State.output = false;
      ne555State.disch = true;
      ne555State.capVoltage = 0.0;
      if (trigLow) {
        ne555State.running = true;
        ne555State.output = true;
        ne555State.disch = false;
        ne555State.activeElapsedSec = 0.0;
      }
    } else if (ne555State.running) {
      ne555State.output = true;
      ne555State.disch = false;
      final var pulseWidth = 1.1 * resistance * capacitance;
      ne555State.activeElapsedSec += dt;
      final var vcc = ne555State.vccVoltage;
      final var timeConstant = resistance * capacitance;
      if (timeConstant > 1e-18) {
        final var chargeFraction = 1.0 - Math.exp(-dt / timeConstant);
        ne555State.capVoltage += (vcc - ne555State.capVoltage) * chargeFraction;
      } else {
        ne555State.capVoltage = vcc;
      }
      if (ne555State.capVoltage >= upperThreshold || ne555State.activeElapsedSec >= pulseWidth) {
        ne555State.output = false;
        ne555State.disch = true;
        ne555State.running = false;
        ne555State.waiting = true;
      }
    } else if (ne555State.waiting) {
      ne555State.output = false;
      ne555State.disch = true;
      updateCap(state.getAttributeSet(), ne555State, dt, 0.0, 1000.0);
      if (!trigLow && ne555State.capVoltage < 0.1) {
        ne555State.waiting = false;
      }
    }
  }

  private static void setOutputs(InstanceState state, NE555State ne555State, double vcc) {
    final var outputValue = ne555State.output ? Value.TRUE : Value.FALSE;
    state.setPort(PIN_OUT, outputValue, 0);
  }

  @Override
  public void paintGhost(InstancePainter painter) {
    final var g = painter.getGraphics();
    final var bounds = painter.getBounds();
    GraphicsUtil.switchToWidth(g, 2);
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    g.drawRoundRect(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(), 8, 8);
    GraphicsUtil.switchToWidth(g, 1);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    final var g = painter.getGraphics();
    final var bounds = painter.getBounds();
    final var showState = painter.getShowState();
    final var ne555State = (NE555State) painter.getData();
    final var attrs = painter.getAttributeSet();
    final var mode = getMode(attrs);

    final var x = bounds.getX();
    final var y = bounds.getY();
    final var w = bounds.getWidth();
    final var h = bounds.getHeight();

    // Outer background box with theme support
    g.setColor(new Color(AppPreferences.CANVAS_BG_COLOR.get()));
    g.fillRoundRect(x, y, w, h, 8, 8);
    g.setColor(new Color(AppPreferences.COMPONENT_SECONDARY_COLOR.get()));
    g.drawRoundRect(x, y, w, h, 8, 8);

    final var r1Ohm = attrs.getValue(ATTR_R1);
    final var c1Uf  = attrs.getValue(ATTR_C1);

    final var r1Clamped = (r1Ohm == null) ? DEFAULT_R1_OHM : Math.max(MIN_R1_OHM, Math.min(MAX_R1_OHM, r1Ohm));
    final var c1Clamped = (c1Uf  == null) ? DEFAULT_C1_UF  : Math.max(MIN_C1_UF,  Math.min(MAX_C1_UF,  c1Uf));

    final var r1Label = (r1Clamped >= 1000) ? (r1Clamped / 1000 + "k") : (r1Clamped + "Ω");
    final var c1Label = c1Clamped + "µF";

    final var centerX = x + w / 2;
    final var centerY = y + h / 2;

    g.translate(centerX, centerY);

    if (mode == MODE_ASTABLE) {
      drawAstableSchematic(painter, g, ne555State, r1Label, c1Label, showState);
    } else {
      drawMonostableSchematic(painter, g, ne555State, r1Label, c1Label, showState);
    }

    drawTimingInfo(g, attrs, mode);

    g.translate(-centerX, -centerY);

    painter.drawPorts();
    painter.drawLabel();
  }

  private void drawInnerChip(Graphics g, boolean showState, NE555State ne555State) {
    final var icX = 10;
    final var icY = -35;
    final var icW = 60;
    final var icH = 70;

    g.setColor(new Color(AppPreferences.CANVAS_BG_COLOR.get()));
    g.fillRect(icX, icY, icW, icH);
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    g.drawRect(icX, icY, icW, icH);

    g.setFont(g.getFont().deriveFont(7.0f).deriveFont(java.awt.Font.BOLD));
    g.drawString("NE555", icX + 19, icY + 57);

    g.setFont(g.getFont().deriveFont(5.5f).deriveFont(java.awt.Font.PLAIN));
    g.drawString("4 RST",  icX + 2,  icY + 9);
    g.drawString("8 VCC",  icX + 34, icY + 9);
    g.drawString("7 DIS",  icX + 2, icY + 20);
    g.drawString("6 THR",  icX + 2, icY + 34);
    g.drawString("2 TRIG", icX + 2, icY + 47);
    g.drawString("1 GND",  icX + 2, icY + icH - 2);
    g.drawString("3 OUT",  icX + 40, icY + 37);
    g.drawString("5 CTRL", icX + 35, icY + icH - 2);

    if (showState && ne555State != null) {
      final var ledSize = 5;
      g.setColor(ne555State.output ? com.cburch.logisim.data.Value.trueColor : com.cburch.logisim.data.Value.falseColor);
      g.fillOval(icX + 30, icY + 32, ledSize, ledSize);
      g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    }
  }

  private void drawCommonFrame(InstancePainter painter, Graphics g, NE555State ne555State, boolean showState) {
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    g.setFont(g.getFont().deriveFont(5.5f).deriveFont(java.awt.Font.PLAIN));

    // Vcc and GND rails
    g.drawLine(-60, -48, 45, -48);
    g.drawString("Vcc", -12, -40);
    g.drawLine(-60, 48, 45, 48);
    g.drawString("GND", -12, 45);

    drawInnerChip(g, showState, ne555State);

    // RST (pin 4) and VCC (pin 8) tied to Vcc rail
    drawDot(g, 25, -48);
    g.drawLine(25, -48, 25, -35);
    drawDot(g, 45, -48);
    g.drawLine(45, -48, 45, -35);

    // CTRL (pin 5) bypass cap to GND
    drawCapacitor(g, 45, 35, 48, "10nF", -1.0, 3);
    drawDot(g, 45, 48);

    // GND (pin 1) to GND rail
    g.drawLine(18, 35, 18, 48);
    drawDot(g, 18, 48);

    // OUT (pin 3) wire to component edge
    final var outPortValue = painter.getPortValue(PIN_OUT);
    final var outColor = (showState && outPortValue != null) ? outPortValue.getColor() : new Color(AppPreferences.COMPONENT_COLOR.get());
    GraphicsUtil.switchToWidth(g, 3);
    g.setColor(outColor);
    g.drawLine(70, 0, 80, 0);
    GraphicsUtil.switchToWidth(g, 1);
  }

  private void drawAstableSchematic(InstancePainter painter, Graphics g, NE555State ne555State, String r1Label, String c1Label, boolean showState) {
    drawCommonFrame(painter, g, ne555State, showState);

    // R2 (fixed 1k) from Vcc to DIS node
    drawResistor(g, -60, -48, -20, "R2 (1k)");
    drawDot(g, -60, -20);
    g.drawLine(-60, -20, 10, -20);

    // R1 from DIS node to TRIG node
    drawResistor(g, -60, -20, 12, "R1 (" + r1Label + ")");
    drawDot(g, -60, 12);
    g.drawLine(-60, 12, 10, 12);

    // THR (pin 6) tied to TRIG (pin 2)
    drawDot(g, 0, 12);
    g.drawLine(0, 12, 0, -3);
    g.drawLine(0, -3, 10, -3);

    final var capDisplayVoltage = (showState && ne555State != null) ? Math.max(0.0, ne555State.capVoltage) : 0.0;
    drawCapacitor(g, -60, 12, 48, "C1 (" + c1Label + ")", capDisplayVoltage, 0);
  }

  private void drawMonostableSchematic(InstancePainter painter, Graphics g, NE555State ne555State, String r1Label, String c1Label, boolean showState) {
    drawCommonFrame(painter, g, ne555State, showState);

    // R1 from Vcc to DIS (pin 7)
    drawResistor(g, -60, -48, -20, "R1 (" + r1Label + ")");
    drawDot(g, -60, -20);
    g.drawLine(-60, -20, 10, -20);

    // DIS (pin 7) tied to THR (pin 6)
    drawDot(g, 0, -20);
    g.drawLine(0, -20, 0, -3);
    g.drawLine(0, -3, 10, -3);

    g.drawLine(-60, -20, -60, 12);

    final var capDisplayVoltage = (showState && ne555State != null) ? Math.max(0.0, ne555State.capVoltage) : 0.0;
    drawCapacitor(g, -60, 12, 48, "C1 (" + c1Label + ")", capDisplayVoltage, 0);

    // External trigger input wire
    final var trigPortValue = painter.getPortValue(PIN_TRIG);
    final var trigColor = (showState && trigPortValue != null) ? trigPortValue.getColor() : new Color(AppPreferences.COMPONENT_COLOR.get());
    GraphicsUtil.switchToWidth(g, 3);
    g.setColor(trigColor);
    g.drawLine(-80, 10, 10, 10);
    GraphicsUtil.switchToWidth(g, 1);
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    g.drawString("Trigger", -55, 6);
  }

  private void drawResistor(Graphics g, int cx, int y1, int y2, String label) {
    final var w = 8;
    final var h = Math.abs(y2 - y1);
    final var topY = Math.min(y1, y2);
    final var boxY = topY + (h - 18) / 2;
    g.setColor(new Color(AppPreferences.CANVAS_BG_COLOR.get()));
    g.fillRect(cx - w / 2, boxY, w, 18);
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    g.drawRect(cx - w / 2, boxY, w, 18);
    g.drawLine(cx, topY, cx, boxY);
    g.drawLine(cx, boxY + 18, cx, topY + h);
    if (label != null && !label.isEmpty()) {
      g.setFont(g.getFont().deriveFont(5.5f).deriveFont(java.awt.Font.PLAIN));
      g.drawString(label, cx + w / 2 + 2, boxY + 11);
    }
  }

  private void drawCapacitor(Graphics g, int cx, int y1, int y2, String label, double voltage, int labelYOffset) {
    final var topY = Math.min(y1, y2);
    final var h = Math.abs(y2 - y1);
    final var midY = topY + h / 2;
    final var gap = 4;
    final var plateW = 10;

    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    g.drawLine(cx, topY, cx, midY - gap / 2);
    g.drawLine(cx, midY + gap / 2, cx, topY + h);
    g.drawLine(cx - plateW / 2, midY - gap / 2, cx + plateW / 2, midY - gap / 2);
    g.drawLine(cx - plateW / 2, midY + gap / 2, cx + plateW / 2, midY + gap / 2);
    if (label != null && !label.isEmpty()) {
      g.setFont(g.getFont().deriveFont(5.5f).deriveFont(java.awt.Font.PLAIN));
      g.drawString(label, cx + plateW / 2 + 2, midY + 4 + labelYOffset);
      if (voltage >= 0.0) {
        final var vStr = String.format(java.util.Locale.US, "%.1fV", voltage);
        g.setFont(g.getFont().deriveFont(5.0f).deriveFont(java.awt.Font.BOLD));
        g.drawString(vStr, cx + plateW / 2 + 2, midY - gap / 2 - 1);
      }
    }
  }

  private void drawDot(Graphics g, int x, int y) {
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    g.fillOval(x - 2, y - 2, 4, 4);
  }

  private void drawTimingInfo(Graphics g, AttributeSet attrs, AttributeOption mode) {
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    g.setFont(g.getFont().deriveFont(5.5f).deriveFont(java.awt.Font.BOLD));
    final var period = getPeriodSeconds(attrs);
    if (mode == MODE_ASTABLE) {
      final var freq = getFrequencyHz(attrs);
      final var displayStr = "T=" + formatTime(period) + "  f=" + formatFrequency(freq);
      g.drawString(displayStr, -70, 56);
    } else {
      final var displayStr = "T=" + formatTime(period);
      g.drawString(displayStr, -70, 56);
    }
  }
}
