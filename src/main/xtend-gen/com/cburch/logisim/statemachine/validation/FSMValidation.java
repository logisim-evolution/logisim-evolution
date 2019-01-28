package com.cburch.logisim.statemachine.validation;

import com.cburch.logisim.statemachine.PrettyPrinter;
import com.cburch.logisim.statemachine.bdd.BDDOptimizer;
import com.cburch.logisim.statemachine.bdd.BitWidthAnalyzer;
import com.cburch.logisim.statemachine.editor.view.FSMCustomFactory;
import com.cburch.logisim.statemachine.fSMDSL.AndExpr;
import com.cburch.logisim.statemachine.fSMDSL.BoolExpr;
import com.cburch.logisim.statemachine.fSMDSL.CmpExpr;
import com.cburch.logisim.statemachine.fSMDSL.Command;
import com.cburch.logisim.statemachine.fSMDSL.CommandList;
import com.cburch.logisim.statemachine.fSMDSL.Constant;
import com.cburch.logisim.statemachine.fSMDSL.DefaultPredicate;
import com.cburch.logisim.statemachine.fSMDSL.FSM;
import com.cburch.logisim.statemachine.fSMDSL.FSMElement;
import com.cburch.logisim.statemachine.fSMDSL.NotExpr;
import com.cburch.logisim.statemachine.fSMDSL.OrExpr;
import com.cburch.logisim.statemachine.fSMDSL.Port;
import com.cburch.logisim.statemachine.fSMDSL.PortRef;
import com.cburch.logisim.statemachine.fSMDSL.Range;
import com.cburch.logisim.statemachine.fSMDSL.State;
import com.cburch.logisim.statemachine.fSMDSL.Transition;
import com.google.common.base.Objects;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.xbase.lib.CollectionLiterals;
import org.eclipse.xtext.xbase.lib.Conversions;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;

@SuppressWarnings("all")
public class FSMValidation {
  private FSM fsm;
  
  private HashSet<State> targets = new HashSet<State>();
  
  private List<String> warnings = new ArrayList<String>();
  
  private List<String> errors = new ArrayList<String>();
  
  private BitWidthAnalyzer analyzer = new BitWidthAnalyzer();
  
  private static HashSet<String> keywords = new HashSet<String>(Collections.<String>unmodifiableList(CollectionLiterals.<String>newArrayList("CLK", "RST", "EN", "if", "then", "while", "for", "do", "end", "begin", "entity", "component")));
  
  private static final Pattern FQCN = Pattern.compile("(?:\\b[_a-zA-Z]|\\B\\$)[_$a-zA-Z0-9]*+");
  
  public FSMValidation(final FSM fsm) {
    this.fsm = fsm;
  }
  
  public List<String> getErrors() {
    return this.errors;
  }
  
  public List<String> getWarnings() {
    return this.warnings;
  }
  
  public Boolean _validate(final FSM e) {
    State _start = e.getStart();
    boolean _equals = Objects.equal(_start, null);
    if (_equals) {
      this.error("No initial state");
    }
    int _size = e.getStates().size();
    boolean _equals_1 = (_size == 0);
    if (_equals_1) {
      this.error("The FSM has no states !");
    }
    EList<Port> _out = e.getOut();
    boolean _equals_2 = Objects.equal(_out, Integer.valueOf(0));
    if (_equals_2) {
      this.warning("The FSM has no output pins !");
    }
    EList<Port> _in = e.getIn();
    boolean _equals_3 = Objects.equal(_in, Integer.valueOf(0));
    if (_equals_3) {
      this.warning("The FSM has no input pins !");
    }
    final HashMap<String, State> map = new HashMap<String, State>();
    EList<State> _states = e.getStates();
    for (final State s : _states) {
      boolean _containsKey = map.containsKey(s.getCode());
      if (_containsKey) {
        StringConcatenation _builder = new StringConcatenation();
        _builder.append("The FSM has two states (");
        String _name = s.getName();
        _builder.append(_name);
        _builder.append(", ");
        String _name_1 = map.get(s.getCode()).getName();
        _builder.append(_name_1);
        _builder.append(") with the same encoding");
        this.error(_builder.toString());
      } else {
        map.put(s.getCode(), s);
      }
    }
    this.checkNames();
    EList<State> _states_1 = e.getStates();
    for (final State s_1 : _states_1) {
      {
        this.validate(s_1);
        EList<Transition> _transition = s_1.getTransition();
        for (final Transition t : _transition) {
          this.targets.add(t.getDst());
        }
      }
    }
    EList<State> _states_2 = e.getStates();
    for (final State s_2 : _states_2) {
      if (((!Objects.equal(s_2, this.fsm.getStart())) && (!this.targets.contains(s_2)))) {
        String _pp = PrettyPrinter.pp(s_2);
        String _plus = ("State " + _pp);
        String _plus_1 = (_plus + " is not reachable from initial state ");
        String _pp_1 = PrettyPrinter.pp(e.getStart());
        String _plus_2 = (_plus_1 + _pp_1);
        this.warning(_plus_2);
      }
    }
    return null;
  }
  
