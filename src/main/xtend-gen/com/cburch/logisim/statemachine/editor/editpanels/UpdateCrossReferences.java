package com.cburch.logisim.statemachine.editor.editpanels;

import com.cburch.logisim.statemachine.fSMDSL.AndExpr;
import com.cburch.logisim.statemachine.fSMDSL.BoolExpr;
import com.cburch.logisim.statemachine.fSMDSL.CmpExpr;
import com.cburch.logisim.statemachine.fSMDSL.Command;
import com.cburch.logisim.statemachine.fSMDSL.ConcatExpr;
import com.cburch.logisim.statemachine.fSMDSL.ConstRef;
import com.cburch.logisim.statemachine.fSMDSL.Constant;
import com.cburch.logisim.statemachine.fSMDSL.ConstantDef;
import com.cburch.logisim.statemachine.fSMDSL.DefaultPredicate;
import com.cburch.logisim.statemachine.fSMDSL.FSM;
import com.cburch.logisim.statemachine.fSMDSL.NotExpr;
import com.cburch.logisim.statemachine.fSMDSL.OrExpr;
import com.cburch.logisim.statemachine.fSMDSL.OutputPort;
import com.cburch.logisim.statemachine.fSMDSL.Port;
import com.cburch.logisim.statemachine.fSMDSL.PortRef;
import com.cburch.logisim.statemachine.fSMDSL.Transition;
import com.google.common.base.Objects;
import java.util.Arrays;
import java.util.HashMap;
import java.util.function.Consumer;
import org.eclipse.emf.ecore.EObject;

@SuppressWarnings("all")
public class UpdateCrossReferences {
  private HashMap<String, Port> portMap = new HashMap<String, Port>();
  
  private HashMap<String, ConstantDef> constMap = new HashMap<String, ConstantDef>();
  
  public UpdateCrossReferences(final FSM fsm) {
    final Consumer<ConstantDef> _function = (ConstantDef cst) -> {
      this.constMap.put(cst.getName(), cst);
    };
    fsm.getConstants().forEach(_function);
    final Consumer<Port> _function_1 = (Port ip) -> {
      this.portMap.put(ip.getName(), ip);
    };
    fsm.getIn().forEach(_function_1);
    final Consumer<Port> _function_2 = (Port op) -> {
      this.portMap.put(op.getName(), op);
    };
    fsm.getOut().forEach(_function_2);
  }
  
  protected void _replaceRef(final Command c) {
    OutputPort _name = c.getName();
    boolean _notEquals = (!Objects.equal(_name, null));
    if (_notEquals) {
      Port _get = this.portMap.get(c.getName().getName());
      c.setName(((OutputPort) _get));
    }
    this.replaceRef(c.getValue());
  }
  
  protected void _replaceRef(final Transition t) {
    this.replaceRef(t.getPredicate());
  }
  
  protected void _replaceRef(final BoolExpr b) {
    String _simpleName = b.getClass().getSimpleName();
    String _plus = ("Support for class " + _simpleName);
    String _plus_1 = (_plus + " NYI");
    throw new UnsupportedOperationException(_plus_1);
  }
  
  protected void _replaceRef(final DefaultPredicate b) {
  }
  
  protected void _replaceRef(final Constant b) {
  }
  
  protected void _replaceRef(final OrExpr b) {
    final Consumer<BoolExpr> _function = (BoolExpr a) -> {
      this.replaceRef(a);
    };
    b.getArgs().forEach(_function);
  }
  
  protected void _replaceRef(final AndExpr b) {
    final Consumer<BoolExpr> _function = (BoolExpr a) -> {
      this.replaceRef(a);
    };
    b.getArgs().forEach(_function);
  }
  
  protected void _replaceRef(final CmpExpr b) {
    final Consumer<BoolExpr> _function = (BoolExpr a) -> {
      this.replaceRef(a);
    };
    b.getArgs().forEach(_function);
  }
  
  protected void _replaceRef(final ConcatExpr b) {
    final Consumer<BoolExpr> _function = (BoolExpr a) -> {
      this.replaceRef(a);
    };
    b.getArgs().forEach(_function);
  }
  
  protected void _replaceRef(final NotExpr b) {
    final Consumer<BoolExpr> _function = (BoolExpr a) -> {
      this.replaceRef(a);
    };
    b.getArgs().forEach(_function);
  }
  
  protected void _replaceRef(final PortRef b) {
    Port _port = b.getPort();
    boolean _notEquals = (!Objects.equal(_port, null));
    if (_notEquals) {
      b.setPort(this.portMap.get(b.getPort().getName()));
    } else {
    }
  }
  
  protected void _replaceRef(final ConstRef b) {
    if (((!Objects.equal(b.getConst(), null)) && this.constMap.containsKey(b.getConst().getName()))) {
      b.setConst(this.constMap.get(b.getConst().getName()));
    }
  }
  
  public void replaceRef(final EObject b) {
    if (b instanceof AndExpr) {
      _replaceRef((AndExpr)b);
      return;
    } else if (b instanceof CmpExpr) {
      _replaceRef((CmpExpr)b);
      return;
    } else if (b instanceof ConcatExpr) {
      _replaceRef((ConcatExpr)b);
      return;
    } else if (b instanceof ConstRef) {
      _replaceRef((ConstRef)b);
      return;
    } else if (b instanceof Constant) {
      _replaceRef((Constant)b);
      return;
    } else if (b instanceof DefaultPredicate) {
      _replaceRef((DefaultPredicate)b);
      return;
    } else if (b instanceof NotExpr) {
      _replaceRef((NotExpr)b);
      return;
    } else if (b instanceof OrExpr) {
      _replaceRef((OrExpr)b);
      return;
    } else if (b instanceof PortRef) {
      _replaceRef((PortRef)b);
      return;
    } else if (b instanceof Transition) {
      _replaceRef((Transition)b);
      return;
    } else if (b instanceof BoolExpr) {
      _replaceRef((BoolExpr)b);
      return;
    } else if (b instanceof Command) {
      _replaceRef((Command)b);
      return;
    } else {
      throw new IllegalArgumentException("Unhandled parameter types: " +
        Arrays.<Object>asList(b).toString());
    }
  }
}
