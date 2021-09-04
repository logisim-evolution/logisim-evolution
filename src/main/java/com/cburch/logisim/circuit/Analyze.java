/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit;

import static com.cburch.logisim.circuit.Strings.S;

import com.cburch.logisim.analyze.model.AnalyzerModel;
import com.cburch.logisim.analyze.model.Entry;
import com.cburch.logisim.analyze.model.Expression;
import com.cburch.logisim.analyze.model.Expressions;
import com.cburch.logisim.analyze.model.TruthTable;
import com.cburch.logisim.analyze.model.Var;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.wiring.Pin;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import lombok.val;

public class Analyze {
  public static class LocationBit {
    final Location loc;
    final int bit;

    public LocationBit(Location l, int b) {
      loc = l;
      bit = b;
    }

    @Override
    public boolean equals(Object other) {
      if (!(other instanceof LocationBit)) return false;
      val that = (LocationBit) other;
      return (that.loc.equals(this.loc) && that.bit == this.bit);
    }

    @Override
    public int hashCode() {
      return loc.hashCode() + bit;
    }
  }

  private static class ExpressionMap extends HashMap<LocationBit, Expression>
      implements ExpressionComputer.Map {
    private static final long serialVersionUID = 1L;
    private final Circuit circuit;
    private final Set<LocationBit> dirtyPoints = new HashSet<>();
    private final Map<LocationBit, Component> causes = new HashMap<>();
    private Component currentCause = null;

    ExpressionMap(Circuit circuit) {
      this.circuit = circuit;
    }

    @Override
    public Expression put(LocationBit point, Expression expression) {
      val ret = super.put(point, expression);
      if (currentCause != null) causes.put(point, currentCause);
      if (!Objects.equals(ret, expression)) {
        dirtyPoints.add(point);
      }
      return ret;
    }

    @Override
    public Expression put(Location point, int bit, Expression expression) {
      return put(new LocationBit(point, bit), expression);
    }

    @Override
    public Expression get(Location point, int bit) {
      return get(new LocationBit(point, bit));
    }
  }

  /**
   * Checks whether any of the recently placed expressions in the expression map are
   * self-referential; if so, return it.
   */
  private static Expression checkForCircularExpressions(ExpressionMap expressionMap) {
    for (val point : expressionMap.dirtyPoints) {
      val expr = expressionMap.get(point);
      if (expr.isCircular()) return expr;
    }
    return null;
  }

  //
  // computeExpression
  //
  /**
   * Computes the expression corresponding to the given circuit, or raises ComputeException if
   * difficulties arise.
   */
  public static void computeExpression(AnalyzerModel model, Circuit circuit, Map<Instance, String> pinNames) throws AnalyzeException {
    val expressionMap = new ExpressionMap(circuit);

    val inputVars = new ArrayList<Var>();
    val outputVars = new ArrayList<Var>();
    val outputPins = new ArrayList<Instance>();
    for (val entry : pinNames.entrySet()) {
      val pin = entry.getKey();
      val label = entry.getValue();
      val width = pin.getAttributeValue(StdAttr.WIDTH).getWidth();
      if (Pin.FACTORY.isInputPin(pin)) {
        expressionMap.currentCause = Instance.getComponentFor(pin);
        for (var b = 0; b < width; b++) {
          val e = Expressions.variable(width > 1 ? label + "[" + b + "]" : label);
          expressionMap.put(new LocationBit(pin.getLocation(), b), e);
        }
        inputVars.add(new Var(label, width));
      } else {
        outputPins.add(pin);
        outputVars.add(new Var(label, width));
      }
    }

    propagateComponents(expressionMap, circuit.getNonWires());

    for (var iterations = 0; !expressionMap.dirtyPoints.isEmpty(); iterations++) {
      if (iterations > MAX_ITERATIONS) {
        throw new AnalyzeException.Circular();
      }

      propagateWires(expressionMap, new HashSet<>(expressionMap.dirtyPoints));

      val dirtyComponents = getDirtyComponents(circuit, expressionMap.dirtyPoints);
      expressionMap.dirtyPoints.clear();
      propagateComponents(expressionMap, dirtyComponents);

      val expr = checkForCircularExpressions(expressionMap);
      if (expr != null) throw new AnalyzeException.Circular();
    }

    model.setVariables(inputVars, outputVars);
    for (val pin : outputPins) {
      val label = pinNames.get(pin);
      val width = pin.getAttributeValue(StdAttr.WIDTH).getWidth();
      for (var b = 0; b < width; b++) {
        val loc = new LocationBit(pin.getLocation(), b);
        val name = (width > 1 ? label + "[" + b + "]" : label);
        model.getOutputExpressions().setExpression(name, expressionMap.get(loc));
      }
    }
  }