  public static boolean isValidBinaryString(final String s, final int width) {
    return (s.matches("\"[0-1]+\"") && (s.length() == (width + 2)));
  }
  
  public static boolean isValidIdentifier(final String identifier) {
    return FSMValidation.FQCN.matcher(identifier).matches();
  }
  
  public static boolean isReservedKeyword(final String identifier) {
    return FSMValidation.keywords.contains(identifier);
  }
  
  public boolean validateIdentifier(final String identifier) {
    boolean _xifexpression = false;
    boolean _isValidIdentifier = FSMValidation.isValidIdentifier(identifier);
    boolean _not = (!_isValidIdentifier);
    if (_not) {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("Ilegal identifier : ");
      _builder.append(identifier);
      _xifexpression = this.error(_builder.toString());
    } else {
      boolean _xifexpression_1 = false;
      boolean _isReservedKeyword = FSMValidation.isReservedKeyword(identifier);
      if (_isReservedKeyword) {
        StringConcatenation _builder_1 = new StringConcatenation();
        _builder_1.append("Reserved keyword : ");
        _builder_1.append(identifier);
        _xifexpression_1 = this.error(_builder_1.toString());
      }
      _xifexpression = _xifexpression_1;
    }
    return _xifexpression;
  }
  
  public void checkNames() {
    final HashMap<String, FSMElement> nameMap = new HashMap<String, FSMElement>();
    EList<State> _states = this.fsm.getStates();
    for (final State s : _states) {
      {
        this.validateIdentifier(s.getName());
        boolean _containsKey = nameMap.containsKey(s.getName());
        if (_containsKey) {
          StringConcatenation _builder = new StringConcatenation();
          _builder.append("The FSM has two states with the same Label ");
          String _name = s.getName();
          _builder.append(_name);
          this.error(_builder.toString());
        } else {
          nameMap.put(s.getName(), s);
        }
      }
    }
    EList<Port> _in = this.fsm.getIn();
    for (final Port s_1 : _in) {
      {
        this.validateIdentifier(s_1.getName());
        boolean _containsKey = nameMap.containsKey(s_1.getName());
        if (_containsKey) {
          StringConcatenation _builder = new StringConcatenation();
          _builder.append("The FSM has two elements using a same identifier (");
          String _pp = PrettyPrinter.pp(s_1);
          _builder.append(_pp);
          _builder.append(", ");
          String _pp_1 = PrettyPrinter.pp(nameMap.get(s_1.getName()));
          _builder.append(_pp_1);
          _builder.append(") ");
          this.error(_builder.toString());
        } else {
          nameMap.put(s_1.getName(), s_1);
        }
      }
    }
    EList<Port> _out = this.fsm.getOut();
    for (final Port s_2 : _out) {
      {
        this.validateIdentifier(s_2.getName());
        boolean _containsKey = nameMap.containsKey(s_2.getName());
        if (_containsKey) {
          StringConcatenation _builder = new StringConcatenation();
          _builder.append("The FSM has two elements using a same identifier (");
          String _pp = PrettyPrinter.pp(s_2);
          _builder.append(_pp);
          _builder.append(", ");
          String _pp_1 = PrettyPrinter.pp(nameMap.get(s_2.getName()));
          _builder.append(_pp_1);
          _builder.append(") ");
          this.error(_builder.toString());
        } else {
          nameMap.put(s_2.getName(), s_2);
        }
      }
    }
  }
  
  public boolean warning(final String string) {
    return this.warnings.add(string);
  }
  
  public boolean error(final String string) {
    return this.errors.add(string);
  }
  
  public Boolean _validate(final FSMElement e) {
    return null;
  }
  
