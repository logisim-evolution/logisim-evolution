package com.cburch.logisim.statemachine.simulator;

import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.statemachine.PrettyPrinter;
import com.cburch.logisim.statemachine.fSMDSL.AndExpr;
import com.cburch.logisim.statemachine.fSMDSL.BoolExpr;
import com.cburch.logisim.statemachine.fSMDSL.CmpExpr;
import com.cburch.logisim.statemachine.fSMDSL.Command;
import com.cburch.logisim.statemachine.fSMDSL.ConcatExpr;
import com.cburch.logisim.statemachine.fSMDSL.ConstRef;
import com.cburch.logisim.statemachine.fSMDSL.Constant;
import com.cburch.logisim.statemachine.fSMDSL.DefaultPredicate;
import com.cburch.logisim.statemachine.fSMDSL.FSM;
import com.cburch.logisim.statemachine.fSMDSL.InputPort;
import com.cburch.logisim.statemachine.fSMDSL.NotExpr;
import com.cburch.logisim.statemachine.fSMDSL.OrExpr;
import com.cburch.logisim.statemachine.fSMDSL.Port;
import com.cburch.logisim.statemachine.fSMDSL.PortRef;
import com.cburch.logisim.statemachine.fSMDSL.Range;
import com.cburch.logisim.statemachine.fSMDSL.State;
import com.cburch.logisim.statemachine.fSMDSL.Transition;
import com.cburch.logisim.statemachine.simulator.ClockState;
import com.google.common.base.Objects;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.eclipse.emf.common.util.EList;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.InputOutput;
import org.eclipse.xtext.xbase.lib.IntegerRange;
import org.eclipse.xtext.xbase.lib.ListExtensions;

@SuppressWarnings("all")
public class FSMSimulator extends ClockState implements InstanceData {
  private FSM fsm;
  
  private State current;
  
  private HashMap<Port, String> inputs;
  
  private HashMap<Port, String> outputs;
  
  private static final char ONE_C = '1';
  
  private static final char ZERO_C = '0';
  
  public FSMSimulator(final FSM fsm) {
    boolean _notEquals = (!Objects.equal(fsm, null));
    if (_notEquals) {
      this.fsm = fsm;
      HashMap<Port, String> _hashMap = new HashMap<Port, String>();
      this.inputs = _hashMap;
      HashMap<Port, String> _hashMap_1 = new HashMap<Port, String>();
      this.outputs = _hashMap_1;
      this.current = fsm.getStart();
      this.refreshInputPorts();
      this.restoreOutputPorts();
    } else {
      throw new RuntimeException("Cannot simulate null FSM");
    }
  }
  
  public State setCurrentState(final State s) {
    return this.current = s;
  }
  
  public State getCurrentState() {
    return this.current;
  }
  
  public String quote(final String s) {
    return (("\"" + s) + "\"");
  }
  
  public String zeros(final int width) {
    String _xblockexpression = null;
    {
      String res = "";
      IntegerRange _upTo = new IntegerRange(0, (width - 1));
      for (final int i : _upTo) {
        String _res = res;
        res = (_res + "0");
      }
      _xblockexpression = this.quote(res);
    }
    return _xblockexpression;
  }
  
  public String ones(final int width) {
    String _xblockexpression = null;
    {
      String res = "";
      IntegerRange _upTo = new IntegerRange(0, (width - 1));
      for (final int i : _upTo) {
        String _res = res;
        res = (_res + "1");
      }
      _xblockexpression = this.quote(res);
    }
    return _xblockexpression;
  }
  
  private static final String ZERO = "\"0\"";
  
  private static final String ONE = "\"1\"";
  
  private final boolean VERBOSE = true;
  
  public boolean isTrue(final String s) {
    return s.equals(FSMSimulator.ONE);
  }
  