  //
  // ComputeTable
  //
  /** Returns a truth table corresponding to the circuit. */
  public static void computeTable(AnalyzerModel model, Project proj, Circuit circuit, Map<Instance, String> pinLabels) {
    val inputPins = new ArrayList<Instance>();
    val inputVars = new ArrayList<Var>();
    val inputNames = new ArrayList<String>();
    val outputPins = new ArrayList<Instance>();
    val outputVars = new ArrayList<Var>();
    val outputNames = new ArrayList<String>();
    for (val entry : pinLabels.entrySet()) {
      val pin = entry.getKey();
      val width = pin.getAttributeValue(StdAttr.WIDTH).getWidth();
      val var = new Var(entry.getValue(), width);
      if (Pin.FACTORY.isInputPin(pin)) {
        inputPins.add(pin);
        for (val name : var) inputNames.add(name);
        inputVars.add(var);
      } else {
        outputPins.add(pin);
        for (val name : var) outputNames.add(name);
        outputVars.add(var);
      }
    }

    val inputCount = inputNames.size();
    val rowCount = 1 << inputCount;
    val columns = new Entry[outputNames.size()][rowCount];

    for (var i = 0; i < rowCount; i++) {
      val circuitState = new CircuitState(proj, circuit);
      var incol = 0;
      for (val pin : inputPins) {
        val width = pin.getAttributeValue(StdAttr.WIDTH).getWidth();
        val v = new Value[width];
        for (var b = width - 1; b >= 0; b--) {
          var value = TruthTable.isInputSet(i, incol++, inputCount);
          v[b] = value ? Value.TRUE : Value.FALSE;
        }
        val pinState = circuitState.getInstanceState(pin);
        Pin.FACTORY.setValue(pinState, Value.create(v));
      }

      val prop = circuitState.getPropagator();
      prop.propagate();
      /*
       * TODO for the SimulatorPrototype class do { prop.step(); } while
       * (prop.isPending());
       */
      // TODO: Search for circuit state

      if (prop.isOscillating()) {
        for (var j = 0; j < columns.length; j++) {
          columns[j][i] = Entry.OSCILLATE_ERROR;
        }
      } else {
        var outcol = 0;
        for (val pin : outputPins) {
          int width = pin.getAttributeValue(StdAttr.WIDTH).getWidth();
          val pinState = circuitState.getInstanceState(pin);
          Entry out;
          for (int b = width - 1; b >= 0; b--) {
            val outValue = Pin.FACTORY.getValue(pinState).get(b);
            if (outValue == Value.TRUE)
              out = Entry.ONE;
            else if (outValue == Value.FALSE)
              out = Entry.ZERO;
            else if (outValue == Value.ERROR)
              out = Entry.BUS_ERROR;
            else
              out = Entry.DONT_CARE;
            columns[outcol++][i] = out;
          }
        }
      }
    }

    model.setVariables(inputVars, outputVars);
    for (var i = 0; i < columns.length; i++) {
      model.getTruthTable().setOutputColumn(i, columns[i]);
    }
  }

  // computes outputs of affected components
  private static HashSet<Component> getDirtyComponents(Circuit circuit, Set<LocationBit> pointsToProcess) {
    val dirtyComponents = new HashSet<Component>();
    for (val point : pointsToProcess) {
      dirtyComponents.addAll(circuit.getNonWires(point.loc));
    }
    return dirtyComponents;
  }

