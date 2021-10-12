/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.gates;

import com.cburch.logisim.analyze.model.Expression;
import com.cburch.logisim.comp.ComponentFactory;
import java.util.ArrayList;

/**
 * This represents the actual gate selection used corresponding to an expression, without any
 * correspondence to how they would be laid down in a circuit. This intermediate representation
 * permits easy manipulation of an expression's translation.
 */
abstract class CircuitDetermination {
  private static class Determine implements Expression.Visitor<CircuitDetermination> {
    private Gate binary(CircuitDetermination aret, CircuitDetermination bret, ComponentFactory factory) {
      if (aret instanceof Gate a) {
        if (a.factory == factory) {
          if (bret instanceof Gate b) {
            if (b.factory == factory) {
              a.inputs.addAll(b.inputs);
              return a;
            }
          }
          a.inputs.add(bret);
          return a;
        }
      }

      if (bret instanceof Gate b) {
        if (b.factory == factory) {
          b.inputs.add(aret);
          return b;
        }
      }

      final var ret = new Gate(factory);
      ret.inputs.add(aret);
      ret.inputs.add(bret);
      return ret;
    }

    @Override
    public CircuitDetermination visitAnd(Expression a, Expression b) {
      return binary(a.visit(this), b.visit(this), AndGate.FACTORY);
    }

    @Override
    public CircuitDetermination visitConstant(int value) {
      return new Value(value);
    }

    @Override
    public CircuitDetermination visitNot(Expression aBase) {
      final var aret = aBase.visit(this);
      if (aret instanceof Gate a) {
        if (a.factory == AndGate.FACTORY) {
          a.factory = NandGate.FACTORY;
          return a;
        } else if (a.factory == OrGate.FACTORY) {
          a.factory = NorGate.FACTORY;
          return a;
        } else if (a.factory == XorGate.FACTORY) {
          a.factory = XnorGate.FACTORY;
          return a;
        }
      }

      if (aret instanceof Input a) {
        a.togleInversion();
        return a;
      }

      final var ret = new Gate(NotGate.FACTORY);
      ret.inputs.add(aret);
      return ret;
    }

    @Override
    public CircuitDetermination visitOr(Expression a, Expression b) {
      return binary(a.visit(this), b.visit(this), OrGate.FACTORY);
    }

    @Override
    public CircuitDetermination visitVariable(String name) {
      return new Input(name);
    }

    @Override
    public CircuitDetermination visitXor(Expression a, Expression b) {
      return binary(a.visit(this), b.visit(this), XorGate.FACTORY);
    }

    @Override
    public CircuitDetermination visitXnor(Expression a, Expression b) {
      return binary(a.visit(this), b.visit(this), XnorGate.FACTORY);
    }

    @Override
    public CircuitDetermination visitEq(Expression a, Expression b) {
      return binary(a.visit(this), b.visit(this), XnorGate.FACTORY);
    }
  }

  //
  // static members
  //
  static class Gate extends CircuitDetermination {
    private ComponentFactory factory;
    private ArrayList<CircuitDetermination> inputs = new ArrayList<>();

    private Gate(ComponentFactory factory) {
      this.factory = factory;
    }

    @Override
    void convertToNands() {
      // first recurse to clean up any children
      for (CircuitDetermination sub : inputs) {
        sub.convertToNands();
      }

      // repair large XOR/XNORs to odd/even parity gates
      if (factory == NotGate.FACTORY) {
        inputs.add(inputs.get(0));
      } else if (factory == AndGate.FACTORY) {
        notOutput();
      } else if (factory == OrGate.FACTORY) {
        notAllInputs();
      } else if (factory == NorGate.FACTORY) {
        notAllInputs(); // the order of these two lines is significant
        notOutput();
      } else if (factory == NandGate.FACTORY) {
      } else {
        throw new IllegalArgumentException("Cannot handle " + factory.getDisplayName());
      }
      factory = NandGate.FACTORY;
    }

    @Override
    void convertToTwoInputs() {
      if (inputs.size() <= 2) {
        for (final var a : inputs) {
          a.convertToTwoInputs();
        }
      } else {
        ComponentFactory subFactory;
        if (factory == NorGate.FACTORY) subFactory = OrGate.FACTORY;
        else if (factory == NandGate.FACTORY) subFactory = AndGate.FACTORY;
        else subFactory = factory;

        int split = (inputs.size() + 1) / 2;
        final var a = convertToTwoInputsSub(0, split, subFactory);
        final var b = convertToTwoInputsSub(split, inputs.size(), subFactory);
        inputs.clear();
        inputs.add(a);
        inputs.add(b);
      }
    }