  public HashMap<Port, String> refreshInputPorts() {
    HashMap<Port, String> _xblockexpression = null;
    {
      HashMap<Port, String> newInputs = new HashMap<Port, String>();
      EList<Port> _in = this.fsm.getIn();
      for (final Port newIp : _in) {
        {
          newInputs.put(newIp, this.zeros(newIp.getWidth()));
          Set<Port> _keySet = this.inputs.keySet();
          for (final Port oldIp : _keySet) {
            {
              final boolean nameEqu = newIp.getName().equals(oldIp.getName());
              int _width = newIp.getWidth();
              int _width_1 = oldIp.getWidth();
              final boolean witdhEqu = (_width == _width_1);
              if ((nameEqu && witdhEqu)) {
                newInputs.put(newIp, this.inputs.get(oldIp));
              }
            }
          }
          StringConcatenation _builder = new StringConcatenation();
          String _name = newIp.getName();
          _builder.append(_name);
          _builder.append(":");
          int _hashCode = newIp.hashCode();
          _builder.append(_hashCode);
          _builder.append(" -> ");
          String _get = newInputs.get(newIp);
          _builder.append(_get);
          this.debug(_builder.toString());
        }
      }
      _xblockexpression = this.inputs = newInputs;
    }
    return _xblockexpression;
  }
  
  public HashMap<Port, String> restoreOutputPorts() {
    HashMap<Port, String> _xblockexpression = null;
    {
      HashMap<Port, String> newOutputs = new HashMap<Port, String>();
      EList<Port> _out = this.fsm.getOut();
      for (final Port newOp : _out) {
        {
          newOutputs.put(newOp, this.zeros(newOp.getWidth()));
          Set<Port> _keySet = this.outputs.keySet();
          for (final Port oldIp : _keySet) {
            {
              final boolean nameEqu = newOp.getName().equals(oldIp.getName());
              int _width = newOp.getWidth();
              int _width_1 = oldIp.getWidth();
              final boolean witdhEqu = (_width == _width_1);
              if ((nameEqu && witdhEqu)) {
                newOutputs.put(newOp, this.outputs.get(oldIp));
              }
            }
          }
          StringConcatenation _builder = new StringConcatenation();
          String _name = newOp.getName();
          _builder.append(_name);
          _builder.append(":");
          int _hashCode = newOp.hashCode();
          _builder.append(_hashCode);
          _builder.append(" -> ");
          String _get = newOutputs.get(newOp);
          _builder.append(_get);
          this.debug(_builder.toString());
        }
      }
      _xblockexpression = this.outputs = newOutputs;
    }
    return _xblockexpression;
  }
  
  public HashMap<Port, String> resetOutputPorts() {
    HashMap<Port, String> _xblockexpression = null;
    {
      HashMap<Port, String> newOutputs = new HashMap<Port, String>();
      EList<Port> _out = this.fsm.getOut();
      for (final Port newOp : _out) {
        {
          newOutputs.put(newOp, this.zeros(newOp.getWidth()));
          StringConcatenation _builder = new StringConcatenation();
          _builder.append("Reset ");
          String _name = newOp.getName();
          _builder.append(_name);
          _builder.append(":");
          int _hashCode = newOp.hashCode();
          _builder.append(_hashCode);
          _builder.append(" -> ");
          String _get = newOutputs.get(newOp);
          _builder.append(_get);
          this.debug(_builder.toString());
        }
      }
      _xblockexpression = this.outputs = newOutputs;
    }
    return _xblockexpression;
  }
  
  public FSM getFSM() {
    return this.fsm;
  }
  
  public HashMap<Port, String> reset() {
    HashMap<Port, String> _xblockexpression = null;
    {
      this.current = this.fsm.getStart();
      this.refreshInputPorts();
      _xblockexpression = this.restoreOutputPorts();
    }
    return _xblockexpression;
  }
  
