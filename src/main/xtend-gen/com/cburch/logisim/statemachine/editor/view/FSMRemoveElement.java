package com.cburch.logisim.statemachine.editor.view;

import com.cburch.logisim.statemachine.fSMDSL.AndExpr;
import com.cburch.logisim.statemachine.fSMDSL.BoolExpr;
import com.cburch.logisim.statemachine.fSMDSL.Command;
import com.cburch.logisim.statemachine.fSMDSL.CommandList;
import com.cburch.logisim.statemachine.fSMDSL.Constant;
import com.cburch.logisim.statemachine.fSMDSL.FSM;
import com.cburch.logisim.statemachine.fSMDSL.FSMDSLFactory;
import com.cburch.logisim.statemachine.fSMDSL.InputPort;
import com.cburch.logisim.statemachine.fSMDSL.NotExpr;
import com.cburch.logisim.statemachine.fSMDSL.OrExpr;
import com.cburch.logisim.statemachine.fSMDSL.OutputPort;
import com.cburch.logisim.statemachine.fSMDSL.PortRef;
import com.cburch.logisim.statemachine.fSMDSL.State;
import com.cburch.logisim.statemachine.fSMDSL.Transition;
import com.google.common.base.Objects;
import com.google.common.collect.Iterators;
import java.util.Arrays;
import java.util.List;
import java.util.function.UnaryOperator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IteratorExtensions;

@SuppressWarnings("all")
public class FSMRemoveElement {
  private FSM fsm;
  
  public FSMRemoveElement(final FSM fsm) {
    this.fsm = fsm;
  }
  
  protected Boolean _remove(final EObject e) {
    return null;
  }
  
  protected Boolean _remove(final State s) {
    this.fsm.getStates().remove(s);
    final Function1<Transition, Boolean> _function = (Transition t) -> {
      State _dst = t.getDst();
      return Boolean.valueOf(Objects.equal(_dst, s));
    };
    final List<Transition> deadTransitions = IteratorExtensions.<Transition>toList(IteratorExtensions.<Transition>filter(Iterators.<Transition>filter(this.fsm.eAllContents(), Transition.class), _function));
    for (final Transition t : deadTransitions) {
      this.remove(t);
    }
    return null;
  }
  
  protected Boolean _remove(final Transition t) {
    EObject _eContainer = t.eContainer();
    ((State) _eContainer).getTransition().remove(t);
    t.setSrc(null);
    t.setDst(null);
    return null;
  }
  
  protected void _replaceByZero(final PortRef pr) {
    final Constant cst = FSMDSLFactory.eINSTANCE.createConstant();
    cst.setValue("0");
    int _width = pr.getPort().getWidth();
    boolean _notEquals = (_width != 1);
    if (_notEquals) {
      throw new UnsupportedOperationException("Support for port width>1 not yet available");
    }
    EObject _eContainer = pr.eContainer();
    final BoolExpr roor = ((BoolExpr) _eContainer);
    boolean _matched = false;
    if (roor instanceof AndExpr) {
      _matched=true;
      final UnaryOperator<BoolExpr> _function = (BoolExpr x) -> {
        BoolExpr _xifexpression = null;
        boolean _equals = Objects.equal(x, pr);
        if (_equals) {
          _xifexpression = cst;
        } else {
          _xifexpression = x;
        }
        return _xifexpression;
      };
      ((AndExpr)roor).getArgs().replaceAll(_function);
    }
    if (!_matched) {
      if (roor instanceof OrExpr) {
        _matched=true;
        final UnaryOperator<BoolExpr> _function = (BoolExpr x) -> {
          BoolExpr _xifexpression = null;
          boolean _equals = Objects.equal(x, pr);
          if (_equals) {
            _xifexpression = cst;
          } else {
            _xifexpression = x;
          }
          return _xifexpression;
        };
        ((OrExpr)roor).getArgs().replaceAll(_function);
      }
    }
    if (!_matched) {
      if (roor instanceof NotExpr) {
        _matched=true;
        final UnaryOperator<BoolExpr> _function = (BoolExpr x) -> {
          BoolExpr _xifexpression = null;
          boolean _equals = Objects.equal(x, pr);
          if (_equals) {
            _xifexpression = cst;
          } else {
            _xifexpression = x;
          }
          return _xifexpression;
        };
        ((NotExpr)roor).getArgs().replaceAll(_function);
      }
    }
  }
  
  protected Boolean _remove(final InputPort e) {
    this.fsm.getIn().remove(e);
    final Function1<PortRef, Boolean> _function = (PortRef c) -> {
      String _name = c.getPort().getName();
      return Boolean.valueOf(Objects.equal(_name, e));
    };
    final List<PortRef> deadRefs = IteratorExtensions.<PortRef>toList(IteratorExtensions.<PortRef>filter(Iterators.<PortRef>filter(this.fsm.eAllContents(), PortRef.class), _function));
    for (final PortRef r : deadRefs) {
      this.replaceByZero(r);
    }
    return null;
  }
  
  protected Boolean _remove(final Command c) {
    EObject _eContainer = c.eContainer();
    return Boolean.valueOf(((CommandList) _eContainer).getCommands().remove(c));
  }
  
  protected Boolean _remove(final OutputPort op) {
    this.fsm.getOut().remove(op);
    final Function1<Command, Boolean> _function = (Command c) -> {
      OutputPort _name = c.getName();
      return Boolean.valueOf(Objects.equal(_name, op));
    };
    final List<Command> deadCommands = IteratorExtensions.<Command>toList(IteratorExtensions.<Command>filter(Iterators.<Command>filter(this.fsm.eAllContents(), Command.class), _function));
    for (final Command t : deadCommands) {
      this.remove(t);
    }
    return null;
  }
  
  public Boolean remove(final EObject e) {
    if (e instanceof InputPort) {
      return _remove((InputPort)e);
    } else if (e instanceof OutputPort) {
      return _remove((OutputPort)e);
    } else if (e instanceof State) {
      return _remove((State)e);
    } else if (e instanceof Transition) {
      return _remove((Transition)e);
    } else if (e instanceof Command) {
      return _remove((Command)e);
    } else if (e != null) {
      return _remove(e);
    } else {
      throw new IllegalArgumentException("Unhandled parameter types: " +
        Arrays.<Object>asList(e).toString());
    }
  }
  
  public void replaceByZero(final PortRef pr) {
    _replaceByZero(pr);
    return;
  }
}