    private CircuitDetermination convertToTwoInputsSub(
        int start, int stop, ComponentFactory subFactory) {
      if (stop - start == 1) {
        final var a = inputs.get(start);
        a.convertToTwoInputs();
        return a;
      } else {
        int split = (start + stop + 1) / 2;
        final var a = convertToTwoInputsSub(start, split, subFactory);
        final var b = convertToTwoInputsSub(split, stop, subFactory);
        Gate ret = new Gate(subFactory);
        ret.inputs.add(a);
        ret.inputs.add(b);
        return ret;
      }
    }

    ComponentFactory getFactory() {
      return factory;
    }

    ArrayList<CircuitDetermination> getInputs() {
      return inputs;
    }

    @Override
    boolean isNandNot() {
      return factory == NandGate.FACTORY && inputs.size() == 2 && inputs.get(0) == inputs.get(1);
    }

    private void notAllInputs() {
      for (int i = 0; i < inputs.size(); i++) {
        final var old = inputs.get(i);
        if (inputs.get(i) instanceof CircuitDetermination.Value inp) {
          inp.value ^= 1;
        } else if (inputs.get(i) instanceof CircuitDetermination.Input inp) {
          inp.togleInversion();
        } else if (old.isNandNot()) {
          inputs.set(i, ((Gate) old).inputs.get(0));
        } else {
          final var now = new Gate(NandGate.FACTORY);
          now.inputs.add(old);
          now.inputs.add(old);
          inputs.set(i, now);
        }
      }
    }

    private void notOutput() {
      final var sub = new Gate(NandGate.FACTORY);
      sub.inputs = this.inputs;
      this.inputs = new ArrayList<>();
      inputs.add(sub);
      inputs.add(sub);
    }

    @Override
    void repair() {
      // check whether we need to split ourself up.
      int num = inputs.size();
      if (num > GateAttributes.MAX_INPUTS) {
        int newNum = (num + GateAttributes.MAX_INPUTS - 1) / GateAttributes.MAX_INPUTS;
        final var oldInputs = inputs;
        inputs = new ArrayList<>();

        ComponentFactory subFactory = factory;
        if (subFactory == NandGate.FACTORY) subFactory = AndGate.FACTORY;
        if (subFactory == NorGate.FACTORY) subFactory = OrGate.FACTORY;

        int per = num / newNum;
        int numExtra = num - per * newNum;
        int k = 0;
        for (int i = 0; i < newNum; i++) {
          final var sub = new Gate(subFactory);
          int subCount = per + (i < numExtra ? 1 : 0);
          for (int j = 0; j < subCount; j++) {
            sub.inputs.add(oldInputs.get(k));
            k++;
          }
          inputs.add(sub);
        }
      }

      // repair large XOR/XNORs to odd/even parity gates
      if (inputs.size() > 2) {
        if (factory == XorGate.FACTORY) {
          factory = OddParityGate.FACTORY;
        } else if (factory == XnorGate.FACTORY) {
          factory = EvenParityGate.FACTORY;
        }
      }

      // finally, recurse to clean up any children
      for (final var sub : inputs) {
        sub.repair();
      }
    }
  }

  static class Input extends CircuitDetermination {
    private final String name;
    private boolean inverted = false;

    private Input(String name) {
      this.name = name;
    }

    String getName() {
      return name;
    }

    public void togleInversion() {
      inverted = !inverted;
    }

    boolean isInvertedVersion() {
      return inverted;
    }
  }

  static class Value extends CircuitDetermination {
    private int value;

    private Value(int value) {
      this.value = value;
    }

    int getValue() {
      return value;
    }
  }

  static CircuitDetermination create(Expression expr) {
    if (expr == null) return null;
    return expr.visit(new Determine());
  }

  /**
   * Converts all gates to NANDs. Note that this will fail with an exception if any XOR/XNOR gates
   * are used.
   */
  void convertToNands() {}

  /** Ensures that all gates have only two inputs. */
  void convertToTwoInputs() {}

  /**
   * A utility method for determining whether this fits the pattern of a NAND representing a NOT.
   */
  boolean isNandNot() {
    return false;
  }

  /**
   * Repairs two errors that may have cropped up in creating the circuit. First, if there are gates
   * with more inputs than their capacity, we repair them. Second, any XOR/XNOR gates with more than
   * 2 inputs should really be Odd/Even Parity gates.
   */
  void repair() {}
}