  public String getOutput(final int i) {
    try {
      final Port op = this.fsm.getOut().get(i);
      this.printIOMap();
      boolean _containsKey = this.outputs.containsKey(op);
      boolean _not = (!_containsKey);
      if (_not) {
        String _name = op.getName();
        String _plus = ("output  " + _name);
        String _plus_1 = (_plus + ":");
        int _hashCode = op.hashCode();
        String _plus_2 = (_plus_1 + Integer.valueOf(_hashCode));
        String _plus_3 = (_plus_2 + " at ");
        String _plus_4 = (_plus_3 + Integer.valueOf(i));
        String _plus_5 = (_plus_4 + " is not a known output in fsm ");
        String _name_1 = this.fsm.getName();
        String _plus_6 = (_plus_5 + _name_1);
        String _plus_7 = (_plus_6 + " ");
        final Function1<Port, String> _function = (Port p) -> {
          int _indexOf = this.fsm.getOut().indexOf(p);
          String _plus_8 = ("Port[" + Integer.valueOf(_indexOf));
          String _plus_9 = (_plus_8 + "]=");
          String _name_2 = p.getName();
          String _plus_10 = (_plus_9 + _name_2);
          String _plus_11 = (_plus_10 + ":");
          int _hashCode_1 = p.hashCode();
          return (_plus_11 + Integer.valueOf(_hashCode_1));
        };
        List<String> _map = ListExtensions.<Port, String>map(this.fsm.getOut(), _function);
        String _plus_8 = (_plus_7 + _map);
        throw new RuntimeException(_plus_8);
      }
      return this.outputs.get(op);
    } catch (final Throwable _t) {
      if (_t instanceof Exception) {
        final Exception e = (Exception)_t;
        String _message = e.getMessage();
        throw new RuntimeException(_message);
      } else {
        throw Exceptions.sneakyThrow(_t);
      }
    }
  }
  
  public String updateInput(final InputPort ip, final String b) {
    String _xblockexpression = null;
    {
      String _name = ip.getName();
      String _plus = ("-> setting " + _name);
      String _plus_1 = (_plus + ":");
      int _hashCode = ip.hashCode();
      String _plus_2 = (_plus_1 + Integer.valueOf(_hashCode));
      String _plus_3 = (_plus_2 + " to ");
      String _plus_4 = (_plus_3 + b);
      this.debug(_plus_4);
      this.printIOMap();
      String _xifexpression = null;
      boolean _containsKey = this.inputs.containsKey(ip);
      if (_containsKey) {
        String _xblockexpression_1 = null;
        {
          int _length = b.length();
          int _minus = (_length - 2);
          int _width = ip.getWidth();
          boolean _notEquals = (_minus != _width);
          if (_notEquals) {
            String _name_1 = ip.getName();
            String _plus_5 = ("port datawidth mismatch" + _name_1);
            String _plus_6 = (_plus_5 + "[");
            int _width_1 = ip.getWidth();
            String _plus_7 = (_plus_6 + Integer.valueOf(_width_1));
            String _plus_8 = (_plus_7 + "]  in ");
            String _name_2 = this.fsm.getName();
            String _plus_9 = (_plus_8 + _name_2);
            throw new RuntimeException(_plus_9);
          }
          _xblockexpression_1 = this.inputs.put(ip, b);
        }
        _xifexpression = _xblockexpression_1;
      } else {
        String _name_1 = ip.getName();
        String _plus_5 = ("Unregistered input port " + _name_1);
        String _plus_6 = (_plus_5 + ":");
        int _hashCode_1 = ip.hashCode();
        String _plus_7 = (_plus_6 + Integer.valueOf(_hashCode_1));
        String _plus_8 = (_plus_7 + "  in ");
        String _name_2 = this.fsm.getName();
        String _plus_9 = (_plus_8 + _name_2);
        throw new RuntimeException(_plus_9);
      }
      _xblockexpression = _xifexpression;
    }
    return _xblockexpression;
  }
  
