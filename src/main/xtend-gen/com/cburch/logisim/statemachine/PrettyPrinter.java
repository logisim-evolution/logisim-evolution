package com.cburch.logisim.statemachine;

import com.cburch.logisim.statemachine.fSMDSL.AndExpr;
import com.cburch.logisim.statemachine.fSMDSL.BoolExpr;
import com.cburch.logisim.statemachine.fSMDSL.CmpExpr;
import com.cburch.logisim.statemachine.fSMDSL.Command;
import com.cburch.logisim.statemachine.fSMDSL.CommandList;
import com.cburch.logisim.statemachine.fSMDSL.ConcatExpr;
import com.cburch.logisim.statemachine.fSMDSL.ConstRef;
import com.cburch.logisim.statemachine.fSMDSL.Constant;
import com.cburch.logisim.statemachine.fSMDSL.ConstantDef;
import com.cburch.logisim.statemachine.fSMDSL.DefaultPredicate;
import com.cburch.logisim.statemachine.fSMDSL.FSMElement;
import com.cburch.logisim.statemachine.fSMDSL.NotExpr;
import com.cburch.logisim.statemachine.fSMDSL.OrExpr;
import com.cburch.logisim.statemachine.fSMDSL.PortRef;
import com.cburch.logisim.statemachine.fSMDSL.Range;
import com.cburch.logisim.statemachine.fSMDSL.State;
import com.cburch.logisim.statemachine.fSMDSL.Transition;
import com.google.common.base.Objects;
import java.util.Arrays;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.Functions.Function2;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.eclipse.xtext.xbase.lib.ListExtensions;

@SuppressWarnings("all")
public class PrettyPrinter {
  protected static String _pp(final FSMElement b) {
    return "";
  }
  
  protected static String _pp(final State b) {
    String _name = b.getName();
    String _plus = (_name + "[");
    String _code = b.getCode();
    String _plus_1 = (_plus + _code);
    return (_plus_1 + "]");
  }
  
  protected static String _pp(final Transition b) {
    String _xifexpression = null;
    State _src = b.getSrc();
    boolean _notEquals = (!Objects.equal(_src, null));
    if (_notEquals) {
      String _pp = PrettyPrinter.pp(b.getSrc());
      String _plus = (_pp + "->");
      String _pp_1 = PrettyPrinter.pp(b.getDst());
      String _plus_1 = (_plus + _pp_1);
      String _plus_2 = (_plus_1 + " when ");
      String _pp_2 = PrettyPrinter.pp(b.getPredicate());
      _xifexpression = (_plus_2 + _pp_2);
    } else {
      String _pp_3 = PrettyPrinter.pp(b.getDst());
      String _plus_3 = ("goto" + _pp_3);
      String _plus_4 = (_plus_3 + " when ");
      String _pp_4 = PrettyPrinter.pp(b.getPredicate());
      _xifexpression = (_plus_4 + _pp_4);
    }
    return _xifexpression;
  }
  
  protected static String _pp(final CommandList b) {
    final Function1<Command, String> _function = (Command c) -> {
      return PrettyPrinter.pp(c);
    };
    final Function2<String, String, String> _function_1 = (String p1, String p2) -> {
      return ((p1 + ";") + p1);
    };
    return IterableExtensions.<String>reduce(ListExtensions.<Command, String>map(b.getCommands(), _function), _function_1);
  }
  
  protected static String _pp(final BoolExpr b) {
    return "";
  }
  
  protected static String _pp(final Command c) {
    String _name = c.getName().getName();
    String _plus = (_name + "=");
    String _pp = PrettyPrinter.pp(c.getValue());
    return (_plus + _pp);
  }
  
  protected static String _pp(final OrExpr b) {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("(");
    {
      EList<BoolExpr> _args = b.getArgs();
      boolean _hasElements = false;
      for(final BoolExpr i : _args) {
        if (!_hasElements) {
          _hasElements = true;
        } else {
          _builder.appendImmediate("+", "");
        }
        Object _pp = PrettyPrinter.pp(i);
        _builder.append(_pp);
      }
    }
    _builder.append(")");
    return _builder.toString();
  }
  
  protected static String _pp(final ConcatExpr b) {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("{");
    {
      EList<BoolExpr> _args = b.getArgs();
      boolean _hasElements = false;
      for(final BoolExpr i : _args) {
        if (!_hasElements) {
          _hasElements = true;
        } else {
          _builder.appendImmediate(",", "");
        }
        Object _pp = PrettyPrinter.pp(i);
        _builder.append(_pp);
      }
    }
    _builder.append("}");
    return _builder.toString();
  }
  