  //
  // getPinLabels
  //
  /**
   * Returns a sorted map from Pin objects to String objects, listed in canonical order (top-down
   * order, with ties broken left-right).
   */
  public static SortedMap<Instance, String> getPinLabels(Circuit circuit) {
    val ret = new TreeMap<Instance, String>(Location.CompareVertical);

    // Put the pins into the TreeMap, with null labels
    for (val pin : circuit.getAppearance().getPortOffsets(Direction.EAST).values()) {
      ret.put(pin, null);
    }

    // Process first the pins that the user has given labels.
    val pinList = new ArrayList<Instance>(ret.keySet());
    val labelsTaken = new HashSet<String>();
    for (val pin : pinList) {
      var label = toValidLabel(pin.getAttributeSet().getValue(StdAttr.LABEL));
      if (label != null) {
        if (labelsTaken.contains(label)) {
          var i = 2;
          while (labelsTaken.contains(label + i)) i++;
          label = label + i;
        }
        ret.put(pin, label);
        labelsTaken.add(label);
      }
    }

    // Now process the unlabeled pins.
    for (val pin : pinList) {
      if (ret.get(pin) != null) continue;

      String defaultList;
      if (Pin.FACTORY.isInputPin(pin)) {
        defaultList = S.get("defaultInputLabels");
        if (!defaultList.contains(",")) {
          defaultList = "a,b,c,d,e,f,g,h";
        }
      } else {
        defaultList = S.get("defaultOutputLabels");
        if (!defaultList.contains(",")) {
          defaultList = "x,y,z,u,v,w,s,t";
        }
      }

      val options = defaultList.split(",");
      String label = null;
      for (var i = 0; label == null && i < options.length; i++) {
        if (!labelsTaken.contains(options[i])) {
          label = options[i];
        }
      }
      if (label == null) {
        // This is an extreme measure that should never happen
        // if the default labels are defined properly and the
        // circuit doesn't exceed the maximum number of pins.
        int i = 1;
        do {
          i++;
          label = "x" + i;
        } while (labelsTaken.contains(label));
      }

      labelsTaken.add(label);
      ret.put(pin, label);
    }

    return ret;
  }

  private static void propagateComponents(ExpressionMap expressionMap, Collection<Component> components) throws AnalyzeException {
    for (val comp : components) {
      val computer = (ExpressionComputer) comp.getFeature(ExpressionComputer.class);
      if (computer != null) {
        try {
          expressionMap.currentCause = comp;
          computer.computeExpression(expressionMap);
        } catch (UnsupportedOperationException e) {
          throw new AnalyzeException.CannotHandle(comp.getFactory().getDisplayName());
        }
      } else if (comp.getFactory() instanceof Pin) { // pins are handled elsewhere
      } else if (comp.getFactory() instanceof SplitterFactory) { // splitters are handled elsewhere
      } else {
        throw new AnalyzeException.CannotHandle(comp.getFactory().getDisplayName());
      }
    }
  }

  // propagates expressions down wires
  private static void propagateWires(ExpressionMap expressionMap, HashSet<LocationBit> pointsToProcess) throws AnalyzeException {
    expressionMap.currentCause = null;
    for (val locationBit : pointsToProcess) {
      val e = expressionMap.get(locationBit);
      expressionMap.currentCause = expressionMap.causes.get(locationBit);
      val bundle = expressionMap.circuit.wires.getWireBundle(locationBit.loc);
      if (e != null && bundle != null && bundle.isValid() && bundle.threads != null) {
        if (bundle.threads.length <= locationBit.bit) {
          throw new AnalyzeException.CannotHandle("incompatible widths");
        }
        val t = bundle.threads[locationBit.bit];
        for (val tb : t.getBundles()) {
          for (val p2 : tb.b.points) {
            if (p2.equals(locationBit.loc)) continue;
            val p2b = new LocationBit(p2, tb.loc);
            val old = expressionMap.get(p2b);
            if (old != null) {
              val eCause = expressionMap.currentCause;
              val oldCause = expressionMap.causes.get(p2b);
              if (eCause != oldCause && !old.equals(e)) {
                throw new AnalyzeException.Conflict();
              }
            }
            expressionMap.put(p2b, e);
          }
        }
      }
    }
  }

  private static String toValidLabel(String label) {
    if (label == null) return null;
    StringBuilder end = null;
    val ret = new StringBuilder();
    var afterWhitespace = false;
    for (var i = 0; i < label.length(); i++) {
      var c = label.charAt(i);
      if (Character.isJavaIdentifierStart(c)) {
        if (afterWhitespace) {
          // capitalize words after the first one
          c = Character.toTitleCase(c);
          afterWhitespace = false;
        }
        ret.append(c);
      } else if (Character.isJavaIdentifierPart(c)) {
        // If we can't place it at the start, we'll dump it
        // onto the end.
        if (ret.length() > 0) {
          ret.append(c);
        } else {
          if (end == null) end = new StringBuilder();
          end.append(c);
        }
        afterWhitespace = false;
      } else if (Character.isWhitespace(c)) {
        afterWhitespace = true;
      } else { // just ignore any other characters
      }
    }
    if (end != null && ret.length() > 0) ret.append(end);
    if (ret.length() == 0) return null;
    return ret.toString();
  }

  private static final int MAX_ITERATIONS = 100;

  private Analyze() {
    // dummy
  }
}