  public Boolean _validate(final CommandList cl) {
    EList<Command> _commands = cl.getCommands();
    for (final Command c : _commands) {
      {
        try {
          this.analyzer.computeBitwidth(c.getValue());
        } catch (final Throwable _t) {
          if (_t instanceof RuntimeException) {
            final RuntimeException e = (RuntimeException)_t;
            this.error(e.getMessage());
          } else {
            throw Exceptions.sneakyThrow(_t);
          }
        }
        this.validateExpr(c.getValue(), false);
        BoolExpr _value = c.getValue();
        final BDDOptimizer optimizer = new BDDOptimizer(_value);
        optimizer.simplify();
        boolean _isAlwaysFalse = optimizer.isAlwaysFalse();
        if (_isAlwaysFalse) {
          String _pp = PrettyPrinter.pp(c);
          String _plus = ("command " + _pp);
          String _plus_1 = (_plus + " is always evaluated to 0");
          this.warning(_plus_1);
        }
        if ((optimizer.isAlwaysTrue() && (!(c instanceof Constant)))) {
          String _pp_1 = PrettyPrinter.pp(c);
          String _plus_2 = ("command " + _pp_1);
          String _plus_3 = (_plus_2 + " is always evaluated to 1");
          this.warning(_plus_3);
        }
      }
    }
    return null;
  }
  
  public Boolean _validate(final Transition t) {
    boolean _xblockexpression = false;
    {
      final BoolExpr p = t.getPredicate();
      BoolExpr _predicate = t.getPredicate();
      boolean _equals = Objects.equal(_predicate, null);
      if (_equals) {
        throw new RuntimeException("null Predicate");
      }
      this.validateExpr(t.getPredicate(), true);
      boolean _xifexpression = false;
      BoolExpr _predicate_1 = t.getPredicate();
      boolean _not = (!(_predicate_1 instanceof DefaultPredicate));
      if (_not) {
        boolean _xtrycatchfinallyexpression = false;
        try {
          boolean _xblockexpression_1 = false;
          {
            final BDDOptimizer optimizer = new BDDOptimizer(p);
            optimizer.simplify();
            boolean _isAlwaysFalse = optimizer.isAlwaysFalse();
            if (_isAlwaysFalse) {
              String _pp = PrettyPrinter.pp(t);
              String _plus = ("Transition  " + _pp);
              String _plus_1 = (_plus + " can never be taken (evaluated to 0)");
              this.error(_plus_1);
            }
            boolean _xifexpression_1 = false;
            if ((optimizer.isAlwaysTrue() && (!(t.getPredicate() instanceof DefaultPredicate)))) {
              String _pp_1 = PrettyPrinter.pp(t);
              String _plus_2 = ("Transition " + _pp_1);
              String _plus_3 = (_plus_2 + " is always taken (evaluated to 1)");
              _xifexpression_1 = this.warning(_plus_3);
            }
            _xblockexpression_1 = _xifexpression_1;
          }
          _xtrycatchfinallyexpression = _xblockexpression_1;
        } catch (final Throwable _t) {
          if (_t instanceof Exception) {
            final Exception e = (Exception)_t;
            String _pp = PrettyPrinter.pp(t);
            String _plus = ("BDD analysis for " + _pp);
            String _plus_1 = (_plus + " failed : ");
            String _message = e.getMessage();
            String _plus_2 = (_plus_1 + _message);
            String _plus_3 = (_plus_2 + "\n");
            StackTraceElement[] _stackTrace = e.getStackTrace();
            String _plus_4 = (_plus_3 + _stackTrace);
            _xtrycatchfinallyexpression = this.error(_plus_4);
          } else {
            throw Exceptions.sneakyThrow(_t);
          }
        }
        _xifexpression = _xtrycatchfinallyexpression;
      }
      _xblockexpression = _xifexpression;
    }
    return Boolean.valueOf(_xblockexpression);
  }
  