  public State updateState() {
    String _name = this.fsm.getName();
    String _plus = ("FSM " + _name);
    String _plus_1 = (_plus + " current state ");
    String _name_1 = this.current.getName();
    String _plus_2 = (_plus_1 + _name_1);
    this.debug(_plus_2);
    int _size = this.fsm.getIn().size();
    int _size_1 = this.inputs.keySet().size();
    boolean _notEquals = (_size != _size_1);
    if (_notEquals) {
      throw new RuntimeException("inconsistent state for input port mapping ");
    }
    int _size_2 = this.fsm.getOut().size();
    int _size_3 = this.outputs.keySet().size();
    boolean _notEquals_1 = (_size_2 != _size_3);
    if (_notEquals_1) {
      throw new RuntimeException("inconsistent state for output port mapping ");
    }
    Set<Port> _keySet = this.inputs.keySet();
    for (final Port e : _keySet) {
      String _name_2 = e.getName();
      String _plus_3 = ("\t- In " + _name_2);
      String _plus_4 = (_plus_3 + "=>");
      String _get = this.inputs.get(e);
      String _plus_5 = (_plus_4 + _get);
      this.debug(_plus_5);
    }
    Set<Port> _keySet_1 = this.outputs.keySet();
    for (final Port e_1 : _keySet_1) {
      String _name_3 = e_1.getName();
      String _plus_6 = ("\t- Out " + _name_3);
      String _plus_7 = (_plus_6 + "=>");
      String _get_1 = this.outputs.get(e_1);
      String _plus_8 = (_plus_7 + _get_1);
      this.debug(_plus_8);
    }
    State defaultDst = null;
    State nextDst = null;
    EList<Transition> _transition = this.current.getTransition();
    for (final Transition t : _transition) {
      {
        String _pp = PrettyPrinter.pp(t);
        String _plus_9 = ("\tTransition= " + _pp);
        this.debug(_plus_9);
        BoolExpr _predicate = t.getPredicate();
        if ((_predicate instanceof DefaultPredicate)) {
          defaultDst = t.getDst();
        } else {
          final String res = this.eval(t.getPredicate());
          String _pp_1 = PrettyPrinter.pp(t.getPredicate());
          String _plus_10 = ("\t\t" + _pp_1);
          String _plus_11 = (_plus_10 + "=");
          String _plus_12 = (_plus_11 + res);
          String _plus_13 = (_plus_12 + "");
          this.debug(_plus_13);
          boolean _isTrue = this.isTrue(res);
          if (_isTrue) {
            nextDst = t.getDst();
            String _name_4 = nextDst.getName();
            String _plus_14 = ("=> transition fired : next state is " + _name_4);
            this.debug(_plus_14);
          } else {
            this.debug("=> transition not fired");
          }
        }
      }
    }
    boolean _notEquals_2 = (!Objects.equal(nextDst, null));
    if (_notEquals_2) {
      this.current = nextDst;
    } else {
      boolean _notEquals_3 = (!Objects.equal(defaultDst, null));
      if (_notEquals_3) {
        this.current = defaultDst;
        String _name_4 = defaultDst.getName();
        String _plus_9 = ("\t\tDefault transition fired " + _name_4);
        this.debug(_plus_9);
      }
    }
    return this.current;
  }
  
  public void updateCommands() {
    this.resetOutputPorts();
    EList<Command> _commands = this.current.getCommandList().getCommands();
    for (final Command c : _commands) {
      {
        final String res = this.eval(c.getValue());
        this.outputs.replace(c.getName(), res);
        String _name = c.getName().getName();
        String _plus = ("\tSet " + _name);
        String _plus_1 = (_plus + ":");
        int _hashCode = c.getName().hashCode();
        String _plus_2 = (_plus_1 + Integer.valueOf(_hashCode));
        String _plus_3 = (_plus_2 + " to ");
        String _plus_4 = (_plus_3 + res);
        this.debug(_plus_4);
      }
    }
  }
  
  protected String _eval(final BoolExpr exp) {
    String _pp = PrettyPrinter.pp(exp);
    String _plus = ("Unsupported operation" + _pp);
    throw new RuntimeException(_plus);
  }
  
  protected String _eval(final ConcatExpr exp) {
    String _xblockexpression = null;
    {
      final StringBuffer r = new StringBuffer();
      EList<BoolExpr> _args = exp.getArgs();
      for (final BoolExpr arg : _args) {
        r.append(this.unquote(this.eval(arg)));
      }
      _xblockexpression = this.quote(r.toString());
    }
    return _xblockexpression;
  }
  