  protected static String _pp(final AndExpr b) {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("(");
    {
      EList<BoolExpr> _args = b.getArgs();
      boolean _hasElements = false;
      for(final BoolExpr i : _args) {
        if (!_hasElements) {
          _hasElements = true;
        } else {
          _builder.appendImmediate(".", "");
        }
        Object _pp = PrettyPrinter.pp(i);
        _builder.append(_pp);
      }
    }
    _builder.append(")");
    return _builder.toString();
  }
  
  protected static String _pp(final CmpExpr b) {
    String _switchResult = null;
    String _op = b.getOp();
    if (_op != null) {
      switch (_op) {
        case "==":
          Object _pp = PrettyPrinter.pp(b.getArgs().get(0));
          String _plus = ("(" + _pp);
          String _plus_1 = (_plus + "==");
          Object _pp_1 = PrettyPrinter.pp(b.getArgs().get(1));
          String _plus_2 = (_plus_1 + _pp_1);
          _switchResult = (_plus_2 + ")");
          break;
        case "/=":
          Object _pp_2 = PrettyPrinter.pp(b.getArgs().get(0));
          String _plus_3 = ("(" + _pp_2);
          String _plus_4 = (_plus_3 + "/=");
          Object _pp_3 = PrettyPrinter.pp(b.getArgs().get(1));
          String _plus_5 = (_plus_4 + _pp_3);
          _switchResult = (_plus_5 + ")");
          break;
        default:
          _switchResult = "????";
          break;
      }
    } else {
      _switchResult = "????";
    }
    return _switchResult;
  }
  
  protected static String _pp(final NotExpr b) {
    Object _pp = PrettyPrinter.pp(b.getArgs().get(0));
    String _plus = ("(/" + _pp);
    return (_plus + ")");
  }
  
  protected static String _pp(final PortRef b) {
    String _xifexpression = null;
    Range _range = b.getRange();
    boolean _notEquals = (!Objects.equal(_range, null));
    if (_notEquals) {
      String _xifexpression_1 = null;
      int _ub = b.getRange().getUb();
      boolean _notEquals_1 = (_ub != (-1));
      if (_notEquals_1) {
        String _name = b.getPort().getName();
        String _plus = (_name + "[");
        int _ub_1 = b.getRange().getUb();
        String _plus_1 = (_plus + Integer.valueOf(_ub_1));
        String _plus_2 = (_plus_1 + ":");
        int _lb = b.getRange().getLb();
        String _plus_3 = (_plus_2 + Integer.valueOf(_lb));
        _xifexpression_1 = (_plus_3 + "]");
      } else {
        String _name_1 = b.getPort().getName();
        String _plus_4 = (_name_1 + "[");
        int _lb_1 = b.getRange().getLb();
        String _plus_5 = (_plus_4 + Integer.valueOf(_lb_1));
        _xifexpression_1 = (_plus_5 + "]");
      }
      _xifexpression = _xifexpression_1;
    } else {
      _xifexpression = b.getPort().getName();
    }
    return _xifexpression;
  }
  
  protected static String _pp(final ConstRef b) {
    String _name = b.getConst().getName();
    return ("#" + _name);
  }
  
  protected static String _pp(final DefaultPredicate b) {
    return "default";
  }
  
  protected static String _pp(final Constant b) {
    return b.getValue();
  }
  
  protected static String _pp(final ConstantDef b) {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("define ");
    String _name = b.getName();
    _builder.append(_name);
    _builder.append("=");
    Object _pp = PrettyPrinter.pp(b.getValue());
    _builder.append(_pp);
    return _builder.toString();
  }
  
  public static String pp(final EObject b) {
    if (b instanceof AndExpr) {
      return _pp((AndExpr)b);
    } else if (b instanceof CmpExpr) {
      return _pp((CmpExpr)b);
    } else if (b instanceof CommandList) {
      return _pp((CommandList)b);
    } else if (b instanceof ConcatExpr) {
      return _pp((ConcatExpr)b);
    } else if (b instanceof ConstRef) {
      return _pp((ConstRef)b);
    } else if (b instanceof Constant) {
      return _pp((Constant)b);
    } else if (b instanceof DefaultPredicate) {
      return _pp((DefaultPredicate)b);
    } else if (b instanceof NotExpr) {
      return _pp((NotExpr)b);
    } else if (b instanceof OrExpr) {
      return _pp((OrExpr)b);
    } else if (b instanceof PortRef) {
      return _pp((PortRef)b);
    } else if (b instanceof State) {
      return _pp((State)b);
    } else if (b instanceof Transition) {
      return _pp((Transition)b);
    } else if (b instanceof BoolExpr) {
      return _pp((BoolExpr)b);
    } else if (b instanceof Command) {
      return _pp((Command)b);
    } else if (b instanceof ConstantDef) {
      return _pp((ConstantDef)b);
    } else if (b instanceof FSMElement) {
      return _pp((FSMElement)b);
    } else {
      throw new IllegalArgumentException("Unhandled parameter types: " +
        Arrays.<Object>asList(b).toString());
    }
  }
}