  public Boolean _validate(final State e) {
    int i = 0;
    int j = 0;
    EObject _eContainer = e.eContainer();
    if ((_eContainer instanceof FSM)) {
      EObject _eContainer_1 = e.eContainer();
      final FSM fsm = ((FSM) _eContainer_1);
      int _length = e.getCode().length();
      int _width = fsm.getWidth();
      int _plus = (_width + 2);
      boolean _notEquals = (_length != _plus);
      if (_notEquals) {
        String _pp = PrettyPrinter.pp(e);
        String _plus_1 = ("State " + _pp);
        String _plus_2 = (_plus_1 + " code is not consistent with FSM configuration (");
        int _width_1 = fsm.getWidth();
        String _plus_3 = (_plus_2 + Integer.valueOf(_width_1));
        String _plus_4 = (_plus_3 + " bits expected");
        this.error(_plus_4);
      }
    }
    int _size = e.getTransition().size();
    boolean _equals = (_size == 0);
    if (_equals) {
      String _pp_1 = PrettyPrinter.pp(e);
      String _plus_5 = ("State " + _pp_1);
      String _plus_6 = (_plus_5 + " has no output transition");
      this.warning(_plus_6);
    }
    final Function1<Transition, Boolean> _function = (Transition t) -> {
      BoolExpr _predicate = t.getPredicate();
      return Boolean.valueOf((!(_predicate instanceof DefaultPredicate)));
    };
    final List<Transition> nonDefaultTransitions = IterableExtensions.<Transition>toList(IterableExtensions.<Transition>filter(e.getTransition(), _function));
    int _size_1 = e.getTransition().size();
    int _length_1 = ((Object[])Conversions.unwrapArray(nonDefaultTransitions, Object.class)).length;
    int _minus = (_size_1 - _length_1);
    boolean _greaterThan = (_minus > 1);
    if (_greaterThan) {
      String _pp_2 = PrettyPrinter.pp(e);
      String _plus_7 = ("State " + _pp_2);
      String _plus_8 = (_plus_7 + " has multiple default transitions");
      this.error(_plus_8);
    }
    for (final Transition a : nonDefaultTransitions) {
      {
        this.validate(a);
        j = 0;
        for (final Transition b : nonDefaultTransitions) {
          {
            if ((i < j)) {
              final BoolExpr pa = a.getPredicate();
              final BoolExpr pb = b.getPredicate();
              final AndExpr and = FSMCustomFactory.and(EcoreUtil.<BoolExpr>copy(pa), EcoreUtil.<BoolExpr>copy(pb));
              final BDDOptimizer optimizer = new BDDOptimizer(and);
              boolean _isAlwaysFalse = optimizer.isAlwaysFalse();
              boolean _not = (!_isAlwaysFalse);
              if (_not) {
                String _pp_3 = PrettyPrinter.pp(pa);
                String _plus_9 = ("Transitions predicates " + _pp_3);
                String _plus_10 = (_plus_9 + " and ");
                String _pp_4 = PrettyPrinter.pp(pb);
                String _plus_11 = (_plus_10 + _pp_4);
                String _plus_12 = (_plus_11 + " are not mutually exclusive");
                this.error(_plus_12);
              }
            }
            int _j = j;
            j = (_j + 1);
          }
        }
        int _i = i;
        i = (_i + 1);
      }
    }
    return null;
  }
  
  public Boolean _validateExpr(final BoolExpr b, final boolean predicate) {
    return null;
  }
  
  public Boolean _validateExpr(final OrExpr b, final boolean predicate) {
    final Consumer<BoolExpr> _function = (BoolExpr a) -> {
      this.validateExpr(a, predicate);
    };
    b.getArgs().forEach(_function);
    return null;
  }
  
  public Boolean _validateExpr(final CmpExpr b, final boolean predicate) {
    int _size = b.getArgs().size();
    boolean _notEquals = (_size != 2);
    if (_notEquals) {
      String _pp = PrettyPrinter.pp(b);
      String _plus = ("Inconsistent number of arguments for " + _pp);
      String _plus_1 = (_plus + " ");
      this.error(_plus_1);
    }
    final Consumer<BoolExpr> _function = (BoolExpr a) -> {
      this.validateExpr(a, predicate);
    };
    b.getArgs().forEach(_function);
    return null;
  }
  
  public Boolean _validateExpr(final AndExpr b, final boolean predicate) {
    final Consumer<BoolExpr> _function = (BoolExpr a) -> {
      this.validateExpr(a, predicate);
    };
    b.getArgs().forEach(_function);
    return null;
  }
  
  public Boolean _validateExpr(final NotExpr b, final boolean predicate) {
    final Consumer<BoolExpr> _function = (BoolExpr a) -> {
      this.validateExpr(a, predicate);
    };
    b.getArgs().forEach(_function);
    return null;
  }
  
  public Boolean _validateExpr(final Constant b, final boolean predicate) {
    return null;
  }
  