  public String unquote(final String s) {
    int _length = s.length();
    int _minus = (_length - 1);
    return s.substring(1, _minus);
  }
  
  protected String _eval(final DefaultPredicate exp) {
    return FSMSimulator.ZERO;
  }
  
  protected String _eval(final CmpExpr b) {
    String _xblockexpression = null;
    {
      int _size = b.getArgs().size();
      boolean _notEquals = (_size != 2);
      if (_notEquals) {
        String _pp = PrettyPrinter.pp(b);
        String _plus = ("Inconsistent arity for expression " + _pp);
        throw new RuntimeException(_plus);
      }
      final String opA = this.eval(b.getArgs().get(0));
      final String opB = this.eval(b.getArgs().get(1));
      final boolean equ = opA.equals(opB);
      String res = "";
      String _op = b.getOp();
      if (_op != null) {
        switch (_op) {
          case "==":
            if (equ) {
              res = this.quote("1");
            } else {
              res = this.quote("0");
            }
            break;
          case "/=":
            if (equ) {
              res = this.quote("0");
            } else {
              res = this.quote("1");
            }
            break;
          default:
            String _op_1 = b.getOp();
            String _plus_1 = ("Inconsistent operator " + _op_1);
            String _plus_2 = (_plus_1 + " for expression ");
            String _pp_1 = PrettyPrinter.pp(b);
            String _plus_3 = (_plus_2 + _pp_1);
            throw new RuntimeException(_plus_3);
        }
      } else {
        String _op_1 = b.getOp();
        String _plus_1 = ("Inconsistent operator " + _op_1);
        String _plus_2 = (_plus_1 + " for expression ");
        String _pp_1 = PrettyPrinter.pp(b);
        String _plus_3 = (_plus_2 + _pp_1);
        throw new RuntimeException(_plus_3);
      }
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("eval(");
      String _pp_2 = PrettyPrinter.pp(b);
      _builder.append(_pp_2);
      _builder.append(")=");
      _builder.append(res);
      this.debug(_builder.toString());
      _xblockexpression = res;
    }
    return _xblockexpression;
  }
  
  public String debug(final String string) {
    String _xifexpression = null;
    if (this.VERBOSE) {
      _xifexpression = InputOutput.<String>println(string);
    }
    return _xifexpression;
  }
  
  protected String _eval(final Constant exp) {
    return exp.getValue();
  }
  
  public String setCharAt(final String s, final char c, final int i) {
    String _xblockexpression = null;
    {
      final StringBuilder myName = new StringBuilder(s);
      myName.setCharAt(i, c);
      _xblockexpression = myName.toString();
    }
    return _xblockexpression;
  }
  
  protected String _eval(final OrExpr b) {
    String _xblockexpression = null;
    {
      int width = (-1);
      List<String> l = new ArrayList<String>();
      String andRes = "";
      EList<BoolExpr> _args = b.getArgs();
      for (final BoolExpr arg : _args) {
        {
          final String res = this.unquote(this.eval(arg));
          l.add(res);
          if ((width == (-1))) {
            width = res.length();
          }
        }
      }
      andRes = this.unquote(this.zeros(width));
      for (final String r : l) {
        {
          int _length = r.length();
          boolean _notEquals = (width != _length);
          if (_notEquals) {
            String _pp = PrettyPrinter.pp(b);
            String _plus = ("Inconsistent width in expression " + _pp);
            throw new RuntimeException(_plus);
          }
          IntegerRange _upTo = new IntegerRange(0, (width - 1));
          for (final Integer i : _upTo) {
            {
              final char opA = andRes.charAt((i).intValue());
              final char opB = r.charAt((i).intValue());
              final char and1 = this.or(opA, opB);
              andRes = this.setCharAt(andRes, and1, (i).intValue());
            }
          }
        }
      }
      final String res = this.quote(andRes);
      _xblockexpression = res;
    }
    return _xblockexpression;
  }
  
  protected String _eval(final AndExpr b) {
    String _xblockexpression = null;
    {
      int width = (-1);
      List<String> l = new ArrayList<String>();
      String andRes = "";
      EList<BoolExpr> _args = b.getArgs();
      for (final BoolExpr arg : _args) {
        {
          final String res = this.unquote(this.eval(arg));
          l.add(res);
          if ((width == (-1))) {
            width = res.length();
          }
        }
      }
      andRes = this.unquote(this.ones(width));
      for (final String r : l) {
        {
          int _length = r.length();
          boolean _notEquals = (width != _length);
          if (_notEquals) {
            String _pp = PrettyPrinter.pp(b);
            String _plus = ("Inconsistent width in expression " + _pp);
            throw new RuntimeException(_plus);
          }
          IntegerRange _upTo = new IntegerRange(0, (width - 1));
          for (final Integer i : _upTo) {
            {
              final char opA = andRes.charAt((i).intValue());
              final char opB = r.charAt((i).intValue());
              final char and1 = this.and(opA, opB);
              andRes = this.setCharAt(andRes, and1, (i).intValue());
            }
          }
        }
      }
      final String res = this.quote(andRes);
      _xblockexpression = res;
    }
    return _xblockexpression;
  }
  
  protected String _eval(final NotExpr b) {
    String _xblockexpression = null;
    {
      int _size = b.getArgs().size();
      boolean _notEquals = (_size != 1);
      if (_notEquals) {
        String _pp = PrettyPrinter.pp(b);
        String _plus = ("Inconsistent arity for expression " + _pp);
        throw new RuntimeException(_plus);
      }
      int width = (-1);
      List<String> l = new ArrayList<String>();
      String notExpr = "";
      String res = this.eval(b.getArgs().get(0));
      res = res.replace("0", "@");
      res = res.replace("1", "0");
      res = res.replace("@", "1");
      String _pp_1 = PrettyPrinter.pp(b);
      String _plus_1 = ("eval(" + _pp_1);
      String _plus_2 = (_plus_1 + ")=");
      String _plus_3 = (_plus_2 + res);
      this.debug(_plus_3);
      _xblockexpression = res;
    }
    return _xblockexpression;
  }
  
  public char or(final char c, final char b) {
    char _xblockexpression = (char) 0;
    {
      final String in = (("" + Character.valueOf(c)) + Character.valueOf(b));
      char res = '0';
      if (in != null) {
        switch (in) {
          case "00":
            res = '0';
            break;
          case "01":
            res = '1';
            break;
          case "10":
            res = '1';
            break;
          case "11":
            res = '1';
            break;
          default:
            throw new RuntimeException((((("Unsupported value \"" + in) + "\",") + Character.valueOf(b)) + " only \'0\' or \'1\' supported"));
        }
      } else {
        throw new RuntimeException((((("Unsupported value \"" + in) + "\",") + Character.valueOf(b)) + " only \'0\' or \'1\' supported"));
      }
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("eval(or(");
      _builder.append(b);
      _builder.append(",");
      _builder.append(c);
      _builder.append("))=");
      _builder.append(res);
      this.debug(_builder.toString());
      _xblockexpression = res;
    }
    return _xblockexpression;
  }
  
  public char and(final char c, final char b) {
    char _xblockexpression = (char) 0;
    {
      final String in = (("" + Character.valueOf(c)) + Character.valueOf(b));
      char res = '0';
      if (in != null) {
        switch (in) {
          case "00":
            res = '0';
            break;
          case "01":
            res = '0';
            break;
          case "10":
            res = '0';
            break;
          case "11":
            res = '1';
            break;
          default:
            throw new RuntimeException((((("Unsupported value " + Character.valueOf(c)) + ",") + Character.valueOf(b)) + " only \'0\' or \'1\' supported"));
        }
      } else {
        throw new RuntimeException((((("Unsupported value " + Character.valueOf(c)) + ",") + Character.valueOf(b)) + " only \'0\' or \'1\' supported"));
      }
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("eval(and(");
      _builder.append(b);
      _builder.append(",");
      _builder.append(c);
      _builder.append("))=");
      _builder.append(res);
      this.debug(_builder.toString());
      _xblockexpression = res;
    }
    return _xblockexpression;
  }
  