  public Boolean _validateExpr(final PortRef b, final boolean predicate) {
    boolean _xifexpression = false;
    Range _range = b.getRange();
    boolean _notEquals = (!Objects.equal(_range, null));
    if (_notEquals) {
      boolean _xblockexpression = false;
      {
        int _ub = b.getRange().getUb();
        int _width = b.getPort().getWidth();
        int _minus = (_width - 1);
        boolean _greaterThan = (_ub > _minus);
        if (_greaterThan) {
          int _ub_1 = b.getRange().getUb();
          String _plus = ("Inconsistent range [" + Integer.valueOf(_ub_1));
          String _plus_1 = (_plus + ":");
          int _lb = b.getRange().getLb();
          String _plus_2 = (_plus_1 + Integer.valueOf(_lb));
          String _plus_3 = (_plus_2 + "] for port ");
          String _name = b.getPort().getName();
          String _plus_4 = (_plus_3 + _name);
          String _plus_5 = (_plus_4 + "[");
          int _width_1 = b.getPort().getWidth();
          int _minus_1 = (_width_1 - 1);
          String _plus_6 = (_plus_5 + Integer.valueOf(_minus_1));
          String _plus_7 = (_plus_6 + ":0]");
          this.error(_plus_7);
        }
        int _lb_1 = b.getRange().getLb();
        int _width_2 = b.getPort().getWidth();
        int _minus_2 = (_width_2 - 1);
        boolean _greaterThan_1 = (_lb_1 > _minus_2);
        if (_greaterThan_1) {
          int _ub_2 = b.getRange().getUb();
          String _plus_8 = ("Inconsistent range [" + Integer.valueOf(_ub_2));
          String _plus_9 = (_plus_8 + ":");
          int _lb_2 = b.getRange().getLb();
          String _plus_10 = (_plus_9 + Integer.valueOf(_lb_2));
          String _plus_11 = (_plus_10 + "] for port ");
          String _name_1 = b.getPort().getName();
          String _plus_12 = (_plus_11 + _name_1);
          String _plus_13 = (_plus_12 + "[");
          int _width_3 = b.getPort().getWidth();
          int _minus_3 = (_width_3 - 1);
          String _plus_14 = (_plus_13 + Integer.valueOf(_minus_3));
          String _plus_15 = (_plus_14 + ":0]");
          this.error(_plus_15);
        }
        boolean _xifexpression_1 = false;
        int _lb_3 = b.getRange().getLb();
        int _lb_4 = b.getRange().getLb();
        boolean _greaterThan_2 = (_lb_3 > _lb_4);
        if (_greaterThan_2) {
          int _ub_3 = b.getRange().getUb();
          String _plus_16 = ("Inconsistent range [" + Integer.valueOf(_ub_3));
          String _plus_17 = (_plus_16 + ":");
          int _lb_5 = b.getRange().getLb();
          String _plus_18 = (_plus_17 + Integer.valueOf(_lb_5));
          String _plus_19 = (_plus_18 + "] ");
          _xifexpression_1 = this.error(_plus_19);
        }
        _xblockexpression = _xifexpression_1;
      }
      _xifexpression = _xblockexpression;
    }
    return Boolean.valueOf(_xifexpression);
  }
  
  public Boolean _validateExpr(final DefaultPredicate b, final boolean predicate) {
    boolean _xifexpression = false;
    if ((!predicate)) {
      _xifexpression = this.error("keyword \"default\" not allowed in command expressions, use \"0\" or \"1\" instead");
    }
    return Boolean.valueOf(_xifexpression);
  }
  
  public Boolean validate(final FSMElement cl) {
    if (cl instanceof CommandList) {
      return _validate((CommandList)cl);
    } else if (cl instanceof FSM) {
      return _validate((FSM)cl);
    } else if (cl instanceof State) {
      return _validate((State)cl);
    } else if (cl instanceof Transition) {
      return _validate((Transition)cl);
    } else if (cl != null) {
      return _validate(cl);
    } else {
      throw new IllegalArgumentException("Unhandled parameter types: " +
        Arrays.<Object>asList(cl).toString());
    }
  }
  
  public Boolean validateExpr(final BoolExpr b, final boolean predicate) {
    if (b instanceof AndExpr) {
      return _validateExpr((AndExpr)b, predicate);
    } else if (b instanceof CmpExpr) {
      return _validateExpr((CmpExpr)b, predicate);
    } else if (b instanceof Constant) {
      return _validateExpr((Constant)b, predicate);
    } else if (b instanceof DefaultPredicate) {
      return _validateExpr((DefaultPredicate)b, predicate);
    } else if (b instanceof NotExpr) {
      return _validateExpr((NotExpr)b, predicate);
    } else if (b instanceof OrExpr) {
      return _validateExpr((OrExpr)b, predicate);
    } else if (b instanceof PortRef) {
      return _validateExpr((PortRef)b, predicate);
    } else if (b != null) {
      return _validateExpr(b, predicate);
    } else {
      throw new IllegalArgumentException("Unhandled parameter types: " +
        Arrays.<Object>asList(b, predicate).toString());
    }
  }
}