  public char not(final char c) {
    char _xblockexpression = (char) 0;
    {
      char res = '0';
      boolean _matched = false;
      if (Objects.equal(c, "0")) {
        _matched=true;
        res = '1';
      }
      if (!_matched) {
        if (Objects.equal(c, "1")) {
          _matched=true;
          res = '0';
        }
      }
      if (!_matched) {
        throw new RuntimeException((("Unsupported value " + Character.valueOf(c)) + ", only \'0\' or \'1\' supported"));
      }
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("eval(not(");
      _builder.append(c);
      _builder.append("))=");
      _builder.append(res);
      this.debug(_builder.toString());
      _xblockexpression = res;
    }
    return _xblockexpression;
  }
  
  protected String _eval(final ConstRef b) {
    return this.eval(b.getConst().getValue());
  }
  
  protected String _eval(final PortRef b) {
    String _xblockexpression = null;
    {
      Port _port = b.getPort();
      boolean _equals = Objects.equal(_port, null);
      if (_equals) {
        String _pp = PrettyPrinter.pp(b);
        String _plus = ("Invalid expression " + _pp);
        String _plus_1 = (_plus + " ");
        throw new RuntimeException(_plus_1);
      }
      String res = "";
      boolean _containsKey = this.inputs.containsKey(b.getPort());
      if (_containsKey) {
        res = this.inputs.get(b.getPort());
      } else {
        this.printIOMap();
        String _name = b.getPort().getName();
        String _plus_2 = ("Port  " + _name);
        String _plus_3 = (_plus_2 + ":");
        int _hashCode = b.getPort().hashCode();
        String _plus_4 = (_plus_3 + Integer.valueOf(_hashCode));
        String _plus_5 = (_plus_4 + " has no value");
        throw new RuntimeException(_plus_5);
      }
      Range _range = b.getRange();
      boolean _notEquals = (!Objects.equal(_range, null));
      if (_notEquals) {
        int _ub = b.getRange().getUb();
        boolean _equals_1 = (_ub == (-1));
        if (_equals_1) {
          int _lb = b.getRange().getLb();
          final int lb = (_lb + 1);
          res = new StringBuilder(res).reverse().toString();
          res = this.quote(res.substring(lb, (lb + 1)));
        } else {
          int _lb_1 = b.getRange().getLb();
          final int lb_1 = (_lb_1 + 1);
          int _ub_1 = b.getRange().getUb();
          final int ub = (_ub_1 + 1);
          res = new StringBuilder(res).reverse().toString();
          res = this.quote(res.substring(lb_1, (ub + 1)));
          res = new StringBuilder(res).reverse().toString();
        }
      } else {
      }
      String _pp_1 = PrettyPrinter.pp(b);
      String _plus_6 = ("eval(" + _pp_1);
      String _plus_7 = (_plus_6 + ")=");
      String _plus_8 = (_plus_7 + res);
      this.debug(_plus_8);
      _xblockexpression = res;
    }
    return _xblockexpression;
  }
  
  public Object printIOMap() {
    return null;
  }
  
  public String eval(final BoolExpr b) {
    if (b instanceof AndExpr) {
      return _eval((AndExpr)b);
    } else if (b instanceof CmpExpr) {
      return _eval((CmpExpr)b);
    } else if (b instanceof ConcatExpr) {
      return _eval((ConcatExpr)b);
    } else if (b instanceof ConstRef) {
      return _eval((ConstRef)b);
    } else if (b instanceof Constant) {
      return _eval((Constant)b);
    } else if (b instanceof DefaultPredicate) {
      return _eval((DefaultPredicate)b);
    } else if (b instanceof NotExpr) {
      return _eval((NotExpr)b);
    } else if (b instanceof OrExpr) {
      return _eval((OrExpr)b);
    } else if (b instanceof PortRef) {
      return _eval((PortRef)b);
    } else if (b != null) {
      return _eval(b);
    } else {
      throw new IllegalArgumentException("Unhandled parameter types: " +
        Arrays.<Object>asList(b).toString());
    }
  }
}
